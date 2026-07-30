export * from './auth'
export * from './system'

export interface R<T = unknown> {
  code: number
  bizCode: string
  message: string
  data: T | null
  timestamp: string
  traceId?: string
  path?: string
  devMessage?: string
}

export interface PageVO<T = unknown> {
  list: T[]
  total: number
  pageNum: number
  pageSize: number
  pages: number
}

export interface PageQuery {
  pageNum: number
  pageSize: number
  orderBy?: string
  keyword?: string
}
