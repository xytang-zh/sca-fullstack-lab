import { request } from '@sca/api'
import type { DeptVO } from '@sca/types'

export function getDeptTree() {
  return request.get<DeptVO[]>('/api/system/depts/tree')
}

export function createDept(dto: Partial<DeptVO>) {
  return request.post<boolean>('/api/system/depts', dto)
}

export function updateDept(id: number, dto: Partial<DeptVO>) {
  return request.put<boolean>(`/api/system/depts/${id}`, dto)
}

export function deleteDept(id: number) {
  return request.delete<boolean>(`/api/system/depts/${id}`)
}
