import { request } from '../index'
import type {
  ArticleCreateDTO,
  ArticleDetailVO,
  ArticlePageQuery,
  ArticleVO,
  ColumnCreateDTO,
  ColumnVO
} from '@sca/types'
import type { PageVO } from '@sca/types'

/** 文章分页列表（游客可访问，sort=time|hot） */
export function pageArticles(query: ArticlePageQuery) {
  return request.get<PageVO<ArticleVO>>('/api/article/articles', { params: query })
}

/** 文章详情（游客可访问） */
export function getArticle(id: string) {
  return request.get<ArticleDetailVO>(`/api/article/articles/${id}`)
}

/** 获取文章用于编辑（仅作者，含草稿/待审核） */
export function getArticleForEdit(id: string) {
  return request.get<ArticleDetailVO>(`/api/article/articles/my/${id}`)
}

/** 发布文章（需登录，status=1草稿/3发布） */
export function createArticle(dto: ArticleCreateDTO) {
  return request.post<ArticleVO>('/api/article/articles', dto)
}

/** 更新文章（仅作者） */
export function updateArticle(id: string, dto: ArticleCreateDTO) {
  return request.put<ArticleVO>(`/api/article/articles/${id}`, dto)
}

/** 删除文章（仅作者，软删除） */
export function deleteArticle(id: string) {
  return request.delete<void>(`/api/article/articles/${id}`)
}

/** 点赞/取消（需登录，幂等） */
export function toggleLike(id: string) {
  return request.post<boolean>(`/api/article/articles/${id}/like`)
}

/** 收藏/取消（需登录，幂等） */
export function toggleFavorite(id: string) {
  return request.post<boolean>(`/api/article/articles/${id}/favorite`)
}

/** 我的已发布文章（需登录） */
export function myArticles(pageNum = 1, pageSize = 10) {
  return request.get<PageVO<ArticleVO>>('/api/article/articles/my', { params: { pageNum, pageSize } })
}

/** 我的草稿（需登录） */
export function myDrafts(pageNum = 1, pageSize = 10) {
  return request.get<PageVO<ArticleVO>>('/api/article/articles/my/drafts', {
    params: { pageNum, pageSize }
  })
}

/** 我点赞的文章（需登录） */
export function myLikes(pageNum = 1, pageSize = 10) {
  return request.get<PageVO<ArticleVO>>('/api/article/articles/my/likes', {
    params: { pageNum, pageSize }
  })
}

/** 我收藏的文章（需登录） */
export function myFavorites(pageNum = 1, pageSize = 10) {
  return request.get<PageVO<ArticleVO>>('/api/article/articles/my/favorites', {
    params: { pageNum, pageSize }
  })
}

/** 专栏分页列表（游客可访问，?userId= 按作者过滤） */
export function pageColumns(params: { userId?: string; pageNum?: number; pageSize?: number }) {
  return request.get<PageVO<ColumnVO>>('/api/article/columns', { params })
}

/** 我的专栏（需登录） */
export function myColumns(pageNum = 1, pageSize = 10) {
  return request.get<PageVO<ColumnVO>>('/api/article/columns/my', { params: { pageNum, pageSize } })
}

/** 我订阅的专栏（需登录） */
export function myColumnSubscriptions(pageNum = 1, pageSize = 10) {
  return request.get<PageVO<ColumnVO>>('/api/article/columns/my/subscriptions', {
    params: { pageNum, pageSize }
  })
}

/** 创建专栏（需登录） */
export function createColumn(dto: ColumnCreateDTO) {
  return request.post<ColumnVO>('/api/article/columns', dto)
}

/** 编辑专栏（仅作者） */
export function updateColumn(id: string, dto: ColumnCreateDTO) {
  return request.put<void>(`/api/article/columns/${id}`, dto)
}

/** 删除专栏（仅作者） */
export function deleteColumn(id: string) {
  return request.delete<void>(`/api/article/columns/${id}`)
}

/** 订阅/取消订阅专栏（需登录，幂等） */
export function toggleColumnSubscribe(id: string) {
  return request.post<boolean>(`/api/article/columns/${id}/subscribe`)
}

/** 文章统计（管理员） */
export function articleStats() {
  return request.get<{
    totalArticles: number
    publishedArticles: number
    pendingArticles: number
    draftArticles: number
    totalLikes: number
    totalFavorites: number
  }>('/api/article/articles/stats')
}

/** 待审核文章列表（管理员） */
export function pendingArticles(pageNum = 1, pageSize = 10) {
  return request.get<PageVO<ArticleVO>>('/api/article/articles/pending', {
    params: { pageNum, pageSize }
  })
}

/** 审核文章（管理员，3=通过 4=驳回） */
export function auditArticle(id: string, status: 3 | 4) {
  return request.post<void>(`/api/article/articles/${id}/audit`, null, {
    params: { status }
  })
}