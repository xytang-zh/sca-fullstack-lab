export interface LoginDTO {
  username: string
  password: string
  captcha: string
  captchaKey: string
  rememberMe?: boolean
}

export interface LoginVO {
  tokenName: string
  tokenValue: string
  expiresIn: number
  userId: number
  username: string
  nickname: string
  avatar?: string
  roles: string[]
  perms: string[]
}

export interface CaptchaVO {
  captchaKey: string
  captchaImg: string
}

export interface UserInfoVO {
  id: number
  username: string
  nickname: string
  email?: string
  phone?: string
  avatar?: string
  deptId?: number
  deptName?: string
  roles?: Array<{ id?: number; code?: string; name?: string; dataScope?: number }>
  perms?: string[]
  lastLoginTime?: string
  lastLoginIp?: string
}

export interface PasswordUpdateDTO {
  oldPassword: string
  newPassword: string
  confirmPassword: string
}
