<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <b>月度值班备班表</b>(Excel 导入与预览校验;同月重传即整月替换;只适配科室现有表格式)
      </template>

      <!-- 上传区 -->
      <el-form inline @submit.prevent>
        <el-form-item label="目标月">
          <el-date-picker v-model="month" type="month" value-format="YYYY-MM" :clearable="false" />
        </el-form-item>
        <el-form-item>
          <input ref="fileInput" type="file" accept=".xlsx,.xls" hidden @change="onFile" />
          <el-button type="primary" :loading="parsing" @click="fileInput?.click()">
            选择文件并解析
          </el-button>
          <span v-if="fileName" class="file">{{ fileName }}</span>
          <el-button v-if="parsed" link type="danger" class="re-parse" @click="resetPreview">
            清除预览
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 预览 -->
      <template v-if="parsed">
        <el-alert
          :type="remainingBlocks > 0 ? 'error' : 'success'"
          :closable="false"
          class="blk"
          :title="`文件月:${parsed.fileMonth || '未识别'} | 解析行数:${parsed.rows.length} | 剩余阻断:${remainingBlocks} | 警告:${warnCount}`"
        />

        <!-- 问题清单 -->
        <el-table v-if="parsed.issues.length" :data="parsed.issues" size="small" max-height="220" class="blk">
          <el-table-column label="级别" width="80">
            <template #default="{ row }">
              <el-tag :type="row.level === 'BLOCK' ? 'danger' : 'warning'" size="small">
                {{ row.level === 'BLOCK' ? '阻断' : '警告' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="location" label="位置" width="130" />
          <el-table-column prop="message" label="问题" min-width="300" show-overflow-tooltip />
        </el-table>

        <!-- 行集合(可修正项白名单:人名候选、日期对齐) -->
        <el-table :data="parsed.rows" size="small" max-height="420" stripe>
          <el-table-column label="位置" width="100">
            <template #default="{ row }">{{ row.sheet }}:{{ row.excelRow }}</template>
          </el-table-column>
          <el-table-column label="岗位" width="90">
            <template #default="{ row }">{{ positionLabel(row) }}</template>
          </el-table-column>
          <el-table-column label="日期" width="120">
            <template #default="{ row }">
              <span v-if="row.type === 'SHORT_RELIEF'">-</span>
              <el-input-number
                v-else
                v-model="row.day"
                :min="1"
                :max="31"
                size="small"
                controls-position="right"
                style="width: 96px"
                @change="row.dateEdited = true"
              />
            </template>
          </el-table-column>
          <el-table-column prop="seq" label="序" width="60" />
          <el-table-column prop="cellText" label="原文" width="120" show-overflow-tooltip />
          <el-table-column label="人员" min-width="190">
            <template #default="{ row }">
              <el-select
                v-if="row.match.needsChoice"
                v-model="row.match.staffId"
                size="small"
                placeholder="同名,请选择"
              >
                <el-option
                  v-for="c in row.match.candidates"
                  :key="c.id"
                  :value="c.id"
                  :label="`${c.name}(#${c.empNo})`"
                />
              </el-select>
              <span v-else-if="row.match.staffId" class="ok">{{ chosenName(row) }}</span>
              <span v-else class="bad">
                {{ row.match.inactiveHit ? '停用人员' : '未匹配' }}(回 Excel 修改后重传)
              </span>
            </template>
          </el-table-column>
        </el-table>

        <div class="actions">
          <el-button type="warning" :loading="loadingMonth" @click="loadMonth">
            查看当月已入库
          </el-button>
          <el-tooltip
            :disabled="remainingBlocks === 0"
            content="存在阻断项:同名需选择、未匹配/岗位无法识别需回 Excel 修改后重传"
          >
            <span>
              <el-button
                type="success"
                :disabled="remainingBlocks > 0"
                :loading="importing"
                @click="doImport"
              >
                确认入库{{ remainingBlocks > 0 ? `(${remainingBlocks} 项阻断未清)` : '' }}
              </el-button>
            </span>
          </el-tooltip>
        </div>
      </template>
    </el-card>

    <!-- 已入库视图 -->
    <el-card v-if="monthData" shadow="never" class="blk">
      <template #header>
        <b>{{ monthData.month }} 已入库</b>({{ monthData.byDate.length }} 条按日 + {{ monthData.pool.length }} 条顺序池,状态 {{ monthData.status }})
      </template>
      <el-tabs>
        <el-tab-pane :label="`按日(${monthData.byDate.length})`">
          <el-table :data="monthData.byDate" size="small" max-height="360" stripe>
            <el-table-column prop="date" label="日期" width="110" />
            <el-table-column label="岗位" width="100">
              <template #default="{ row }">{{ POSITION_LABELS[row.positionType as RosterPositionType] || row.positionType }}</template>
            </el-table-column>
            <el-table-column prop="staffName" label="人员" min-width="120" />
          </el-table>
        </el-tab-pane>
        <el-tab-pane :label="`顺序池(${monthData.pool.length})`">
          <el-table :data="monthData.pool" size="small" max-height="360" stripe>
            <el-table-column prop="seq" label="顺序" width="80" />
            <el-table-column prop="staffName" label="人员" min-width="120" />
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { extractIssues, getRosterMonth, importRoster, parseRoster } from '@/api/roster'
import type { MonthResp, ParseResp, RosterPositionType, RosterRow } from '@/types/roster'
import { POSITION_LABELS } from '@/types/roster'
import { useAuthStore } from '@/stores/auth'

type PreviewRow = RosterRow & { dateEdited?: boolean }
type PreviewResp = Omit<ParseResp, 'rows'> & { rows: PreviewRow[] }

const auth = useAuthStore()
const month = ref(new Date().toISOString().slice(0, 7))
const fileInput = ref<HTMLInputElement>()
const fileName = ref('')
const parsing = ref(false)
const importing = ref(false)
const loadingMonth = ref(false)
const parsed = ref<PreviewResp | null>(null)
const monthData = ref<MonthResp | null>(null)

const warnCount = computed(() => parsed.value?.issues.filter((i) => i.level === 'WARN').length ?? 0)

/** 剩余阻断 = 文件级硬阻断(岗位无法识别/跨行合并,只能回Excel) + 行级未解决(待选/未匹配/日期未修正) */
const remainingBlocks = computed(() => {
  if (!parsed.value) return 0
  const hard = parsed.value.issues.filter(
    (i) => i.level === 'BLOCK' && (i.fixType === 'NONE' || i.fixType === 'POSITION')
  ).length
  const rowBlocks = parsed.value.rows.filter((r) => {
    if (r.match.needsChoice || !r.match.staffId) return true
    return r.type !== 'SHORT_RELIEF' && !r.dateEdited && (r.day === null || r.day < 1 || r.day > 31)
  }).length
  return hard + rowBlocks
})

const positionLabel = (row: PreviewRow) =>
  row.type ? POSITION_LABELS[row.type as RosterPositionType] || row.type : row.label

const chosenName = (row: PreviewRow) =>
  row.match.candidates.find((c) => c.id === row.match.staffId)?.name ?? `#${row.match.staffId}`

const onFile = async (e: Event) => {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  if (!auth.isAdmin) {
    ElMessage.warning('仅总值班可导入月表')
    return
  }
  parsing.value = true
  monthData.value = null
  try {
    const resp = await parseRoster(month.value, file)
    const preview: PreviewResp = { ...resp, rows: resp.rows }
    parsed.value = preview
    fileName.value = file.name
    ElMessage.success(`解析完成:${preview.rows.length} 行,${remainingBlocks.value} 项阻断`)
  } catch {
    parsed.value = null
    fileName.value = ''
  } finally {
    parsing.value = false
    input.value = ''
  }
}

const resetPreview = () => {
  parsed.value = null
  fileName.value = ''
}

const doImport = async () => {
  if (!parsed.value) return
  await ElMessageBox.confirm(
    `确认导入 ${month.value} 月表(${parsed.value.rows.length} 行)?同月已有数据将被整月替换。`,
    '导入确认',
    { type: 'warning' }
  )
  importing.value = true
  try {
    const summary = await importRoster(month.value, parsed.value.rows)
    ElMessage.success(`入库成功:月表 #${summary.rosterId},共 ${summary.itemCount} 条明细`)
    await loadMonth()
  } catch (error) {
    // 服务端复核拒绝:更新问题清单
    const issues = extractIssues(error)
    if (issues && parsed.value) {
      parsed.value = { ...parsed.value, issues, hasBlock: true }
      ElMessage.warning('服务端复核存在阻断项,已更新问题清单')
    }
  } finally {
    importing.value = false
  }
}

const loadMonth = async () => {
  loadingMonth.value = true
  try {
    monthData.value = await getRosterMonth(month.value)
  } catch {
    monthData.value = null
  } finally {
    loadingMonth.value = false
  }
}
</script>

<style scoped>
.page {
  max-width: 1180px;
}
.file {
  margin-left: 10px;
  color: #909399;
  font-size: 12px;
}
.re-parse {
  margin-left: 8px;
}
.blk {
  margin-bottom: 12px;
}
.actions {
  margin-top: 12px;
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}
.ok {
  color: #67c23a;
}
.bad {
  color: #f56c6c;
  font-size: 12px;
}
</style>
