import { request } from '../index'
import type { UserVO } from '@sca/types'
import type { PageVO } from '@sca/types'

/** 我的完整资料（需登录，含 bio/关注数/粉丝数） */
export function getMyProfile() {
  return request.get<UserVO>('/api/system/users/me/mine')
}

/** 关注/取消关注（需登录，幂等） */
export function toggleFollow(id: string) {
  return request.post<boolean>(`/api/system/users/${id}/follow`)
}

/** 粉丝列表（关注该用户的人） */
export function followers(userId: string, pageNum = 1, pageSize = 10) {
  return request.get<PageVO<UserVO>>(`/api/system/users/${userId}/followers`, {
    params: { pageNum, pageSize }
  })
}

/** 关注列表（该用户关注的人） */
export function following(userId: string, pageNum = 1, pageSize = 10) {
  return request.get<PageVO<UserVO>>(`/api/system/users/${userId}/following`, {
    params: { pageNum, pageSize }
  })
}

/** 用户分页（管理员） */
export function pageUsers(query: { pageNum?: number; pageSize?: number; keyword?: string }) {
  return request.get<PageVO<UserVO>>('/api/system/users', { params: query })
}