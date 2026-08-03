## Context

- 现状：auth 服务已有账号密码登录 `POST /login`、滑块验证码（tianai-captcha，`GET/POST /captcha` + `POST /captcha/check`，校验通过签发一次性 checkToken 存 Redis 60s）、SaTokenConfig 白名单鉴权；sys_user 已有 `phone` 字段但无唯一索引；无短信验证码与注册接口
- 前端 portal 为 MVP 空壳（无路由/布局/登录页），`packages/api` 仅含 request 封装，无 services
- 跨端契约：`R<T>` 响应、Long→String、`Authorization: Bearer`、`X-Login-Id` 透传，详见根 CLAUDE.md §4
- 动机与范围见 proposal.md

## Goals / Non-Goals

**Goals:**
- 游客免登录浏览已发布文章列表/详情，支持时间、热度排序；写操作保持登录要求（401 引导）
- 复用现有滑块验证码能力作为短信发送前置校验
- 验证码登录/注册一体（新用户自动注册）、手机号密码登录、登录后跳个人主页
- 短信通道 Mock 先行，接口抽象预留阿里云 SMS 接入点

**Non-Goals:**
- 不接入网关 Auth 过滤器（`AuthGatewayFilterFactory` 已存在但本次不启用，保持现有服务侧鉴权不变，避免回归）
- 不做 OAuth2/找回密码/多端会话管理
- 滑块验证码不做点击式验证与设备指纹

## Decisions

### D1: 短信验证码流程（滑块前置校验）

```
前端登录卡片                          auth 服务                        Redis
    │ 点击"获取验证码"                    │                              │
    │─────────────────── 弹出滑块弹窗 ────│                              │
    │ GET /api/auth/captcha              │→ 生成拼图+答案               │
    │─────────────────── 拖拽完成 ────────│                              │
    │ POST /api/auth/captcha/check       │→ 校验轨迹，签发 checkToken   │→ sms:captcha:check:{id} TTL 60s
    │─────────────────── 携带 checkToken ─│                              │
    │ POST /api/auth/sms/send            │→ 校验并消费 checkToken       │
    │  {phone, checkToken}               │→ 生成6位码，Mock 发送        │→ auth:sms:code:{phone} TTL 5min
    │←─────────────────── 成功(60s倒计时) │                              │
```

- 复用现有 `CaptchaService`/tianai-captcha，**不新建滑块体系**；`POST /sms/send` 校验 checkToken（消费制，一次性）
- 同一手机号 60s 内重复发送 → 拒绝（Redis key 加发送时间戳或直接检查 code key 存在性）；失败计数复用 `LoginRiskService` 模式防爆破
- 替代方案：把滑块校验逻辑内嵌进 `/sms/send` 接口（不再下发 checkToken）——否决：会破坏现有 `/captcha/check` 与登录链路的复用，且弹窗独立组件需要独立的"校验结果凭证"

### D2: 验证码登录/注册一体

- 新增 `POST /api/auth/sms/login`，入参 `{phone, code}`：
  - 校验 `auth:sms:code:{phone}`，通过后**立即作废**该 key
  - 按 phone 查 `sys_user`：存在 → 直接 `StpUtil.login`；不存在 → 自动注册（`username = phone`，密码置为随机 Argon2id 串不可登录，默认角色 USER）后登录
- 登录响应沿用现有 `/login` 的响应结构（satoken + 用户信息）
- 替代方案：注册走 system 服务 `POST /users`（管理语义，含审计/管理员操作痕迹）——否决：管理接口与用户自助注册语义冲突，且跨服务事务复杂；用户自助注册集中在 auth 服务，直接操作 user 表（复用 system 服务的 UserMapper 依赖？不——auth 已有 `AuthUser` 实体与表，保持 auth 直连）

### D3: 手机号密码登录

- 现有 `POST /login` 按 username 匹配，注册时 `username = phone`，天然支持手机号+密码登录，**无需改后端**，前端仅把登录卡片"手机号"输入框映射为 username 字段
- 新增 migration 为 `sys_user.phone` 加唯一索引 `uk_phone`（防止并发注册重复账号），同时校验"注册时 username=phone"不冲突
- 密码错误统一提示"手机号或密码错误"（现状已是如此，保持）

