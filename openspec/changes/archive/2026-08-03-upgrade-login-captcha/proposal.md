## Why

现有登录验证码为自绘 4 位字符码 + 简单干扰线（`CaptchaServiceImpl`），2026 年 AI/OCR 对其识别率已超过 99%，无法抵御撞库与自动化爆破。项目文档（`spring-cloud-auth/CLAUDE.md` §5.5）原规划引入 tianai-captcha 行为滑块验证码，本次落地并同步改造登录交互：用户输入账号密码后点击登录，弹出滑块验证码，滑块通过后才提交认证。

## What Changes

- 引入 tianai-captcha（`tianai-captcha-springboot-starter` 1.5.2），版本声明在父 POM `<properties>`（`<tianai-captcha.version>`），子模块不写版本
- 后端 `CaptchaService`/`CaptchaServiceImpl`：自绘字符码 → 行为滑块（图片 + 拼图坐标 + 轨迹评分 `min-score` 阈值）
- **BREAKING** `GET /captcha` 响应结构变化：字符图 base64 → 滑块图（含背景图、缺口坐标、captchaId）
- **BREAKING** `LoginDTO`：`captcha`/`captchaKey` 字段 → 滑块 token（captchaId + 前端轨迹 `ImageCaptchaTrack`）
- `AuthServiceImpl.login` 第 1 步：字符码比对 → tianai `matching()` 轨迹校验，保持"先验证码、后查库"顺序与一次性失效
- 前端登录页：`GET /captcha` 预加载 → 点击登录弹滑块 → 通过后携带滑块 token 提交 `POST /login`（复用 `@sca/api` request）
- 保留 Redis 存储（tianai Redis 缓存扩展）与 `auth:captcha:{key}` Key 规范

## Capabilities

### New Capabilities

- `login-captcha`: 登录行为滑块验证码——生成、轨迹校验、一次性失效、与登录请求绑定

### Modified Capabilities

（无既有 spec，`openspec/specs/` 为空）

## Impact

- `spring-cloud-alibaba/pom.xml`：新增 tianai-captcha 版本属性
- `spring-cloud-auth`：`CaptchaService`/`CaptchaServiceImpl`/`CaptchaController`/`AuthServiceImpl`/`LoginDTO`/`CaptchaVO`/`CaptchaConfig`（新增）
- `vue-web-ui`：`apps/admin` 登录页（滑块弹窗交互）、`packages/types` 登录接口类型定义
- 现有测试 `CaptchaServiceTest`/`AuthServiceImplTest` 需同步改造
