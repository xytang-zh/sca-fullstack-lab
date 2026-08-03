# Design — 登录滑块验证码（tianai-captcha）

## Context

现状见 proposal.md。关键约束：`AuthServiceImpl.login` 已按"先验证码、后查库"顺序实现；`LoginRiskService` 已有失败 5 次锁定 15 分钟机制（`auth:login:fail:{username}`）；`GET /captcha` 与 `POST /login` 均为匿名接口；前端登录页在 `apps/admin`，HTTP 统一走 `@sca/api` request。验证码与登录必须同请求绑定（一次性凭据，防绕过）。

## Goals / Non-Goals

**Goals**
- 后端无痛替换验证码内核（自绘 → tianai），对外契约改为滑块语义
- 登录交互改为"点登录 → 弹滑块 → 通过后提交认证"
- 验证码与登录请求强绑定，攻击者无法跳过滑块直接打登录接口

**Non-Goals**
- 不做商业级风控引擎（设备指纹、无感验证、风险动态放行）——超出开源组件能力，后续如需再评估商业方案
- 不做"失败 N 次后强制验证码"的渐进式增强——本次每次登录都出滑块，该增强可在风控模块迭代时追加
- 不替换短信/邮箱验证码场景

## Decisions

### D1: 验证码内核选 tianai-captcha 1.5.2（Central 最新）

**选择**：`cloud.tianai.captcha:tianai-captcha-springboot-starter`，版本 `1.5.2` 由父 POM `<properties>` 声明（`<tianai-captcha.version>`），子模块 POM 不写版本。

**理由**：
- 与 `spring-cloud-auth/CLAUDE.md` 原规划一致（滑动验证码）
- 轨迹规则检测（7 项：时长/点数/起点/y 轴波动/区间跳跃/速率变化/x 轴超宽），抵御模拟拖拽——注意 1.5.2 无"行为分数 min-score"机制，`BasicCaptchaTrackValidator` 为规则式检测（已据实修正 spec）
- 支持滑块/点选/旋转多种类型，后续可扩展
- Spring Boot Starter 自动装配 + 自带 `RedisCacheStore`（存在 StringRedisTemplate 即自动启用），集成成本低

**替代方案**：
- AJ-Captcha 1.4.0：Maven Central 仅 1.4.0 一个版本（2025-01 发布），维护频率低；滑块+点选够用但轨迹分析弱于 tianai
- 商业方案（易盾/极验/腾讯防水墙）：风控最强但需付费 + SDK 较大，对自研项目过重，列为远期选项

### D2: 登录时序——点登录后弹滑块，滑块通过后同请求提交

**选择**：用户输入账号密码 → 点击登录 → 前端 `GET /captcha` 获取滑块数据 → 弹窗展示 → 用户滑动 → 前端将 captchaId + 滑动轨迹 `ImageCaptchaTrack` 连同 username/password 一次提交 `POST /login` → 后端先 `matching()` 校验轨迹（`min-score` 阈值）→ 通过后查库验密。

**理由**：
- 若依系主流模式，用户预期一致
- 验证码凭据与登录请求同一 HTTP 请求到达，从结构上杜绝"跳过验证码直接打登录"（`LoginDTO` 现有"一次提交"结构天然契合）
- 后端无需维护"预校验通过凭证"的二次凭证生命周期

**替代方案**：
- 两步式（滑块通过后签发一次性 token，登录时带 token）：多一跳凭证管理，且二次凭证本身成为新攻击面，不选
- 登录页加载即弹验证码：用户未点登录就被打扰，体验差，不选

### D3: 凭据存储用 Starter 自带 RedisCacheStore

**选择**：不自行实现缓存，Starter 的 `CacheStoreAutoConfiguration` 在检测到 `StringRedisTemplate` Bean 时自动装配 `RedisCacheStore`（项目已有 Redis 配置，自动生效）；Key 格式 `{captcha.prefix}:{id}`，通过配置 `captcha.prefix: auth:captcha` 对齐 `auth:captcha:{key}` 规范；TTL 通过 `captcha.expire` 配置（默认仅 20 秒，配置为 2 分钟满足 spec）。

**理由**：auth 服务多实例部署时本地缓存互相不可见，Redis 保证一致性；复用官方实现减少自研维护面；`getAndRemoveCache` 用 Lua 原子"取并删"，天然满足一次性消费。

### D4: 验证器替换为规则式轨迹检测 + 业务开关配置

**选择**：
- 自定义 `ImageCaptchaValidator` Bean 替换默认 `SimpleImageCaptchaValidator`（仅缺口位置容差校验）为 `BasicCaptchaTrackValidator`（在位置校验基础上叠加 7 项轨迹规则检测），默认容差 0.02
- `captcha.init-default-resource: true` 加载 jar 内置滑块素材（默认 false 不加载）
- 业务开关 `auth.captcha.enabled: true` 走 Nacos 配置（`spring-cloud-auth.yaml`），与文档 §10.3 既有配置对齐，代码中按开关短路验证码流程

**理由**：1.5.2 无评分制，规则式检测为库原生能力；素材与过期时间均需显式配置否则生成失败/TTL 过短。

## Risks / Trade-offs

- **[tianai 与 Spring Boot 3.5 兼容性未官方确认]** → 集成阶段先做 smoke test（起 auth 服务调 `GET /captcha`），1.5.x 官方声明支持 Boot 2/3；若遇自动装配冲突，回退为手动注入 `ImageCaptchaApplication` Bean
- **[前端 SDK 引入方式非标准 npm 包]** → tianai 前端组件从 Gitee Releases 下载（tac 目录），放入 `apps/admin` 本地资源目录，组件内 `import` 相对路径；不发布到 npm registry
- **[滑块素材资源体积]** → 默认素材 jar 内自带（约几百 KB），可接受；后续可在 `docker/` 或 Nacos 配置自定义素材路径
- **[轨迹规则误伤真实用户]** → 规则为常见人机差异阈值（300ms/点数/波动），失败不锁定（验证码失败不计入锁定计数，见 spec），仅提示重试
- **[破坏性契约变更]** → `GET /captcha` 响应与 `LoginDTO` 字段同时变更，前后端必须同版本发布；后端先行兼容阶段不可行（验证码语义本身变了），发布窗口内登录功能整体不可用（内部系统可接受）

## Migration Plan

1. 后端：父 POM 加版本属性 → 新增 `CaptchaConfig`（min-score/enabled 配置绑定）→ 重写 `CaptchaServiceImpl`（tianai 生成/校验 + Redis 缓存）→ 调整 `LoginDTO`/`CaptchaVO` → 同步改造 `AuthServiceImpl` 第 1 步与测试
2. 前端：下载 tianai SDK → `packages/types` 更新登录接口类型 → 登录页加入滑块弹窗组件 → 联调
3. 验证：`mvn clean test -pl spring-cloud-auth -am` + checkstyle；前端 `pnpm typecheck` + `pnpm dev:admin` 手工走通登录全流程
4. 回滚：`git revert` 对应提交（前后端一起回滚）；Nacos 配置 `auth.captcha.enabled: false` 可在代码含开关时临时关闭（本设计不引入代码开关，回滚以 revert 为主）

## Open Questions

- 滑块弹窗的视觉样式（Naive UI 风格 vs tianai 默认皮肤）——实现阶段按 UI 规范定，不影响行为契约
- 后续是否追加"失败 N 次后强制验证码"渐进式策略——留给风控模块迭代，不影响本 change 的 tasks
