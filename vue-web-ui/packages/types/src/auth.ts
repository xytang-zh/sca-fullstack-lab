export interface LoginDTO {
  account: string
  password: string
  captchaKey: string
  captchaCode: string
  rememberMe?: boolean
}

export interface RegisterDTO {
  account: string
  password: string
  confirmPassword: string
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
  captchaKey: string
  imageBase64: string
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
