export interface Rule {
  id: number
  ruleCode: string
  category: string
  name: string
  nlText: string
  payload: string
  ruleType: string
  status: 'DRAFT' | 'ACTIVE' | 'RETIRED' | string
  priority?: number
  version?: number
}

export interface RuleVersion {
  version: number
  nlText: string
  payload: string
  publishedBy?: number
  publishedAt?: string
}

export const RULE_CATEGORIES = [
  { value: 'DUTY', label: '值班备班' },
  { value: 'OR', label: '手术室' },
  { value: 'STAFF_AVAILABILITY', label: '人员可用性' },
  { value: 'PERIPHERAL', label: '外围(二期)' },
  { value: 'LEAVE', label: '假期(二期)' },
  { value: 'GENERAL', label: '通用' }
] as const
