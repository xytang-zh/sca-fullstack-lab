import axios, {
  type AxiosInstance,
  type AxiosRequestConfig,
  type AxiosResponse,
  type InternalAxiosRequestConfig
} from 'axios'
import { getToken, setToken, clearToken } from '@sca/utils'
import type { R } from '@sca/types'

const SUCCESS_CODE = 200

/** 登录态失效类业务码：触发清理登录态并跳转登录页 */
const LOGIN_REQUIRED_CODES = [
  21005, // 用户被禁用
  21006, // 用户被锁定
  21007, // Token 过期
  21008, // 被踢下线
  21009, // 未登录
  21010, // Refresh Token 无效
  29001, // 网关 Token 缺失
  29002 // 网关 Token 无效
]

export interface RequestConfig extends AxiosRequestConfig {
  skipErrorHandler?: boolean
  skipAuth?: boolean
}

const instance: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' }
})

instance.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = getToken()
  if (token && !(config as RequestConfig).skipAuth) {
    config.headers.Authorization = token
  }
  if (!config.headers['X-Trace-Id']) {
    config.headers['X-Trace-Id'] = crypto.randomUUID()
  }
  return config
})

instance.interceptors.response.use(
  (response: AxiosResponse<R<unknown>>) => {
    const r = response.data
    if (!r || typeof r.code !== 'number') {
      return response
    }
    if (response.status >= 200 && response.status < 300 && r.code === SUCCESS_CODE) {
      return response
    }
    if (!(response.config as RequestConfig).skipErrorHandler) {
      handleBizError(r)
    }
    return Promise.reject(r)
  },
  (error) => {
    const status = error.response?.status
    const r = error.response?.data as R<unknown> | undefined
    const skip = (error.config as RequestConfig)?.skipErrorHandler

    if (!skip) {
      if (r && typeof r.code === 'number') {
        handleBizError(r)
        if (LOGIN_REQUIRED_CODES.includes(r.code)) {
          clearToken()
          redirectToLogin()
        }
      } else if (status === 401) {
        clearToken()
        redirectToLogin()
      } else if (r?.message) {
        handleMessage(r.message)
      } else {
        handleMessage(error.message || '网络异常')
      }
    }
    return Promise.reject(error)
  }
)

let messageFn: ((content: string, type?: 'info' | 'error' | 'warning') => void) | null = null

export function registerMessageHandler(
  fn: (content: string, type?: 'info' | 'error' | 'warning') => void
) {
  messageFn = fn
}

function handleMessage(content: string, type: 'info' | 'error' | 'warning' = 'error') {
  if (messageFn) {
    messageFn(content, type)
  } else {
    console[type === 'error' ? 'error' : 'warn'](content)
  }
}

function handleBizError(r: R<unknown>) {
  handleMessage(r.message || '操作失败')
}

function redirectToLogin() {
  if (window.location.pathname !== '/login') {
    const redirect = encodeURIComponent(window.location.pathname + window.location.search)
    window.location.href = `/login?redirect=${redirect}`
  }
}

export const request = {
  get<T>(url: string, config?: RequestConfig) {
    return instance.get<R<T>>(url, config).then((res) => res.data.data as T)
  },
  post<T>(url: string, data?: unknown, config?: RequestConfig) {
    return instance.post<R<T>>(url, data, config).then((res) => res.data.data as T)
  },
  put<T>(url: string, data?: unknown, config?: RequestConfig) {
    return instance.put<R<T>>(url, data, config).then((res) => res.data.data as T)
  },
  patch<T>(url: string, data?: unknown, config?: RequestConfig) {
    return instance.patch<R<T>>(url, data, config).then((res) => res.data.data as T)
  },
  delete<T>(url: string, config?: RequestConfig) {
    return instance.delete<R<T>>(url, config).then((res) => res.data.data as T)
  },
  raw() {
    return instance
  }
}

export function storeTokenFromHeader(response: AxiosResponse) {
  const auth = response.headers['authorization'] as string | undefined
  if (auth) {
    setToken(auth.startsWith('Bearer ') ? auth : `Bearer ${auth}`)
  }
}

export default request