/**
 * @sca/api 包统一出口：全项目唯一的 HTTP 访问入口。
 * @description 应用层禁止直接 import axios，统一从本包获取 request 实例与各服务 API 模块。
 */
export { default as request, registerMessageHandler, storeTokenFromHeader } from './request'
export type { RequestConfig } from './request'
export * as authApi from './services/auth'
export * as articleApi from './services/article'
export * as commentApi from './services/comment'
export * as userApi from './services/user'
