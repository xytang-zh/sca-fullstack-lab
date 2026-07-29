---

description: "Platform MVP Foundation 实现任务清单"
---

# 任务清单：Platform MVP Foundation（一体化平台基线）

**输入**：来自 `specs/001-platform-mvp/` 的设计文档（spec.md / plan.md / research.md / data-model.md / contracts/ / quickstart.md）

**前置条件**：plan.md（必需）、spec.md（4 个用户故事）、research.md、data-model.md、contracts/

**测试**：按宪法 §质量门禁要求，集成测试集中在 Phase 7 打磨阶段；各用户故事内不强制先写测试。

**组织方式**：任务按用户故事分组，使每个故事可独立实现与测试，并作为 MVP 增量交付。

## 格式：`[ID] [P?] [Story] 描述`

- **[P]**：可并行（不同文件、无依赖）
- **[Story]**：本任务属于哪个用户故事（US1/US2/US3/US4）
- 描述中包含精确文件路径（仓库相对路径）

## 路径约定

- **后端**：`spring-cloud-alibaba/{module}/src/main/java/com/xytang/{module}/...`
- **前端**：`vue-web-ui/apps/{admin|portal}/src/...`
- **共享前端包**：`vue-web-ui/packages/{ui|types|utils}/...`
- **SQL 与配置**：`spring-cloud-alibaba/spring-cloud-test/sql/` 与 `spring-cloud-alibaba/{module}/src/main/resources/`

---

## Phase 1：初始化（共享基础设施）

**目的**：搭建后端聚合工程、前端聚合工程与基础项目骨架

- [ ] T001 创建后端父 POM `spring-cloud-alibaba/pom.xml`：声明 Spring Boot 3.5.0 / Spring Cloud 2025.0.0 / Spring Cloud Alibaba 2025.0.0 / Sa-Token 1.44.0 / MyBatis-Plus 3.5.9 / Redisson 4.0.0 / ShardingSphere 5.5.2 / Hutool 5.8.27 等版本与 `<dependencyManagement>`
- [ ] T002 创建公共模块聚合 `spring-cloud-alibaba/spring-cloud-common/pom.xml` 与 17 个子模块骨架（spring-cloud-common-core/redis/mybatis/security/cache/mq/log/lock/rpc/es/mongo/oss/tdengine/job/starter-web/starter-log/test）
- [ ] T003 创建自定义 Starter 聚合 `spring-cloud-alibaba/spring-cloud-starters/pom.xml` 与 2 个 Starter 骨架（spring-cloud-starter-satoken、spring-cloud-starter-rbac）
- [ ] T004 创建 5 个业务服务模块骨架（spring-cloud-gateway/auth/system/log/portal）+ spring-cloud-test 集成测试模块，每个含 `pom.xml` + `Application.java` + `bootstrap.yaml`
- [ ] T005 [P] 创建前端聚合工程 `vue-web-ui/pnpm-workspace.yaml` + `package.json`，初始化 `apps/admin`、`apps/portal`、`packages/{ui,types,utils}`
- [ ] T006 [P] 配置前端代码规范 `vue-web-ui/.eslintrc.cjs`、`.prettierrc`、`tsconfig.base.json`（strict 模式）
- [ ] T007 [P] 配置后端代码规范 `spring-cloud-alibaba/src/checkstyle.xml`、`spotbugs-exclude.xml`，在父 POM 绑定 `maven-checkstyle-plugin` 与 `spotbugs-maven-plugin`

---

## Phase 2：基础（阻塞性前置）

**目的**：在任何用户故事开工前必须完成的核心基础设施

**⚠️ 关键**：本阶段未完成前，不得开始任何用户故事

