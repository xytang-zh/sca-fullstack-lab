import { request } from '../index'
import type {
  ArticleCreateDTO,
  ArticleDetailVO,
  ArticlePageQuery,
  ArticleVO,
  ColumnCreateDTO,
  ColumnVO
} from '@sca/types'
import type { PageResult } from '@sca/types'

/**
 * 文章分页列表（游客可访问，sort=time|hot）。
 * @param query 分页 + 排序 + 作者过滤
 * @returns 分页文章列表
 */
export function pageArticles(query: ArticlePageQuery) {
  return request.get<PageResult<ArticleVO>>('/api/article/articles', { params: query })
}

/**
 * 文章详情（游客可访问，含 Markdown 正文）。
 * @param id 文章 ID（雪花 ID，string）
 */
export function getArticle(id: string) {
  return request.get<ArticleDetailVO>(`/api/article/articles/${id}`)
}

/**
 * 获取文章用于编辑（仅作者，含草稿/待审核）。
 * @param id 文章 ID
 */
export function getArticleForEdit(id: string) {
  return request.get<ArticleDetailVO>(`/api/article/articles/my/${id}`)
}

/**
 * 发布文章（需登录，status=1草稿/3发布）。
 * @param dto 文章内容与发布状态
 */
export function createArticle(dto: ArticleCreateDTO) {
  return request.post<ArticleVO>('/api/article/articles', dto)
}

/**
 * 更新文章（仅作者）。
 * @param id 文章 ID
 * @param dto 更新内容
 */
export function updateArticle(id: string, dto: ArticleCreateDTO) {
  return request.put<ArticleVO>(`/api/article/articles/${id}`, dto)
}

/**
 * 删除文章（仅作者，软删除）。
 * @param id 文章 ID
 */
export function deleteArticle(id: string) {
  return request.delete<void>(`/api/article/articles/${id}`)
}

/**
 * 点赞/取消（需登录，幂等）。
 * @param id 文章 ID
 * @returns true=已点赞 false=已取消点赞
 */
export function toggleLike(id: string) {
  return request.post<boolean>(`/api/article/articles/${id}/like`)
}

/**
 * 收藏/取消（需登录，幂等）。
 * @param id 文章 ID
 * @returns true=已收藏 false=已取消收藏
 */
export function toggleFavorite(id: string) {
  return request.post<boolean>(`/api/article/articles/${id}/favorite`)
}

/**
 * 我的已发布文章（需登录）。
 * @param page 页码
 * @param size 每页条数
 */
export function myArticles(page = 1, size = 10) {
  return request.get<PageResult<ArticleVO>>('/api/article/articles/my', { params: { page, size } })
}

/**
 * 我的草稿（需登录）。
 * @param page 页码
 * @param size 每页条数
 */
export function myDrafts(page = 1, size = 10) {
  return request.get<PageResult<ArticleVO>>('/api/article/articles/my/drafts', {
    params: { page, size }
  })
}

/**
 * 我点赞的文章（需登录）。
 * @param page 页码
 * @param size 每页条数
 */
export function myLikes(page = 1, size = 10) {
  return request.get<PageResult<ArticleVO>>('/api/article/articles/my/likes', {
    params: { page, size }
  })
}

/**
 * 我收藏的文章（需登录）。
 * @param page 页码
 * @param size 每页条数
 */
export function myFavorites(page = 1, size = 10) {
  return request.get<PageResult<ArticleVO>>('/api/article/articles/my/favorites', {
    params: { page, size }
  })
}

/**
 * 专栏分页列表（游客可访问，?userId= 按作者过滤）。
 * @param params 按作者/分页过滤
 */
export function pageColumns(params: { userId?: string; page?: number; size?: number }) {
  return request.get<PageResult<ColumnVO>>('/api/article/columns', { params })
}

/**
 * 我的专栏（需登录）。
 * @param page 页码
 * @param size 每页条数
 */
export function myColumns(page = 1, size = 10) {
  return request.get<PageResult<ColumnVO>>('/api/article/columns/my', { params: { page, size } })
}

/**
 * 我订阅的专栏（需登录）。
 * @param page 页码
 * @param size 每页条数
 */
export function myColumnSubscriptions(page = 1, size = 10) {
  return request.get<PageResult<ColumnVO>>('/api/article/columns/my/subscriptions', {
    params: { page, size }
  })
}

/**
 * 创建专栏（需登录）。
 * @param dto 专栏信息
 */
export function createColumn(dto: ColumnCreateDTO) {
  return request.post<ColumnVO>('/api/article/columns', dto)
}

/**
 * 编辑专栏（仅作者）。
 * @param id 专栏 ID
 * @param dto 更新内容
 */
export function updateColumn(id: string, dto: ColumnCreateDTO) {
  return request.put<void>(`/api/article/columns/${id}`, dto)
}

/**
 * 删除专栏（仅作者）。
 * @param id 专栏 ID
 */
export function deleteColumn(id: string) {
  return request.delete<void>(`/api/article/columns/${id}`)
}

/**
 * 订阅/取消订阅专栏（需登录，幂等）。
 * @param id 专栏 ID
 * @returns true=已订阅 false=已取消订阅
 */
export function toggleColumnSubscribe(id: string) {
  return request.post<boolean>(`/api/article/columns/${id}/subscribe`)
}

/** 文章统计（管理员，数据大盘用） */
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

/**
 * 待审核文章列表（管理员）。
 * @param page 页码
 * @param size 每页条数
 */
export function pendingArticles(page = 1, size = 10) {
  return request.get<PageResult<ArticleVO>>('/api/article/articles/pending', {
    params: { page, size }
  })
}

/**
 * 审核文章（管理员，3=通过 4=驳回）。
 * @param id 文章 ID
 * @param status 审核结论
 */
export function auditArticle(id: string, status: 3 | 4) {
  return request.post<void>(`/api/article/articles/${id}/audit`, null, {
    params: { status }
  })
}