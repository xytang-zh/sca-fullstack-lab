/**
 * 账号密码登录入参。
 * @description 验证码为一次性消费，校验时忽略大小写；captchaKey 用于后端定位本地验证码
 */
export interface LoginDTO {
  /** 登录账号（用户名） */
  account: string
  /** 明文密码（HTTPS 传输，后端使用 Argon2id 校验） */
  password: string
  /** 验证码唯一标识（后端缓存验证码时的 Key） */
  captchaKey: string
  /** 用户输入的图形验证码内容 */
  captchaCode: string
  /** 是否记住登录（为 true 时延长会话有效期） */
  rememberMe?: boolean
}

/** 账号注册入参（注册成功返回登录态，前端直接进入系统） */
export interface RegisterDTO {
  /** 注册账号（6-18 位，字母开头仅含字母数字） */
  account: string
  /** 密码（8-32 位） */
  password: string
  /** 确认密码，前端校验与 password 一致 */
  confirmPassword: string
}

/** 登录成功出参：一次性下发登录态与用户基础信息 */
export interface LoginVO {
  /** Token 名称（惯例为 Bearer，用于拼装 Authorization 头） */
  tokenName: string
  /** Token 值（可带 Bearer 前缀，前端统一按返回值原样存储） */
  tokenValue: string
  /** Token 有效期（秒） */
  expiresIn: number
  /** 用户 ID（雪花 ID，为 string，禁止转 number） */
  userId: string
  /** 登录账号 */
  username: string
  /** 显示昵称 */
  nickname: string
  /** 头像 URL */
  avatar?: string
  /** 角色编码列表（如 ADMIN/AUTHOR，用于路由权限过滤） */
  roles: string[]
  /** 权限标识列表（保留，暂未启用按钮级鉴权） */
  perms: string[]
  /** 刷新令牌（可选，用于 accessToken 过期后的续期） */
  refreshToken?: string
}

/** 图形验证码出参：图片以 Base64 内联返回，点击图片可刷新 */
export interface CaptchaVO {
  /** 验证码标识（提交登录时原样回传） */
  captchaKey: string
  /** 验证码图片（data:image 的 Base64 数据） */
  imageBase64: string
}

/** 当前登录用户信息（GET /api/auth/me 出参） */
export interface UserInfoVO {
  /** 用户 ID（雪花 ID，为 string，禁止转 number） */
  id: string
  /** 登录账号 */
  username: string
  /** 显示昵称 */
  nickname: string
  /** 邮箱 */
  email?: string
  /** 手机号 */
  phone?: string
  /** 头像 URL */
  avatar?: string
  /** 部门 ID（雪花 ID，为 string，禁止转 number） */
  deptId?: string
  /** 部门名称 */
  deptName?: string
  /** 角色列表（dataScope 为数据权限范围枚举） */
  roles?: Array<{ id?: string; code?: string; name?: string; dataScope?: number }>
  /** 权限标识列表 */
  perms?: string[]
  /** 最近登录时间 */
  lastLoginTime?: string
  /** 最近登录 IP */
  lastLoginIp?: string
}

/** 修改密码入参（成功后需重新登录） */
export interface PasswordUpdateDTO {
  /** 原密码 */
  oldPassword: string
  /** 新密码（8-32 位） */
  newPassword: string
  /** 确认新密码，须与新密码一致 */
  confirmPassword: string
}
