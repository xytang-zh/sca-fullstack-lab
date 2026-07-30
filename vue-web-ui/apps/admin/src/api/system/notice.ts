import { request } from '@sca/api'
import type { NoticeVO, PageVO, PageQuery } from '@sca/types'

export function pageNotices(query: PageQuery) {
  return request.get<PageVO<NoticeVO>>('/api/system/notices', { params: query })
}

export function createNotice(dto: Partial<NoticeVO>) {
  return request.post<boolean>('/api/system/notices', dto)
}

export function updateNotice(id: number, dto: Partial<NoticeVO>) {
  return request.put<boolean>(`/api/system/notices/${id}`, dto)
}

export function publishNotice(id: number) {
  return request.post<void>(`/api/system/notices/${id}/publish`)
}

export function revokeNotice(id: number) {
  return request.post<void>(`/api/system/notices/${id}/revoke`)
}

export function deleteNotice(id: number) {
  return request.delete<boolean>(`/api/system/notices/${id}`)
}
