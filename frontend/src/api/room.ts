import request from './request'
import type { PageResult } from '@/types/common'
import type { Room, RoomQuery } from '@/types/room'

export const pageRooms = (params: RoomQuery) =>
  request.get<any, PageResult<Room>>('/rooms', { params })

export const roomAreas = () => request.get<any, string[]>('/rooms/areas')

export const createRoom = (data: Omit<Room, 'id' | 'active'>) =>
  request.post<any, void>('/rooms', data)

export const updateRoom = (id: number, data: Omit<Room, 'id'>) =>
  request.put<any, void>(`/rooms/${id}`, data)

/** 停用(逻辑删除) */
export const disableRoom = (id: number) => request.delete<any, void>(`/rooms/${id}`)
