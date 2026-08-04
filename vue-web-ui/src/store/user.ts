import { defineStore } from 'pinia'
import { ref } from 'vue'
import { authApi } from '@sca/api'
import { getToken, setToken, clearToken } from '@sca/utils'
import type { LoginDTO, LoginVO, RegisterDTO, UserInfoVO } from '@sca/types'

/** 刷新令牌存储键（与 @sca/utils 的 KEY 保持一致的语义） */
const REFRESH_TOKEN_KEY = 'refresh_token'

/**
 * 用户状态 Store：维护登录态、用户信息与角色，是全局唯一的认证状态源。
 * @description 登录/注册/登出/拉取用户信息均收敛于此，组件只消费 state 与 action，
 * 禁止组件直接操作 localStorage 或调用 API。
 */
export const useUserStore = defineStore('user', () => {
  /** 访问令牌（带 Bearer 前缀，空串表示未登录） */
  const token = ref<string>(getToken())
  /** 当前登录用户信息（懒加载，首次进入受保护页时拉取） */
  const userInfo = ref<UserInfoVO | null>(null)
  /** 角色编码列表（用于路由守卫与菜单权限过滤） */
  const roles = ref<string[]>([])

  /**
   * 保存登录态：统一处理 Bearer 前缀，并落盘到 localStorage。
   * @param data 登录/注册接口返回的登录态数据
   */
  function saveLogin(data: LoginVO) {
    const bearer = data.tokenValue.startsWith('Bearer ')
      ? data.tokenValue
      : `Bearer ${data.tokenValue}`
    token.value = bearer
    setToken(bearer)
    if (data.refreshToken) {
      localStorage.setItem(REFRESH_TOKEN_KEY, data.refreshToken)
    }
    roles.value = data.roles ?? []
    userInfo.value = {
      id: data.userId,
      username: data.username,
      nickname: data.nickname,
      avatar: data.avatar
    }
  }

  /**
   * 账号密码登录并保存登录态。
   * @param dto 登录入参（账号、密码、验证码）
   */
  async function loginByAccount(dto: LoginDTO) {
    saveLogin(await authApi.login(dto))
  }

  /**
   * 注册并自动登录。
   * @param dto 注册入参
   */
  async function register(dto: RegisterDTO) {
    saveLogin(await authApi.register(dto))
  }

  /**
   * 拉取当前用户完整信息（含角色），供路由守卫做角色鉴权。
   * @returns 用户信息
   */
  async function fetchUserInfo() {
    userInfo.value = await authApi.getMe()
    roles.value = (userInfo.value.roles ?? []).map((r) => r.code ?? '').filter(Boolean)
    return userInfo.value
  }

  /** 登出：通知服务端会话失效，无论是否成功都清理本地登录态 */
  async function logout() {
    try {
      await authApi.logout()
    } finally {
      reset()
    }
  }

  /** 清理本地登录态（登出 / 登录态失效时调用） */
  function reset() {
    token.value = ''
    userInfo.value = null
    roles.value = []
    clearToken()
    localStorage.removeItem(REFRESH_TOKEN_KEY)
  }

  return {
    token,
    userInfo,
    roles,
    loginByAccount,
    register,
    fetchUserInfo,
    logout,
    reset
  }
})