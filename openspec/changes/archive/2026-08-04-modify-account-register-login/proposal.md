## Why

当前 portal 普通用户登录依赖两条路径：短信验证码登录（滑块防刷 → 发短信 → 验证码登录）与手机号+密码登录。短信通道依赖第三方服务、流程重、成本高，且以手机号作为登录标识存在隐私敏感问题。改为轻量的「账号+密码」登录 + 独立注册流程，并配套文字图形验证码防刷，降低复杂度与成本。

## What Changes

- **后端 `spring-cloud-auth`**：
  - **BREAKING** 移除短信登录链路：`POST /sms/send`、`POST /sms/login`、`POST /captcha/check`，删除 tianai 滑块验证码依赖与 `CaptchaConfig`
  - **BREAKING** `GET/POST /captcha` 改造为文字图形验证码：返回图片 base64 + `captchaKey`，答案存 Redis（5 分钟有效、一次性消费、校验忽略大小写）
  - **BREAKING** `POST /login` 入参调整：`username` → `account`，新增必填 `captchaKey` + `captchaCode`（文字验证码），移除可选 `checkToken`
  - 新增 `POST /register`：入参账号 + 密码 + 确认密码；账号规则 6-18 位、仅大小写英文字母与数字、必须以字母开头；账号重复返回 409
- **前端 portal**：
  - 重写 `Login.vue`：账号、密码、文字验证码三个输入框（验证码输入框右侧为验证码小图片，点击刷新），移除 Tab 切换与滑块弹窗
  - 新增 `Register.vue` 注册页：账号、密码、再次输入密码，登录页与注册页互跳
  - `store/user.ts`、`packages/api` 的 `auth.ts`、`packages/types` 的 `auth.ts` 适配；删除 `CaptchaSlider.vue`
- **前端 admin**：登录页滑块验证码弹窗适配为文字图形验证码（`/captcha` 返回格式变更，需同步修改）
- **页面规范**：登录/注册页面将原生 HTML 标签尽可能替换为 Naive UI 组件（`n-form`/`n-input`/`n-button`/`n-image` 等）

## Capabilities

### New Capabilities

无（注册与登录能力均归属既有 `user-auth` 域）

### Modified Capabilities

- `user-auth`: 登录方式由「短信验证码 / 手机号+密码」整体改为「账号+密码+文字验证码」；新增独立注册能力（账号 6-18 位、字母开头、字母数字、不可重复、两次密码一致）；删除短信验证码、滑块验证码相关需求

## Impact

| 范围 | 模块 | 影响 |
|------|------|------|
| 后端 | `spring-cloud-alibaba/spring-cloud-auth` | `AuthController`、`AuthService`/`AuthServiceImpl`、`CaptchaService`/`CaptchaServiceImpl`、`LoginDTO`（改造）、`RegisterDTO`（新增）、`CaptchaVO`（改造）、删除 `SmsLoginDTO`/`SmsSendDTO`/`SmsService` 相关入口；移除 tianai-captcha 依赖；`LoginTypeEnum.SMS` 枚举保留但不再产生新记录 |
| 前端 | `vue-web-ui/apps/portal` | `views/Login.vue`（重写）、`views/Register.vue`（新增）、`router`（新增 /register）、`store/user.ts`、删除 `components/CaptchaSlider.vue` |
| 前端 | `vue-web-ui/apps/admin` | `views/login/Login.vue`、`components/common/CaptchaModal.vue`、`api/auth.ts` 适配文字验证码 |
| 共享包 | `vue-web-ui/packages/api`、`vue-web-ui/packages/types` | `services/auth.ts` 接口调整，`auth.ts` 类型调整（`LoginDTO` 改造、`ImageCaptchaVO`/`RegisterDTO` 新增、删除短信相关类型） |
| 数据库 | `sys_user` | 无 DDL 变更；`username` 字段承载账号。注意：历史手机号用户名（11 位数字）数据可继续登录（登录仅校验非空），注册强制账号规则 |

## Non-goals

- 不改造 SSO / OAuth2 / 在线用户 / 风控锁定等既有模块（`LoginRiskService` 逻辑沿用）
- 不做邮箱注册/登录，不引入短信/邮件第三方服务
- 不修改密码找回/修改密码流程（`PATCH /me/password` 沿用）
- 不调整 admin 管理端账号体系与 RBAC，仅适配验证码展示形态
- 注册密码强度规则沿用现有 8-32 位约束，不额外引入复杂度要求
