## 1. 前端类型对齐

- [x] 1.1 `packages/types/src/auth.ts` 更新 `LoginDTO`：`captcha`/`captchaKey` → `checkToken`（string），`CaptchaVO` 改为滑块语义（captchaId/backgroundImage/templateImage）（验收：`pnpm typecheck` 通过）
- [x] 1.2 `apps/admin/src/api/auth.ts` 更新 `getCaptcha()` 返回值类型为 `CaptchaVO`，`login()` 入参 `LoginDTO` 含 `checkToken`（验收：类型正确）

## 2. CaptchaModal 滑块弹窗组件

- [x] 2.1 新建 `apps/admin/src/components/common/CaptchaModal.vue`：Naive UI `NModal` + `NCard` 弹窗，调 `GET /api/auth/captcha` 获取背景图+滑块图，用 Canvas 实现拖拽滑块交互（验收：弹窗展示滑块，拖动有效果）
- [x] 2.2 CaptchaModal 实现滑块拖动逻辑：鼠标/触摸事件监听，计算滑块偏移百分比，调 `POST /api/auth/captcha/check` 提交轨迹坐标，成功后 `emit('success', checkToken)`（验收：滑块通过后拿到 checkToken）

## 3. Login.vue 改造

- [x] 3.1 Login.vue 移除旧字符验证码代码（`captcha`/`captchaKey`/`captchaImg`/`refreshCaptcha`），引入 CaptchaModal 组件，点登录按钮弹出 CaptchaModal（验收：旧验证码输入框消失，弹窗弹出）
- [x] 3.2 Login.vue 登录流程改为：CaptchaModal 成功回调 → 获取 checkToken → 调 `POST /login` 带 `{ username, password, checkToken, rememberMe }`（验收：登录成功跳转首页）
- [x] 3.3 移除 `form.username` 的 `admin` 默认值，登录页不再硬编码用户名（验收：用户名输入框为空）

## 4. userStore refreshToken 处理

- [x] 4.1 `userStore.login()` 登录成功后保存 `refreshToken` 到 localStorage，新增 `getRefreshToken()`/`setRefreshToken()`/`removeRefreshToken()` 方法（验收：登录后 localStorage 有 refreshToken）

## 5. 验证

- [x] 5.1 前端验证：`pnpm typecheck` + `pnpm build`（验收：无类型错误、构建通过）
- [ ] 5.2 手工验证：登录页加载 → 输入账号密码 → 点登录 → 弹滑块 → 拖动通过 → 登录成功跳转首页（需后端环境启动后验证）