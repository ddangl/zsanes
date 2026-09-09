import request from './request'
import type { PageResult } from '@/types/common'
import type { Rule, RuleVersion } from '@/types/rule'

export interface RuleQuery {
  category?: string
  status?: string
  keyword?: string
  pageNum?: number
  pageSize?: number
}

export const pageRules = (params: RuleQuery) =>
  request.get<any, PageResult<Rule>>('/rules', { params })

export const ruleVersions = (id: number) =>
  request.get<any, RuleVersion[]>(`/rules/${id}/versions`)

export const createRule = (data: Partial<Rule>) => request.post<any, void>('/rules', data)

export const updateRule = (id: number, data: Partial<Rule>) =>
  request.put<any, void>(`/rules/${id}`, data)

/** 发布:版本+1 并置 ACTIVE */
export const publishRule = (id: number) =>
  request.post<any, number>(`/rules/${id}/publish`)

/** 退役 */
export const retireRule = (id: number) => request.delete<any, void>(`/rules/${id}`)

/** payload 中文回读预览 */
export const previewRulePayload = (payload: string) =>
  request.post<any, string>('/rules/preview', { payload })