- [ ] T008 [P] 实现 `spring-cloud-common-core` 模块：`R<T>` 统一响应、`BusinessException`/`AuthException`/`PermissionException`/`BizException`/`SystemException` 异常体系、`GlobalExceptionHandler`、`PageVO<T>`、`PageQuery` 基类、`MetaObjectHandler` 自动填充审计字段、雪花 ID 生成器
- [ ] T009 [P] 实现 `spring-cloud-common-mybatis` 模块：`MybatisPlusInterceptor` 配置（顺序：DataPermission → Pagination → OptimisticLocker）、`OptimisticLockerInnerInterceptor` 注册、`@Version` 注解支持、`MetaObjectHandler` 自动填充 `creator/create_time/updater/update_time`
- [ ] T010 [P] 实现 `spring-cloud-common-mybatis` 自研 `DataPermissionInnerInterceptor`（基于 JsqlParser 拼接 SQL `IN` 子查询）+ `@DataScope(deptAlias, userAlias)` 注解 + `DataPermissionContext` ThreadLocal 传递当前用户与角色
- [ ] T011 [P] 实现 `spring-cloud-common-redis` 模块：Redis + Redisson 配置、`@DistributedLock(lockKey, ttl=30s)` 注解 + AOP 切面、缓存 Key 工具类（格式 `spring-cloud:{service}:{biz}:{id}` + TTL ±10% 随机）
- [ ] T012 [P] 实现 `spring-cloud-common-mq` 模块：RabbitMQ 配置、`AbstractEventListener<T>` 抽象类（幂等消费 + `eventId` 去重）、事件总线 `EventBus.publish(event)`
- [ ] T013 [P] 实现 `spring-cloud-common-security` 模块：Sa-Token 公共配置、`StpInterfaceImpl`（权限点与角色查询）、`SaTokenConfigure`（路由拦截器 + 全局过滤器）
- [ ] T014 实现 `spring-cloud-common-log` 模块：`@OperationLog` 注解 + AOP 切面（捕获模块/操作类型/入参/IP/耗时）、`LogParamFilter` 敏感字段脱敏（密码/Token/身份证号/手机号）、RabbitMQ 异步发送 `log.operation.create` / `log.login.create` 事件 + 本地兜底落盘 `logs/operation-fallback.log` + 告警
- [ ] T015 实现 `spring-cloud-common-starter-web` 模块：`ThreadPoolTaskExecutor` 显式线程池配置、全局 CORS 配置、`XssFilter` 跨站脚本清洗、`TraceFilter` 链路追踪（生成 `X-Trace-Id` 写入 MDC）
- [ ] T016 [P] 创建数据库初始化脚本 `spring-cloud-alibaba/spring-cloud-test/sql/init.sql`：建表 `sys_user` / `sys_role` / `sys_menu` / `sys_dept` / `sys_dict` / `sys_param` / `sys_notice` / `portal_content` / `sys_user_role` / `sys_role_menu` / `sys_role_dept` / `sys_oauth2_client` + 12 张日志分表 `sys_operation_log_202607` ~ `sys_login_log_202607`（覆盖 1 年滚动窗口）
- [ ] T017 [P] 实现 `spring-cloud-starter-satoken`：一站式 Sa-Token SSO + RBAC Starter，封装登录/登出/踢人下线/权限校验/SSO 模式三自动配置
- [ ] T018 [P] 实现 `spring-cloud-starter-rbac`：一站式数据权限 + 乐观锁 Starter，封装 `@DataScope` 注解 + `DataPermissionInnerInterceptor` 自动注册 + `OptimisticLockerInnerInterceptor` 自动注册
- [ ] T019 实现 `spring-cloud-gateway` 网关：路由表（按 `contracts/gateway-routes.md` §1 配置 5 条路由）、`AuthFilter`（鉴权 + `excludePaths` 白名单）、`RateLimitFilter`（Redis 令牌桶，按 §6 限流维度）、`TraceFilter`（透传 `X-Trace-Id` + `X-Login-Id`）、全局 CORS、Knife4j 文档聚合端点 `/doc.html`

**检查点**：基础设施就绪——可开始用户故事并行实现；后端 5 个服务能启动并健康检查通过

---

## Phase 3：用户故事 1 - 超级管理员登录与基础系统管理（优先级：P1）🎯 MVP

**目标**：超级管理员登录后完成用户/角色/菜单/部门/字典/参数/通知七类系统管理 CRUD

**独立测试**：创建一个角色 → 分配菜单 → 创建用户并绑定 → 用新用户登录验证权限（对齐 quickstart 场景 2 + SC-006）

