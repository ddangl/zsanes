import request from './request'
import type { PageResult } from '@/types/common'
import type { ImportResult, Staff, StaffLite, StaffQuery } from '@/types/staff'

export const pageStaff = (params: StaffQuery) =>
  request.get<any, PageResult<Staff>>('/staff', { params })

export const liteStaff = (onlyActive = true) =>
  request.get<any, StaffLite[]>('/staff/lite', { params: { onlyActive } })

export const createStaff = (data: Partial<Staff>) => request.post<any, void>('/staff', data)

export const updateStaff = (id: number, data: Partial<Staff>) =>
  request.put<any, void>(`/staff/${id}`, data)

/** 停用(逻辑删除) */
export const disableStaff = (id: number) => request.delete<any, void>(`/staff/${id}`)

export const importStaff = (file: File) => {
  const form = new FormData()
  form.append('file', file)
  return request.post<any, ImportResult>('/staff/import', form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/** 模板下载(blob 流,触发浏览器保存) */
export const downloadStaffTemplate = async () => {
  const resp = await request.get('/staff/import/template', { responseType: 'blob' })
  const url = URL.createObjectURL(new Blob([resp as unknown as Blob]))
  const a = document.createElement('a')
  a.href = url
  a.download = '人员档案导入模板.xlsx'
  a.click()
  URL.revokeObjectURL(url)
}
