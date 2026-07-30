import { request } from '@sca/api'
import type { ParamVO, PageVO, PageQuery } from '@sca/types'

export function pageParams(query: PageQuery) {
  return request.get<PageVO<ParamVO>>('/api/system/params', { params: query })
}

export function createParam(dto: Partial<ParamVO>) {
  return request.post<boolean>('/api/system/params', dto)
}

export function updateParam(id: number, dto: Partial<ParamVO>) {
  return request.put<boolean>(`/api/system/params/${id}`, dto)
}

export function deleteParam(id: number) {
  return request.delete<boolean>(`/api/system/params/${id}`)
}
