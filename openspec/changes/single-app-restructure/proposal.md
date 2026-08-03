## Why

前端虽已在上一次变更合并为 `apps/portal` 单应用，但仍保留 `apps/` 目录套一层，结构冗余；且登录页沿用 portal 风格（左品牌区 + 右卡片），与用户期望的"旧 admin 登录页风格"不一致。需要将单应用代码上提到根级 `vue-web-ui/src`，删除 `apps/` 目录，统一登录/注册页为"全屏渐变背景 + 居中卡片"风格，并调整登录后跳转。

## What Changes

- **前端上提为根级单应用**：将 `apps/portal` 的全部代码迁移至 `vue-web-ui/src`，删除 `vue-web-ui/apps` 目录；根 `package.json` 由 monorepo 根变为应用本体（包名 `@sca/web`），`packages/*` 公共包保留
- **端口 5173**：应用 dev 端口由 5174 改为 5173
- **登录/注册二合一卡片**：登录页改为全屏渐变背景（indigo→purple）+ 居中白色卡片，登录/注册在卡片内切换（不再有独立 `/register` 页）
  - 登录态：一级标题"个人博客"、二级标题"登录后开启完整的博客体验"、账号输入框、密码输入框、验证码输入框+验证码图片、7 天免登录、登录按钮、"还没有账号？立即注册"
  - 点击"立即注册"切换：一级标题"注册账号"、二级标题"注册后开启完整的博客体验"、账号/密码/确认密码、注册按钮、"已有账号？去登录"
- **登录后跳转 `/`**：登录成功跳转到博客列表首页（`http://localhost:5173/`），dashboard 仍通过头像菜单"我的主页"进入
- **CORS 与跨域**：网关 CORS 白名单同步为 5173

## Capabilities

### New Capabilities

（无新增能力）

### Modified Capabilities

- `project-structure`: 前端由 `apps/portal` + `apps/admin` 双应用改为根级 `vue-web-ui/src` 单应用，删除 `apps/` 目录，包名 `@sca/web`，dev 端口 5173
- `user-auth`: 登录/注册页改为单卡片二合一（全屏渐变背景 + 居中卡片，卡片内切换），登录成功跳转 `/`（博客列表）而非 dashboard

## Impact

- 前端：`vue-web-ui/`（根 package.json、vite.config.ts、tsconfig、index.html、`.env`、`src/` 全部应用代码）、删除 `apps/`、`packages/*` 引用同步（`@sca/portal` → `@sca/web`）
- 后端：`spring-cloud-gateway`（CORS 白名单 5174→5173）
- 依赖：无新增；`pnpm-lock.yaml` 需重新生成
- 契约：`R<T>`、雪花 ID String、`X-Login-Id` 等跨端契约不变

## Non-goals

- ❌ 不做登录方式的变更（保留账号密码 + 文字验证码，滑块验证码不做）
- ❌ 不改变 dashboard 各页面功能与权限菜单
- ❌ 不迁移 `packages/*` 公共包结构
- ❌ 不做移动端专属适配（沿用响应式）