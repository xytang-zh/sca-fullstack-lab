# CLAUDE.md — apps/admin 一体化管理平台

> 本文档面向 AI 编码助手，用于在 `apps/admin/` 目录下工作时提供应用约束、技术栈、功能范围与开发规范。
> 工作前**必须**先读取 [`vue-web-ui/CLAUDE.md`](../../CLAUDE.md) 与仓库根 [`sca-fullstack-lab/CLAUDE.md`](../../../CLAUDE.md) 了解全局规范。

---

## 1. 应用定位

一体化管理平台（`@sca/admin`），管理员主入口，dev 端口 5173。按个人博客需求文档承担**后台管理**（P1）：博客审核、用户管理、数据统计面板。

| 维度 | 值 |
|------|-----|
| 包名 | `@sca/admin` |
| dev 端口 | 5173（Vite） |
| 构建产物 | `dist/` |
| 技术栈 | Vue 3.5 + TypeScript + Vite 5 + Naive UI + Pinia + Vue Router + UnoCSS |

---

## 2. 核心功能

| 模块 | 功能 |
|------|------|
| 登录 | 账号密码登录（Sa-Token）、Token 续期、踢人下线提示 |
| 系统管理 | 用户 CRUD、角色/菜单分配（RBAC：USER/AUTHOR/ADMIN） |
| 博客管理 ★ | 文章审核（通过/驳回）、评论审核、博客数据统计（ECharts 面板） |
| 服务器监控 | 实时大盘（WebSocket `/ws/monitor/{userId}`）、历史曲线、告警列表 |
| 消息中心 | 站内信、未读数（WebSocket `/ws/messages/{userId}`）、在线客服 |
| 文件管理 | 分片上传、断点续传、预览（图片/PDF/Office via KKFileView） |
| 日志查询 | 操作日志、登录日志、审计日志 |
| 定时任务 | XXL-JOB Admin 跳转（iframe） |

---

## 3. 对接的后端服务

| 前端模块 | 后端服务 | API 前缀 |
|----------|---------|----------|
| 登录 | spring-cloud-auth | `/api/auth/*` |
| 系统管理 | spring-cloud-system | `/api/system/*` |
| 博客管理 ★ | spring-cloud-article / spring-cloud-comment | `/api/articles/*`、`/api/comments/*` |
| 监控 | spring-cloud-monitor | `/api/monitor/*`、WS `/ws/monitor/*` |
| 消息 | spring-cloud-message | `/api/messages/*`、WS `/ws/messages/*` |
| 文件 | spring-cloud-file | `/api/files/*` |
| 日志 | spring-cloud-log | `/api/logs/*` |

---

## 4. 技术栈（版本以 vue-web-ui 根 package.json 为准）

Vue 3.5、TypeScript 5.5、Vite 5.4、Naive UI 2.39、Pinia 2.2、Vue Router 4.4、axios（经 `@sca/api`）、ECharts 5.5 + vue-echarts 7、UnoCSS 0.62、@vueuse/core 11

---

## 5. 开发规范（本应用特有）

1. **强制** `<script setup>`，**禁止** Options API
2. **强制** Setup Store 风格 Pinia
3. HTTP 调用**必须**经 `@sca/api` 的 `request`，**禁止**直接 `import axios`
4. 所有页面组件**必须**懒加载 `() => import('...')`
5. 博客审核页**必须**展示审核原因（驳回时必填）
6. 统计面板用 ECharts，数据来自 `/api/articles/stats` 等聚合接口
7. 组件卸载**必须**清理 WebSocket / EventListener
8. 路由菜单由后端 `/api/system/menus/routes` 动态注入

---

## 6. 红线（违反即拒绝）

1. ❌ 直接 `import axios`（必须 `@sca/api`）
2. ❌ Options API
3. ❌ `number` 类型接收 ID（必须 `string`）
4. ❌ 硬编码 API/SSO 地址（必须环境变量 `VITE_*`）
5. ❌ 组件卸载不清理 WebSocket / EventListener
6. ❌ 用 setTimeout 轮询代替 WebSocket
