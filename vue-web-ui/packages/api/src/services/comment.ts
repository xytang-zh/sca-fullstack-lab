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

/**
 * 文章评论分页列表（游客可访问，仅返回已审核评论）。
 * @param articleId 文章 ID
 * @param query 分页参数
 */
export function pageComments(articleId: string, query: CommentPageQuery) {
  return request.get<PageResult<CommentVO>>(`/api/comments/articles/${articleId}`, {
    params: query
  })
}

/**
 * 发表评论（需登录，评论进入审核队列）。
 * @param dto 评论内容与昵称快照
 */
export function createComment(dto: CommentCreateDTO) {
  return request.post<CommentVO>('/api/comments', dto)
}

/**
 * 回复评论（需登录）。
 * @param id 被回复的评论 ID
 * @param dto 回复内容（含 parentId 与 replyTo 昵称）
 */
export function replyComment(id: string, dto: CommentReplyDTO) {
  return request.post<CommentVO>(`/api/comments/${id}/reply`, dto)
}

/**
 * 评论点赞/取消（需登录，幂等）。
 * @param id 评论 ID
 * @returns true=已点赞 false=已取消点赞
 */
export function toggleCommentLike(id: string) {
  return request.post<boolean>(`/api/comments/${id}/like`)
}

/**
 * 我的评论（需登录，按用户隔离）。
 * @param page 页码
 * @param size 每页条数
 */
export function myComments(page = 1, size = 10) {
  return request.get<PageResult<CommentMyVO>>('/api/comments/my', { params: { page, size } })
}

/**
 * 待审核评论列表（管理员）。
 * @param page 页码
 * @param size 每页条数
 */
export function pendingComments(page = 1, size = 10) {
  return request.get<PageResult<CommentVO>>('/api/comments/pending', { params: { page, size } })
}

/**
 * 审核评论（管理员，2=通过 3=驳回）。
 * @param id 评论 ID
 * @param dto 审核结论
 */
export function auditComment(id: string, dto: CommentAuditDTO) {
  return request.post<void>(`/api/comments/${id}/audit`, dto)
}