import axios from 'axios'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

request.interceptors.request.use((config) => {
  // TODO(实施步骤2):登录后附带 JWT
  // const token = localStorage.getItem('token')
  // if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

request.interceptors.response.use(
  (resp) => resp.data,
  (error) => {
    // TODO(实施步骤2):统一错误处理(401 跳转登录、提示后端 message)
    return Promise.reject(error)
  }
)

export default request
