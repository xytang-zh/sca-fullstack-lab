## Why

后端已升级为 tianai-captcha 滑块验证码（`checkToken` 一次凭据），但前端 Login.vue 仍使用旧的自绘字符验证码（`captcha`/`captchaKey` 字段），导致前后端字段不匹配，登录功能不可用。需要将前端登录交互对齐后端滑块验证码流程。

## What Changes

- **Login.vue**：字符验证码输入框 → 点登录弹出滑块验证码弹窗（CaptchaModal），滑块通过后获取 `checkToken` 再提交登录
- **CaptchaModal**：新建滑块验证码弹窗组件，通过 tianai-captcha 前端 SDK 渲染滑块，调 `POST /captcha/check` 获取 `checkToken`
- **auth API**：`getCaptcha()` 返回值类型对齐滑块语义，`login()` 入参改为 `checkToken`
- **types/auth.ts**：`LoginDTO` 字段从 `captcha`/`captchaKey` 改为 `checkToken`，`CaptchaVO` 改为滑块语义
- **userStore**：登录成功后保存 `refreshToken`（后端已返回，前端未使用）

## Capabilities

### New Capabilities
- `login-ui`: 滑块验证码登录交互——点登录弹滑块、滑块通过后提交认证

## Impact

- `vue-web-ui/apps/admin/src/views/login/Login.vue`：重写登录交互
- `vue-web-ui/apps/admin/src/components/common/CaptchaModal.vue`：新建滑块弹窗组件
- `vue-web-ui/apps/admin/src/api/auth.ts`：更新接口定义
- `vue-web-ui/packages/types/src/auth.ts`：更新类型定义
- `vue-web-ui/apps/admin/src/store/user.ts`：新增 refreshToken 处理