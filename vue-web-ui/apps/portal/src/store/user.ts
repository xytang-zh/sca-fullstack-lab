import { defineStore } from 'pinia'
import { ref } from 'vue'
import { authApi } from '@sca/api'
import { getToken, setToken, clearToken } from '@sca/utils'
import type { LoginDTO, LoginVO, SmsLoginDTO, UserInfoVO } from '@sca/types'

const REFRESH_TOKEN_KEY = 'refresh_token'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(getToken())
  const userInfo = ref<UserInfoVO | null>(null)

  function saveLogin(data: LoginVO) {
    const bearer = data.tokenValue.startsWith('Bearer ')
      ? data.tokenValue
      : `Bearer ${data.tokenValue}`
    token.value = bearer
    setToken(bearer)
    if (data.refreshToken) {
      localStorage.setItem(REFRESH_TOKEN_KEY, data.refreshToken)
    }
    userInfo.value = {
      id: data.userId,
      username: data.username,
      nickname: data.nickname,
      avatar: data.avatar
    }
  }

  async function loginByPassword(dto: LoginDTO) {
    saveLogin(await authApi.login(dto))
  }

  async function loginBySms(dto: SmsLoginDTO) {
    saveLogin(await authApi.smsLogin(dto))
  }

  async function fetchUserInfo() {
    userInfo.value = await authApi.getMe()
    return userInfo.value
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
    clearToken()
    localStorage.removeItem(REFRESH_TOKEN_KEY)
  }

  return {
    token,
    userInfo,
    loginByPassword,
    loginBySms,
    fetchUserInfo,
    logout,
    reset
  }
})
