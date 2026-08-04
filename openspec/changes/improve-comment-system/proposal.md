# Proposal: 完善博客评论功能（知乎风格评论区 + 点赞/收藏图标）

## Why

博客详情页已有评论区骨架（`CommentPanel.vue` + `spring-cloud-comment` 服务），但存在明显短板：评论区交互与知乎差距大（无删除入口、点赞/回复无图标、发表后不提示审核状态）、文章点赞/收藏按钮纯文本无图标、后端评论内容未做 XSS/敏感词过滤（违反安全红线）、待审核/审核接口未按 ADMIN 鉴权、评论数与文章数据不一致。本变更补齐这些短板，让评论区达到企业级体验。

## What Changes

**后端（spring-cloud-comment）**
- 新增 `DELETE /api/comments/{id}` 删除评论（本人或管理员，软删并级联其子回复）
- 待审核/审核接口补 `@SaCheckRole("ADMIN")` 权限
- 评论内容 XSS 过滤（Jsoup）+ 敏感词过滤后存纯文本
- 评论数一致性：评论创建/审核通过/删除时同步文章评论数（实现方案待设计定稿）

**前端（vue-web-ui）**
- `CommentPanel` 重构为知乎风格评论区：顶部写评论文本域、下方评论列表（头像/昵称/时间/内容/操作栏）、二级回复缩进与 @ 人名、点赞/回复图标、发表后按审核状态提示、删除自己评论入口
- 文章详情页点赞/收藏按钮加图标（大拇指 `ThumbsUp` / 五角星 `Star`），未点击灰色、点击后黄色

## Capabilities

### New Capabilities

（无）

### Modified Capabilities

- `blog-domain`: 增强"评论管理"需求（删除评论、审核 ADMIN 权限、敏感词/XSS 过滤、评论数一致性）
- `blog-detail-page`: 增强"评论区交互"需求（知乎风格 UI、点赞/回复图标、删除入口、审核可见性提示）+ 文章点赞/收藏按钮图标

## Impact

- 后端：`spring-cloud-alibaba/spring-cloud-services/spring-cloud-comment`（Controller / Service / impl / VO / DTO）、`spring-cloud-alibaba/spring-cloud-services/spring-cloud-article`（评论数回写，如采用）、`spring-cloud-alibaba/pom.xml`（新增 sensitive-word 版本，如采用）
- 前端：`vue-web-ui/src/components/CommentPanel.vue`、`vue-web-ui/src/views/ArticleDetail.vue`、`vue-web-ui/packages/api/src/services/comment.ts`、`vue-web-ui/packages/types/src/blog.ts`
- 数据库：`t_comment` 无需改表（`status` 已含 DELETED=4）

## Non-goals

- 不做评论热评/推荐排序（保持时间序）
- 不做评论富文本（图片/表情），保持纯文本
- 不做 @ 通知站内信（MQ 事件，留待后续变更）
- 不引入三级以上无限嵌套（保持一/二级展开）
- 不调整文章列表页评论数来源（列表页仍读 `t_article.comments`）