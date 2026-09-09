export interface Room {
  id: number
  code: string
  area: string
  starred: boolean
  schedulable: boolean
  sort: number
  active: boolean
}

export interface RoomQuery {
  area?: string
  keyword?: string
  pageNum?: number
  pageSize?: number
}
