## Context

现状（见 proposal.md - Why）：`ArticleDetail.vue` 为极简单列布局，`<pre>` 原样输出 Markdown 源码，无目录导航、无评论区、无 Markdown 渲染能力。

关键约束与现状：
- portal 无任何 markdown 渲染依赖；`DefaultLayout.vue` 顶栏已具备用户头像/登录/下拉菜单能力（可复用），但 header 内容容器为 `max-w-5xl`（详情页三栏需 1440px 级宽度）
- `@sca/api` 仅有 article 模块（列表/详情/点赞/收藏），无评论 API 封装
- `packages/types/src/blog.ts` 有 `ArticleDetailVO`，无评论类型
- 规范红线：ID 必须 `string`、禁止直接 `import axios`、UnoCSS 原子类、XSS 双重防护、`<script setup>`

## Goals / Non-Goals

**Goals:**
- 详情页三栏布局（顶栏 / 左目录 1:2:1 / 右评论），桌面端 + 窄屏降级
- Markdown 渲染链路完整落地：markdown-it 渲染 → DOMPurify 白名单 → 代码高亮
- 目录导航：标题自动提取、点击滚动、滚动高亮、折叠开关
- 评论全交互：列表/发表/回复/点赞/未登录引导
- 视觉设计有辨识度：不落"模板化博客站"俗套（详见 Decisions 视觉决策）

**Non-Goals:**
- 不改后端接口契约与后端代码
- 不做文章编辑/发布侧改动
- 不引入富文本编辑器（md-editor-v3 等）
- 不做服务端目录生成（目录纯客户端解析）

## Decisions

### 1. Markdown 渲染栈：markdown-it + highlight.js + DOMPurify

选择 `markdown-it`（渲染）+ `highlight.js`（代码高亮）+ `dompurify`（XSS 白名单），三者为各自领域事实标准、体积可控、支持自定义渲染规则。

- **替代方案 A：`md-editor-v3` / `@kangc/v-md-editor`** —— 完整编辑器组件，自带工具栏/编辑模式，本项目只需要只读预览，引入过重 → 拒绝
- **替代方案 B：`marked`** —— 更轻但插件生态与自定义能力弱于 markdown-it（目录注入锚点 id 需要自定义渲染规则）→ 拒绝

渲染链路（单向，禁止混合）：
```
contentMd ──► markdown-it（自定义 h2/h3 规则注入 id + 收集标题树）
          ──► highlight.js（代码块高亮，先于 sanitize）
          ──► DOMPurify.sanitize（白名单过滤，XSS 最后防线）
          ──► v-html 注入
```
XSS 双保险：markdown-it 关闭原始 HTML 直通（`html: false`）+ DOMPurify 白名单兜底，链接 `href` 过滤 `javascript:` 等危险协议。

### 2. 目录导航：渲染期提取 + IntersectionObserver 高亮

标题锚点 id 与目录树在 markdown-it 渲染单遍中同时生成（自定义 `heading_open` 规则，slugify 标题 + 收集 `{level, text, id}` 层级树），保证"目录 id == 页面锚点 id"天然一致，杜绝二次解析漂移。

滚动高亮用 `IntersectionObserver` 监听各标题元素（root 为窗口，`rootMargin: -20% 0px -70% 0px` 取当前阅读章节），替代手动 scroll 监听——性能好且语义清晰。

### 3. 布局：CSS Grid 三栏 + 折叠过渡

```
┌──────────────────────────────────────────────┐
│ header (sticky, 毛玻璃)  Logo   面包屑   头像 │
├───────────┬──────────────────┬───────────────┤
│ 目录导航    │ 文章内容 (2/4)     │ 评论区 (1/4)   │
│ 01 章节     │ 标题/元信息区      │ 发表评论框      │
│ 02 章节     │ Markdown 内容     │ 一级评论        │
│ 03 章节     │ 点赞/收藏/分享    │  └ 嵌套回复     │
│            │ ▣ 折叠按钮(左下)   │ 加载更多        │
├───────────┴──────────────────┴───────────────┤
│ footer                                      │
└──────────────────────────────────────────────┘
```

- 外层容器 `max-w-[1440px] mx-auto`，`grid grid-cols-[1fr_2fr_1fr]`（UnoCSS）
- 左栏 sticky + 独立滚动（`n-scrollbar` 适配目录高度）；中/右随页面滚动
- **折叠交互**：≥1024px 时 grid 列 `1fr→0fr` 过渡（`transition-[grid-template-columns]`，内层宽度动画）；<1024px 时左栏改为绝对定位覆盖层（transform 移出），与 spec"窄屏默认隐藏"对齐
- 折叠按钮：内容区左下角圆形悬浮按钮（teal 主色），图标随状态切换
- header 容器放宽为 `max-w-[1440px]` 与详情页对齐（首页 header 留白略增，可接受）

