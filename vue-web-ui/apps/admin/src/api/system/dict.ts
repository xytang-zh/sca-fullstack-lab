import { request } from '@sca/api'
import type { DictVO, PageVO, PageQuery } from '@sca/types'

export function listDictByType(type: string) {
  return request.get<DictVO[]>(`/api/system/dicts/data/${encodeURIComponent(type)}`)
}

export function pageDicts(query: PageQuery) {
  return request.get<PageVO<DictVO>>('/api/system/dicts', { params: query })
}

export function createDict(dto: Partial<DictVO>) {
  return request.post<boolean>('/api/system/dicts', dto)
}

export function updateDict(id: number, dto: Partial<DictVO>) {
  return request.put<boolean>(`/api/system/dicts/${id}`, dto)
}

export function deleteDict(id: number) {
  return request.delete<boolean>(`/api/system/dicts/${id}`)
}