### D4: 游客浏览与写操作鉴权

```
网关(8080) ── /api/article/** ──> article 服务
                                   │ SaTokenConfig：/articles(GET), /articles/{id}(GET) 加入白名单
                                   │ 其余（POST/PUT/DELETE 评论/点赞/收藏/发布）保持 @SaCheckLogin
```
- article 服务文章列表/详情 GET 接口加入匿名白名单；写接口保持登录校验，未登录抛 `NotLoginException` → 全局异常处理返回 `R.fail(401, "请先登录")`
- 热度排序：列表接口新增 `sort=time|hot` 参数；`hot` 按 `score = w1*views + w2*likes + w3*favorites + w4*comments` 在 SQL 层计算，权重走配置中心（Nacos），列表结果缓存 5 分钟（Caffeine/Redis）
- 网关 `AuthGatewayFilterFactory` 本次**不启用**（详见 Non-Goals），行为不变

### D5: 前端（portal 从零搭建）

- `packages/api` 新增 `services/auth.ts`（captcha/sms/send/smsLogin/login）、`services/article.ts`、`services/user.ts`；`packages/types` 补充登录、文章列表、用户类型
- portal 新增 `router/`（`/` 博客列表、`/login`、`/profile` 个人主页）、`layouts/` 顶部导航（右端：未登录显示"登录"按钮；已登录显示头像/昵称下拉 → 个人主页）、`views/login`（右侧登录卡片，验证码/密码双 Tab，按钮文案"登录/注册"与"登录"）、`views/home`（文章列表 + 时间/热度排序切换）、`views/profile`
- 滑块弹窗组件 `components/CaptchaSlider.vue`：拉取 `/captcha` 拼图，鼠标拖拽比对缺口位置，成功调 `/captcha/check`，失败自动刷新重试，弹窗超时 2 分钟失效；倒计时组件复用 Naive UI 能力
- 未登录写操作：现有 request.ts 401 拦截已存在，补充跳转 `/login` 并携带 `redirect` 回跳参数
- 替代方案：引入第三方滑块组件（如 vue-monoplasty-slide-verify）——否决：项目红线禁止引入未经审阅的第三方组件，且需对接现有后端 `/captcha` 协议，自研组件更可控

## Risks / Trade-offs

- [短信 Mock 阶段无法验证真实链路] → 通过抽象 `SmsSender` 接口 + `MockSmsSender`/`AliyunSmsSender` 实现，配置开关切换，Aliyun 接入时仅加实现类
- [验证码爆破/滥用] → 60s 发送频率限制 + 验证码错误次数计数（复用 LoginRiskService 模式）+ checkToken 一次性消费
- [username=phone 导致手机号变更不便] → 本版本不做改手机号功能，后续可加独立字段迁移
- [phone 唯一索引新增可能失败（存量脏数据）] → 项目未上线，直接修改 V1.0.0 SQL 并同步 AuthUser 表，无存量数据风险
- [热度权重拍脑袋] → 权重配置化（Nacos），上线后按数据调优，默认 `views:1, likes:3, favorites:4, comments:2`
- [滑块 2 分钟超时在服务端无法强制] → 依赖 checkToken 60s TTL 兜底（超时后 /sms/send 校验失败），前端弹窗超时仅为 UX 体验

## Migration Plan

1. DB：更新 `V1.0.0__init_system_tables.sql`（加 `uk_phone` 唯一索引）
2. auth 服务：新增 `/sms/send`、`/sms/login` 接口与 SmsSender 抽象，加入 SaTokenConfig 白名单
3. article 服务：列表/详情加入白名单 + sort 参数 + 热度计算
4. 前端：packages/api、types 扩展 → portal 路由/布局/登录页/滑块组件/个人主页
5. 回滚：删除新增接口白名单与前端新路由即可，不影响现有登录链路

## Open Questions

- 网关 `AuthGatewayFilterFactory` 是否在后续版本启用（统一网关侧鉴权）？本次保持现状，不影响本变更
- 热度排序是否需要"时间衰减"（如 7 天内新文章加权）？当前按全量静态权重实现，后续可迭代