### 用户故事 1 的实现

- [ ] T020 [P] [US1] 创建 `sys_user` 实体 + Mapper + DTO/VO（`spring-cloud-system/src/main/java/com/xytang/system/entity/User.java`、`mapper/UserMapper.java`、`dto/UserCreateDTO.java`、`dto/UserUpdateDTO.java`、`dto/UserPageQuery.java`、`vo/UserVO.java`、`vo/UserListVO.java`）含 `@Version` 乐观锁、`@TableLogic` 逻辑删除、五态状态字段
- [ ] T021 [P] [US1] 创建 `sys_role` + `sys_role_menu` + `sys_role_dept` 实体 + Mapper + DTO/VO（`entity/Role.java`、`entity/RoleMenu.java`、`entity/RoleDept.java`、`vo/RoleVO.java`、`dto/RoleCreateDTO.java`）含五级 `dataScope` 字段
- [ ] T022 [P] [US1] 创建 `sys_menu` 实体 + Mapper + DTO/VO（`entity/Menu.java`、`vo/MenuTreeVO.java` 树形结构）含 `menu_type`/`perms`/`parent_id` 字段
- [ ] T023 [P] [US1] 创建 `sys_dept` 实体 + Mapper + DTO/VO（`entity/Dept.java`、`vo/DeptTreeVO.java`）含 `ancestors` 字段与 Service 层自动维护逻辑
- [ ] T024 [P] [US1] 创建 `sys_dict` / `sys_param` / `sys_notice` 实体 + Mapper + DTO/VO（`entity/Dict.java`、`entity/Param.java`、`entity/Notice.java`）含 Caffeine+Redis 多级缓存配置与 `@RefreshScope` 热刷新
- [ ] T025 [US1] 实现 `UserService`（`spring-cloud-system/src/main/java/com/xytang/system/service/UserService.java`）：CRUD + 五态状态机迁移（FR-026）+ 乐观锁 + 数据权限 + 禁止删除最后一个超级管理员（FR-012）+ 禁止禁用/踢自己 + 密码 BCrypt 加密
- [ ] T026 [US1] 实现 `RoleService` + `MenuService` + `DeptService` + `DictService` + `ParamService` + `NoticeService`（同 service 包）：菜单树编辑走 `@DistributedLock(lock:menu:tree)`、角色权限批量分配走 `@DistributedLock(lock:role:{roleId})`（FR-028）
- [ ] T027 [US1] 实现 `AuthController`（`spring-cloud-auth/src/main/java/com/xytang/auth/controller/AuthController.java`）：`POST /api/auth/login` + `GET /api/auth/captcha` + `POST /api/auth/logout` + `GET /api/auth/me` + `PATCH /api/auth/me/password`，按 `contracts/auth-api.md` §1/§2/§6/§7 实现，登录失败 5 次锁定 15 分钟（FR-003）
- [ ] T028 [US1] 实现 `UserController` + `RoleController` + `MenuController` + `DeptController` + `DictController` + `ParamController` + `NoticeController`（`spring-cloud-system/.../controller/`）：按 `contracts/system-api.md` §1-§7 实现 CRUD + 业务动作（reset-password / unlock / allocate-menus / allocate-data-scope / publish / revoke）+ `@SaCheckPermission` + `@DataScope`
- [ ] T029 [US1] 实现 `spring-cloud-auth` 模块的 Sa-Token 集成：登录成功后 `StpUtil.login(userId)`、踢人下线预埋 `StpUtil.kickout(userId)` 接口（具体逻辑在 US2 完成）、登录日志异步发送 `log.login.create` 事件
- [ ] T030 [US1] 实现前端 admin 登录页 + 工作台首页（`vue-web-ui/apps/admin/src/views/login/Login.vue` + `views/dashboard/Dashboard.vue`）：含验证码组件 + Axios 拦截器（统一处理 `R<T>` 与 401/403/429）+ Pinia user store + 路由守卫
- [ ] T031 [US1] 实现前端 admin 系统管理 7 个页面（`views/system/UserList.vue` / `RoleList.vue` / `MenuTree.vue` / `DeptTree.vue` / `DictList.vue` / `ParamList.vue` / `NoticeList.vue`）+ 共享 `packages/ui/` 通用 CRUD 组件 + `packages/types/` TS 类型定义

