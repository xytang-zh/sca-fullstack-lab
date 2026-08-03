import { request } from '../index'
import type { CaptchaVO, LoginDTO, LoginVO, PasswordUpdateDTO, RegisterDTO, UserInfoVO } from '@sca/types'

/** 获取文字图形验证码 */
export function getCaptcha() {
  return request.get<CaptchaVO>('/api/auth/captcha')
}

/** 账号密码登录（需文字验证码，校验忽略大小写、一次性消费） */
export function login(dto: LoginDTO) {
  return request.post<LoginVO>('/api/auth/login', dto)
}

/** 账号注册（注册成功自动登录） */
export function register(dto: RegisterDTO) {
  return request.post<LoginVO>('/api/auth/register', dto)
}

/** 登出 */
export function logout() {
  return request.post<void>('/api/auth/logout')
}

/** 当前登录用户信息 */
export function getMe() {
  return request.get<UserInfoVO>('/api/auth/me')
}

/** 修改密码（需登录，成功后需重新登录） */
export function updatePassword(dto: PasswordUpdateDTO) {
  return request.patch<void>('/api/auth/me/password', dto)
}
