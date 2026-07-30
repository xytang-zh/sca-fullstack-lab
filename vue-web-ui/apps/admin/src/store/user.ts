import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as authApi from '@/api/auth'
import { getToken, setToken, clearToken } from '@sca/utils'
import type { LoginDTO, UserInfoVO } from '@sca/types'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(getToken())
  const userInfo = ref<UserInfoVO | null>(null)
  const perms = ref<string[]>([])
  const roles = ref<string[]>([])

  async function login(dto: LoginDTO) {
    const data = await authApi.login(dto)
    if (!data?.tokenValue) {
      throw new Error('登录失败：服务端未返回 Token')
    }
    const bearer = data.tokenValue.startsWith('Bearer ')
      ? data.tokenValue
      : `Bearer ${data.tokenValue}`
    token.value = bearer
    setToken(bearer)
    roles.value = data.roles ?? []
    perms.value = data.perms ?? []
    userInfo.value = {
      id: data.userId,
      username: data.username,
      nickname: data.nickname,
      avatar: data.avatar
    }
    return data
  }

  async function fetchUserInfo() {
    const info = await authApi.getMe()
    userInfo.value = info
    roles.value = (info.roles ?? []).map((r) => r.code ?? '').filter(Boolean)
    perms.value = info.perms ?? []
    return info
  }

  async function logout() {
    try {
      await authApi.logout()
    } finally {
      reset()
    }
  }

  function reset() {
    token.value = ''
    userInfo.value = null
    perms.value = []
    roles.value = []
    clearToken()
  }

  function hasPerm(perm: string): boolean {
    if (perms.value.includes('*:*:*')) return true
    return perms.value.includes(perm)
  }

  function hasRole(role: string): boolean {
    if (roles.value.includes('super_admin')) return true
    return roles.value.includes(role)
  }

  return {
    token,
    userInfo,
    perms,
    roles,
    login,
    fetchUserInfo,
    logout,
    reset,
    hasPerm,
    hasRole
  }
})
