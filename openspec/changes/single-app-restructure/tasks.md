## 1. 前端根级单应用迁移

- [ ] 1.1 将 `vue-web-ui/apps/portal/src` 上提为 `vue-web-ui/src`（`git mv`），`apps/portal` 其余文件（index.html、vite.config.ts、uno.config.ts、tsconfig.json、.env.development/.env.production、package.json）上提到根
- [ ] 1.2 删除 `vue-web-ui/apps` 目录（含 portal、admin）
- [ ] 1.3 合并根 `package.json`：`name` 改为 `@sca/web`，合并 `apps/portal` 的 dependencies/scripts，保留 `packages/*` workspace 依赖与 `packageManager` 声明
- [ ] 1.4 更新 `pnpm-workspace.yaml` 为 `packages: ['packages/*']`；`pnpm install` 重新生成 `pnpm-lock.yaml`
- [ ] 1.5 更新 `vite.config.ts`：`server.port = 5173`、`resolve.alias` 指向 `src/`、代理 `/api`→`http://localhost:8080` 不变；确认 `tsconfig.json` 的 `paths` 与 `vue-tsc` 配置就绪

## 2. 登录/注册二合一卡片

- [ ] 2.1 重写 `src/views/Login.vue`：全屏渐变背景（`linear-gradient(135deg, #667eea 0%, #764ba2 100%)`）+ 居中白色卡片（420px），`mode: 'login' | 'register'` 内部切换
- [ ] 2.2 登录态表单：一级标题"个人博客"、二级标题"登录后开启完整的博客体验"、账号/密码/验证码输入框+验证码图片、7 天免登录复选、登录按钮、"还没有账号？立即注册"；调 `authApi.login`
- [ ] 2.3 注册态表单：一级标题"注册账号"、二级标题"注册后开启完整的博客体验"、账号/密码/确认密码、注册按钮、"已有账号？去登录"；调 `authApi.register`
- [ ] 2.4 删除 `src/views/Register.vue` 与 `/register` 路由；`/login?mode=register` 支持直达注册态
- [ ] 2.5 登录/注册成功跳转改为 `/`（博客列表）；`redirect` 参数存在时回跳原页面

## 3. 引用清理与 CORS

- [ ] 3.1 全局清理 `@sca/portal`、`apps/portal`、`apps/admin`、端口 `5174` 的残留引用（`packages/*`、文档、脚本）
- [ ] 3.2 网关 `CorsConfig` 的 `allowedOriginPatterns` 增加 `http://localhost:5173`
- [ ] 3.3 根 `package.json` 脚本 `dev`/`build`/`typecheck`/`lint` 指向根应用；更新 `vue-web-ui/CLAUDE.md` 与仓库根 `CLAUDE.md` 的前端结构说明

## 4. 验证

- [ ] 4.1 `pnpm install` 后 `pnpm typecheck` 与 `pnpm build` 通过
- [ ] 4.2 `pnpm dev` 在 `http://localhost:5173` 启动，首页知乎顶栏 + 文章列表正常
- [ ] 4.3 浏览器验证：`/login` 显示渐变背景 + 居中卡片，登录/注册卡内切换正常；登录成功跳转 `/`；未登录访问 `/dashboard` 重定向登录且登录后回跳
- [ ] 4.4 验证 `apps/` 目录不存在，`src/` 为应用根，`pnpm typecheck` 无报错