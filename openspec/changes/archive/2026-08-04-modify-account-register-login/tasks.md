## 1. 后端 spring-cloud-auth 改造

- [x] 1.1 移除 tianai 依赖与配置：父 POM `spring-cloud-alibaba/pom.xml` 删除 `<tianai-captcha.version>` property 与 dependencyManagement 中 tianai-captcha-springboot-starter 条目；`spring-cloud-auth/pom.xml` 删除 tianai-captcha 依赖并新增 `hutool-captcha` 依赖（仅 groupId/artifactId，版本由父 POM hutool-bom 统一管理）；删除 `config/CaptchaConfig.java`；清理 `application.yml` 中 tianai-captcha 配置段。验收：`mvn validate -pl spring-cloud-auth -am` 通过 checkstyle，且全库 grep 无 tianai 依赖声明
- [x] 1.2 改造 `vo/CaptchaVO.java`：字段改为 `captchaKey`（String）+ `imageBase64`（String），删除滑块相关字段（type/backgroundImage/templateImage/尺寸）。验收：编译通过
- [x] 1.3 改造 `service/CaptchaService.java` 接口与 `impl/CaptchaServiceImpl.java`：删除 tianai 相关方法 `check()`/`verifyCheckToken()`；`generate()` 用 `CaptchaUtil.createLineCaptcha` 生成 4 位文字验证码，答案存 Redis `auth:captcha:image:{captchaKey}`（TTL 5 分钟，key 用 UUID），返回 `CaptchaVO{captchaKey, imageBase64}`；新增 `verify(captchaKey, code)`：Redis 取值后 `equalsIgnoreCase` 比较、校验成功即 delete（一次性）；删除 tianai 自定义验证器 `service/impl/TrackNormalizeCaptchaValidator.java`。`AuthConstants` 新增验证码 Redis key 前缀常量。验收：`mvn compile -pl spring-cloud-auth -am`，且全库 grep 无 tianai/imageCaptcha 代码引用
- [x] 1.4 `dto/LoginDTO.java`：`username` 更名 `account`（@NotBlank @Size(6,18)）；新增必填 `captchaKey`/`captchaCode`（@NotBlank）；删除可选 `checkToken`；保留 `rememberMe`。新增 `dto/RegisterDTO.java`：`account`（@NotBlank @Pattern `^[a-zA-Z][a-zA-Z0-9]{5,17}$`）、`password`（@NotBlank @Size(8,32)）、`confirmPassword`（@NotBlank）。验收：编译通过
- [x] 1.5 `service/AuthService.java` + `impl/AuthServiceImpl.java`：`login()` 先校验文字验证码（失败抛"验证码错误或已过期"，不查账号），再走锁定检查/查用户/密码校验/状态校验/doLogin 原流程（密码失败仍统一"账号或密码错误"）；新增 `register(RegisterDTO)`：确认两次密码一致、查重（已存在抛 409"账号已存在"）、Argon2id 加密插入（默认 USER 角色，复用 registerByPhone 的 insertUserRole 模式，DuplicateKeyException 兜底），成功后复用 doLogin 自动登录。验收：`mvn compile -pl spring-cloud-auth -am`
- [x] 1.6 `controller/AuthController.java`：`GET/POST /captcha` 保留但返回新 CaptchaVO；`POST /login` 入参换 LoginDTO；新增 `POST /register`；删除 `POST /captcha/check`、`POST /sms/send`、`POST /sms/login` 及 `smsService` 注入。删除 `dto/SmsLoginDTO.java`、`dto/SmsSendDTO.java`、`dto/CaptchaCheckDTO.java`、`service/SmsService.java`、`service/impl/SmsServiceImpl.java`（确认无其他引用后）。验收：`mvn clean compile -pl spring-cloud-auth -am` 通过
- [x] 1.7 后端编译与规范验证：`mvn clean install -pl spring-cloud-auth -am -DskipTests`（含 checkstyle 强制校验）。验收：构建成功

## 2. 共享包 types / api 适配

