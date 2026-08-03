## Context

当前前端为 `vue-web-ui/apps/portal` 单应用（上一次变更已合并 admin+portal），`apps/` 仅剩 portal 一个目录。用户要求：应用代码上提到根级 `vue-web-ui/src`，删除 `apps/`，包名 `@sca/web`，端口 5173，登录/注册页改为"全屏渐变背景 + 居中卡片"二合一，登录后跳转 `/`。

## Goals / Non-Goals

**Goals:**
- 前端结构收敛为根级单应用（`src/` + `packages/*`），删除 `apps/`
- 登录/注册为单卡片二合一（视觉复用旧 admin 登录页的渐变背景风格）
- 登录成功跳转 `/`（博客列表）
- dev 端口 5173，网关 CORS 同步

**Non-Goals:**
- 不改登录方式（保留账号密码 + 文字验证码，不做滑块）
- 不改 dashboard 功能与权限菜单
- 不改 `packages/*` 公共包结构

## Decisions

### D1 根级单应用迁移

```
vue-web-ui/
├── package.json          # 应用本体（name=@sca/web）+ 公共包 devDeps
├── vite.config.ts        # 从 apps/portal 上提，端口 5173
├── tsconfig.json / tsconfig.node.json
├── index.html
├── .env.development / .env.production
├── uno.config.ts
├── src/                  # 原 apps/portal/src 全部上提
│   ├── main.ts / App.vue
│   ├── router/ store/ layouts/ views/ components/ hooks/ api/ utils/
│   └── auto-imports.d.ts / components.d.ts
├── packages/             # 保留：api/types/utils/ui/uno-preset
└── pnpm-workspace.yaml   # packages: ['packages/*']
```

- 步骤：`git mv apps/portal/* → .`（src 上提），删除 `apps/`；根 `package.json` 合并 `apps/portal/package.json` 的 dependencies/scripts 与原有 devDependencies
- `@/` 别名指向 `src/`（vite resolve.alias + tsconfig paths），代码无需改动路径
- 依赖：`packages/*` 通过 `workspace:*` 引用；`pnpm-lock.yaml` 重新生成
- 包名：应用 `@sca/web`；`packages/*` 内部如有引用 `@sca/portal` 的需改为 `@sca/web`（一般只有应用引用包，反向引用需检查）

### D2 登录/注册二合一卡片

- 新建 `src/views/Login.vue`（替代原 Login.vue 与 Register.vue），内部 `mode: 'login' | 'register'` 切换
- 视觉：外层 `min-h-screen` + `linear-gradient(135deg, #667eea 0%, #764ba2 100%)`（复用旧 admin 登录页渐变），居中白色卡片（420px，圆角 + 阴影）
- 登录态表单：账号（`account`）、密码（`password`）、验证码（`captchaKey` + `captchaCode` + 图片）、`rememberMe` 复选框 → 调 `authApi.login`；点击"立即注册"→ `mode='register'`
- 注册态表单：账号、密码、确认密码 → 调 `authApi.register`；成功后跳转 `/`
- 登录成功：`router.replace(redirect ?? '/')`（redirect 存在则回跳，否则 `/`）
- 删除 `/register` 路由（`/login` 二合一即可）；`/login?mode=register` 可支持外部直达注册态
- 验证码图片：复用现有 `authApi.getCaptcha()`，点击刷新，校验忽略大小写（后端已实现）

### D3 登录后跳转 `/`

- `Login.vue` 与 `Register.vue` 的跳转目标统一改为 `/`（博客列表）
- `NavBar` 头像下拉"我的主页"仍进 `/dashboard/profile`，dashboard 不受影响
- 路由守卫：未登录访问 dashboard 仍重定向 `/login?redirect=...`

### D4 端口与 CORS

- `vite.config.ts`：`server.port = 5173`（若占用则提示，不用 auto-increment）
- 网关 `CorsConfig`：allowedOriginPatterns 增加 `http://localhost:5173`（保留 5173/5174/5180 等旧项过渡）
- 代理 `/api`、`/ws` 目标不变（`http://localhost:8080`）

## Risks / Trade-offs

- [根级 package.json 同时承担应用与 monorepo 根职责] → 明确 `name=@sca/web`、`private=true`、`type=module`，scripts 合并；`packages/*` 仍为 workspace
- [`pnpm-lock.yaml` 变动大] → 迁移后执行 `pnpm install` 重新生成，验证 `pnpm dev` 可启动
- [删除 apps/ 后旧引用残留] → 全局 grep `@sca/portal`、`apps/portal`、`5174`，逐一清理
- [登录页从旧 admin 恢复的字段（username/rememberMe）与后端不符] → 明确表单字段用后端契约（account/captchaKey/captchaCode），仅复用视觉，不复用旧字段名

## Migration Plan

1. `git mv apps/portal/src → src`，`git mv apps/portal/{index.html,uno.config.ts,vite.config.ts,tsconfig.json,package.json,env}` 到根
2. 合并根 `package.json`（name@sca/web、deps、scripts、packageManager），更新 `pnpm-workspace.yaml`
3. 改 `vite.config.ts` 端口 5173、alias 指向 `src/`；删除 `apps/`
4. 重写 `Login.vue` 为二合一卡片；删除 `Register.vue` 与 `/register` 路由；登录/注册跳转改 `/`
5. 全局清理 `@sca/portal`/`apps/portal`/端口 5174 引用；网关 CORS 加 5173
6. `pnpm install` 重新生成 lock，`pnpm typecheck` + `pnpm build` + 浏览器验证登录/注册/跳转

## Open Questions

- 无（关键决策已与用户确认）