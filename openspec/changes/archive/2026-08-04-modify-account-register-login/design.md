## Context

现状（见 proposal.md - Why）：`spring-cloud-auth` 提供滑块验证码（tianai-captcha）、短信验证码登录（`/sms/send`、`/sms/login`）与手机号+密码登录（`POST /login`，`username` 传手机号，`checkToken` 可选）；portal 登录页为 Tab 双通道（短信 / 密码），依赖 `CaptchaSlider.vue` 滑块组件；admin 登录页在失败后弹出 `CaptchaModal.vue` 滑块弹窗。`/captcha` 接口被 portal 与 admin 共用，`/login` 亦为两端共用，改造时需同步适配 admin。

技术约束：Hutool 5.8.27 已在依赖中（`AuthServiceImpl` 已用 `RandomUtil`）；密码 Argon2id（Bouncy Castle）；验证码答案经 Redis 存储；所有接口返回 `R<T>`；前端强制 `<script setup>` + Naive UI + `@sca/api`。

## Goals / Non-Goals

**Goals:**

- 全项目统一为「账号+密码+文字图形验证码」登录契约，消除短信与滑块两条链路
- 新增独立注册能力，账号规则（6-18 位、字母开头、字母+数字、唯一）在前后端双端校验
- 登录页 / 注册页全面 Naive UI 化，移除原生 HTML 表单标签
- 验证码服务端存储答案、忽略大小写、一次性消费、5 分钟过期

**Non-Goals:**

- 不引入新第三方验证码服务（如极验/腾讯防水墙）
- 不改造 SSO / OAuth2 / 风控锁定逻辑
- 不改 `sys_user` 表结构（`username` 已承载账号）
- 不做密码找回 / 邮箱注册

## Decisions

### 1. 文字验证码生成：Hutool `hutool-captcha`（替代 tianai 滑块）

- **决策**：移除 tianai-captcha 依赖与 `CaptchaConfig`，`CaptchaService` 改造为文字验证码：`CaptchaUtil.createLineCaptcha(120, 40, 4, 20)` 生成，`getCode()` 取答案、`getImageBase64()` 输出 data URI 图片；答案存 Redis `auth:captcha:image:{captchaKey}`（TTL 5 分钟）。
- **理由**：Hutool 已在依赖中（需补 `hutool-captcha` 模块，版本由父 POM `hutool-bom` 统一管理）；代码量最小，无需维护绘制逻辑；tianai 滑块与短信登录强耦合，短信废弃后无存在意义。
- **替代方案**：① 原生 Graphics2D 自绘——无新依赖但需自行处理字体/干扰线/抗锯齿，代码约 100+ 行，收益低；② 保留 tianai 滑块供 admin——两套验证码机制并存，与"统一契约"目标冲突。

### 2. 验证码契约：改造 `CaptchaVO`，一次性消费 + 忽略大小写

```
GET /api/auth/captcha  ──►  R<CaptchaVO> { captchaKey, imageBase64 }
POST /api/auth/login   ──►  { account, password, captchaKey, captchaCode, rememberMe }
```

- 答案在 Redis 中存原始字符串（生成时大小写随机），校验用 `equalsIgnoreCase`（满足"不区分大小写"），校验通过后 `delete` 该 key 并判定删除结果（一次性）。
- 刷新：点击图片重新请求 `GET /captcha` 拿新 `captchaKey`，旧 key 不主动删除（TTL 兜底），校验时旧 key 不存在即失败。
- `CaptchaVO` 字段由滑块字段（`backgroundImage`/`templateImage` 等）改造为 `{ captchaKey, imageBase64 }`；admin 的 `CaptchaModal.vue` 因引用旧字段必须同步改造。
- **替代方案**：验证码答案存内存 Caffeine——多实例下不一致，Redis 为唯一正确选项；无状态 JWT 验证码（不存答案）——无法过期/作废，安全性差。

### 3. 登录接口改造：验证码必校验，账号密码统一错误提示

```
登录流程：
用户提交(account, password, captchaKey, captchaCode)
  │
  ├─ 1. 校验验证码（Redis 查 key → equalsIgnoreCase → delete）
  │       失败 ──► 返回"验证码错误或已过期"（不查账号，防枚举）
  │       成功 ──┐
  ├─ 2. LoginRiskService 锁定检查（沿用）
  ├─ 3. 按 username=account 查用户，密码 Argon2id 校验（沿用）
  │       失败 ──► 统一"账号或密码错误" + recordFailure
  ├─ 4. 状态校验 → 登录成功 → clearFailure → doLogin（沿用）
  └─ 5. 返回 LoginVO
```

