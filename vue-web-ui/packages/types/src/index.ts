export * from './auth'
export * from './blog'
export * from './system'

export interface R<T = unknown> {
  code: number
  message: string
  data: T | null
  timestamp: number
  traceId?: string
}

export interface PageResult<T = unknown> {
  records: T[]
  total: number
  page: number
  size: number
  pages: number
  hasPrevious: boolean
  hasNext: boolean
}

export interface PageQuery {
  page: number
  size: number
  orderBy?: string
  keyword?: string
}
