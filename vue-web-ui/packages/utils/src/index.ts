/**
 * @sca/utils 认证模块：Token 的唯一存取入口。
 * @description 应用层禁止直接操作 localStorage，统一经本模块读写，
 * 保证后续切换存储介质（如 cookie/内存）时无需改动业务代码。
 */

/** 访问令牌在 localStorage 中的存储键 */
const TOKEN_KEY = 'access_token'
/** 刷新令牌在 localStorage 中的存储键 */
const REFRESH_TOKEN_KEY = 'refresh_token'

/**
 * 读取访问令牌。
 * @returns 令牌字符串（未登录时返回空串，调用方据此判断登录态）
 */
export function getToken(): string {
  return localStorage.getItem(TOKEN_KEY) ?? ''
}

/**
 * 写入访问令牌。
 * @param token 完整令牌（含 Bearer 前缀）
 */
export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

/**
 * 读取刷新令牌。
 * @returns 刷新令牌字符串
 */
export function getRefreshToken(): string {
  return localStorage.getItem(REFRESH_TOKEN_KEY) ?? ''
}

/**
 * 写入刷新令牌。
 * @param token 刷新令牌
 */
export function setRefreshToken(token: string): void {
  localStorage.setItem(REFRESH_TOKEN_KEY, token)
}

/** 清理全部登录态（访问令牌 + 刷新令牌），登出与登录态失效时调用 */
export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
}