- [x] 2.1 `packages/types/src/auth.ts`：`LoginDTO` 改为 `{ account, password, captchaKey, captchaCode, rememberMe }`；新增 `RegisterDTO { account, password, confirmPassword }`；`CaptchaVO` 改为 `{ captchaKey, imageBase64 }`；删除 `CaptchaCheckResult`/`SmsLoginDTO`/`SmsSendDTO`（确认无其他引用后）。验收：`pnpm typecheck`
- [x] 2.2 `packages/api/src/services/auth.ts`：`getCaptcha()` 返回新 `CaptchaVO`；新增 `register(dto)`；`login(dto)` 不变；删除 `checkCaptcha`/`sendSmsCode`/`smsLogin`。验收：`pnpm typecheck`

## 3. portal 前端改造

- [x] 3.1 `apps/portal/src/store/user.ts`：`loginByPassword` 更名 `loginByAccount(dto: LoginDTO)`；新增 `register(dto: RegisterDTO)`（成功后走 `saveLogin` 自动登录）；删除 `loginBySms`。验收：`pnpm --filter @sca/portal typecheck`
- [x] 3.2 重写 `apps/portal/src/views/Login.vue`：`<script setup>` + Naive UI（`n-form`/`n-form-item`/`n-input`/`n-button`/`n-image`/`n-text`）；三个输入框：账号、密码（type=password）、验证码（输入框右侧 `n-image` 小图展示 `captchaVO.imageBase64`，点击刷新重新请求 `getCaptcha`）；登录成功后 `router.replace(redirect || '/profile')`；底部"立即注册"入口跳 `/register`；删除 Tab/滑块/倒计时/手机号校验逻辑；features 文案改为账号密码登录。验收：`pnpm --filter @sca/portal typecheck`
- [x] 3.3 新增 `apps/portal/src/views/Register.vue`：`n-form` 三个输入框（账号、密码、再次输入密码），rules 校验账号正则（6-18 位、字母开头、字母数字）与两次密码一致，提交 `userStore.register` 成功跳 `/profile`；"已有账号？去登录"跳 `/login`；页面风格与 Login.vue 一致。验收：`pnpm --filter @sca/portal typecheck`
- [x] 3.4 `apps/portal/src/router/index.ts`：新增 `/register` 路由（懒加载 `() => import('@/views/Register.vue')`）。验收：`pnpm --filter @sca/portal typecheck`
- [x] 3.5 删除 `apps/portal/src/components/CaptchaSlider.vue` 及所有引用（Login.vue、components.d.ts 由插件重生成）。验收：`pnpm --filter @sca/portal typecheck && pnpm lint`

## 4. admin 前端适配（连带：/captcha 与 /login 契约变更）

- [x] 4.1 `apps/admin/src/api/auth.ts`：`getCaptcha()` 返回新 `CaptchaVO`；删除 `checkCaptcha`。验收：`pnpm --filter @sca/admin typecheck`
- [x] 4.2 修改 `apps/admin/src/views/login/Login.vue`：移除 `CaptchaModal` 依赖与 `captchaRequired` 条件弹窗逻辑；登录表单内嵌验证码输入框 + 图片（点击刷新），提交携带 `captchaKey`/`captchaCode`。验收：`pnpm --filter @sca/admin typecheck`
- [x] 4.3 删除 `apps/admin/src/components/common/CaptchaModal.vue`。验收：`pnpm --filter @sca/admin typecheck && pnpm lint`

## 5. 端到端验证

- [x] 5.1 启动后端（auth + gateway）与 portal，浏览器验证注册页：合法账号注册成功并自动登录跳个人主页；重复账号提示"账号已存在"；非法账号（过短/数字开头/含特殊字符）提示格式错误；两次密码不一致被拦截。验收：Playwright 浏览器操作
- [x] 5.2 验证登录页：正确账号密码+验证码登录成功；密码错误提示"账号或密码错误"；验证码错误提示并刷新图片；大小写不同验证码可通过；同一验证码二次使用失败；点击图片可刷新新验证码。验收：Playwright 浏览器操作
- [x] 5.3 验证 admin 登录页：内嵌文字验证码展示正常，账号+密码+验证码登录成功。验收：Playwright 浏览器操作
- [x] 5.4 回归：portal 已登录用户右上角信息与个人主页入口正常；`pnpm build`（admin + portal）与后端 `mvn clean install -DskipTests` 全绿。验收：构建命令通过
