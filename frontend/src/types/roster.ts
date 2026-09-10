/** 月度值班备班表类型(与后端 service/roster 域契约对齐) */

export type RosterForm = 'BY_DATE' | 'SEQUENTIAL_POOL'

export type RosterPositionType =
  | 'ON_CALL_CHIEF'
  | 'ON_CALL_T2'
  | 'ON_CALL_T3'
  | 'ON_CALL_T4'
  | 'STANDBY1'
  | 'STANDBY2'
  | 'SHORT_RELIEF'
  | 'LONG_RELIEF'
  | 'ASSISTANT_RELIEF'
  | 'ASSISTANT_MID'

export const POSITION_LABELS: Record<RosterPositionType, string> = {
  ON_CALL_CHIEF: '值班老总',
  ON_CALL_T2: '二档',
  ON_CALL_T3: '三档',
  ON_CALL_T4: '四档',
  STANDBY1: '备班1',
  STANDBY2: '备班2',
  SHORT_RELIEF: '短接班',
  LONG_RELIEF: '长接班',
  ASSISTANT_RELIEF: '副麻接班',
  ASSISTANT_MID: '副麻中班'
}

export const POOL_ONLY: RosterPositionType = 'SHORT_RELIEF'

export interface StaffRef {
  id: number
  empNo?: string
  name: string
  active: boolean
}

export interface StaffMatch {
  staffId: number | null
  candidates: StaffRef[]
  needsChoice: boolean
  inactiveHit: boolean
}

export interface RosterRow {
  sheet: string
  excelRow: number
  label: string
  type: RosterPositionType | null
  day: number | null
  seq: number | null
  cellText: string
  match: StaffMatch
}

export interface RosterIssue {
  level: 'BLOCK' | 'WARN'
  location: string
  message: string
  fixType: 'STAFF_CANDIDATES' | 'POSITION' | 'DATE' | 'NONE'
}

export interface ParseResp {
  fileMonth: string | null
  rows: RosterRow[]
  issues: RosterIssue[]
  hasBlock: boolean
}

export interface ImportSummary {
  rosterId: number
  itemCount: number
}

export interface MonthResp {
  rosterId: number
  month: string
  status: string
  byDate: { date: string; positionType: RosterPositionType; staffId: number; staffName?: string }[]
  pool: { seq: number; staffId: number; staffName?: string }[]
}
