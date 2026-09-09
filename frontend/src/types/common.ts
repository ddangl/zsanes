/** 通用类型(与后端 common 包对应) */
export interface PageResult<T> {
  total: number
  list: T[]
}

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}
