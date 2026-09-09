import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/workbench' },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/login/LoginView.vue'),
      meta: { title: '登录' }
    },
    {
      path: '/workbench',
      name: 'workbench',
      component: () => import('@/views/workbench/WorkbenchView.vue'),
      meta: { title: '排班工作台' }
    },
    {
      path: '/roster/monthly',
      name: 'monthlyRoster',
      component: () => import('@/views/roster/MonthlyRosterView.vue'),
      meta: { title: '月度值班备班表' }
    },
    {
      path: '/overview',
      name: 'overview',
      component: () => import('@/views/overview/OverviewView.vue'),
      meta: { title: '排班总览' }
    },
    {
      path: '/my',
      name: 'mySchedule',
      component: () => import('@/views/overview/MyScheduleView.vue'),
      meta: { title: '我的排班' }
    },
    {
      path: '/admin/staff',
      name: 'staff',
      component: () => import('@/views/admin/StaffView.vue'),
      meta: { title: '人员档案' }
    },
    {
      path: '/admin/rooms',
      name: 'rooms',
      component: () => import('@/views/admin/RoomView.vue'),
      meta: { title: '手术室房间' }
    },
    {
      path: '/admin/config',
      name: 'scheduleConfig',
      component: () => import('@/views/admin/ConfigView.vue'),
      meta: { title: '排班参数' }
    }
  ]
})

// TODO(实施步骤2):JWT 登录完成后,增加全局前置守卫(未登录跳转 /login,按角色控制 /admin/**)

export default router
