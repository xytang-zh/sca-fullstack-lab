import { request } from '../index'
import type { UserInfoVO } from '@sca/types'

/** 个人主页：当前登录用户信息（需登录） */
export function getMyProfile() {
  return request.get<UserInfoVO>('/api/auth/me')
}
