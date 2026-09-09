<template>
  <el-container class="layout">
    <el-aside width="210px" class="aside">
      <div class="brand">麻醉科排班系统</div>
      <el-menu :default-active="route.path" router class="menu">
        <el-menu-item index="/workbench">排班工作台</el-menu-item>
        <el-menu-item v-if="auth.isAdmin" index="/roster/monthly">月度值班备班表</el-menu-item>
        <el-menu-item index="/overview">排班总览</el-menu-item>
        <el-menu-item index="/my">我的排班</el-menu-item>
        <el-divider class="divider" />
        <el-menu-item v-if="auth.isAdmin" index="/admin/staff">人员档案</el-menu-item>
        <el-menu-item v-if="auth.isAdmin" index="/admin/rooms">手术室房间</el-menu-item>
        <el-menu-item v-if="auth.isAdmin" index="/admin/specialty">亚专科</el-menu-item>
        <el-menu-item v-if="auth.isAdmin" index="/rules">规则中心</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span class="env-tag">一期开发中</span>
        <div class="user">
          <span>{{ auth.user?.name }}({{ auth.isAdmin ? '总值班' : '成员' }})</span>
          <el-button link type="danger" @click="onLogout">退出登录</el-button>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const onLogout = async () => {
  await ElMessageBox.confirm('确定退出登录?', '提示', { type: 'warning' })
  auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.layout {
  height: 100vh;
}
.aside {
  border-right: 1px solid #e4e7ed;
  background: #fff;
}
.brand {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 16px;
  color: #1f2d3d;
  border-bottom: 1px solid #e4e7ed;
}
.menu {
  border-right: none;
}
.divider {
  margin: 8px 16px;
  width: auto;
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e4e7ed;
  background: #fff;
}
.env-tag {
  color: #909399;
  font-size: 12px;
}
.user {
  display: flex;
  align-items: center;
  gap: 12px;
}
.main {
  background: #f0f2f5;
  overflow: auto;
}
</style>
