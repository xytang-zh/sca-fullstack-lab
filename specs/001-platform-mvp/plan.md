# 实现计划：Platform MVP Foundation（一体化平台基线）

**分支**：`001-platform-mvp` | **日期**：2026-07-30 | **规格**：[spec.md](./spec.md)

**输入**：来自 `specs/001-platform-mvp/spec.md` 的功能规格

**说明**：本计划由 `/speckit-plan` 命令产出；Phase 0 调研见 [research.md](./research.md)；Phase 1 设计见 [data-model.md](./data-model.md) / [contracts/](./contracts/) / [quickstart.md](./quickstart.md)。

## 概要

本计划覆盖 `sca-fullstack-lab` 一体化智能管理平台的 MVP 基线，对齐实现路线图前 4 周——项目骨架 + 单体 CRUD + 登录 + RBAC + 拆微服务 + 网关 + SSO + 踢人下线 + 公开门户基线 + 审计日志。

技术方案核心（详见 [research.md](./research.md)）：
- **后端**：Spring Boot 3.5.0 + Spring Cloud 2025.0.0 + Spring Cloud Alibaba 2025.0.0 + Sa-Token 1.44.0 + MyBatis-Plus 3.5.9 + Redisson 4.0.0 + RabbitMQ + ShardingSphere 5.5.2；JDK 21。
- **前端**：Vue 3 + TypeScript + Naive UI + Pinia + Vite（admin 为 SPA、portal 为 SSG）。
- **认证**：Sa-Token SSO 模式三（前后端分离 + 跨域 Ticket），`StpUtil.kickout()` 实现踢人下线，OAuth2 Server 仅启用 Authorization Code 模式。
- **数据权限**：五级经典模型 + 自研 `DataPermissionInnerInterceptor`（基于 JsqlParser 拼接 `IN` 子查询）。
- **并发**：乐观锁（`@Version` + `OptimisticLockerInnerInterceptor`）+ 关键资源分布式锁（Redisson `@DistributedLock`）。
- **日志**：操作/登录日志按月分表（ShardingSphere 精确分片），1 年前数据由 XXL-JOB 月度任务归档至冷存表。
- **国产化**：通过 ShardingSphere 适配层同时兼容 MySQL / KingbaseES / DM8，业务代码不写方言分支。

## 技术上下文

**语言/版本**：OpenJDK 21（LTS）；前端 TypeScript 5.x（strict 模式）。

**主要依赖**：
- 后端基座：Spring Boot 3.5.0、Spring Cloud 2025.0.0、Spring Cloud Alibaba 2025.0.0
- 注册配置：Nacos 2.4+（client 3.0.3，由 SCA BOM 管理）
- 限流熔断：Sentinel 1.8.8+
- 分布式事务：Seata 2.2+
- RPC：Dubbo 3.3+
- 网关：Spring Cloud Gateway 4.x
- 认证：Sa-Token 1.44.0
- ORM：MyBatis-Plus 3.5.9
- 多数据源：dynamic-datasource 4.3.1
- 缓存：Redis 7.4+ / Redisson 4.0.0 / Caffeine 3.2+
- 消息队列：RabbitMQ 3.13+
- 关系库：MySQL 8.4 LTS（主）/ PostgreSQL 16+（向量库载体，MVP 后启用）
- 国产化：人大金仓 KingbaseES V8R6、达梦 DM8（通过 ShardingSphere 5.5.2 适配）
- 实时通信：Netty 4.1.x（WebSocket，MVP 后启用）
- 分库分表：Apache ShardingSphere 5.5.2
- 工具集：Hutool 5.8.27
- API 文档：springdoc-openapi 2.6+ + Knife4j（网关聚合）
- 前端：Vue 3（Composition API + `<script setup>`）+ Naive UI + Pinia + Axios + Vite

**存储**：
- 关系库主选 MySQL 8.4 LTS；通过 ShardingSphere 适配层兼容 KingbaseES / DM8。
- 缓存 Redis 7.4+（Redisson 4.0.0 客户端）。
- 操作/登录日志按月分表，1 年前数据归档至冷存表（ShardingSphere 精确分片）。
- MVP 不使用 ES / MongoDB / TDengine / MinIO（这些在第 7–11 周逐步启用）。

**测试**：
- 后端：JUnit 5 + Mockito + AssertJ + Spring Boot Test + Testcontainers（MySQL/Redis/RabbitMQ 真实容器）。
- 前端：Vitest（单元）+ Vue Test Utils（组件）+ Playwright（E2E）。
- 契约测试：springdoc-openapi 生成的 OpenAPI 3 spec 作为前后端契约源。
- 集成测试模块：`spring-cloud-test`。

**目标平台**：
- 部署：先单节点开发环境（Nacos 注册中心已就绪），后扩展为多节点集群；规格不约束部署拓扑。
- 浏览器：现代版 Chrome / Edge / Safari；不兼容 IE。
- 操作系统：Linux 服务器（生产）/ Windows 或 macOS（开发）。

