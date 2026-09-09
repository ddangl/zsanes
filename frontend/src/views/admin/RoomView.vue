<template>
  <div class="page">
    <el-card shadow="never">
      <template #header><b>手术室房间</b>(89 间种子数据已入库;眼科/心外科标记不参与排班)</template>

      <!-- 筛选栏 -->
      <el-form inline :model="query" @submit.prevent>
        <el-form-item label="区域">
          <el-select v-model="query.area" placeholder="全部" clearable style="width: 200px" @change="load(1)">
            <el-option v-for="a in areas" :key="a" :label="a" :value="a" />
          </el-select>
        </el-form-item>
        <el-form-item label="房号">
          <el-input v-model="query.keyword" placeholder="如 802" clearable style="width: 140px" @keyup.enter="load(1)" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load(1)">查询</el-button>
          <el-button @click="reset">重置</el-button>
          <el-button type="success" @click="openDialog()">新增房间</el-button>
        </el-form-item>
      </el-form>

      <!-- 表格 -->
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="code" label="房号" width="90" />
        <el-table-column prop="area" label="区域" min-width="180" />
        <el-table-column label="星标" width="80">
          <template #default="{ row }">
            <el-switch v-model="row.starred" :disabled="!auth.isAdmin" @change="toggle(row, 'starred')" />
          </template>
        </el-table-column>
        <el-table-column label="参与排班" width="100">
          <template #default="{ row }">
            <el-switch v-model="row.schedulable" :disabled="!auth.isAdmin" @change="toggle(row, 'schedulable')" />
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.active ? 'success' : 'info'" size="small">{{ row.active ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialog.visible" :title="dialog.form.id ? '编辑房间' : '新增房间'" width="460px">
      <el-form ref="formRef" :model="dialog.form" :rules="rules" label-width="90px">
        <el-form-item label="房号" prop="code">
          <el-input v-model="dialog.form.code" placeholder="如 22、802" />
        </el-form-item>
        <el-form-item label="区域" prop="area">
          <el-select v-model="dialog.form.area" filterable allow-create style="width: 100%" placeholder="选择或输入新区域">
            <el-option v-for="a in areas" :key="a" :label="a" :value="a" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="dialog.form.sort" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="dialog.saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { createRoom, disableRoom, pageRooms, roomAreas, updateRoom } from '@/api/room'
import type { Room } from '@/types/room'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const loading = ref(false)
const rows = ref<Room[]>([])
const total = ref(0)
const areas = ref<string[]>([])
const query = reactive({ area: '', keyword: '', pageNum: 1, pageSize: 20 })

const formRef = ref<FormInstance>()
const dialog = reactive({
  visible: false,
  saving: false,
  form: { id: 0, code: '', area: '', starred: false, schedulable: true, sort: 0, active: true } as Room
})
const rules: FormRules = {
  code: [{ required: true, message: '房号不能为空', trigger: 'blur' }],
  area: [{ required: true, message: '区域不能为空', trigger: 'change' }]
}

const load = async (page?: number) => {
  if (page) query.pageNum = page
  loading.value = true
  try {
    const result = await pageRooms(query)
    rows.value = result.list
    total.value = result.total
  } finally {
    loading.value = false
  }
}

const reset = () => {
  query.area = ''
  query.keyword = ''
  load(1)
}

const openDialog = (row?: Room) => {
  dialog.form = row
    ? { ...row }
    : { id: 0, code: '', area: '', starred: false, schedulable: true, sort: 0, active: true }
  dialog.visible = true
}

const save = async () => {
  await formRef.value?.validate()
  dialog.saving = true
  try {
    const { id, ...data } = dialog.form
    if (id) {
      await updateRoom(id, data as Omit<Room, 'id'>)
    } else {
      await createRoom(data as Omit<Room, 'id' | 'active'>)
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

/** 行内开关:星标/参与排班 即时保存 */
const toggle = async (row: Room, field: 'starred' | 'schedulable') => {
  const { id, ...data } = row
  await updateRoom(id!, data as Omit<Room, 'id'>)
  ElMessage.success(`${field === 'starred' ? '星标' : '参与排班'}已${row[field] ? '开启' : '关闭'}:${row.code}`)
}

const onDisable = async (row: Room) => {
  await ElMessageBox.confirm(`确定停用房间 ${row.code}(${row.area})?`, '提示', { type: 'warning' })
  await disableRoom(row.id)
  ElMessage.success('已停用')
  load()
}

onMounted(async () => {
  areas.value = await roomAreas()
  load(1)
})
</script>

<style scoped>
.page {
  max-width: 1100px;
}
.pager {
  margin-top: 12px;
  justify-content: flex-end;
}
</style>
