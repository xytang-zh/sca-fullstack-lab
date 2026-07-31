# CLAUDE.md — vue-web-ui 前端 Monorepo

> 本文档面向 AI 编码助手（Claude Code / Codex / Cursor），用于在 `vue-web-ui/` 目录下工作时提供统一的工程约束、技术栈版本、应用结构与开发规范。
> 任何 AI 在本目录（或任意子应用 / 子包）下生成代码、配置、样式时，**必须**先读取本文件并严格遵守其中的规范。
> 工作前**必须**先读取仓库根 [`sca-fullstack-lab/CLAUDE.md`](../CLAUDE.md) 了解跨项目契约。

---

## 1. 项目定位

`vue-web-ui` 是 `sca-fullstack-lab` 项目的 **前端 pnpm Monorepo**，包含 2 个独立部署的 Vue 3 应用 + 4 个共享包：

- 一体化管理平台（`apps/admin`）— 管理员主入口，对接 Spring Cloud Gateway
- 公开门户（`apps/portal`）— SEO/GEO 友好的对外内容站，Vite SSG 静态生成
- 公共包（`packages/*`）— UI 二次封装、统一 API、工具函数、TS 类型

**包管理器**：pnpm 9+（强制，**禁止**使用 npm 或 yarn）
**Node 版本**：20+
**模块规范**：ESM（`"type": "module"`）

---

## 2. Monorepo 目录结构

```
vue-web-ui/
├── package.json                 monorepo 根 package
├── pnpm-workspace.yaml          workspace 声明
├── tsconfig.base.json           共享 TS 配置
├── .npmrc                       pnpm 配置
├── .eslintrc.cjs                ESLint 配置
├── .prettierrc                  Prettier 配置
├── apps/                        子应用
│   ├── admin/                   一体化管理平台
│   │   ├── package.json
│   │   ├── vite.config.ts
│   │   ├── tsconfig.json
│   │   ├── index.html
│   │   └── src/
│   │       ├── main.ts
│   │       ├── App.vue
│   │       ├── api/             接口调用（按服务分子目录）
│   │       │   ├── system/      system-service 接口
│   │       │   ├── auth/        auth-center 接口
│   │       │   ├── monitor/     monitor 接口
│   │       │   ├── workflow/    workflow 接口
│   │       │   ├── ai/          ai 接口
│   │       │   ├── message/     message 接口
│   │       │   ├── file/        file 接口
│   │       │   ├── search/      search 接口
│   │       │   └── request.ts   axios 实例 + 拦截器
│   │       ├── components/      通用组件
│   │       │   ├── business/    业务通用（用户选择器、部门树）
│   │       │   └── common/      基础通用（表格、表单、图标）
│   │       ├── layouts/         布局
│   │       │   ├── default/    默认布局（含侧栏 + 顶栏 + 多 Tab）
│   │       │   └── blank/       空白布局（登录页用）
│   │       ├── views/           页面
│   │       │   ├── login/
│   │       │   ├── system/      系统管理（用户/角色/菜单/部门/字典/参数）
│   │       │   ├── monitor/     服务器监控大盘
│   │       │   ├── workflow/    工作流（发起/待办/已办/历史）
│   │       │   ├── ai/          AI 助手（对话界面、知识库管理）
│   │       │   ├── message/     消息中心（站内信、在线客服）
│   │       │   ├── file/         文件管理（上传、预览）
│   │       │   ├── log/         日志查询
│   │       │   ├── portal/      公开门户管理
│   │       │   ├── job/         定时任务
│   │       │   ├── report/      报表设计器入口
│   │       │   └── error/       403 / 404 / 500
│   │       ├── router/          路由
│   │       │   ├── index.ts
│   │       │   ├── routes.ts    静态路由
│   │       │   └── permission.ts  动态路由守卫
│   │       ├── store/           Pinia
│   │       │   ├── user.ts      用户信息 + Token
│   │       │   ├── permission.ts  动态路由 + 菜单
│   │       │   ├── app.ts       全局 UI 状态（侧栏折叠、主题）
│   │       │   ├── tags.ts      多 Tab
│   │       │   └── dict.ts      字典缓存
│   │       ├── hooks/           组合式函数
│   │       │   ├── useTable.ts  表格通用逻辑
│   │       │   ├── useForm.ts   表单通用逻辑
│   │       │   ├── useDict.ts   字典加载
│   │       │   └── usePermission.ts  按钮权限
│   │       ├── utils/           工具函数
│   │       ├── types/           TS 类型声明
│   │       ├── styles/          全局样式
│   │       │   ├── index.scss
│   │       │   └── variables.scss
│   │       ├── assets/          静态资源
│   │       └── uno.config.ts   应用级 UnoCSS 配置
│   └── portal/                  公开门户（SSG）
│       └── src/
│           ├── pages/           页面（about、blog、news、product）
│           ├── components/
│           └── ...
└── packages/                    共享包
    ├── ui/                      Naive UI 二次封装
    │   └── src/
    │       ├── BasicTable/      基础表格（封装 NDataTable）
    │       ├── BasicForm/       基础表单
    │       ├── BasicModal/      基础弹窗
    │       ├── PageContainer/   页面容器
    │       └── index.ts
    ├── api/                     统一 API 调用（被各 app 复用）
    │   └── src/
    │       ├── types.ts         API 类型
    │       ├── request.ts      axios 实例
    │       └── services/       各服务的 API 模块
    ├── utils/                   工具函数
    │   └── src/
    │       ├── auth.ts          Token 管理
    │       ├── date.ts          日期格式化
    │       ├── validator.ts     表单校验器
    │       ├── tree.ts          树形数据处理
    │       └── download.ts      文件下载
    └── types/                   全局 TS 类型
        └── src/
            ├── api.d.ts         R<T>、PageVO<T>
            ├── system.d.ts      sys_user、sys_role 等
            └── workflow.d.ts
```

