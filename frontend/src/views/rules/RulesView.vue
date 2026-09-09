<template>
  <div class="page">
    <el-card shadow="never">
      <template #header><b>规则中心</b>(结构化参数 + 自然语言原文双形态;引擎只执行已发布 ACTIVE 的 payload)</template>

      <el-form inline :model="query" @submit.prevent>
        <el-form-item label="分类">
          <el-select v-model="query.category" placeholder="全部分类" clearable style="width: 150px" @change="load(1)">
            <el-option v-for="c in RULE_CATEGORIES" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 110px" @change="load(1)">
            <el-option label="草稿 DRAFT" value="DRAFT" />
            <el-option label="生效 ACTIVE" value="ACTIVE" />
            <el-option label="退役 RETIRED" value="RETIRED" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键字">
          <el-input v-model="query.keyword" placeholder="名称/编码" clearable style="width: 150px" @keyup.enter="load(1)" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load(1)">查询</el-button>
          <el-button type="success" @click="openDialog()">新增规则</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="ruleCode" label="编码" width="240" show-overflow-tooltip />
        <el-table-column label="分类" width="110">
          <template #default="{ row }">{{ categoryLabel(row.category) }}</template>
        </el-table-column>
        <el-table-column prop="name" label="名称" width="160" show-overflow-tooltip />
        <el-table-column prop="nlText" label="自然语言原文" min-width="260" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="版本" width="70">
          <template #default="{ row }">v{{ row.version ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
            <el-button link type="success" :disabled="row.status === 'RETIRED'" @click="onPublish(row)">发布</el-button>
            <el-button link @click="openVersions(row)">版本</el-button>
            <el-button link type="danger" :disabled="row.status === 'RETIRED'" @click="onRetire(row)">退役</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pager"
        layout="total, prev, pager, next"
        :total="total"
        v-model:current-page="query.pageNum"
        :page-size="query.pageSize"
        @current-change="load()"
      />
    </el-card>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="dialog.visible" :title="dialog.form.id ? '编辑规则' : '新增规则'" width="720px" top="5vh">
      <el-form :model="dialog.form" label-width="90px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="分类" required>
              <el-select v-model="dialog.form.category" style="width: 100%">
                <el-option v-for="c in RULE_CATEGORIES" :key="c.value" :label="c.label" :value="c.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12"><el-form-item label="规则编码"><el-input v-model="dialog.form.ruleCode" placeholder="留空自动生成" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="名称" required><el-input v-model="dialog.form.name" /></el-form-item></el-col>
          <el-col :span="24">
            <el-form-item label="自然语言" required>
              <el-input v-model="dialog.form.nlText" type="textarea" :rows="3" placeholder="科室原话,存档溯源" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="payload" required>
              <el-input v-model="dialog.form.payload" type="textarea" :rows="5" placeholder='JSON,如 {"maxRetry":20}' />
              <div class="json-tip">{{ payloadError || '结构化参数(引擎执行)' }}</div>
            </el-form-item>
          </el-col>
          <el-col :span="12"><el-form-item label="ruleType"><el-input v-model="dialog.form.ruleType" placeholder="引擎挂载类型" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="优先级"><el-input-number v-model="dialog.form.priority" :min="0" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="onPreview" :disabled="!!payloadError">回读预览</el-button>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="dialog.saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- 回读预览结果 -->
    <el-dialog v-model="previewVisible" title="payload 中文回读" width="560px">
      <div class="preview">{{ previewText }}</div>
    </el-dialog>

    <!-- 版本历史 -->
    <el-dialog v-model="versions.visible" :title="`版本历史:${versions.ruleName}`" width="700px" top="5vh">
      <el-table :data="versions.rows" stripe size="small">
        <el-table-column label="版本" width="70">
          <template #default="{ row }">v{{ row.version }}</template>
        </el-table-column>
        <el-table-column prop="publishedAt" label="发布时间" width="160" />
        <el-table-column prop="nlText" label="自然语言原文" min-width="200" show-overflow-tooltip />
        <el-table-column prop="payload" label="payload" min-width="200" show-overflow-tooltip />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createRule, pageRules, previewRulePayload, publishRule, retireRule, ruleVersions, updateRule } from '@/api/rule'
