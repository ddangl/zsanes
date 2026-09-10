import request from './request'
import type { ImportSummary, MonthResp, ParseResp, RosterIssue, RosterRow } from '@/types/roster'

/** 阶段一:上传解析(零写入) */
export const parseRoster = (month: string, file: File) => {
  const form = new FormData()
  form.append('file', file)
  return request.post<any, ParseResp>(`/roster/parse?month=${month}`, form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/** 阶段二:回传修正后行集合并入库;阻断时 reject 携带问题清单 */
export const importRoster = (month: string, rows: RosterRow[]) =>
  request.post<any, ImportSummary>(`/roster/import?month=${month}`, rows)

/** 按月查询(按日 + 顺序池) */
export const getRosterMonth = (month: string) =>
  request.get<any, MonthResp>(`/roster/${month}`)

/** import 的 400 载荷在错误对象的 body.data(问题清单) */
export const extractIssues = (error: unknown): RosterIssue[] | null => {
  const body = (error as { body?: { code?: number; data?: RosterIssue[] } })?.body
  if (body?.code === 400 && Array.isArray(body.data)) {
    return body.data
  }
  return null
}