> ⚠️ 原规划的 `apps/flow-web`（工作流子系统）和 `packages/uno-preset`（UnoCSS 预设）**尚未实现**，落地时再补充。

---

## 3. pnpm-workspace.yaml

```yaml
packages:
  - 'apps/*'
  - 'packages/*'
```

---

## 4. 根 package.json

```json
{
  "name": "@sca/vue-web-ui",
  "version": "1.0.0",
  "private": true,
  "type": "module",
  "packageManager": "pnpm@9.0.0",
  "scripts": {
    "dev:admin": "pnpm --filter @sca/admin dev",
    "dev:portal": "pnpm --filter @sca/portal dev",
    "build:admin": "pnpm --filter @sca/admin build",
    "build:portal": "pnpm --filter @sca/portal build",
    "build": "pnpm -r build",
    "lint": "pnpm -r lint",
    "lint:fix": "pnpm -r lint:fix",
    "typecheck": "pnpm -r typecheck",
    "test": "vitest"
  },
  "devDependencies": {
    "@typescript-eslint/eslint-plugin": "^8.0.0",
    "@typescript-eslint/parser": "^8.0.0",
    "@vue/eslint-config-prettier": "^10.0.0",
    "eslint": "^9.0.0",
    "eslint-plugin-vue": "^9.0.0",
    "prettier": "^3.3.0",
    "typescript": "^5.5.0",
    "vitest": "^1.6.0",
    "vue-tsc": "^2.0.0"
  },
  "engines": {
    "node": ">=20",
    "pnpm": ">=9"
  }
}
```

---

## 5. 技术栈版本矩阵（强制约束）

