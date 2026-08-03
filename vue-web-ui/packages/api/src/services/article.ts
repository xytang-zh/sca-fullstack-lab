import { request } from '../index'
import type { ArticleCreateDTO, ArticleDetailVO, ArticlePageQuery, ArticleVO } from '@sca/types'
import type { PageVO } from '@sca/types'

/** 文章分页列表（游客可访问，sort=time|hot） */
export function pageArticles(query: ArticlePageQuery) {
  return request.get<PageVO<ArticleVO>>('/api/article/articles', { params: query })
}

/** 文章详情（游客可访问） */
export function getArticle(id: string) {
  return request.get<ArticleDetailVO>(`/api/article/articles/${id}`)
}

/** 发布文章（需登录） */
export function createArticle(dto: ArticleCreateDTO) {
  return request.post<ArticleVO>('/api/article/articles', dto)
}

/** 点赞/取消（需登录，幂等） */
export function toggleLike(id: string) {
  return request.post<boolean>(`/api/article/articles/${id}/like`)
}

/** 收藏/取消（需登录，幂等） */
export function toggleFavorite(id: string) {
  return request.post<boolean>(`/api/article/articles/${id}/favorite`)
}
