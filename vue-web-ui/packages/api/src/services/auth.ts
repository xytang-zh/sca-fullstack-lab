import { request } from '../index'
import type {
  CaptchaCheckResult,
  CaptchaVO,
  LoginDTO,
  LoginVO,
  SmsLoginDTO,
  SmsSendDTO,
  UserInfoVO
} from '@sca/types'

/** 获取滑块验证码（拼图） */
export function getCaptcha() {
  return request.get<CaptchaVO>('/api/auth/captcha')
}

/** 校验滑块轨迹，签发一次性 checkToken */
export function checkCaptcha(captchaId: string, data: unknown) {
  return request.post<CaptchaCheckResult>('/api/auth/captcha/check', { id: captchaId, data })
}

/** 发送短信验证码（需先通过滑块验证） */
export function sendSmsCode(dto: SmsSendDTO) {
  return request.post<void>('/api/auth/sms/send', dto)
}

/** 手机验证码登录/注册（新用户自动注册） */
export function smsLogin(dto: SmsLoginDTO) {
  return request.post<LoginVO>('/api/auth/sms/login', dto)
}

/** 账号密码登录（username 支持手机号） */
export function login(dto: LoginDTO) {
  return request.post<LoginVO>('/api/auth/login', dto)
}

/** 登出 */
export function logout() {
  return request.post<void>('/api/auth/logout')
}

/** 当前登录用户信息 */
export function getMe() {
  return request.get<UserInfoVO>('/api/auth/me')
}
