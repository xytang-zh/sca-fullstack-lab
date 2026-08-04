/**
 * Vite 环境变量类型声明（供 import.meta.env 获得类型提示）。
 * @description 所有环境变量均为可选，运行时由 .env 文件注入；禁止在代码中硬编码 URL。
 */
interface ImportMetaEnv {
  /** 后端网关地址（dev 走 vite 代理时可为空） */
  readonly VITE_API_BASE_URL?: string
  /** SSO 认证服务器地址（预留，未启用时可为空） */
  readonly VITE_SSO_SERVER_URL?: string
  /** WebSocket 服务地址（预留） */
  readonly VITE_WS_BASE_URL?: string
  /** 应用标题 */
  readonly VITE_APP_TITLE?: string
  /** 应用版本号 */
  readonly VITE_APP_VERSION?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
