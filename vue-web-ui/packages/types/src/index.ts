/**
 * 全局类型入口：聚合各服务类型并定义跨项目通用契约。
 * @description 后端统一响应 R<T> 与分页 PageResult<T> 是前后端契约的基线，
 * 所有 API 出参类型都基于二者派生。
 */
export * from './auth'
export * from './blog'
export * from './system'

/** 后端统一响应包装（前后端契约核心） */
export interface R<T = unknown> {
  /** 业务状态码：200 成功 / 1xxxx 参数 / 2xxxx 权限 / 3xxxx 业务 / 4xxxx 第三方 / 5xxxx 系统 */
  code: number
  /** 提示信息（失败时前端直接展示） */
  message: string
  /** 业务数据（code 非 200 时为 null） */
  data: T | null
  /** 服务端时间戳（毫秒） */
  timestamp: number
  /** 全链路追踪 ID（网关生成并透传） */
  traceId?: string
}

/** 分页响应包装（后端统一分页出参结构） */
export interface PageResult<T = unknown> {
  /** 当前页数据列表 */
  records: T[]
  /** 总条数 */
  total: number
  /** 当前页码（从 1 开始） */
  page: number
  /** 每页条数 */
  size: number
  /** 总页数 */
  pages: number
  /** 是否有上一页 */
  hasPrevious: boolean
  /** 是否有下一页 */
  hasNext: boolean
}

/** 通用分页查询入参（各业务分页查询可继承扩展） */
export interface PageQuery {
  /** 当前页码（从 1 开始） */
  page: number
  /** 每页条数 */
  size: number
  /** 排序字段（如 create_time_desc） */
  orderBy?: string
  /** 关键字模糊搜索 */
  keyword?: string
}
