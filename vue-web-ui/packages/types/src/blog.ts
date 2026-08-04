/** 文章分页查询入参 */
export interface ArticlePageQuery {
  /** 当前页码（从 1 开始） */
  page: number
  /** 每页条数 */
  size: number
  /** time=按发布时间倒序，hot=按热度降序 */
  sort?: 'time' | 'hot'
  /** 作者 ID 过滤（逗号分隔，用于关注 Feed） */
  authorIds?: string
}

/** 文章列表项（卡片/列表页复用） */
export interface ArticleVO {
  /** 文章 ID（雪花 ID，为 string，禁止转 number） */
  id: string
  /** 文章标题 */
  title: string
  /** 摘要（列表页展示，截断两行） */
  summary?: string
  /** 封面图 URL */
  coverImage?: string
  /** 作者 ID（雪花 ID，为 string，禁止转 number） */
  authorId: string
  /** 所属专栏 ID（雪花 ID，为 string，禁止转 number） */
  columnId?: string
  /** 1=草稿 2=待审核 3=已发布 4=已驳回 */
  status?: number
  /** 浏览量 */
  views: number
  /** 点赞数 */
  likes: number
  /** 收藏数 */
  favorites: number
  /** 评论数 */
  comments: number
  /** 发布时间 */
  publishTime?: string
}

/** 文章详情出参（含 Markdown 正文） */
export interface ArticleDetailVO {
  /** 文章 ID（雪花 ID，为 string，禁止转 number） */
  id: string
  /** 文章标题 */
  title: string
  /** 摘要 */
  summary?: string
  /** Markdown 源文本（前端渲染为 HTML） */
  contentMd: string
  /** 封面图 URL */
  coverImage?: string
  /** 作者 ID（雪花 ID，为 string，禁止转 number） */
  authorId: string
  /** 所属专栏 ID（雪花 ID，为 string，禁止转 number） */
  columnId?: string
  /** 浏览量 */
  views: number
  /** 点赞数 */
  likes: number
  /** 收藏数 */
  favorites: number
  /** 评论数 */
  comments: number
  /** 发布时间 */
  publishTime?: string
}

/** 文章创建入参 */
export interface ArticleCreateDTO {
  /** 文章标题 */
  title: string
  /** 摘要 */
  summary?: string
  /** Markdown 正文 */
  contentMd: string
  /** URL 别名（用于 SEO 语义化路径，可选） */
  slug?: string
  /** 封面图 URL */
  coverImage?: string
  /** 所属专栏 ID */
  columnId?: string
  /** 1=草稿 3=发布（默认发布） */
  status?: 1 | 3
}

/** 专栏视图对象 */
export interface ColumnVO {
  /** 专栏 ID（雪花 ID，为 string，禁止转 number） */
  id: string
  /** 创建者用户 ID（雪花 ID，为 string，禁止转 number） */
  userId: string
  /** 专栏名称 */
  name: string
  /** 专栏简介 */
  description?: string
  /** 封面图 URL */
  coverImage?: string
  /** 文章数 */
  articleCount: number
  /** 订阅数 */
  subscribeCount: number
  /** 当前用户是否已订阅 */
  subscribed?: boolean
  /** 创建时间 */
  createTime?: string
}

/** 专栏创建入参 */
export interface ColumnCreateDTO {
  /** 专栏名称 */
  name: string
  /** 专栏简介 */
  description?: string
  /** 封面图 URL */
  coverImage?: string
}

/** 评论分页查询入参 */
export interface CommentPageQuery {
  /** 当前页码（从 1 开始） */
  page: number
  /** 每页条数 */
  size: number
}

/** 评论视图对象（一级评论与回复共用） */
export interface CommentVO {
  /** 评论 ID（雪花 ID，为 string，禁止转 number） */
  id: string
  /** 所属文章 ID（雪花 ID，为 string，禁止转 number） */
  articleId: string
  /** 父评论 ID：为空表示一级评论，非空表示回复（雪花 ID，为 string，禁止转 number） */
  parentId?: string
  /** 被回复的评论作者昵称（仅回复场景） */
  replyTo?: string
  /** 评论者昵称 */
  nickname: string
  /** 评论者头像 URL */
  avatar?: string
  /** 评论内容 */
  content: string
  /** 点赞数 */
  likeCount: number
  /** 当前用户是否已点赞 */
  liked?: boolean
  /** 创建时间 */
  createdAt: string
}

/** 发表评论入参 */
export interface CommentCreateDTO {
  /** 所属文章 ID */
  articleId: string
  /** 文章标题（冗余存储，便于后台展示） */
  articleTitle?: string
  /** 评论内容 */
  content: string
  /** 评论者昵称（冗余快照，文章删除后仍可展示） */
  nickname?: string
  /** 评论者头像（冗余快照） */
  avatar?: string
}

/** 回复评论入参 */
export interface CommentReplyDTO {
  /** 所属文章 ID */
  articleId: string
  /** 文章标题（冗余存储） */
  articleTitle?: string
  /** 被回复的评论 ID */
  parentId: string
  /** 被回复的评论作者昵称（展示 "@某人" 用） */
  replyTo?: string
  /** 回复内容 */
  content: string
  /** 回复者昵称 */
  nickname?: string
  /** 回复者头像 */
  avatar?: string
}

/** 评论审核入参 */
export interface CommentAuditDTO {
  /** 2=通过 3=驳回 */
  status: 2 | 3
}

/** 我的评论出参（个人中心"我的回答"列表） */
export interface CommentMyVO {
  /** 评论 ID（雪花 ID，为 string，禁止转 number） */
  id: string
  /** 所属文章 ID（雪花 ID，为 string，禁止转 number） */
  articleId: string
  /** 所属文章标题 */
  articleTitle: string
  /** 评论内容 */
  content: string
  /** 点赞数 */
  likeCount: number
  /** 1=待审核 2=已审核 3=已驳回 */
  status: number
  /** 创建时间 */
  createdAt: string
}