**检查点**：用户故事 1 功能完整、可独立测试——admin 可登录、完成 7 类系统管理 CRUD、数据权限生效

---

## Phase 4：用户故事 2 - SSO 单点登录与单点注销（优先级：P2）

**目标**：管理员在一体化平台登录后免登访问任何子系统；登出或被踢下线时所有子系统同步下线；OAuth2 Server 供第三方接入

**独立测试**：在一体化平台登录 → 新标签访问子系统受保护页面免登进入 → 一体化平台登出 → 子系统下次操作跳回登录中心（对齐 quickstart 场景 3 + SC-002）

### 用户故事 2 的实现

- [ ] T032 [P] [US2] 创建 `sys_oauth2_client` 实体 + Mapper + DTO/VO（`spring-cloud-auth/.../entity/OAuth2Client.java`）含 `client_id` / `client_secret`（BCrypt）/ `redirect_uris` / `grant_types` 字段
- [ ] T033 [US2] 实现 Sa-Token SSO 模式三配置（`spring-cloud-auth/src/main/resources/application-sso.yaml` + `spring-cloud-auth/.../config/SsoConfig.java`）：`sa-token.sso.mode=3`、Server 端 `SaSsoServerUtil` 调用、Client 端 `SaSsoClientUtil.pushMessage()` / `buildCheckTicketMessage()` 集成
- [ ] T034 [US2] 实现 `SsoServerController`（`spring-cloud-auth/.../controller/SsoServerController.java`）：`GET /api/auth/sso/login` + `POST /api/auth/sso/checkTicket` + `POST /api/auth/sso/logout`，按 `contracts/auth-api.md` §4，Ticket 一次性 TTL 60 秒
- [ ] T035 [US2] 实现 `OAuth2ServerController`（`spring-cloud-auth/.../controller/OAuth2ServerController.java`）：`GET /api/auth/oauth2/authorize` + `POST /api/auth/oauth2/token` + `POST /api/auth/oauth2/refresh`，按 `contracts/auth-api.md` §5，仅启用 Authorization Code 模式
- [ ] T036 [US2] 实现 `KickoutController`（`spring-cloud-auth/.../controller/KickoutController.java`）：`POST /api/auth/kickout` 调用 `StpUtil.kickout(userId)` + Redis Pub/Sub 通知所有子系统 + 写入 `log.login.create` 事件（`login_type=3`）+ 禁止踢自己
- [ ] T037 [US2] 实现前端 admin 用户管理"踢下线"按钮（`vue-web-ui/apps/admin/src/views/system/UserList.vue` 增量）+ 子系统模拟客户端（`vue-web-ui/apps/admin/src/views/sso-demo/`）演示免登跳转与 SLO
- [ ] T038 [US2] 实现集成测试 `spring-cloud-test/src/test/java/com/xytang/test/it/SsoFlowIT.java`：覆盖 quickstart 场景 3（SSO 跳转 ≤ 2s，SC-002）与场景 4（踢人下线 5 秒内跳回，SC-008）

**检查点**：用户故事 1 与 2 均独立可用——SSO 跳转 + SLO + 踢人下线 + OAuth2 第三方接入端到端打通

---

## Phase 5：用户故事 3 - 公开访客浏览门户（优先级：P3）

**目标**：公开访客无需登录浏览博客/新闻/产品介绍；SEO/GEO 友好，Lighthouse SEO ≥ 90

**独立测试**：未登录访问门户首页 → 博客详情 → 禁用 JS 仍可见正文 → Lighthouse SEO ≥ 90（对齐 quickstart 场景 9 + SC-005）

### 用户故事 3 的实现