**项目类型**：Web 服务（微服务聚合）+ SPA / SSG 前端（前后端分离）。

**性能目标**（对齐 spec SC-001 ~ SC-009）：
- 登录端到端 ≤ 30 秒（含验证码）。
- SSO 跳转端到端 ≤ 2 秒。
- 500 并发管理员在线不降级；登录/查询/写操作 P95 ≤ 1 秒。
- 公开门户首屏 ≤ 1 秒；Lighthouse SEO ≥ 90。
- 跨分页查询审计日志 P95 ≤ 3 秒。
- 被踢下线用户 5 秒内跳回登录中心。

**约束**：
- 宪法 §质量门禁 红线（违反即拒绝合入）：子模块覆盖父 POM 版本、`@Autowired` 字段注入、Controller 直连数据库、非 RESTful API、GET 执行写、敏感字段泄露、`System.out.println` / `e.printStackTrace()`、`throw new RuntimeException`、`new Thread` / `Executors.newXxx`、SQL 字符串拼接。
- 学习导向：所有集成的中间件 MUST 封装在 `spring-cloud-common-*` 模块，业务服务仅引用。

**规模/范围**（对齐 spec Clarifications Q1）：
- ≤1 万用户、≤100 万日志/日、日志保留 1 年。
- 后端 13 个微服务 + 17 个公共 jar + 2 个自定义 Starter + 1 个集成测试模块（共 33 个 Maven 模块，MVP 阶段仅启动 gateway/auth/system/log/portal + common-*）。
- 前端 3 个应用（admin / portal / flow-web，MVP 阶段仅启动 admin / portal）。

## 宪法核对

*门禁：Phase 0 调研前必须通过。Phase 1 设计后再次核对。*

| 宪法原则 | 核对项 | 合规性 | 证据 |
|----------|--------|--------|------|
| 一、前后端分离与契约驱动 | 后端 `spring-cloud-alibaba/` 与前端 `vue-web-ui/` 独立工程、独立版本化 | ✅ | 项目结构 §8 of research.md |
| 一、前后端分离与契约驱动 | 所有对外接口为 RESTful API，统一 `R<T>` 包装 | ✅ | FR-013 / FR-014 / contracts/ |
| 一、前后端分离与契约驱动 | 前端通过 Axios 通信，禁止后端渲染页面、禁止前端直连 DB | ✅ | 前端结构 §8.2 of research.md |
| 一、前后端分离与契约驱动 | OpenAPI 3 为唯一契约，Knife4j 在网关聚合 | ✅ | 主要依赖"API 文档"行 |
| 一、前后端分离与契约驱动 | DTO/VO 严格分离，禁止实体直接序列化 | ✅ | data-model.md §3 |
| 二、微服务聚合与网关统一拦截 | 1 网关 + 1 认证 + 11 业务 + 17 公共 + 2 Starter + 1 测试 | ✅ | 项目结构 §8.1 of research.md |
| 二、微服务聚合与网关统一拦截 | 所有外部请求经 Gateway:8080 鉴权/限流/转发 | ✅ | FR-013 |
| 二、微服务聚合与网关统一拦截 | 内部同步用 Dubbo、内部异步用 RabbitMQ、实时推送用 WebSocket | ✅ | 主要依赖与 contracts/ |
| 二、微服务聚合与网关统一拦截 | 公共能力封装在 `spring-cloud-common-*` | ✅ | 项目结构 §8.1 of research.md |
| 三、RESTful API 契约 | URI 复数名词 + `/api/{service}/` 前缀 + `StripPrefix=2` | ✅ | contracts/ 全部接口遵循 |
| 三、RESTful API 契约 | HTTP 方法语义准确，禁止 GET 写操作 | ✅ | FR-015 + contracts/ |
| 三、RESTful API 契约 | 业务动作用动词子资源（`POST /users/{id}/disable`） | ✅ | contracts/ |
| 三、RESTful API 契约 | 版本化通过 `X-API-Version` Header，禁止 URI 嵌入 `/v1/` | ✅ | contracts/ |
| 三、RESTful API 契约 | 业务异常继承 `BusinessException`，`GlobalExceptionHandler` 统一捕获 | ✅ | data-model.md §5 异常体系 |
| 三、RESTful API 契约 | 分页入参 `pageNum`(从1起) / `pageSize`(≤100) / `orderBy`，出参 `PageVO<T>` | ✅ | contracts/ 通用模式 |
| 三、RESTful API 契约 | Controller 仅做参数解析 + 调用 Service | ✅ | 项目结构 + 实现要点 |
| 四、标准 Java 规范 | Google Java Style，缩进 4 空格、行宽 ≤120 | ✅ | 实现要点 |
| 四、标准 Java 规范 | `@RequiredArgsConstructor` 构造器注入，禁止 `@Autowired` 字段注入 | ✅ | 实现要点 |
| 四、标准 Java 规范 | `@Slf4j` 日志，禁止 `System.out.println` / `e.printStackTrace()` | ✅ | 实现要点 |
| 四、标准 Java 规范 | 异常继承 `BusinessException`，禁止 `throw new RuntimeException` | ✅ | data-model.md §5 |
| 四、标准 Java 规范 | `@Validated` + Hibernate Validator，禁止手写 if-else 校验 | ✅ | contracts/ DTO 章节 |
| 四、标准 Java 规范 | 中文 Javadoc，类/接口/公共方法/字段均有注释 | ✅ | 实现要点 |
| 四、标准 Java 规范 | 数据库访问经 Mapper，禁止 Service 拼 SQL | ✅ | data-model.md §4 |
| 四、标准 Java 规范 | 线程池经 `ThreadPoolTaskExecutor` 显式配置 | ✅ | 实现要点 |
| 五、学习导向的技术栈整合 | 每个集成技术封装在 `spring-cloud-common-*` | ✅ | 项目结构 §8.1 of research.md |
| 五、学习导向的技术栈整合 | YAGNI，引入的依赖 12 个月内有真实业务场景 | ✅ | 主要依赖全部对应 spec FR |
| 五、学习导向的技术栈整合 | 版本统一在父 POM `<properties>` 与 `<dependencyManagement>` | ✅ | 实现要点 |
| 红线 | 子模块 POM 不覆盖父 POM 版本 | ✅ | 实现要点 |
| 红线 | 不使用 `@Autowired` 字段注入 | ✅ | 实现要点 |
| 红线 | Controller 不直连数据库 | ✅ | 实现要点 |
| 红线 | 所有 API 为 RESTful | ✅ | contracts/ |
| 红线 | 不使用 GET 执行写操作 | ✅ | contracts/ |
| 红线 | 不泄露密码/Token/身份证 | ✅ | FR-022 + 实现要点 |
| 红线 | 不使用 `System.out.println` / `e.printStackTrace()` | ✅ | 实现要点 |
| 红线 | 不使用 `throw new RuntimeException` | ✅ | data-model.md §5 |
| 红线 | 不使用 `new Thread` / `Executors.newXxx` | ✅ | 实现要点 |
| 红线 | 不拼接 SQL 字符串 | ✅ | data-model.md §4 + MyBatis-Plus 拦截器 |

