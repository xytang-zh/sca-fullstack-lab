## 1. 后端 core 响应体基座

模块：`spring-cloud-alibaba/spring-cloud-common/spring-cloud-common-core`

- [x] 1.1 新建 `ErrorCode` 接口（`getCode()/getUserMessage()/getDevMessage()/getHttpStatus()`），放 `com.xytang.common.core.response`
- [x] 1.2 改造 `BizCode` 枚举实现 `ErrorCode`：字段改为 `(code, httpStatus, userMessage, devMessage)`，按 design D2 区段+模块映射全部错误码（含 `SUCCESS(200)`、参数 1xxxx、用户权限 2xxxx、业务 3xxxx、系统 5xxxx、网关码），提供 `formatDevMessage(Object...)` 与 `fromCode(int)` 反查
- [x] 1.3 改造 `R<T>`：字段改为 `code(Integer)/message/data/timestamp(Long)/traceId`，移除 `bizCode/path/devMessage`，工厂方法 `ok()/ok(data)/ok(message,data)/fail(ErrorCode)/fail(ErrorCode,message)/fail(code,message)`，`isSuccess()` 改为 `code == 200`，`timestamp` 用 `System.currentTimeMillis()`
- [x] 1.4 新建 `PageResult<T>`（records/total/page/size/pages/hasPrevious/hasNext），提供 `of(list,total,page,size)`、`empty(page,size)` 工厂（core 红线禁止依赖 MyBatis-Plus，不提供 `from(IPage)`）
- [x] 1.5 改造 `PageQuery`：`pageNum/pageSize` 改为 `page=1/size=10`（保留 `@Min/@Max` 校验），删除 `PageVO` 并同步删除其引用
- [x] 1.6 改造 `BusinessException` 体系：基类字段 `BizCode` → `ErrorCode`，方法 `httpCode()/code()` → `getHttpStatus()/getCode()/getUserMessage()/getDevMessage()`，同步更新 12 个子类（保留枚举名，子类构造器签名不变）
- [x] 1.7 删除 `DevMessageHolder`、`PathHolder`（`com.xytang.common.core.response`），确认无残留引用
- [x] 1.8 更新 core 单测：`RTest`（新字段/工厂/isSuccess）、`BizCodeTest`（新码值/区段/formatDevMessage/fromCode）

验收：`cd spring-cloud-alibaba && mvn clean install -pl spring-cloud-common/spring-cloud-common-core -am -DskipTests`

## 2. 后端 web 通用层

模块：`spring-cloud-alibaba/spring-cloud-common/spring-cloud-common-web`

- [x] 2.1 改造 `GlobalExceptionHandler`：`BusinessException` → `R.fail(e.getErrorCode(), e.getMessage())` + `ResponseEntity.status(e.getErrorCode().getHttpStatus())`；参数校验/缺参/类型不匹配 → `R.fail(PARAM_ERROR...)`（HTTP 400）；`NotLoginException` → `R.fail(AUTH_TOKEN_MISSING)`（HTTP 401）；兜底 → `R.fail(SYS_ERROR)`（HTTP 500）；移除 `DevMessageHolder` 填充逻辑（堆栈仅打日志）
- [x] 2.2 改造 `RResponseAdvice`：仅回填 `traceId`（MDC → 回退 `X-Trace-Id` 头），移除 path/devMessage 填充与 `Environment`/`PathHolder`/`DevMessageHolder` 依赖
- [x] 2.3 改造 `TraceIdFilter`：保留 `X-Trace-Id` 读写与 MDC 清理，移除 `PathHolder`/`DevMessageHolder` 的 set/clear
- [x] 2.4 更新 web 单测 `GlobalExceptionHandlerTest`（新错误码/HTTP 映射/无 devMessage 字段）

验收：`cd spring-cloud-alibaba && mvn clean install -pl spring-cloud-common/spring-cloud-common-web -am -DskipTests`

## 3. 后端网关与安全

模块：`spring-cloud-alibaba/spring-cloud-gateway`、`spring-cloud-alibaba/spring-cloud-common/spring-cloud-common-security`

- [x] 3.1 改造 `AuthGatewayFilterFactory`：`GW_TOKEN_MISSING` → `GW_TOKEN_MISSING(29001)`、`GW_TOKEN_INVALID` → `GW_TOKEN_INVALID(29002)`，移除响应模板中的 `bizCode` 字段
- [x] 3.2 改造 `GatewayExceptionHandler`：`R.fail(bizCode).path(...)` → `R.fail(errorCode).traceId(...)`，移除 path 序列化与兜底 JSON 中的 `bizCode`/`path`

验收：`cd spring-cloud-alibaba && mvn clean install -pl spring-cloud-gateway -am -DskipTests`

