## 1. 后端依赖与配置

- [x] 1.1 `spring-cloud-alibaba/pom.xml` 新增 `<tianai-captcha.version>1.5.2</tianai-captcha.version>` 属性（验收：`mvn validate` 通过）
- [x] 1.2 `spring-cloud-auth/pom.xml` 引入 `cloud.tianai.captcha:tianai-captcha-springboot-starter`（不写版本）（验收：`mvn clean install -DskipTests -pl spring-cloud-auth -am` 通过）
- [x] 1.3 新增 `config/CaptchaConfig.java`：`@ConfigurationProperties("auth.captcha")` 绑定 `enabled`（默认 true），并提供 `BasicCaptchaTrackValidator` Bean 替换默认验证器（验收：配置可被注入、轨迹验证器生效）

## 2. 后端验证码服务改造

- [x] 2.1 `CaptchaServiceImpl` 重写 `generate()`：调用 tianai `ImageCaptchaApplication.generateCaptcha(SLIDER)`，返回 captchaId + 背景图(base64) + 滑块图 + 缺口坐标（验收：`GET /captcha` 返回滑块语义结构）
- [x] 2.2 配置 Redis 缓存与素材：`application-dev.yml`/Nacos 配置 `captcha.prefix: auth:captcha`、`captcha.expire.default: 120000`、`captcha.init-default-resource: true`（Starter 检测 StringRedisTemplate 自动装配 RedisCacheStore，Key 形如 `auth:captcha:{id}`）（验收：生成后 Redis 可见对应 Key，2 分钟过期）
- [x] 2.3 重写 `verify(captchaId, track)`：`application.matching(captchaId, track)` 规则检测，内部 `getAndRemoveCache` 原子消费（一次性）（验收：同凭据二次校验失败）
- [x] 2.4 `CaptchaVO` 结构调整为滑块语义（captchaId/背景图/滑块图/缺口坐标），同步 `CaptchaController`（验收：接口文档结构正确）

## 3. 登录链路改造

- [x] 3.1 `LoginDTO`：`captcha`/`captchaKey` → `captchaId`（String）+ `track`（tianai `ImageCaptchaTrack`），保留 `@Valid` 校验（验收：编译通过，字段类型对齐前端）
- [x] 3.2 `AuthServiceImpl.login` 第 1 步改为 tianai 轨迹校验，验证码失败**不计入**登录失败锁定计数（验收：`AuthServiceImplTest` 覆盖"验证码失败不锁定"）
- [x] 3.3 同步改造 `CaptchaServiceTest`/`AuthServiceImplTest`（验收：`mvn clean test -pl spring-cloud-auth -am` 全绿）

## 4. 前端改造

- [ ] 4.1 下载 tianai 前端 SDK（Gitee Releases tac 目录）放入 `vue-web-ui/apps/admin` 本地资源目录（验收：SDK 文件存在且可被 import）
- [ ] 4.2 `packages/types` 更新登录相关类型：`CaptchaVO`、`LoginDTO` 对齐滑块语义（验收：`pnpm typecheck` 通过）
- [x] 4.3 登录页改造：点登录 → 请求 `GET /captcha` → 弹滑块组件 → 滑动通过后携 captchaId + track 提交 `POST /login`（走 `@sca/api` request）（验收：`pnpm dev:admin` 手工走通登录全流程）

## 5. 联调与验证

- [x] 5.1 后端全量验证：`mvn clean test -pl spring-cloud-auth -am` + `mvn checkstyle:check -pl spring-cloud-auth`（验收：全绿）
- [ ] 5.2 前端验证：`pnpm typecheck` + `pnpm build`（验收：无类型错误、构建通过）
- [x] 5.3 端到端场景验证（对照 specs/login-captcha 场景）：正常滑块登录成功 / 直接登录无凭据被拒 / 同凭据复用被拒 / 过期失效 / 密码错误后凭据失效（验收：所有场景符合 spec）
