import axios, {
  type AxiosInstance,
  type AxiosRequestConfig,
  type AxiosResponse,
  type InternalAxiosRequestConfig
} from 'axios'
import { getToken, setToken, clearToken } from '@sca/utils'
import type { R } from '@sca/types'

/** 业务成功状态码（与后端 R<T> 契约对齐） */
const SUCCESS_CODE = 200

/** 登录态失效类业务码：命中说明会话不可用，需清理登录态并跳转登录页 */
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

/**
 * 请求配置扩展项（透传给 axios 原配置）。
 * @param skipErrorHandler 为 true 时跳过统一错误拦截，由调用方自行处理（如登录接口密码错误）
 * @param skipAuth 为 true 时请求不携带 Token（公开接口）
 */
export interface RequestConfig extends AxiosRequestConfig {
  skipErrorHandler?: boolean
  skipAuth?: boolean
}

/** 全局唯一 axios 实例：baseURL 走环境变量，时间戳接口会超时中断重试 */
const instance: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' }
})

// 请求拦截器：统一注入鉴权令牌与链路追踪 ID，避免每个 API 重复拼装头
instance.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = getToken()
  if (token && !(config as RequestConfig).skipAuth) {
    config.headers.Authorization = token
  }
  // X-Trace-Id 由前端生成并透传，网关与下游服务据此串联全链路日志
  if (!config.headers['X-Trace-Id']) {
    config.headers['X-Trace-Id'] = crypto.randomUUID()
  }
  return config
})

instance.interceptors.response.use(
  (response: AxiosResponse<R<unknown>>) => {
    const r = response.data
    // 非 R<T> 包装（如文件流、无 code 字段）直接放行，由调用方处理
    if (!r || typeof r.code !== 'number') {
      return response
    }
    // 业务成功：HTTP 2xx 且业务 code 为 200，直接返回原始响应
    if (response.status >= 200 && response.status < 300 && r.code === SUCCESS_CODE) {
      return response
    }
    // 业务失败（HTTP 2xx 但 code 非 200）：统一提示错误信息并 reject，
    // 让调用方走 catch；skipErrorHandler 时静默，供登录等场景自行处理
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
        // 登录态失效：清空令牌并跳转登录页（带 redirect 回跳），
        // 否则后续所有请求都会因无有效会话而反复报错
        if (LOGIN_REQUIRED_CODES.includes(r.code)) {
          clearToken()
          redirectToLogin()
        }
      } else if (status === 401) {
        // HTTP 401 兜底：网关未返回业务包装时也视为会话失效
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

/** 注册全局消息提示函数（由应用层注入 Naive UI 的 message，避免包内耦合 UI 库） */
let messageFn: ((content: string, type?: 'info' | 'error' | 'warning') => void) | null = null

/**
 * 注册全局消息提示处理器。
 * @param fn 应用层注入的提示函数（如 Naive UI 的 message）
 */
export function registerMessageHandler(
  fn: (content: string, type?: 'info' | 'error' | 'warning') => void
) {
  messageFn = fn
}

/** 统一输出提示：优先走注入的 message，未注入时降级到 console */
function handleMessage(content: string, type: 'info' | 'error' | 'warning' = 'error') {
  if (messageFn) {
    messageFn(content, type)
  } else {
    console[type === 'error' ? 'error' : 'warn'](content)
  }
}

/** 业务错误提示：展示后端返回的 message（R<T> 包装下的通用失败） */
function handleBizError(r: R<unknown>) {
  handleMessage(r.message || '操作失败')
}

/** 跳转登录页：保留当前路径作为 redirect 参数，登录成功后回跳原页面 */
function redirectToLogin() {
  if (window.location.pathname !== '/login') {
    const redirect = encodeURIComponent(window.location.pathname + window.location.search)
    window.location.href = `/login?redirect=${redirect}`
  }
}

/**
 * 统一请求出口：所有方法自动解包 R<T>，直接返回 data 字段。
 * 业务层拿到的是纯数据，无需再关心 code/timestamp 等包装信息。
 */
export const request = {
  /** GET 请求，返回 data 字段 */
  get<T>(url: string, config?: RequestConfig) {
    return instance.get<R<T>>(url, config).then((res) => res.data.data as T)
  },
  /** POST 请求，返回 data 字段 */
  post<T>(url: string, data?: unknown, config?: RequestConfig) {
    return instance.post<R<T>>(url, data, config).then((res) => res.data.data as T)
  },
  /** PUT 全量更新，返回 data 字段 */
  put<T>(url: string, data?: unknown, config?: RequestConfig) {
    return instance.put<R<T>>(url, data, config).then((res) => res.data.data as T)
  },
  /** PATCH 部分更新，返回 data 字段 */
  patch<T>(url: string, data?: unknown, config?: RequestConfig) {
    return instance.patch<R<T>>(url, data, config).then((res) => res.data.data as T)
  },
  /** DELETE 请求，返回 data 字段 */
  delete<T>(url: string, config?: RequestConfig) {
    return instance.delete<R<T>>(url, config).then((res) => res.data.data as T)
  },
  /** 暴露原始 axios 实例（测试与文件下载等特殊场景使用） */
  raw() {
    return instance
  }
}

/**
 * 从响应头提取 authorization 并写入本地存储。
 * @param response axios 原始响应（用于登录态刷新等场景）
 */
export function storeTokenFromHeader(response: AxiosResponse) {
  const auth = response.headers['authorization'] as string | undefined
  if (auth) {
    setToken(auth.startsWith('Bearer ') ? auth : `Bearer ${auth}`)
  }
}

export default request