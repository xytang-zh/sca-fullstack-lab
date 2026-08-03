export interface LoginDTO {
  username: string
  password: string
  checkToken?: string
  rememberMe?: boolean
}

export interface SmsSendDTO {
  phone: string
  checkToken: string
}

export interface SmsLoginDTO {
  phone: string
  code: string
}

export interface CaptchaCheckResult {
  checkToken: string
}

export interface LoginVO {
  tokenName: string
  tokenValue: string
  expiresIn: number
  userId: string
  username: string
  nickname: string
  avatar?: string
  roles: string[]
  perms: string[]
  refreshToken?: string
}

export interface CaptchaVO {
  captchaId: string
  id: string
  type: string
  backgroundImage: string
  templateImage: string
  backgroundImageWidth?: number
  backgroundImageHeight?: number
  templateImageWidth?: number
  templateImageHeight?: number
}

export interface UserInfoVO {
  id: string
  username: string
  nickname: string
  email?: string
  phone?: string
  avatar?: string
  deptId?: string
  deptName?: string
  roles?: Array<{ id?: string; code?: string; name?: string; dataScope?: number }>
  perms?: string[]
  lastLoginTime?: string
  lastLoginIp?: string
}

export interface PasswordUpdateDTO {
  oldPassword: string
  newPassword: string
  confirmPassword: string
}
