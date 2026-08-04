export interface ArticlePageQuery {
  page: number
  size: number
  /** time=按发布时间倒序，hot=按热度降序 */
  sort?: 'time' | 'hot'
  /** 作者 ID 过滤（逗号分隔，用于关注 Feed） */
  authorIds?: string
}

export interface ArticleVO {
  id: string
  title: string
  summary?: string
  coverImage?: string
  authorId: string
  columnId?: string
  /** 1=草稿 2=待审核 3=已发布 4=已驳回 */
  status?: number
  views: number
  likes: number
  favorites: number
  comments: number
  publishTime?: string
}

export interface ArticleDetailVO {
  id: string
  title: string
  summary?: string
  contentMd: string
  coverImage?: string
  authorId: string
  columnId?: string
  views: number
  likes: number
  favorites: number
  comments: number
  publishTime?: string
}

export interface ArticleCreateDTO {
  title: string
  summary?: string
  contentMd: string
  slug?: string
  coverImage?: string
  columnId?: string
  /** 1=草稿 3=发布（默认发布） */
  status?: 1 | 3
}

export interface ColumnVO {
  id: string
  userId: string
  name: string
  description?: string
  coverImage?: string
  articleCount: number
  subscribeCount: number
  subscribed?: boolean
  createTime?: string
}

export interface ColumnCreateDTO {
  name: string
  description?: string
  coverImage?: string
}

export interface CommentPageQuery {
  page: number
  size: number
}

export interface CommentVO {
  id: string
  articleId: string
  parentId?: string
  /** 被回复的评论作者昵称（仅回复场景） */
  replyTo?: string
  nickname: string
  avatar?: string
  content: string
  likeCount: number
  liked?: boolean
  createdAt: string
}

export interface CommentCreateDTO {
  articleId: string
  articleTitle?: string
  content: string
  nickname?: string
  avatar?: string
}

export interface CommentReplyDTO {
  articleId: string
  articleTitle?: string
  parentId: string
  replyTo?: string
  content: string
  nickname?: string
  avatar?: string
}

export interface CommentAuditDTO {
  /** 2=通过 3=驳回 */
  status: 2 | 3
}

export interface CommentMyVO {
  id: string
  articleId: string
  articleTitle: string
  content: string
  likeCount: number
  /** 1=待审核 2=已审核 3=已驳回 */
  status: number
  createdAt: string
}
