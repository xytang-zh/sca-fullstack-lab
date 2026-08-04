import { request } from '../index'
import type { UserVO } from '@sca/types'
import type { PageResult } from '@sca/types'

/** 我的完整资料（需登录，含 bio/关注数/粉丝数） */
export function getMyProfile() {
  return request.get<UserVO>('/api/system/users/me/mine')
}

/**
 * 关注/取消关注（需登录，幂等，返回操作后的关注状态）。
 * @param id 目标用户 ID（雪花 ID，string）
 * @returns true=已关注 false=已取消关注
 */
export function toggleFollow(id: string) {
  return request.post<boolean>(`/api/system/users/${id}/follow`)
}

/**
 * 粉丝列表（关注该用户的人）。
 * @param userId 目标用户 ID
 * @param page 页码（从 1 开始）
 * @param size 每页条数
 * @returns 分页用户列表
 */
export function followers(userId: string, page = 1, size = 10) {
  return request.get<PageResult<UserVO>>(`/api/system/users/${userId}/followers`, {
    params: { page, size }
  })
}

/**
 * 关注列表（该用户关注的人）。
 * @param userId 目标用户 ID
 * @param page 页码（从 1 开始）
 * @param size 每页条数
 * @returns 分页用户列表
 */
export function following(userId: string, page = 1, size = 10) {
  return request.get<PageResult<UserVO>>(`/api/system/users/${userId}/following`, {
    params: { page, size }
  })
}

/**
 * 用户分页（管理员，按用户名/昵称模糊搜索）。
 * @param query 分页与关键字参数
 * @returns 分页用户列表
 */
export function pageUsers(query: { page?: number; size?: number; keyword?: string }) {
  return request.get<PageResult<UserVO>>('/api/system/users', { params: query })
}