- [ ] T039 [P] [US3] 创建 `portal_content` 实体 + Mapper + DTO/VO（`spring-cloud-portal/src/main/java/com/xytang/portal/entity/PortalContent.java`）含四态 `status` 字段 + 作者/审核人字段 + SEO meta 字段
- [ ] T040 [US3] 实现 `PortalContentService`（`spring-cloud-portal/.../service/PortalContentService.java`）：CRUD + 四态发布工作流（FR-029）+ 状态迁移校验（草稿→待审核→已发布→已下架→草稿）+ RabbitMQ 事件 `portal.content.published` / `portal.content.unpublished` 触发 SSG 重建 + jsoup XSS 清洗
- [ ] T041 [US3] 实现公开访客接口 `PortalPublicController`（`spring-cloud-portal/.../controller/PortalPublicController.java`）：`GET /api/portal/contents` + `GET /api/portal/contents/{slug}` + `GET /api/portal/sitemap.xml`，仅返回 `status=3` 已发布；已下架返回 410 Gone，不存在返回 404
- [ ] T042 [US3] 实现管理员接口 `PortalAdminController`（`spring-cloud-portal/.../controller/PortalAdminController.java`）：`/api/portal/admin/contents` CRUD + `submit` / `approve` / `reject` / `unpublish` / `republish` 业务动作，按 `contracts/portal-api.md` §2
- [ ] T043 [US3] 实现 SSG 重建监听器（`spring-cloud-portal/.../listener/PortalContentSsgListener.java`）：消费 `portal.content.published` / `unpublished` 事件，调用 portal 前端 SSG rebuild API（仅"已发布→已下架"或"待审核→已发布"状态变更触发）
- [ ] T044 [US3] 实现前端 portal SSG（`vue-web-ui/apps/portal/`）：Vite SSG 配置（`vite.config.ts` 启用 SSG 预渲染）+ 博客/新闻/产品列表与详情页（`views/BlogList.vue` / `BlogDetail.vue` / `NewsList.vue` / `ProductList.vue`）+ 多语言路由 `/zh/` `/en/` + 完整 meta 标签 + Open Graph
- [ ] T045 [US3] 实现前端 admin 门户内容管理页（`vue-web-ui/apps/admin/src/views/portal/ContentList.vue` + `ContentEdit.vue`）：CRUD + 四态发布工作流按钮（提交审核 / 审批通过 / 驳回 / 下架 / 重新发布）

**检查点**：用户故事 1/2/3 均独立可用——公开访客可浏览门户，SEO 评分 ≥ 90，发布工作流完整

---

## Phase 6：用户故事 4 - 操作审计与登录日志（优先级：P3）

**目标**：所有写操作与登录事件留下可追溯的审计日志；查询响应 ≤ 3 秒；敏感字段脱敏

**独立测试**：在系统管理中执行写操作 → 操作日志查询页看到完整记录 → 跨 2 个月份分表查询 P95 ≤ 3 秒（对齐 quickstart 场景 5/8 + SC-007/SC-009）

### 用户故事 4 的实现

- [ ] T046 [P] [US4] 配置 ShardingSphere 5.5.2 精确分片（`spring-cloud-log/src/main/resources/application-sharding.yaml` + `spring-cloud-log/.../config/ShardingConfig.java`）：`sys_operation_log` 按 `create_time` 月份路由到 `sys_operation_log_YYYYMM`、`sys_login_log` 同策略；不分库；归并查询配置
- [ ] T047 [P] [US4] 创建操作/登录日志实体 + Mapper（`spring-cloud-log/.../entity/OperationLog.java` + `LoginLog.java` + `mapper/OperationLogMapper.java` + `LoginLogMapper.java`）：字段对齐 `data-model.md` §2.9/§2.10，含 `@TableName` 动态表名（实际由 ShardingSphere 路由）
- [ ] T048 [US4] 实现 `OperationLogService` + `LoginLogService`（`spring-cloud-log/.../service/`）：跨月归并查询 + 数据权限（`@DataScope(userAlias="l")` 按 `user_id`）+ 多条件组合检索 + 分页
- [ ] T049 [US4] 实现 `LogController`（`spring-cloud-log/.../controller/LogController.java`）：`GET /api/log/operations` + `GET /api/log/operations/{id}` + `GET /api/log/logins` + `GET /api/log/logins/by-user/{userId}`，按 `contracts/log-api.md` §1/§2，敏感字段在 VO 序列化时再次脱敏
- [ ] T050 [US4] 完善日志异步落盘链路：`spring-cloud-log/.../listener/OperationLogListener.java` + `LoginLogListener.java` 继承 `AbstractEventListener<T>` 实现幂等消费（FR-019）+ MQ 不可用时由 common-log 本地兜底落盘 + 告警（边界情况）
- [ ] T051 [US4] 实现前端 admin 日志查询页（`vue-web-ui/apps/admin/src/views/log/OperationLogList.vue` + `LoginLogList.vue`）：多条件组合检索 + 分页 + 从操作日志快速跳转到同账号登录日志（FR-021）+ 详情弹窗展示脱敏后的入参

