export interface ArticlePageQuery {
  pageNum: number
  pageSize: number
  /** time=按发布时间倒序，hot=按热度降序 */
  sort?: 'time' | 'hot'
}

export interface ArticleVO {
  id: string
  title: string
  summary?: string
  coverImage?: string
  authorId: string
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
}