| 类别 | 技术 | 版本 | 用途 |
|------|------|------|------|
| 框架 | Vue | ^3.5.0 | 视图框架 |
| 语言 | TypeScript | ^5.5.0 | 类型安全 |
| 构建 | Vite | ^5.4.0 | 极速 HMR + 构建 |
| UI 库 | Naive UI | ^2.39.0 | 组件库 |
| 图标 | @vicons/ionicons5 | ^0.12.0 | 图标库 |
| 状态 | Pinia | ^2.2.0 | 状态管理 |
| 路由 | Vue Router | ^4.4.0 | SPA 路由 |
| HTTP | axios | ^1.7.0 | API 调用 |
| 组合式工具 | @vueuse/core | ^11.0.0 | 工具集 |
| 图表 | ECharts + vue-echarts | ^5.5.0 / ^7.0.0 | 可视化 |
| CSS 引擎 | UnoCSS | ^0.62.0 | 原子 CSS |
| 自动导入 | unplugin-auto-import | ^0.18.0 | 自动 import |
| 组件自动注册 | unplugin-vue-components | ^0.27.0 | 组件自动注册 |
| 测试 | Vitest | ^1.6.0 | 单元测试 |
| 包管理器 | pnpm | 9+ | Monorepo |

> 版本升级**必须**在根 package.json 的 devDependencies 或各子应用 package.json 中统一处理，**禁止**直接修改 pnpm-lock.yaml。

---

## 6. 子应用 package.json 模板（以 admin 为例）

```json
{
  "name": "@sca/admin",
  "version": "1.0.0",
  "private": true,
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vue-tsc -b && vite build",
    "preview": "vite preview",
    "typecheck": "vue-tsc --noEmit -p tsconfig.json",
    "lint": "eslint . --ext .vue,.ts,.tsx --fix"
  },
  "dependencies": {
    "vue": "^3.5.0",
    "vue-router": "^4.4.0",
    "pinia": "^2.2.0",
    "naive-ui": "^2.39.0",
    "@vicons/ionicons5": "^0.12.0",
    "axios": "^1.7.0",
    "@vueuse/core": "^11.0.0",
    "echarts": "^5.5.0",
    "vue-echarts": "^7.0.0",
    "@sca/ui": "workspace:*",
    "@sca/api": "workspace:*",
    "@sca/utils": "workspace:*",
    "@sca/types": "workspace:*"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^5.0.0",
    "@unocss/preset-uno": "^0.62.0",
    "@unocss/preset-attributify": "^0.62.0",
    "unocss": "^0.62.0",
    "unplugin-auto-import": "^0.18.0",
    "unplugin-vue-components": "^0.27.0",
    "typescript": "^5.5.0",
    "vue-tsc": "^2.0.0",
    "vite": "^5.4.0",
    "sass": "^1.77.0"
  }
}
```

---

## 7. vite.config.ts 模板（admin）

```typescript
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import UnoCSS from 'unocss/vite'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { NaiveUiResolver } from 'unplugin-vue-components/resolvers'
import path from 'node:path'

export default defineConfig({
  plugins: [
    vue(),
    UnoCSS(),
    AutoImport({
      imports: ['vue', 'vue-router', 'pinia', '@vueuse/core'],
      dts: 'src/auto-imports.d.ts'
    }),
    Components({
      resolvers: [NaiveUiResolver()],
      dts: 'src/components.d.ts'
    })
  ],
  resolve: {
    alias: { '@': path.resolve(__dirname, 'src') }
  },
  server: {
    port: 5173,
    host: '0.0.0.0',
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/ws': {
        target: 'ws://localhost:8080',
        ws: true,
        changeOrigin: true
      }
    }
  },
  build: {
    target: 'es2022',
    sourcemap: false,
    rollupOptions: {
      output: {
        manualChunks: {
          'vue-vendor': ['vue', 'vue-router', 'pinia'],
          'naive-ui': ['naive-ui'],
          'echarts': ['echarts', 'vue-echarts']
        }
      }
    }
  }
})
```

---

## 8. 前后端契约（跨项目）

### 8.1 雪花 ID Long → String 接收

后端 `spring-cloud-common-web` 计划把所有 `Long` 类型序列化为 String，避免前端 JS Number 精度丢失（最大安全整数 `2^53 - 1`，雪花 ID 超出）。