**检查点**：所有用户故事均独立可用——审计日志完整可追溯，跨月分表查询 P95 ≤ 3 秒

---

## Phase 7：打磨与横切关注点

**目的**：影响多个用户故事的改进项与最终验证

- [ ] T052 [P] 补充后端单元测试（`spring-cloud-test/src/test/java/com/xytang/test/unit/`）：覆盖 UserService/RoleService/PortalContentService 等核心 Service + DataPermissionInnerInterceptor 拦截器
- [ ] T053 [P] 补充后端集成测试（`spring-cloud-test/src/test/java/com/xytang/test/it/`）：基于 Testcontainers（MySQL 8.4 / Redis 7.4 / RabbitMQ 3.13）覆盖 quickstart 场景 1/2/5/6/7
- [ ] T054 实现契约测试（`spring-cloud-test/src/test/java/com/xytang/test/contract/`）：基于 springdoc-openapi 生成的 OpenAPI 3 spec 校验前后端契约一致性
- [ ] T055 [P] 补充前端单元测试（`vue-web-ui/apps/admin/tests/unit/`，Vitest）+ E2E 测试（`vue-web-ui/apps/admin/tests/e2e/`，Playwright，覆盖 quickstart 场景 1/2/3/4）
- [ ] T056 实现性能压测脚本（`spring-cloud-test/src/test/resources/perf/`，wrk 脚本）：登录 500 并发不降级 P95 ≤ 1s（SC-003）+ 跨月分表查询 P95 ≤ 3s（SC-009）
- [ ] T057 实现安全加固：扫描所有接口 SQL 注入风险（参数化校验）+ XSS 清洗覆盖率扫描 + 敏感字段泄露扫描（密码/Token/身份证号/手机号在日志与响应中是否脱敏）+ 红线检查（宪法 §质量门禁 10 条红线）
- [ ] T058 [P] 更新 `docs/` 学习记录：为每个集成的中间件（Sa-Token SSO / MyBatis-Plus 拦截器 / Redisson 分布式锁 / RabbitMQ 事件总线 / ShardingSphere 分表）留下踩坑笔记与关键配置说明（宪法 §五"学习记录留存"）
- [ ] T059 按 `quickstart.md` §4 端到端验证场景 1–10 全部跑通，记录通过情况与未达标项的修复 plan
- [ ] T060 [P] 实现国产化适配冒烟测试（`spring-cloud-test/src/test/java/com/xytang/test/i18n/`）：通过 ShardingSphere 适配层在 KingbaseES V8R6 与 DM8 上跑通"建表 + 增删改查"基础流程（FR-023）；深度适配留待第 12 周

---

## 依赖与执行顺序

### 阶段依赖

- **初始化（Phase 1）**：无依赖——可立即开始
- **基础（Phase 2）**：依赖 Phase 1 完成——阻塞所有用户故事
- **用户故事（Phase 3–6）**：均依赖 Phase 2 完成
  - US1 必须先完成（其他故事依赖登录态与系统管理基础）
  - US2/US3/US4 可并行推进（若人力允许）
- **打磨（Phase 7）**：依赖所有目标用户故事完成

### 用户故事依赖

