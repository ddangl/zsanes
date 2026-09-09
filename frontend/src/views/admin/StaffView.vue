<template>
  <div class="page">
    <el-card shadow="never">
      <template #header><b>人员档案</b>(418 人;工号为唯一键,导入按工号 upsert)</template>

      <el-form inline :model="query" @submit.prevent>
        <el-form-item label="关键字">
          <el-input v-model="query.keyword" placeholder="姓名/工号" clearable style="width: 160px" @keyup.enter="load(1)" />
        </el-form-item>
        <el-form-item label="职称">
          <el-select v-model="query.jobRole" placeholder="全部" clearable style="width: 140px" @change="load(1)">
            <el-option v-for="r in JOB_ROLES" :key="r" :label="r" :value="r" />
          </el-select>
        </el-form-item>
        <el-form-item label="在职">
          <el-select v-model="query.active" placeholder="全部" clearable style="width: 100px" @change="load(1)">
            <el-option label="在职" :value="true" />
            <el-option label="停用" :value="false" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load(1)">查询</el-button>
          <el-button type="success" @click="openDialog()">新增</el-button>
          <el-button type="warning" @click="importVisible = true">Excel 导入</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="empNo" label="工号" width="90" />
        <el-table-column prop="name" label="姓名" width="110" />
        <el-table-column prop="jobRole" label="职称" width="90" />
        <el-table-column prop="title" label="职位" width="80" />
        <el-table-column label="亚专科" min-width="150">
          <template #default="{ row }">
            {{ [row.specialty1Name, row.specialty2Name].filter(Boolean).join(' / ') || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="mentorName" label="带教上级" width="100" />
        <el-table-column prop="grade" label="年级" width="70" />
        <el-table-column label="周六" width="70">
          <template #default="{ row }">
            <span v-if="row.saturdayWork === null || row.saturdayWork === undefined">-</span>
            <el-tag v-else :type="row.saturdayWork ? 'success' : 'danger'" size="small">
              {{ row.saturdayWork ? '上' : '休' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="note" label="特殊说明" min-width="140" show-overflow-tooltip />
        <el-table-column label="状态" width="70">
          <template #default="{ row }">
            <el-tag :type="row.active ? 'success' : 'info'" size="small">{{ row.active ? '在职' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
            <el-button link type="danger" :disabled="!row.active" @click="onDisable(row)">停用</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pager"
        layout="total, prev, pager, next, sizes"
        :total="total"
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        @current-change="load()"
        @size-change="load(1)"
      />
    </el-card>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="dialog.visible" :title="dialog.form.id ? '编辑人员' : '新增人员'" width="640px" top="6vh">
      <el-form :model="dialog.form" label-width="90px">
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="工号"><el-input v-model="dialog.form.empNo" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="姓名" required><el-input v-model="dialog.form.name" /></el-form-item></el-col>
          <el-col :span="12">
            <el-form-item label="职称" required>
              <el-select v-model="dialog.form.jobRole" style="width: 100%">
                <el-option v-for="r in JOB_ROLES" :key="r" :label="r" :value="r" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="职位">
              <el-select v-model="dialog.form.title" clearable style="width: 100%">
                <el-option label="主任" value="主任" />
                <el-option label="副主任" value="副主任" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="第一亚专科">
              <el-select v-model="dialog.form.specialty1Id" clearable filterable style="width: 100%">
                <el-option v-for="s in specialties" :key="s.id" :label="s.name" :value="s.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="第二亚专科">
              <el-select v-model="dialog.form.specialty2Id" clearable filterable style="width: 100%">
                <el-option v-for="s in specialties" :key="s.id" :label="s.name" :value="s.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="带教上级">
              <el-select v-model="dialog.form.mentorId" clearable filterable style="width: 100%">
                <el-option v-for="s in staffLite" :key="s.id"
                  :label="`${s.name}(${s.jobRole}${s.empNo ? '#' + s.empNo : ''})`" :value="s.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12"><el-form-item label="规培年级"><el-input v-model="dialog.form.grade" placeholder="1/2/3" /></el-form-item></el-col>
          <el-col :span="12">
            <el-form-item label="周六上班">
              <el-select v-model="dialog.form.saturdayWork" clearable placeholder="未标注" style="width: 100%">
                <el-option label="是" :value="true" />
                <el-option label="否" :value="false" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12"><el-form-item label="电话"><el-input v-model="dialog.form.phone" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="特殊说明"><el-input v-model="dialog.form.note" type="textarea" :rows="2" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="周期缺席"><el-input v-model="dialog.form.partTimeRule" placeholder="如:每周四金山(含周/半天说明会自动识别)" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="dialog.saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- Excel 导入弹窗 -->
    <el-dialog v-model="importVisible" title="Excel 批量导入(13 列,工号 upsert)" width="620px">
      <el-alert type="info" :closable="false" class="tip"
        title="同名不同工号会提示但不阻断;带教学生工号在导入后自动转为学生行带教关系;亚专科不存在时自动新建。" />
      <div class="import-actions">
        <el-button @click="onDownloadTemplate">下载导入模板</el-button>
        <input ref="fileInput" type="file" accept=".xlsx,.xls" hidden @change="onFileChange" />
        <el-button type="primary" :loading="importing" @click="fileInput?.click()">选择文件并导入</el-button>
        <span v-if="importFile" class="file">{{ importFile.name }}</span>
      </div>
      <div v-if="importResult" class="result">
        <el-descriptions :column="4" border size="small">
          <el-descriptions-item label="总行数">{{ importResult.totalRows }}</el-descriptions-item>
          <el-descriptions-item label="新增">{{ importResult.inserted }}</el-descriptions-item>
          <el-descriptions-item label="更新">{{ importResult.updated }}</el-descriptions-item>
          <el-descriptions-item label="跳过">{{ importResult.skipped }}</el-descriptions-item>
        </el-descriptions>
        <el-alert v-for="(w, i) in importResult.warnings" :key="i" type="warning" :closable="false" class="warn" :title="w" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createStaff, disableStaff, downloadStaffTemplate, importStaff, liteStaff, pageStaff, updateStaff } from '@/api/staff'
import type { ImportResult, Staff, StaffLite } from '@/types/staff'
import { JOB_ROLES } from '@/types/staff'
import type { Specialty } from '@/types/specialty'
import { listSpecialties } from '@/api/specialty'

const loading = ref(false)
const rows = ref<Staff[]>([])
const total = ref(0)
const query = reactive({ keyword: '', jobRole: '', active: undefined as boolean | undefined, pageNum: 1, pageSize: 20 })

const specialties = ref<Specialty[]>([])
const staffLite = ref<StaffLite[]>([])

const dialog = reactive({
  visible: false,
  saving: false,
  form: {} as Partial<Staff>
})

const importVisible = ref(false)
const importing = ref(false)
const importFile = ref<File | null>(null)
const importResult = ref<ImportResult | null>(null)
const fileInput = ref<HTMLInputElement>()

const load = async (page?: number) => {
  if (page) query.pageNum = page
  loading.value = true
  try {
    const result = await pageStaff(query)
    rows.value = result.list
    total.value = result.total
  } finally {
    loading.value = false
  }
}

const openDialog = (row?: Staff) => {
  dialog.form = row ? { ...row } : { jobRole: '主麻', active: true }
  dialog.visible = true
}

const save = async () => {
  if (!dialog.form.name || !dialog.form.jobRole) {
    ElMessage.warning('姓名与职称必填')
    return
  }
  dialog.saving = true
  try {
    if (dialog.form.id) {
      await updateStaff(dialog.form.id, dialog.form)
    } else {
      await createStaff(dialog.form)
    }
    ElMessage.success('保存成功')
    dialog.visible = false
    load()
  } catch {
    // 拦截器已报错
  } finally {
    dialog.saving = false
  }
}

const onDisable = async (row: Staff) => {
  await ElMessageBox.confirm(`确定停用 ${row.name}(${row.jobRole})?`, '提示', { type: 'warning' })
  await disableStaff(row.id)
  ElMessage.success('已停用')
  load()
}

const onDownloadTemplate = async () => {
  await downloadStaffTemplate()
}

const onFileChange = async (e: Event) => {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  importFile.value = file
  importing.value = true
  importResult.value = null
  try {
    importResult.value = await importStaff(file)
    ElMessage.success('导入完成')
    load(1)
  } catch {
    // 拦截器已报错
  } finally {
    importing.value = false
    input.value = ''
  }
}

onMounted(async () => {
  load(1)
  specialties.value = await listSpecialties(false)
  staffLite.value = await liteStaff(true)
})
</script>

<style scoped>
.page {
  max-width: 1280px;
}
.pager {
  margin-top: 12px;
  justify-content: flex-end;
}
.tip {
  margin-bottom: 12px;
}
.import-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}
.file {
  color: #909399;
  font-size: 12px;
}
.result {
  margin-top: 16px;
}
.warn {
  margin-top: 6px;
}
</style>
