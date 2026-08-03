import { request } from '@sca/api'
import type {
  CaptchaVO,
  LoginDTO,
  LoginVO,
  PasswordUpdateDTO,
  UserInfoVO
} from '@sca/types'

export function getCaptcha() {
  return request.get<CaptchaVO>('/api/auth/captcha')
}

export function checkCaptcha(id: string, data: unknown) {
  return request.post<{ checkToken: string }>('/api/auth/captcha/check', { id, data })
}

export function login(dto: LoginDTO) {
  return request.post<LoginVO>('/api/auth/login', dto, { skipErrorHandler: true })
}

export function logout() {
  return request.post<void>('/api/auth/logout')
}

export function getMe() {
  return request.get<UserInfoVO>('/api/auth/me')
}

export function updatePassword(dto: PasswordUpdateDTO) {
  return request.patch<void>('/api/auth/me/password', dto)
}