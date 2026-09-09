import axios from 'axios'
import { ElMessage } from 'element-plus'

// 统一 API 出口:自动附带 JWT;code=0 解包返回 data;401 跳登录;其余统一报错
const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (resp) => {
    const body = resp.data
    if (body.code === 0) {
      return body.data
    }
    if (body.code === 401) {
      toLogin()
      return Promise.reject(new Error('未登录或登录已过期'))
    }
    const msg = body.message || '请求失败'
    ElMessage.error(msg)
    return Promise.reject(new Error(msg))
  },
  (error) => {
    if (error.response?.status === 401) {
      toLogin()
    } else {
      ElMessage.error(error.response?.data?.message || '网络异常或服务不可用')
    }
    return Promise.reject(error)
  }
)

function toLogin() {
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
  if (window.location.pathname !== '/login') {
    window.location.assign('/login?redirect=' + encodeURIComponent(window.location.pathname))
  }
}

export default request
