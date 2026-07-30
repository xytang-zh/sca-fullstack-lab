import { request } from '@sca/api'
import type { PageVO, RoleVO, PageQuery } from '@sca/types'

export function pageRoles(query: PageQuery) {
  return request.get<PageVO<RoleVO>>('/api/system/roles', { params: query })
}

export function createRole(dto: Partial<RoleVO>) {
  return request.post<boolean>('/api/system/roles', dto)
}

export function updateRole(id: number, dto: Partial<RoleVO>) {
  return request.put<boolean>(`/api/system/roles/${id}`, dto)
}

export function deleteRole(id: number) {
  return request.delete<boolean>(`/api/system/roles/${id}`)
}