- `LoginDTO`：`username` 更名 `account`；删除可选 `checkToken`；新增必填 `captchaKey`/`captchaCode`；保留 `rememberMe`。
- 登录账号校验仅 `@NotBlank @Size(6,18)`，**不做字符集校验**——历史手机号用户（username 为 11 位数字）仍可登录；字符集规则只施加于注册。
- 验证码失败不触发登录失败计数（与账号密码失败区分），避免验证码乱输入导致账号锁定。

### 4. 注册接口：`POST /register`，唯一性双保险 + 注册即登录

```
POST /api/auth/register  { account, password, confirmPassword }
  │
  ├─ 校验：账号 ^[a-zA-Z][a-zA-Z0-9]{5,17}$（6-18 位、字母开头、字母+数字）
  │        密码 8-32 位；confirmPassword == password
  ├─ 查重：username = account 已存在 ──► 409"账号已存在"
  ├─ 插入 AuthUser（Argon2id 密码，默认 USER 角色，沿用 registerByPhone 的模式）
  │        并发下 DuplicateKeyException ──► 重新查询，存在则 409
  └─ 注册成功 ──► 复用 doLogin 自动登录，返回 LoginVO，前端跳个人主页
```

- **决策**：注册成功自动登录（与旧短信登录"注册即登录"体验一致），非跳转登录页——减少一次验证码输入摩擦。
- **替代方案**：注册后跳转登录页——更保守但多一次交互，与既有体验不一致。
- 不新增 `user.register` MQ 事件（现有代码无此消费者链路，保持现状，登录事件由 `doLogin` 照常发布）。

### 5. 前端 portal：重写 Login.vue + 新增 Register.vue

- `Login.vue`：删除 `n-tabs`、`CaptchaSlider`、倒计时逻辑；`n-form` 三个 `n-form-item`（账号 / 密码 / 验证码）；验证码行 = `n-input` + `n-image`（小图，`:src="captchaVO.imageBase64"`，`@click` 重新拉取）；登录按钮 `n-button type="primary" block`；底部 `n-text` + `n-button text` "立即注册" 跳 `/register`；features 文案改为"账号密码登录"。
- `Register.vue`（新增）：`n-form` 三个 `n-form-item`（账号 / 密码 / 再次输入密码），前端规则校验（正则 + 两次一致，`n-form` rules），提交成功 `router.replace('/profile')`；"已有账号？去登录" 跳 `/login`。
- 路由：`/register` 懒加载注册。
- `store/user.ts`：`loginByAccount(dto)`、`register(dto)`（注册返回 LoginVO 后走 `saveLogin`），移除 `loginBySms`。
- `packages/api` / `packages/types`：`getCaptcha(): CaptchaVO{captchaKey,imageBase64}`、`login`、`register`、`logout`、`getMe`；删除 `checkCaptcha`/`sendSmsCode`/`smsLogin` 及对应类型。

### 6. 前端 admin：滑块弹窗 → 内嵌文字验证码（连带适配）

- 后端 `/login` 契约变更后 admin 登录必须携带 `captchaKey`/`captchaCode`，否则无法登录。
- `Login.vue`：移除 `CaptchaModal` 依赖与 `captchaRequired` 条件逻辑，登录表单内嵌验证码输入框 + 图片（与 portal 同构）；每次登录必填验证码。
- `CaptchaModal.vue`：删除；`components.d.ts` 由插件自动重生成。

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| hutool-captcha 依赖 AWT 字体渲染，Linux 无中文字体环境可能乱码 | 验证码为纯 ASCII 字母数字，依赖内置字体可渲染；若生产异常，fallback 为 Graphics2D 自绘（决策 1 已备） |
| 登录账号不校验字符集，历史手机号用户名与字母账号混存 | 仅影响登录入口，不影响注册规则；查询按 username 精确匹配 |
| 验证码生成/校验为同步 Redis 读写，高并发登录下成为热点 | 5 分钟 TTL + 一次性消费，单用户单 key；如遇压力可后续加 Caffeine 二级缓存（本期不做） |
| admin 登录强制每次输入验证码，操作略变繁琐 | 与 portal 契约统一，换取消块交互与第三方依赖，整体成本更低 |
| BREAKING 变更：旧前端（sms 登录）与旧接口并存期间不可用 | 前后端同仓同版本发布；网关路由不变，仅 auth 服务接口变更 |

## Migration Plan

1. **部署顺序**：后端 auth 服务先行（新 `/captcha`、`/login`、`/register`，删旧接口）→ admin 与 portal 前端同时发布（两者均依赖新契约）。
2. **数据兼容**：无 DDL；历史手机号用户（username=手机号）可凭已设密码登录；未设密码的历史短信用户需管理员重置密码（沿用现有 `PATCH /me/password` 链路）。
3. **回滚**：`git revert` 对应 commit 后重新构建发布；前端回滚后登录将 404（旧接口已删），需前后端同时回滚。

## Open Questions

无（涉及 spec 或任务拆分的未知点均已在上方决策中确定）。