**Phase 0 核对结论**：✅ 全部通过，无宪法违反项。

**Phase 1 核对结论**：✅ 全部通过，data-model.md / contracts/ / quickstart.md 设计均符合宪法。

## 项目结构

### 文档（本功能）

```text
specs/001-platform-mvp/
├── spec.md              # 功能规格（/speckit-specify 输出）
├── plan.md              # 本文件（/speckit-plan 输出）
├── research.md          # Phase 0 调研（/speckit-plan 输出）
├── data-model.md        # Phase 1 数据模型（/speckit-plan 输出）
├── quickstart.md        # Phase 1 快速验证手册（/speckit-plan 输出）
├── contracts/           # Phase 1 接口契约（/speckit-plan 输出）
│   ├── auth-api.md       # 认证中心契约（登录/SSO/踢人下线/OAuth2）
│   ├── system-api.md     # 系统管理契约（用户/角色/菜单/部门/字典/参数/通知）
│   ├── log-api.md        # 日志查询契约
│   ├── portal-api.md     # 公开门户契约
│   ├── gateway-routes.md # 网关路由与限流规则
│   └── common-patterns.md # 统一响应/异常/分页/校验模式
└── tasks.md             # Phase 2 任务清单（/speckit-tasks 输出，非本命令创建）
```

### 源代码（仓库根）

```text
spring-cloud-alibaba/                    # 后端聚合工程
├── pom.xml                              # 父 POM（版本统一管理）
├── spring-cloud-common/                 # 17 个公共 jar 模块（详见 research.md §8.1）
├── spring-cloud-starters/               # 2 个自定义 Starter（Sa-Token / RBAC）
├── spring-cloud-gateway/                # 网关（端口 8080）
├── spring-cloud-auth/                   # 认证中心（端口 8081）
├── spring-cloud-system/                 # 系统管理（端口 8082）
├── spring-cloud-log/                    # 日志服务（端口 8089）
├── spring-cloud-portal/                 # 公开门户内容管理（端口 8090）
└── spring-cloud-test/                   # 端到端集成测试

vue-web-ui/                              # 前端聚合工程
├── pnpm-workspace.yaml
├── package.json
├── apps/
│   ├── admin/                           # Vue3 SPA + Naive UI（一体化平台）
│   └── portal/                         # Vue3 + Vite SSG（公开门户）
└── packages/
    ├── ui/                              # 共享 UI 组件
    ├── types/                           # 共享 TS 类型
    └── utils/                           # 共享工具
```

**结构决策**：采用"前后端分离 + 微服务聚合"双工程结构，对齐宪法 §一"前后端分离与契约驱动"与 §二"微服务聚合与网关统一拦截"。后端按"公共模块下沉 + 业务服务引用"模式组织，避免重复造轮子。

## 复杂度追踪

> **仅当宪法核对有需正当理由的违反项时才填写**

无宪法违反项，本表留空。