- **US1（P1）**：Phase 2 完成后即可开始——不依赖其他故事；MVP 必经路径
- **US2（P2）**：依赖 US1 完成登录基础；SSO Server 复用 US1 的 Sa-Token 配置；OAuth2 独立
- **US3（P3）**：依赖 US1 完成登录与 RBAC（管理员接口需鉴权）；公开访客接口完全独立
- **US4（P3）**：依赖 Phase 2 的 `common-log` 切面已就绪；查询接口依赖 US1 的数据权限基础设施

### 每个用户故事内部

- 实体（Entity）先于 Mapper
- Mapper 先于 Service
- Service 先于 Controller
- 后端先于前端
- 核心实现先于集成测试
- 完成当前故事再进入下一优先级

### 并行机会

- Phase 1 中 T005/T006/T007 可并行
- Phase 2 中 T008–T013、T016、T017、T018 可并行（不同 common 模块）
- Phase 2 T019 网关依赖 common-security / common-redis 完成
- Phase 3 US1 内 T020–T024 可并行（不同实体）
- Phase 3 US1 内 T030/T031 前端可与后端 T025–T029 并行（基于 contracts 接口先实现 mock）
- 不同用户故事可由不同开发者并行推进（US2/US3/US4 在 US1 完成后并行）

---

## 并行示例：用户故事 1

```bash
# 一并启动用户故事 1 的所有实体创建任务（不同文件、无依赖）：
Task T020: "创建 sys_user 实体 + Mapper + DTO/VO"
Task T021: "创建 sys_role + sys_role_menu + sys_role_dept 实体 + Mapper + DTO/VO"
Task T022: "创建 sys_menu 实体 + Mapper + DTO/VO"
Task T023: "创建 sys_dept 实体 + Mapper + DTO/VO"
Task T024: "创建 sys_dict / sys_param / sys_notice 实体 + Mapper + DTO/VO"

# 实体完成后，并行启动前端与后端开发：
# 后端：T025 UserService → T026 其他 Service → T027 AuthController → T028 System Controllers
# 前端：T030 登录页 + Dashboard（基于 contracts/auth-api.md mock）‖ T031 系统管理 7 个页面
```

---

## 实现策略

### MVP 优先（仅用户故事 1）

1. 完成 Phase 1：初始化（T001–T007）
2. 完成 Phase 2：基础（T008–T019）——关键，阻塞所有故事
3. 完成 Phase 3：用户故事 1（T020–T031）
4. **停下并验证**：独立测试用户故事 1（登录 + 7 类系统管理 CRUD + 数据权限 + 乐观锁）
5. 视情况部署/演示 MVP

### 增量交付

1. 完成初始化 + 基础 → 基础就绪
2. 增加 US1 → 独立测试 → 部署/演示（MVP！）
3. 增加 US2（SSO + OAuth2 + 踢人下线）→ 独立测试 → 部署/演示
4. 增加 US3（公开门户）→ 独立测试 → 部署/演示
5. 增加 US4（审计日志查询）→ 独立测试 → 部署/演示
6. 每个故事的加入都不破坏前序故事

### 并行团队策略

多位开发者时：

1. 团队共同完成 Phase 1 + Phase 2
2. Phase 2 完成后：
   - 开发者 A：用户故事 1（MVP 关键路径，单人专注）
3. US1 完成后：
   - 开发者 B：用户故事 2（SSO）
   - 开发者 C：用户故事 3（门户）
   - 开发者 D：用户故事 4（审计日志）
4. 各故事独立完成与集成

---

## 备注

- [P] 任务 = 不同文件、无依赖
- [Story] 标签把任务映射到具体用户故事，便于追溯
- 每个用户故事应可独立完成与测试
- 每个任务或逻辑分组完成后提交（Conventional Commits：`feat(system): add user CRUD`）
- 可在任何检查点停下，独立验证该故事
- 避免：模糊任务、同文件冲突、破坏独立性的跨故事依赖
- 后端端口分配：gateway=8080 / auth=8081 / system=8082 / log=8089 / portal=8090
- 所有 Sa-Token SSO / OAuth2 实现需对照 context7 文档（`/dromara/sa-token`）的 API 签名
- 所有 MyBatis-Plus 拦截器需对照 context7 文档（`/websites/baomidou`）的 `OptimisticLockerInnerInterceptor` API
