## Context

后端已对接 tianai-captcha 滑块验证码，提供三个端点：
- `GET /captcha` → CaptchaVO（captchaId + backgroundImage + templateImage）
- `POST /captcha/check` → { checkToken }（滑块轨迹校验通过后签发）
- `POST /login` → LoginVO（接收 checkToken 而非 captcha/captchaKey）

前端 Login.vue 仍使用旧字符验证码，请求字段不匹配导致登录失败。

## Goals / Non-Goals

**Goals:**
- 前端登录页对齐后端滑块验证码流程
- 删掉旧字符验证码代码

**Non-Goals:**
- 不改后端任何代码
- 不做 SSO/OAuth2 集成

## Decisions

### D1: 滑块 SDK 使用方式

**选择**：不使用 tianai 前端 SDK（tac.min.js），改为前端直接调后端 API 手动渲染滑块。

**理由**：tianai 前端 SDK 需要从 Gitee Releases 下载，且与 Vue 3 组件集成有一定复杂度。手动实现更可控：`GET /captcha` 获取滑块图和背景图 → 前端用 Canvas 渲染拖拽交互 → `POST /captcha/check` 提交轨迹坐标。

**替代方案**：tianai 前端 SDK —— 需要额外下载和资源管理，对本次小改动而言过重。

### D2: 登录交互时序

**选择**：用户输入账号密码 → 点击登录 → 弹出滑块弹窗 → 用户滑动 → 前端直接调 `POST /captcha/check` → 获取 checkToken → 关闭弹窗 → 提交 `POST /login`。

**理由**：与后端现有流程完全匹配，无需修改后端。

## Risks / Trade-offs

- 手动滑块交互不如 tianai SDK 的轨迹分析丰富（无滑动时长/加速度检测），但后端 tianai 的 `matching()` 方法会做完整轨迹验证，前端只负责传递轨迹数据
- 滑块弹窗样式需与 Naive UI 风格一致

## 交互流程

```
Login.vue                          CaptchaModal.vue                  后端
    │                                    │                            │
    │ 用户点登录                          │                            │
    │───→ 弹出 CaptchaModal ──→           │                            │
    │                                    │── GET /captcha ───────────→│
    │                                    │←── CaptchaVO ─────────────│
    │                                    │  渲染滑块背景+滑块图        │
    │                                    │                            │
    │                                    │ 用户拖动滑块                │
    │                                    │── POST /captcha/check ───→│
    │                                    │   { id, data: track }      │
    │                                    │←── { checkToken } ────────│
    │                                    │                            │
    │←── emit('success', checkToken) ────│                            │
    │                                    │                            │
    │── POST /login ────────────────────────────────────────────────→│
    │   { username, password, checkToken, rememberMe }               │
    │←── LoginVO ───────────────────────────────────────────────────│
    │  保存 token + refreshToken                                      │
    │  跳转 /dashboard                                                │
```