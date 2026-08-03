## Why

当前 portal 博客详情页（`ArticleDetail.vue`）为极简单列布局：`<pre>` 原样输出 Markdown 源码、无目录导航、无评论区，与市面上主流博客站（CSDN、知乎、掘金）的阅读体验差距明显。用户从列表页进入详情后无法快速定位章节、无法参与讨论，影响博客前台的核心体验。

## What Changes

- 将详情页重构为**三栏布局**：顶部顶栏（右侧用户头像区）、左侧 1/4 目录导航、中间 2/4 Markdown 渲染内容、右侧 1/4 评论区
- 引入 Markdown 渲染组件：支持标题/代码块高亮/表格/图片等标准语法，渲染前做 XSS 白名单过滤（DOMPurify），满足"前后端双重防护"红线
- 新增**目录导航**：从 Markdown 的 h2/h3 标题自动提取 TOC，点击平滑滚动到对应章节，滚动时高亮当前章节
- 新增**导航栏折叠按钮**：位于中间内容区左下角（悬浮），可显示/隐藏左侧导航栏
- 新增**评论区**：评论列表（嵌套回复二级缩进）、发表评论、回复评论、评论点赞，未登录操作引导跳登录页
- 顶栏用户头像：登录后显示头像与下拉菜单，未登录显示登录入口
- 响应式适配：窄屏（<1024px）默认折叠左栏，小屏（<768px）右侧评论堆叠到内容下方

## Capabilities

### New Capabilities

- `blog-detail-page`: 博客详情页的展示与交互能力 —— 三栏布局、Markdown 渲染（XSS 防护）、目录导航与滚动联动、导航栏折叠、评论区交互、顶栏用户区

### Modified Capabilities

（无 —— blog-domain 与 public-browse 的后端行为需求不变，本变更仅实现前端呈现）

## Impact

- **前端（主要）**
  - `vue-web-ui/apps/portal/`：`src/views/ArticleDetail.vue` 重构；新增 `src/components/ArticleMarkdown.vue`（Markdown 渲染）、`src/components/TocNav.vue`（目录导航）、`src/components/CommentPanel.vue`（评论区）等组件；顶栏布局复用/改造 `src/layouts/DefaultLayout.vue`
  - `vue-web-ui/apps/portal/package.json`：新增依赖 `markdown-it`、`highlight.js`、`dompurify`（含类型包）
  - `vue-web-ui/packages/api/src/services/`：新增 `comment.ts` 评论 API 封装（列表/发表/回复/点赞，路径对齐 `/api/comments/*` 契约）
  - `vue-web-ui/packages/types/src/`：`blog.ts` 新增 `CommentVO`、评论分页/发表参数类型
- **后端**：无代码变更（评论接口按 spring-cloud-comment 既有规划对接）
- **非目标（Non-goals）**：不改动文章编辑/发布流程；不引入富文本编辑器；不做服务端目录生成；不改后端接口契约