import type { Rule, RuleVersion } from '@/types/rule'
import { RULE_CATEGORIES } from '@/types/rule'

const loading = ref(false)
const rows = ref<Rule[]>([])
const total = ref(0)
const query = reactive({ category: '', status: '', keyword: '', pageNum: 1, pageSize: 50 })

const dialog = reactive({
  visible: false,
  saving: false,
  form: {} as Partial<Rule>
})

const previewVisible = ref(false)
const previewText = ref('')
const versions = reactive({ visible: false, ruleName: '', rows: [] as RuleVersion[] })

const payloadError = computed(() => {
  const raw = (dialog.form.payload || '').trim()
  if (!raw) return 'payload 不能为空'
  try {
    JSON.parse(raw)
    return ''
  } catch (e) {
    return 'JSON 格式错误:' + (e as Error).message
  }
})

watch(() => dialog.visible, (v) => {
  if (v) {
    // 打开弹窗时先算一次错误提示
  }
})

const categoryLabel = (value: string) =>
  RULE_CATEGORIES.find((c) => c.value === value)?.label ?? value

const statusTag = (status: string) =>
  status === 'ACTIVE' ? 'success' : status === 'RETIRED' ? 'info' : 'warning'

const load = async (page?: number) => {
  if (page) query.pageNum = page
  loading.value = true
  try {
    const result = await pageRules(query)
    rows.value = result.list
    total.value = result.total
  } finally {
    loading.value = false
  }
}

const openDialog = (row?: Rule) => {
  dialog.form = row
    ? { ...row }
    : { category: 'DUTY', priority: 100, payload: '{}' }
  dialog.visible = true
}

const save = async () => {
  if (!dialog.form.name || !dialog.form.category || !dialog.form.nlText) {
    ElMessage.warning('名称/分类/自然语言原文必填')
    return
  }
  if (payloadError.value) {
    ElMessage.warning(payloadError.value)
    return
  }
  dialog.saving = true
  try {
    if (dialog.form.id) {
      await updateRule(dialog.form.id, dialog.form)
    } else {
      await createRule(dialog.form)
    }
    ElMessage.success('保存成功(草稿);发布后才会被引擎执行')
    dialog.visible = false
    load()
  } catch {
    // 拦截器已报错
  } finally {
    dialog.saving = false
  }
}

const onPreview = async () => {
  previewText.value = await previewRulePayload((dialog.form.payload || '').trim())
  previewVisible.value = true
}

const onPublish = async (row: Rule) => {
  await ElMessageBox.confirm(
    `发布规则「${row.name}」?将生成 v${(row.version ?? 0) + 1} 并置为生效。`, '发布确认',
    { type: 'warning' }
  )
  const version = await publishRule(row.id)
  ElMessage.success(`已发布 v${version}`)
  load()
}

const onRetire = async (row: Rule) => {
  await ElMessageBox.confirm(
    `退役规则「${row.name}」?退役后不再参与执行,历史版本保留。`, '退役确认',
    { type: 'warning' }
  )
  await retireRule(row.id)
  ElMessage.success('已退役')
  load()
}

const openVersions = async (row: Rule) => {
  versions.ruleName = row.name
  versions.rows = await ruleVersions(row.id)
  versions.visible = true
}

onMounted(() => load(1))
</script>

<style scoped>
.page {
  max-width: 1280px;
}
.pager {
  margin-top: 12px;
  justify-content: flex-end;
}
.json-tip {
  font-size: 12px;
  color: #909399;
  line-height: 1.4;
  margin-top: 4px;
  white-space: pre-wrap;
  word-break: break-all;
}
.preview {
  background: #f5f7fa;
  padding: 12px;
  border-radius: 4px;
  line-height: 1.8;
}
</style>