### 4. 组件拆分

| 组件 | 职责 |
|------|------|
| `ArticleMarkdown.vue` | 渲染链路封装，props: `contentMd`；emit: `toc-updated`（标题树） |
| `TocNav.vue` | 目录树渲染、点击 `scrollIntoView({behavior:'smooth'})`、activeId 高亮（props: toc/activeId） |
| `CommentPanel.vue` | 评论列表（嵌套回复缩进）、发表/回复输入框、点赞、分页"加载更多"、未登录引导 |
| `useArticleToc`（composable） | 标题树提取 + IntersectionObserver 高亮逻辑（供 ArticleDetail 编排） |

`ArticleDetail.vue` 负责编排：并行加载文章详情 + 评论首页，组合上述组件。顶栏复用 `DefaultLayout` 现有实现，不改动用户区逻辑。

### 5. 评论 API 与类型

新增 `packages/api/src/services/comment.ts`（对齐 CLAUDE.md 契约 `/api/comments/*`）：
- `GET /api/comments/articles/{articleId}` 分页列表（游客可访问，仅已审核）
- `POST /api/comments` 发表（登录）
- `POST /api/comments/{id}/reply` 回复（登录）
- `POST /api/comments/{id}/like` 点赞/取消（登录，幂等）

`packages/types/src/blog.ts` 新增 `CommentVO`（id/nickname/avatar/content/createdTime/likeCount/parentId/replyTo，ID 一律 `string`）。

### 6. 视觉设计（设计 token）

以"编辑器阅读"为设计母题，区别于模板化博客站（CSDN 红白、知乎蓝白均不取）：

| Token | 值 | 落地（UnoCSS） |
|-------|----|--------------|
| 纸感背景 | `#FAFAF9`（暖白，阅读友好） | `bg-zinc-50` 系列或主题扩展 |
| 主文字 | `#1F2329` | `text-zinc-800` |
| 次要文字 | `#6B7280` | `text-zinc-500` |
| 主色（编辑器青） | `#0D9488` | `teal-600` |
| 代码块背景 | `#1E1E2E`（深墨蓝黑，VS Code 质感） | 主题扩展 |
| 链接/行内代码 | 蓝 `#2563EB` / 青底 `teal-50` | `blue-600` / `bg-teal-50` |

- 字体：正文系统无衬线栈（`PingFang SC`/`Microsoft YaHei`），**等宽栈**（`ui-monospace, JetBrains Mono, Consolas`）用于代码、目录序号、元信息——编辑器的原生语言
- **签名元素**：左侧目录导航采用"代码大纲"风格——章节真实有序，以等宽序号 `01/02/03…` 标注（信息有意义而非装饰），VS Code 式缩进引导线，当前章节 teal 高亮竖条 + 序号变色
- 卡片化内容区：圆角 `rounded-lg`、细边框 `border-zinc-200`、柔和阴影 `shadow-sm`；顶栏毛玻璃 `backdrop-blur`
- 元信息区：等宽字体的小号标签（阅读量/发布时间/作者），配分割线，克制的留白
- 动效克制：折叠过渡 200ms、目录高亮颜色过渡，无多余入场动画

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| XSS：内容或链接注入脚本 | 双重防线（markdown-it `html:false` + DOMPurify 白名单 + 危险协议过滤），spec 场景覆盖验证 |
| highlight.js 全量引入体积大 | 按需注册常用语言（js/ts/java/xml/sql/bash 等），`cdn` 构建分块 |
| 评论接口契约与后端实现有出入（后端 comment 服务未最终实现） | 按 CLAUDE.md 规划契约封装；实现时若字段差异，以实际后端为准微调类型 |
| 滚动高亮在页面级滚动 + sticky 左栏下锚点定位偏移 | 滚动锚定用 `scroll-margin-top` 补偿 sticky 顶栏高度 |
| 窄屏折叠状态与 grid 过渡叠加复杂 | 两套实现隔离：≥1024 grid 过渡 / <1024 overlay，由同一开关状态驱动 |

## Migration Plan

纯前端改动，无数据迁移：
1. 依赖安装（portal package.json：markdown-it、highlight.js、dompurify + 对应 @types）
2. 类型与 API 先行（types → services → 组件）
3. 组件独立开发（Markdown 渲染 → 目录 → 评论 → 详情页编排）
4. 验证：`pnpm typecheck`、`pnpm lint`、`pnpm build` + 浏览器实测
- 回滚：还原 `ArticleDetail.vue` 与 package.json 即可，无副作用

## Open Questions

（无 —— 契约与实现路径均已确定）
