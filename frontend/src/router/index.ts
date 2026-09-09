import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    /** 仅总值班(ADMIN)可访问 */
    requiresAdmin?: boolean
  }
}

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/login/LoginView.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/components/layout/MainLayout.vue'),
    children: [
      { path: '', redirect: '/workbench' },
      {
        path: 'workbench',
        name: 'workbench',
        component: () => import('@/views/workbench/WorkbenchView.vue'),
        meta: { title: '排班工作台' }
      },
      {
        path: 'roster/monthly',
        name: 'monthlyRoster',
        component: () => import('@/views/roster/MonthlyRosterView.vue'),
        meta: { title: '月度值班备班表', requiresAdmin: true }
      },
      {
        path: 'overview',
        name: 'overview',
        component: () => import('@/views/overview/OverviewView.vue'),
        meta: { title: '排班总览' }
      },
      {
        path: 'my',
        name: 'mySchedule',
        component: () => import('@/views/overview/MyScheduleView.vue'),
        meta: { title: '我的排班' }
      },
      {
        path: 'admin/staff',
        name: 'staff',
        component: () => import('@/views/admin/StaffView.vue'),
        meta: { title: '人员档案', requiresAdmin: true }
      },
      {
        path: 'admin/rooms',
        name: 'rooms',
        component: () => import('@/views/admin/RoomView.vue'),
        meta: { title: '手术室房间', requiresAdmin: true }
      },
      {
        path: 'admin/specialty',
        name: 'specialty',
        component: () => import('@/views/admin/SpecialtyView.vue'),
        meta: { title: '亚专科', requiresAdmin: true }
      },
      {
        path: 'rules',
        name: 'rules',
        component: () => import('@/views/rules/RulesView.vue'),
        meta: { title: '规则中心', requiresAdmin: true }
      }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局守卫:未登录跳登录;ADMIN 专属路由拦截普通成员
router.beforeEach((to) => {
  const auth = useAuthStore()
  document.title = to.meta.title ? `${to.meta.title} · 麻醉科排班系统` : '麻醉科排班系统'
  if (!auth.isLogin && to.path !== '/login') {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.path === '/login' && auth.isLogin) {
    return { path: '/' }
  }
  if (to.meta.requiresAdmin && !auth.isAdmin) {
    ElMessage.warning('该页面仅总值班可访问')
    return { path: '/overview' }
  }
  return true
})

export default router
