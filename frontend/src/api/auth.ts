import request from './request'

export interface LoginResp {
  token: string
  user: { username: string; name: string; role: string }
}

export const loginApi = (username: string, password: string) =>
  request.post<any, LoginResp>('/auth/login', { username, password })