**前端约定**：
- 所有 ID 字段（`userId`、`orderId`、`id` 等）**必须**用 TypeScript `string` 类型接收
- **禁止**用 `number` 类型接收 ID，会导致精度丢失
- DTO/VO 类型声明示例：`type UserVO = { id: string; username: string; ... }`

### 8.2 统一响应格式 `R<T>`

后端返回统一格式，前端 axios 响应拦截器据此处理：

```json
{
  "code": 200,
  "msg": "success",
  "data": { ... },
  "timestamp": 1722470400000
}
```

| code | 处理逻辑 |
|------|---------|
| 200 | 返回 `data` 字段 |
| 401 | Token 过期，尝试 Refresh，失败跳 SSO 登录 |
| 403 | 提示"无权限" |
| 429 | 提示"操作过于频繁" |
| 其他 | 显示 `msg` 错误提示 |

### 8.3 HTTP 头

| 头 | 方向 | 用途 |
|----|------|------|
| `Authorization: Bearer {token}` | 前端 → 网关 | Sa-Token 访问令牌 |
| `X-Trace-Id` | 网关 → 后端 | 链路追踪 ID（前端可生成并透传） |

---

## 9. 前端开发规范

### 9.1 命名规范

| 类型 | 风格 | 示例 |
|------|------|------|
| 组件文件 | PascalCase.vue | `UserList.vue`、`BasicTable.vue` |
| 组合式函数 | camelCase + use 前缀 | `useUser.ts`、`useTable.ts` |
| 类型文件 | kebab-case | `user.d.ts`、`api.d.ts` |
| 工具函数 | camelCase | `formatDate.ts`、`downloadBlob.ts` |
| API 模块 | kebab-case 目录 | `api/system/user.ts` |
| Pinia Store | camelCase + use 前缀 | `useUserStore.ts`、`usePermissionStore.ts` |
| CSS 类 | kebab-case + BEM | `user-list__item--active` |
| 常量 | UPPER_SNAKE_CASE | `MAX_PAGE_SIZE` |
| Event | kebab-case | `@row-click`、`@form-submit` |

### 9.2 Vue 3 组合式 API 规范

1. **强制使用 `<script setup>` 语法**，**禁止**用 Options API
2. **禁止**用 `defineComponent({ setup() {...} })`，必须用 `<script setup>`
3. ref/reactive 命名：ref 用 camelCase，**禁止**用 `xxxRef` 后缀
4. computed 用 camelCase，命名要表达"是什么"而非"做什么"
5. watch 显式声明 `immediate` 和 `deep`，**禁止**用默认值
6. props 用 `withDefaults` 定义默认值，**禁止**用 `default:` 函数

### 9.3 状态管理（Pinia）规范

1. **强制用 Setup Store 风格**（不用 Options Store）
2. Store **必须**按职责拆分，**禁止**单个 Store 超过 300 行
3. 跨组件状态**必须**用 Store，组件内状态用 ref/reactive
4. Store 中**禁止**直接操作 API，必须通过 `@sca/api` 包

### 9.4 路由规范

1. **动态路由**：登录后从后端 `/api/system/menu/routes` 拉取，通过 `addRoute()` 注入
2. **路由守卫**：`router.beforeEach` 中校验 Token、加载用户信息、生成动态路由
3. **路由元信息 meta**：
   ```typescript
   declare module 'vue-router' {
     interface RouteMeta {
       title: string
       icon?: string
       roles?: string[]
       permissions?: string[]
       keepAlive?: boolean
       affix?: boolean
       activeMenu?: string
       hideInMenu?: boolean
       noTagsView?: boolean
     }
   }
   ```
4. **禁止**在路由组件中直接做权限判断，必须用 `meta.permissions` + 路由守卫
5. **懒加载**：所有页面组件**必须**用 `() => import('...')` 动态导入

### 9.5 API 调用规范

1. **API 模块**统一放在 `src/api/{服务名}/` 目录
2. **必须**用 TypeScript 定义入参与出参类型
3. **必须**通过 `@sca/api` 包的 `request.ts` 创建 axios 实例，**禁止**直接 `import axios from 'axios'`
4. **响应拦截器**统一处理：
   - HTTP 401 → 尝试 Refresh Token，失败跳转登录
   - HTTP 403 → 提示无权限
   - HTTP 429 → 提示"操作过于频繁"
   - 业务 code !== 200 → 显示 `msg` 错误提示
