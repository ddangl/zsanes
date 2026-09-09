export interface Staff {
  id: number
  empNo?: string
  name: string
  jobRole: string
  title?: string
  subLevel?: number
  specialty1Id?: number
  specialty2Id?: number
  specialty1Name?: string
  specialty2Name?: string
  mentorId?: number
  mentorName?: string
  grade?: string
  saturdayWork?: boolean | null
  partTimeRule?: string
  note?: string
  phone?: string
  active: boolean
}

/** 人员轻量项(带教下拉) */
export interface StaffLite {
  id: number
  empNo?: string
  name: string
  jobRole: string
}

export interface StaffQuery {
  keyword?: string
  jobRole?: string
  active?: boolean
  pageNum?: number
  pageSize?: number
}

/** Excel 导入结果报告 */
export interface ImportResult {
  totalRows: number
  inserted: number
  updated: number
  skipped: number
  warnings: string[]
}

/** 职称选项(2026-08 实况,"预备"暂无人员) */
export const JOB_ROLES = ['主麻', '总值班', '本院住院', '麻护', '规培', '进修', '轮转', '预备'] as const
