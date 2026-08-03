# CLAUDE.md — apps/portal 公开门户（博客前台）

> 本文档面向 AI 编码助手，用于在 `apps/portal/` 目录下工作时提供应用约束、技术栈、功能范围与开发规范。
> 工作前**必须**先读取 [`vue-web-ui/CLAUDE.md`](../../CLAUDE.md) 与仓库根 [`sca-fullstack-lab/CLAUDE.md`](../../../CLAUDE.md) 了解全局规范。

---

## 1. 应用定位

公开门户 / 博客前台（`@sca/portal`），dev 端口 5174。按个人博客需求文档承担**博客前台**展示：文章列表/详情（Markdown 渲染）、评论、搜索、点赞收藏。

| 维度 | 值 |
|------|-----|
| 包名 | `@sca/portal` |
| dev 端口 | 5174（Vite） |
| 构建产物 | `dist/`（SSG 静态生成，SEO 友好） |
| 技术栈 | Vue 3.5 + TypeScript + Vite 5 + Naive UI + Pinia + Vue Router + UnoCSS |

---

## 2. 核心功能

| 模块 | 功能 |
|------|------|
| 博客首页 ★ | 文章列表（分页 + 分类/标签筛选）、热门文章、置顶 |
| 文章详情 ★ | Markdown 渲染、目录导航、阅读量、点赞/收藏按钮 |
| 评论 ★ | 发表评论/二级嵌套回复、评论列表（按 parent_id 分组）、评论点赞 |
| 搜索 ★ | 全文搜索（高亮显示）、搜索建议（前缀自动补全） |
| 用户中心 | 登录（跳 auth）、我的文章、我的收藏 |
| 新闻/产品 | 栏目、新闻详情、产品介绍 |
| SEO | sitemap.xml、robots.txt、Meta、JSON-LD |

---

## 3. 对接的后端服务

| 前端模块 | 后端服务 | API 前缀 |
|----------|---------|----------|
| 文章列表/详情 | spring-cloud-article / spring-cloud-portal | `/api/articles/*`、`/api/portal/*` |
| 评论 ★ | spring-cloud-comment | `/api/comments/*`、`/api/articles/{id}/comments` |
| 搜索/RSS ★ | spring-cloud-search | `/api/search/*`、`/api/rss` |
| 点赞/收藏 ★ | spring-cloud-article | `/api/articles/{id}/like`、`/favorite` |
| 登录 | spring-cloud-auth | `/api/auth/*` |
| 新闻/产品 | spring-cloud-portal | `/api/portal/*` |

---

## 4. 技术栈（版本以 vue-web-ui 根 package.json 为准）

Vue 3.5、TypeScript 5.5、Vite 5.4、Naive UI 2.39、Pinia 2.2、Vue Router 4.4、axios（经 `@sca/api`）、UnoCSS 0.62、markdown 渲染组件（计划）

---

## 5. 开发规范（本应用特有）

1. **强制** `<script setup>`，**禁止** Options API
2. HTTP 调用**必须**经 `@sca/api` 的 `request`，**禁止**直接 `import axios`
3. 页面组件**必须**懒加载
4. 文章详情页 Markdown 渲染**必须**做 XSS 防护（后端已过滤，前端渲染库仍须白名单）
5. 搜索框**必须**防抖（300ms）+ 搜索建议
6. 公开页面 SEO：Meta/JSON-LD 动态生成，sitemap 由后端 portal 服务提供
7. 未登录用户可浏览（GET），写操作（评论/点赞）跳转登录

---

## 6. 红线（违反即拒绝）

1. ❌ 直接 `import axios`（必须 `@sca/api`）
2. ❌ Options API
3. ❌ `number` 类型接收 ID（必须 `string`）
4. ❌ 硬编码 API 地址（必须环境变量 `VITE_*`）
5. ❌ Markdown 渲染不转义（XSS 风险）
6. ❌ 组件卸载不清理 WebSocket / EventListener
