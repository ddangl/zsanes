<template>
  <div class="page">
    <el-card shadow="never">
      <template #header><b>亚专科</b>(人员档案第一/第二亚专科的受控词表)</template>
      <el-button type="success" class="add" @click="openDialog()">新增亚专科</el-button>
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="name" label="名称" min-width="160" />
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.active ? 'success' : 'info'" size="small">{{ row.active ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
            <el-button link type="danger" :disabled="!row.active" @click="onDisable(row)">停用</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialog.visible" :title="dialog.form.id ? '编辑亚专科' : '新增亚专科'" width="400px">
      <el-form :model="dialog.form" label-width="70px">
        <el-form-item label="名称" required>
          <el-input v-model="dialog.form.name" placeholder="如 心外科" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="dialog.form.sort" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createSpecialty, disableSpecialty, listSpecialties, updateSpecialty } from '@/api/specialty'
import type { Specialty } from '@/types/specialty'

const loading = ref(false)
const rows = ref<Specialty[]>([])
const dialog = reactive({
  visible: false,
  form: { id: 0, name: '', sort: 0, active: true } as Specialty
})

const load = async () => {
  loading.value = true
  try {
    rows.value = await listSpecialties(false)
  } finally {
    loading.value = false
  }
}

const openDialog = (row?: Specialty) => {
  dialog.form = row ? { ...row } : { id: 0, name: '', sort: 0, active: true }
  dialog.visible = true
}

const save = async () => {
  if (!dialog.form.name.trim()) {
    ElMessage.warning('名称不能为空')
    return
  }
  const { id, name, sort } = dialog.form
  if (id) {
    await updateSpecialty(id, { name, sort })
  } else {
    await createSpecialty({ name, sort })
  }
  ElMessage.success('保存成功')
  dialog.visible = false
  load()
}

const onDisable = async (row: Specialty) => {
  await ElMessageBox.confirm(`确定停用亚专科"${row.name}"?`, '提示', { type: 'warning' })
  await disableSpecialty(row.id)
  ElMessage.success('已停用')
  load()
}

onMounted(load)
</script>

<style scoped>
.page {
  max-width: 700px;
}
.add {
  margin-bottom: 12px;
}
</style>
