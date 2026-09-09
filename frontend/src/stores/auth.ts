import { defineStore } from 'pinia'
import { loginApi } from '@/api/auth'

interface UserInfo {
  username: string
  name: string
  role: string
}

/** 登录态:token + 用户信息,持久化到 localStorage */
export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    user: JSON.parse(localStorage.getItem('userInfo') || 'null') as UserInfo | null
  }),
  getters: {
    isLogin: (s) => !!s.token,
    isAdmin: (s) => s.user?.role === 'ADMIN'
  },
  actions: {
    async login(username: string, password: string) {
      const resp = await loginApi(username, password)
      this.token = resp.token
      this.user = resp.user
      localStorage.setItem('token', resp.token)
      localStorage.setItem('userInfo', JSON.stringify(resp.user))
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
    }
  }
})
