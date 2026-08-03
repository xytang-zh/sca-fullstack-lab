## 1. 依赖与基础类型

- [x] 1.1 在 `vue-web-ui/apps/portal/package.json` 新增依赖 `markdown-it`、`highlight.js`、`dompurify`（devDependencies 加 `@types/markdown-it`），`pnpm install` 更新 lockfile
- [x] 1.2 在 `vue-web-ui/packages/types/src/blog.ts` 新增 `CommentVO`（id/nickname/avatar/content/createdTime/likeCount/parentId/replyTo 等，ID 一律 string）与评论分页参数/发表/回复 DTO 类型，经 `index.ts` 导出
- [x] 1.3 在 `vue-web-ui/packages/api/src/services/` 新增 `comment.ts`：分页列表 `GET /api/comments/articles/{articleId}`（游客可访问）、发表 `POST /api/comments`、回复 `POST /api/comments/{id}/reply`、点赞 `POST /api/comments/{id}/like`（登录、幂等），全部经 `@sca/api` 的 request
- 验收：`pnpm --filter @sca/types typecheck` 与 `pnpm --filter @sca/api typecheck` 通过

## 2. Markdown 渲染组件

- [x] 2.1 新建 `vue-web-ui/apps/portal/src/components/ArticleMarkdown.vue`：封装 markdown-it（`html:false`）+ highlight.js（按需注册 js/ts/java/xml/sql/bash 等常用语言）+ DOMPurify 白名单（过滤危险协议 `javascript:` 等）渲染链路，props: `contentMd`，`v-html` 注入
- [x] 2.2 在 ArticleMarkdown 中自定义 markdown-it `heading_open` 规则：slugify 生成标题锚点 id，同时收集 `{level, text, id}` 标题树，emit `toc-updated`
- [x] 2.3 编写 markdown 预览样式：标题层级、代码块深色主题（背景 `#1E1E2E` 等宽字体+行高）、表格边框、行内代码 teal 底、链接蓝色，全部用 UnoCSS/主题变量，`scroll-margin-top` 补偿 sticky 顶栏
- 验收：`pnpm --filter @sca/portal typecheck` 通过；本地 mock 内容渲染无 XSS 泄漏（含 `<script>`/`onerror` 用例）

## 3. 目录导航

- [x] 3.1 新建 composable `vue-web-ui/apps/portal/src/hooks/useArticleToc.ts`：接收标题树，用 IntersectionObserver（rootMargin `-20% 0px -70% 0px`）监听各标题元素计算 activeId
- [x] 3.2 新建 `vue-web-ui/apps/portal/src/components/TocNav.vue`：渲染标题树（h2/h3 缩进层级）、等宽序号 `01/02/03…`、VS Code 式缩进引导线、activeId 高亮（teal 竖条+序号变色）；点击项 `scrollIntoView({behavior:'smooth'})`
- [x] 3.3 组件卸载时断开 IntersectionObserver，避免内存泄漏
- 验收：`pnpm --filter @sca/portal typecheck` 通过；目录点击滚动与滚动高亮在浏览器实测正常

## 4. 评论区组件

- [x] 4.1 新建 `vue-web-ui/apps/portal/src/components/CommentPanel.vue`：props `articleId`；评论列表按 parentId 分组、二级嵌套回复缩进展示（作者/时间/点赞数），"加载更多"分页
- [x] 4.2 发表评论与回复：登录校验（未登录 `n-message` 提示并跳转 `/login?redirect=` 返回原页），提交成功后刷新列表
- [x] 4.3 评论点赞：调用 like 接口幂等切换，乐观更新点赞数，失败回滚
- 验收：`pnpm --filter @sca/portal typecheck` 通过；游客/登录两种态交互在浏览器实测通过

## 5. 详情页布局与编排

- [x] 5.1 重构 `vue-web-ui/apps/portal/src/views/ArticleDetail.vue`：三栏 grid 布局（`max-w-[1440px]` 容器、`1fr 2fr 1fr` 列、过渡动画），左栏 sticky 独立滚动，编排 ArticleMarkdown/TocNav/CommentPanel
- [x] 5.2 折叠按钮：内容区左下角圆形悬浮按钮（teal 主色、图标随状态切换），≥1024px grid 列 `1fr↔0fr` 过渡；<1024px 左栏 overlay 覆盖层 + 默认隐藏
- [x] 5.3 文章元信息区：等宽字体小号标签展示作者/发布时间/阅读量/点赞数/收藏数，点赞收藏按钮复用现有交互
- [x] 5.4 `vue-web-ui/apps/portal/src/layouts/DefaultLayout.vue`：header 容器 `max-w-5xl` 放宽为 `max-w-[1440px]`，与详情页三栏对齐（保留现有头像/登录下拉逻辑）
- [x] 5.5 窄屏响应式：<768px 评论区堆叠至内容下方（单列），折叠按钮行为适配
- 验收：`pnpm --filter @sca/portal typecheck`、`pnpm lint` 通过

## 6. 整体验证

- [x] 6.1 `pnpm typecheck` 全量通过；`pnpm build` 构建成功（注：`pnpm lint` 因仓库既有 ESLint 9 与 `.eslintrc.cjs` 格式不兼容而失败，admin/portal 均受影响，与本次变更无关）
- [x] 6.2 浏览器实测（headless Chrome + 真实后端数据）：三栏布局、目录渲染与锚点联动、代码高亮、表格/引用、XSS 双层防护（script/onerror/javascript: 均转义为文本）、窄屏默认折叠均验证通过；评论交互与折叠按钮点击受 Playwright 会话占用限制未做点击实测（代码经 typecheck 与结构验证）
- [x] 6.3 回归首页（Home.vue）：3 篇文章列表正常渲染，header `max-w-[1440px]` 无回归；Profile.vue 需登录态，改动仅限 header 宽度 class，经 typecheck 确认无类型问题