5. **请求拦截器**自动带 Token、`X-Trace-Id`

```typescript
// src/api/system/user.ts
import { request } from '@sca/api'
import type { PageVO, UserVO, UserCreateDTO, UserPageQuery } from '@sca/types'

export function pageUsers(query: UserPageQuery) {
  return request.get<PageVO<UserVO>>('/api/system/users', { params: query })
}

export function createUser(dto: UserCreateDTO) {
  return request.post<UserVO>('/api/system/users', dto)
}

export function updateUser(id: string, dto: Partial<UserCreateDTO>) {
  return request.put<UserVO>(`/api/system/users/${id}`, dto)
}

export function deleteUser(id: string) {
  return request.delete<void>(`/api/system/users/${id}`)
}
```

> ⚠️ 注意 `id` 类型为 `string`，不是 `number`（雪花 ID Long→String 契约）。

### 9.6 与后端 RESTful API 的对齐

| 后端 | 前端调用 | 说明 |
|------|----------|------|
| `GET /api/system/users?pageNum=1&pageSize=10` | `pageUsers({ pageNum: 1, pageSize: 10 })` | 分页查询 |
| `GET /api/system/users/{id}` | `getUserById(id)` | 详情 |
| `POST /api/system/users` | `createUser(dto)` | 新增 |
| `PUT /api/system/users/{id}` | `updateUser(id, dto)` | 全量更新 |
| `PATCH /api/system/users/{id}/password` | `resetPassword(id, newPwd)` | 部分更新 |
| `DELETE /api/system/users/{id}` | `deleteUser(id)` | 删除 |
| `POST /api/system/users/{id}/disable` | `disableUser(id)` | 业务动作 |

### 9.7 SSO 集成规范

1. Token 存储：localStorage `access_token` + `refresh_token`
2. 401 拦截逻辑：
   ```typescript
   if (err.response?.status === 401) {
     const code = err.response.data?.code
     if (code === 40101) {
       // Token 过期，尝试 Refresh
       const refreshed = await tryRefreshToken()
       if (refreshed) return retry(err.config)
     }
     if (code === 40102) {
       // 被踢下线
       window.$message.error('您的登录已失效，请重新登录')
       await redirectToSSO()
     }
   }
   ```
