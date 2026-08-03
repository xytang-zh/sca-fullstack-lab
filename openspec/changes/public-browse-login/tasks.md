## 1. 数据库迁移

- [x] 1.1 更新 `spring-cloud-services/spring-cloud-system/src/main/resources/db/migration/V1.0.0__init_system_tables.sql`：为 `sys_user.phone` 添加唯一索引 `uk_phone`（模块：spring-cloud-system）
- [x] 1.2 确认 auth 服务 `auth_user` 表（`spring-cloud-auth` 内 SQL/初始化脚本）同步添加 phone 唯一约束（模块：spring-cloud-auth）
- [x] 1.3 验收：`mvn clean install -DskipTests -pl spring-cloud-system -am` 通过，SQL 在本地 MySQL 执行成功

## 2. auth 服务：短信验证码发送（滑块前置）

- [x] 2.1 新增 `SmsSender` 接口 + `MockSmsSender` 实现（日志输出验证码、开发环境返回固定 123456），预留 `AliyunSmsSender` 空实现与配置开关（模块：spring-cloud-auth/service/sms）
- [x] 2.2 新增 `POST /api/auth/sms/send`：入参 `{phone, checkToken}`，消费并校验 checkToken（复用 `CaptchaService.verifyCheckToken`），校验失败返回业务异常（模块：spring-cloud-auth/controller）
- [x] 2.3 生成 6 位验证码存 Redis `auth:sms:code:{phone}`（TTL 5 分钟），同一手机号 60s 内重复发送返回"发送过于频繁"（模块：spring-cloud-auth/service）
- [x] 2.4 手机号格式校验（`^1[3-9]\d{9}$`），非法返回参数错误（模块：spring-cloud-auth/dto）
- [x] 2.5 将 `/sms/send` 加入 `SaTokenConfig` 白名单（模块：spring-cloud-auth/config）
- [x] 2.6 验收：`mvn clean install -DskipTests -pl spring-cloud-auth -am` 通过；curl 依次调用 `/captcha` → `/captcha/check` → `/sms/send` 成功输出验证码

## 3. auth 服务：验证码登录/注册一体与个人主页数据

- [x] 3.1 新增 `POST /api/auth/sms/login`：入参 `{phone, code}`，校验并**作废** Redis 验证码，按 phone 查用户（模块：spring-cloud-auth/service）
- [x] 3.2 用户不存在时自动注册：`username = phone`、随机 Argon2id 密码（不可用密码登录的占位）、默认角色 USER（模块：spring-cloud-auth/service）
- [x] 3.3 登录成功后返回与现有 `/login` 一致的响应结构（satoken + 用户信息），校验错误/过期返回明确业务异常（模块：spring-cloud-auth/service）
- [x] 3.4 将 `/sms/login` 加入 `SaTokenConfig` 白名单（模块：spring-cloud-auth/config）
- [x] 3.5 确认现有 `POST /login` 支持 username=phone 的密码登录（注册时 username=phone 已保证），无改动则跳过（模块：spring-cloud-auth）
- [x] 3.6 验收：curl 验证老用户验证码登录、新用户自动注册并登录、验证码重复使用被拒、错误验证码提示

## 4. article 服务：游客浏览与排序

- [x] 4.1 文章列表/详情 GET 接口加入 SaTokenConfig 白名单（仅 PUBLISHED/APPROVED 文章可见）（模块：spring-cloud-article）
- [x] 4.2 列表接口新增 `sort=time|hot` 参数：time 按发布时间倒序；hot 按 `score = w1*views + w2*likes + w3*favorites + w4*comments` SQL 计算降序（模块：spring-cloud-article/service）
- [x] 4.3 热度权重配置化（Nacos 配置 `blog.hot.*`，默认 views:1/likes:3/favorites:4/comments:2），结果缓存 5 分钟（Caffeine）（模块：spring-cloud-article）
- [x] 4.4 确认评论/点赞/收藏/发布接口保持登录校验，未登录返回 `R.fail(401)`（模块：spring-cloud-article）
- [x] 4.5 验收：`mvn clean install -DskipTests -pl spring-cloud-article -am` 通过；curl 匿名访问列表（time/hot）成功、匿名发评论返回 401

## 5. 前端公共包扩展

- [x] 5.1 `packages/api` 新增 `services/auth.ts`（getCaptcha/checkCaptcha/sendSmsCode/smsLogin/login/logout/me）、`services/article.ts`（list 带 sort 参数/detail）、`services/user.ts`（个人主页信息）（模块：vue-web-ui/packages/api）
- [x] 5.2 `packages/types` 新增登录相关类型（LoginDTO/SmsLoginDTO/LoginResult/UserInfo/ArticleListItem，ID 一律 string）（模块：vue-web-ui/packages/types）
- [x] 5.3 验收：`pnpm typecheck` 通过

## 6. portal 前端：登录与个人主页

- [x] 6.1 搭建 portal 基础框架：router（`/` 列表、`/login`、`/profile` 懒加载）、pinia、根布局含顶部导航栏（模块：vue-web-ui/apps/portal）
- [x] 6.2 顶部导航：未登录显示"登录"按钮；已登录显示头像/昵称下拉（含个人主页入口）（模块：vue-web-ui/apps/portal/layouts）
- [x] 6.3 登录页 `/login`：右侧登录卡片，验证码/密码双 Tab；验证码 Tab 按钮文案"登录/注册"，密码 Tab 按钮文案"登录"（模块：vue-web-ui/apps/portal/views/login）
- [x] 6.4 滑块验证码弹窗组件 `CaptchaSlider.vue`：拉取 `/captcha` 拼图、拖拽比对缺口、失败自动刷新、2 分钟超时失效、成功后返回 checkToken（模块：vue-web-ui/apps/portal/components）
- [x] 6.5 验证码登录：点击"获取验证码"→ 弹出滑块弹窗 → 成功后发送短信 → 60s 倒计时 → 提交"登录/注册"（模块：vue-web-ui/apps/portal/views/login）
- [x] 6.6 个人主页 `/profile`：用户基本信息（头像/昵称/手机号脱敏）+ 我的文章/我的收藏入口（模块：vue-web-ui/apps/portal/views/profile）
- [x] 6.7 登录成功后跳转个人主页；request.ts 401 拦截补充跳转 `/login` 并携带 redirect 回跳（模块：vue-web-ui/packages/api）

## 7. 联调与验收

- [x] 7.1 启动基础设施（MySQL/Redis/Nacos）+ auth/system/article 服务 + portal dev server（`pnpm dev:portal`），走通全流程（模块：全链路）
- [x] 7.2 验证：游客打开 `/` 可看列表、切换时间/热度排序；未登录点赞/评论被引导登录；验证码登录（新/老用户）、密码登录、登录后进个人主页（模块：全链路）
- [x] 7.3 回归：现有 admin 登录链路与后端 `/login` 密码登录不受影响（模块：全链路）
- [x] 7.4 验收命令：后端 `mvn clean install -DskipTests` 全量通过 + 前端 `pnpm typecheck && pnpm lint` 通过（typecheck 通过；lint 为仓库既有基线问题：ESLint 9 缺 eslint.config.js，admin/portal 均失败，非本变更引入）