## 4. 业务服务适配

模块：`spring-cloud-alibaba/spring-cloud-auth`、`spring-cloud-alibaba/spring-cloud-services/spring-cloud-{system,article,comment}`

- [x] 4.1 适配 `spring-cloud-auth`：`AuthServiceImpl` 的 `DevMessageHolder.set()` 改为日志（去掉敏感信息），`AuthServiceImplTest` 的 `getBizCode()` → `getErrorCode()` 断言更新
- [x] 4.2 适配 `spring-cloud-system`：`UserServiceImpl`/`FollowServiceImpl`/Controller 分页返回 `R<PageVO<...>>` → `R<PageResult<...>>`，入参 `pageNum/pageSize` → `page/size`
- [x] 4.3 适配 `spring-cloud-article`：`ArticleServiceImpl`/`ColumnServiceImpl`/`CaffeineConfig`/Controller 分页返回 `PageResult`，`ArticlePageQuery` 字段改 `page/size`
- [x] 4.4 适配 `spring-cloud-comment`：`CommentServiceImpl`/`CommentController` 分页返回 `PageResult`
- [x] 4.5 后端全量编译与 checkstyle 通过（`mvn clean install -DskipTests` 全绿）

验收：`cd spring-cloud-alibaba && mvn clean install -DskipTests`

## 5. 前端类型与 API 层

模块：`vue-web-ui/packages/types`、`vue-web-ui/packages/api`

- [x] 5.1 更新 `packages/types/src/index.ts`：`R` 改为 `{code:number; message:string; data:T|null; timestamp:number; traceId?:string}`，删除 `PageVO` 新增 `PageResult<T>`（records/total/page/size/pages/hasPrevious/hasNext），`PageQuery` 改为 `page/size`
- [x] 5.2 更新 `packages/types/src/blog.ts`、`system.ts` 中引用 `PageVO` 的类型为 `PageResult`，分页查询字段改 `page/size`
- [x] 5.3 改造 `packages/api/src/request.ts`：成功判定 `code === 200`（移除 `SUCCESS_BIZ_CODE`），`LOGIN_REQUIRED_CODES=[21005,21006,21007,21008,21009,21010,29001,29002]` 触发 `clearToken()`+跳登录，其余弹 `message`；更新 `request.test.ts`
- [x] 5.4 更新 `packages/api/src/services/{article,user,comment}.ts` 分页 API 返回类型为 `PageResult`，参数改 `page/size`

验收：`cd vue-web-ui && pnpm typecheck` ✓

## 6. 前端页面适配

模块：`vue-web-ui/src`

- [x] 6.1 适配 `src/views/Home.vue`、`src/components/CommentPanel.vue` 及 dashboard 各页面（Answers/ArticleAudit/Columns/CommentAudit/Drafts/Favorites/Follows/Likes/MyArticles/UserList/Write/Profile）改为 `PageResult.records`/`page`/`size`

验收：`cd vue-web-ui && pnpm typecheck && pnpm lint`

## 7. 文档与契约同步

- [x] 7.1 更新仓库根 `CLAUDE.md` §4.1 跨端契约：响应体五字段 `{code,message,data,timestamp,traceId}`、code 业务码区段、分页 `PageResult` 字段
- [x] 7.2 更新 `spring-cloud-alibaba/CLAUDE.md` §8.3/§8.4、`spring-cloud-services/CLAUDE.md`、`spring-cloud-auth/CLAUDE.md`、`spring-cloud-gateway/CLAUDE.md`、`spring-cloud-article/CLAUDE.md`、`spring-cloud-search/CLAUDE.md`、`spring-cloud-common-mongo/CLAUDE.md`、`spring-cloud-common-core/CLAUDE.md` 响应/分页/错误码说明
- [x] 7.3 更新 `vue-web-ui/CLAUDE.md` §8.2 响应格式与 `packages/api/CLAUDE.md`、`packages/types/CLAUDE.md` 契约说明

## 8. 整体验证

- [x] 8.1 后端 `mvn clean install -DskipTests` 全绿（含 checkstyle）；core/web/auth 测试 38 个全通过
- [x] 8.2 前端 `pnpm typecheck` 通过、`request.test.ts` 8 个测试通过（jsdom）；`pnpm lint` 因项目原有 ESLint 9 配置问题（`.eslintrc.cjs` 需迁移为 `eslint.config.js`）无法运行，非本次改动引入
- [ ] 8.3 启动网关 + auth + system 服务，联调验证：登录成功/失败返回新结构、分页接口返回 `PageResult`、未登录返回 `code=21009` 且前端跳登录（需本地基础设施 MySQL/Redis/Nacos，待用户环境手动验证）