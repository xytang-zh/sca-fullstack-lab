import { request } from '../index'
import type { CaptchaVO, LoginDTO, LoginVO, PasswordUpdateDTO, RegisterDTO, UserInfoVO } from '@sca/types'

/**
 * 获取文字图形验证码。
 * @returns 验证码标识与 Base64 图片（提交登录时需回传 captchaKey）
 */
export function getCaptcha() {
  return request.get<CaptchaVO>('/api/auth/captcha')
}

/**
 * 账号密码登录（需文字验证码，校验忽略大小写、一次性消费）。
 * @param dto 登录入参（账号、密码、验证码）
 * @returns 登录态（Token + 用户基础信息）
 */
export function login(dto: LoginDTO) {
  return request.post<LoginVO>('/api/auth/login', dto)
}

/**
 * 账号注册（注册成功自动登录，直接返回登录态）。
 * @param dto 注册入参（账号、密码、确认密码）
 * @returns 登录态（与登录一致）
 */
export function register(dto: RegisterDTO) {
  return request.post<LoginVO>('/api/auth/register', dto)
}

/** 登出（使服务端会话失效，前端同步清理本地登录态） */
export function logout() {
  return request.post<void>('/api/auth/logout')
}

/** 当前登录用户信息（用于路由守卫与顶栏展示） */
export function getMe() {
  return request.get<UserInfoVO>('/api/auth/me')
}

/**
 * 修改密码（需登录，成功后需重新登录）。
 * @param dto 旧密码 + 新密码 + 确认密码
 */
export function updatePassword(dto: PasswordUpdateDTO) {
  return request.patch<void>('/api/auth/me/password', dto)
}
