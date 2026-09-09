import request from './request'
import type { Specialty } from '@/types/specialty'

export const listSpecialties = (onlyActive = true) =>
  request.get<any, Specialty[]>('/specialties', { params: { onlyActive } })

export const createSpecialty = (data: { name: string; sort?: number }) =>
  request.post<any, void>('/specialties', data)

export const updateSpecialty = (id: number, data: { name: string; sort?: number }) =>
  request.put<any, void>(`/specialties/${id}`, data)

export const disableSpecialty = (id: number) =>
  request.delete<any, void>(`/specialties/${id}`)
