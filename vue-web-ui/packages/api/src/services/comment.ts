import { request } from '../index'
import type {
  CommentAuditDTO,
  CommentCreateDTO,
  CommentMyVO,
  CommentPageQuery,
  CommentReplyDTO,
  CommentVO
} from '@sca/types'
import type { PageResult } from '@sca/types'

/** 文章评论分页列表（游客可访问，仅已审核） */
export function pageComments(articleId: string, query: CommentPageQuery) {
  return request.get<PageResult<CommentVO>>(`/api/comments/articles/${articleId}`, {
    params: query
  })
}

/** 发表评论（需登录） */
export function createComment(dto: CommentCreateDTO) {
  return request.post<CommentVO>('/api/comments', dto)
}

/** 回复评论（需登录） */
export function replyComment(id: string, dto: CommentReplyDTO) {
  return request.post<CommentVO>(`/api/comments/${id}/reply`, dto)
}

/** 评论点赞/取消（需登录，幂等） */
export function toggleCommentLike(id: string) {
  return request.post<boolean>(`/api/comments/${id}/like`)
}

/** 我的评论（需登录，按用户隔离） */
export function myComments(page = 1, size = 10) {
  return request.get<PageResult<CommentMyVO>>('/api/comments/my', { params: { page, size } })
}

/** 待审核评论列表（管理员） */
export function pendingComments(page = 1, size = 10) {
  return request.get<PageResult<CommentVO>>('/api/comments/pending', { params: { page, size } })
}

/** 审核评论（管理员，2=通过 3=驳回） */
export function auditComment(id: string, dto: CommentAuditDTO) {
  return request.post<void>(`/api/comments/${id}/audit`, dto)
}