3. 跳转 SSO Server：
   ```typescript
   function redirectToSSO() {
     const redirect = encodeURIComponent(window.location.href)
     window.location.href = `https://auth.example.com/sso/login?redirect=${redirect}`
   }
   ```
4. **禁止**在前端硬编码 SSO Server 地址，必须从环境变量读取：`VITE_SSO_SERVER_URL`

### 9.8 WebSocket 集成规范

1. 监控大盘、消息中心**必须**用 WebSocket（不轮询）
2. 封装 `useWebSocket(url)` 组合式函数，自动处理重连、心跳、消息分发
3. 心跳间隔 30s，超时 60s 主动重连
4. **必须**在组件卸载时主动关闭连接，避免内存泄漏

### 9.9 样式规范（UnoCSS）

1. **强制用 UnoCSS** 原子类，**禁止**写大段 scoped CSS（除组件库样式覆盖）
2. attributify 模式：
   ```html
   <div text="sm gray-500" font="bold" p="4" bg="white dark:gray-900">
     内容
   </div>
   ```
3. **禁止**直接用 hex 颜色，必须用 UnoCSS theme 定义的颜色变量
4. 暗黑模式：`dark:` 前缀，**禁止**用 `prefers-color-scheme`
5. 响应式断点：`sm:` 640px、`md:` 768px、`lg:` 1024px、`xl:` 1280px

---

## 10. 前端必须实现的功能（按应用）

### 10.1 admin（一体化管理平台）

| 模块 | 功能 |
|------|------|
| 登录 | SSO 跳转、Token 续期、踢人下线提示 |
| 系统管理 | 用户/角色/菜单/部门/岗位/字典/参数/通知/在线用户 |
| 服务器监控 | 实时大盘（WebSocket）、历史曲线（1h/24h/7d）、告警列表 |
| 工作流 | 流程发起、待办、已办、流程图、审批（通过/驳回/转办/委托） |
| AI 助手 | 对话界面（SSE 流式）、知识库管理、文档上传 |
| 消息中心 | 站内信、未读数（WebSocket 推送）、在线客服 |
| 文件管理 | 分片上传、断点续传、预览（图片/PDF/Office） |
| 日志查询 | 操作日志、登录日志、审计日志 |
| 定时任务 | XXL-JOB Admin 跳转（iframe） |
| 报表设计 | JimuReport 跳转（iframe） |
| 个人中心 | 修改密码、个人信息、登录日志 |

### 10.2 portal（公开门户）

| 模块 | 功能 |
|------|------|
| 博客 | 文章列表、详情、标签、分类、搜索 |
| 新闻 | 栏目、新闻详情 |
| 产品 | 产品介绍、规格 |
| 友链/关于 | 简单页面 |
| SEO | sitemap.xml、robots.txt、Meta、JSON-LD |

---

## 11. 环境变量规范

### 11.1 `.env.development`（admin）

```bash
VITE_API_BASE_URL=http://localhost:8080
VITE_SSO_SERVER_URL=http://localhost:8081
VITE_WS_BASE_URL=ws://localhost:8080
VITE_APP_TITLE=Aurora 一体化管理平台
VITE_APP_VERSION=1.0.0
```

### 11.2 `.env.production`

```bash
VITE_API_BASE_URL=https://api.example.com
VITE_SSO_SERVER_URL=https://auth.example.com
VITE_WS_BASE_URL=wss://api.example.com
```

> **禁止**在代码中硬编码任何 URL，**必须**用 `import.meta.env.VITE_XXX`。

---

## 12. 构建与部署

### 12.1 构建命令

```bash
# 安装依赖（在 vue-web-ui 根目录）
pnpm install

# 开发模式启动 admin
pnpm dev:admin

# 构建所有应用
pnpm build

# 类型检查
pnpm typecheck

# Lint
pnpm lint

# 单元测试
pnpm test
```

### 12.2 部署

构建产物在 `apps/{app}/dist/`，部署到服务器 `/var/www/{app}/`：

```bash
rsync -avz --delete apps/admin/dist/ user@server:/var/www/admin/
rsync -avz --delete apps/portal/dist/ user@server:/var/www/portal/
```

### 12.3 Nginx 静态资源缓存

- HTML：`no-store`（强制每次拉取最新）
- JS/CSS/图片/字体：`Cache-Control: public, immutable`（一年）
- 文件名含 hash，更新自动失效

---

## 13. 红线（违反即拒绝）

1. ❌ 在子应用中直接 `import axios from 'axios'`（必须用 `@sca/api` 包的 `request`）
2. ❌ 在组件中用 Options API（必须用 `<script setup>`）
3. ❌ 在路由组件中直接做权限判断（必须用 `meta` + 路由守卫）
4. ❌ 硬编码 SSO Server / API 地址（必须用环境变量）
5. ❌ 用 `localStorage.setItem('token', ...)` 直接操作（必须用 `@sca/utils` 的 `auth.ts`）
6. ❌ 在 Pinia Store 中直接调 API（必须用 `@sca/api`）
7. ❌ 非 RESTful API 调用（如 `POST /api/getUser?id=1`）
8. ❌ 用 setTimeout/setInterval 做轮询（必须用 WebSocket）
9. ❌ 组件卸载时不清理 WebSocket / EventListener（内存泄漏）
10. ❌ 用 hex 颜色（必须用 UnoCSS theme 变量）
11. ❌ 用 `number` 类型接收 ID（必须 `string`，避免雪花 ID 精度丢失）
