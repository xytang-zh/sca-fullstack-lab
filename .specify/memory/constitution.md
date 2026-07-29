<!--
Sync Impact Report
==================
版本变更：[未版本化模板] → 1.0.0
  - 首次正式确立项目宪法（先前文件为纯占位符模板，未承载任何治理语义）。
  - 按 SemVer：从无到有的初次确立采用 1.0.0；后续修订按 MAJOR/MINOR/PATCH 递进。

修改的原则（占位符 → 具名原则）：
  - [PRINCIPLE_1_NAME]        → 一、前后端分离与契约驱动
  - [PRINCIPLE_2_NAME]        → 二、微服务聚合与网关统一拦截
  - [PRINCIPLE_3_NAME]        → 三、RESTful API 契约（不可妥协）
  - [PRINCIPLE_4_NAME]        → 四、标准 Java 规范与中文详尽注释
  - [PRINCIPLE_5_NAME]        → 五、学习导向的技术栈整合

新增章节：
  - ## 技术栈与架构约束（原 [SECTION_2_NAME]）
  - ## 开发工作流与质量门禁（原 [SECTION_3_NAME]）
  - ## 治理（治理规则细化）

移除章节：无

依赖模板同步状态：
  - .specify/templates/constitution-template.md  — ✅ 无需更新（保持占位符以服务新项目）
  - .specify/templates/plan-template.md          — ✅ 已与宪法对齐（"宪法核对"门禁引用 constitution.md）
  - .specify/templates/spec-template.md          — ✅ 已与宪法对齐（spec 阶段禁止技术细节，与"学习导向"原则不冲突）
  - .specify/templates/tasks-template.md          — ✅ 已与宪法对齐（按用户故事/Phase 分组，符合"前后端分离"路径约定）
  - spring-cloud-alibaba/CLAUDE.md                — ✅ 作为运行时开发指引被治理章节引用（[GUIDANCE_FILE] 占位符替换为该路径）

待办（无）：所有占位符均已完成替换，无故意延后定义的字段。
==================
-->

# sca-fullstack-lab 项目宪法

> 项目定位：企业级一体化智能管理平台（sca-fullstack-lab）的学习型全栈实验室。
> 后端为 Spring Cloud Alibaba 微服务聚合工程，前端为 Vue 3 + TypeScript 单页应用。
> 本宪法为项目级根本原则，所有功能规格（spec.md）、实现计划（plan.md）、任务清单（tasks.md）与代码实现 MUST 与之一致。

## 核心原则

### 一、前后端分离与契约驱动

后端与前端 MUST 作为两个独立工程维护，分别位于 `spring-cloud-alibaba/` 与 `vue-web-ui/`，独立版本化、独立构建、独立部署。

- 后端所有对外接口 MUST 为 RESTful API，统一返回 `R<T>` 包装结构（`code`/`msg`/`data`/`timestamp`）。
- 前端 MUST 通过 Axios 与后端通信，禁止在后端渲染页面或在前端直连数据库。
- 前后端对齐的唯一契约 MUST 是 OpenAPI 3 规范（springdoc-openapi 生成，Knife4j 在网关聚合）。
- DTO（入参）与 VO（出参）MUST 严格分离，禁止将实体（Entity）直接序列化为接口响应。

**为何**：分离使两端可独立演化与测试；契约驱动避免"前端等后端、后端改前端"的耦合返工。

### 二、微服务聚合与网关统一拦截

后端按业务/基础设施拆分为独立可启动的微服务（1 网关 + 1 认证中心 + 11 业务服务 + 17 公共模块 + 2 自定义 Starter + 1 集成测试模块）。

- 所有外部 HTTP 请求 MUST 经 `spring-cloud-gateway`（端口 8080）统一拦截、鉴权、限流、转发；禁止客户端直连业务服务端口。
- 内部同步调用 MUST 使用 Dubbo（接口定义在 `spring-cloud-common-core` 的 `rpc` 包）。
- 内部异步通信 MUST 使用 RabbitMQ 事件（Listener MUST 继承 `AbstractEventListener<T>` 以实现幂等消费）。
- 实时推送 MUST 使用 WebSocket（端点形如 `/ws/{service}/{userId}`）。
- 公共能力（Redis、MQ、MyBatis、安全、日志、缓存等）MUST 封装在 `spring-cloud-common-*` 模块，业务服务仅引用而非自行实现。

**为何**：网关是统一的安全与流量入口；公共模块下沉避免重复造轮子，是学习"框架如何被聚合"的最佳载体。

### 三、RESTful API 契约（不可妥协）

- URI MUST 使用复数名词、全小写、短横线分隔，并以 `/api/{服务名}/` 为前缀（由 Gateway 通过 `StripPrefix=2` 剥离）。
- HTTP 方法语义 MUST 准确：GET 查询、POST 新增、PUT 全量更新、PATCH 部分更新、DELETE 删除。
- 禁止用 GET 执行写操作；禁止用 POST 同时承担新增与更新（更新 MUST 用 PUT/PATCH）。
- 业务动作（非 CRUD）使用动词子资源：`POST /api/system/users/{id}/disable`。
- 版本化通过 Header `X-API-Version` 实现，禁止在 URI 中嵌入 `/v1/`。
- 业务异常 MUST 继承 `BusinessException`，由 `GlobalExceptionHandler` 统一捕获，禁止用 HTTP 200 返回业务错误。
- 分页入参 `pageNum`（从 1 起）/`pageSize`（≤100）/`orderBy`，出参 `PageVO<T>`。
- Controller MUST 仅做参数解析与调用 Service；禁止在 Controller 写业务逻辑或直接操作数据库。

**为何**：RESTful 是前后端契约的语法；统一响应与异常是契约的语义。违反会让前端无法稳定消费接口。

### 四、标准 Java 规范与中文详尽注释

- 遵循 Google Java Style，缩进 4 空格、行宽 ≤120、import 顺序规范、禁止 `*` 通配符 import。
- 依赖注入 MUST 使用 `@RequiredArgsConstructor` 构造器注入；禁止 `@Autowired` 字段注入。
- 日志 MUST 使用 SLF4J（`@Slf4j`）；禁止 `System.out.println` 与 `e.printStackTrace()`。
- 异常 MUST 抛出具体业务异常（继承 `BusinessException`）；禁止 `throw new RuntimeException(...)`。
- 边界校验 MUST 使用 `@Validated` + Hibernate Validator；禁止手动 if-else 校验。
- 所有注释 MUST 使用简体中文；类、接口、公共方法、公共字段 MUST 有 Javadoc 说明用途、参数、返回值与边界条件。
- 复杂业务逻辑、并发控制、分布式事务、缓存策略等非显而易见的决策 MUST 在代码注释中解释"为什么"，而非"做什么"。
- 数据库访问 MUST 经 Mapper；禁止在 Service 拼接 SQL 字符串。
- 线程池 MUST 通过 `ThreadPoolTaskExecutor` 显式配置；禁止 `Executors.newXxx` 与 `new Thread`。

**为何**：标准规范降低协作成本；中文详尽注释是本学习型项目的核心产出——把"为什么这样集成"留在代码里，比写一遍代码更值得。

### 五、学习导向的技术栈整合

- 项目首要目标是学习与整合热门 Java 框架与中间件，而非交付商业产品；技术选型优先覆盖"值得学习"的主流方案。
- 每个集成的技术栈 MUST 落在对应的 `spring-cloud-common-*` 模块中封装，并在业务服务中通过引用而非复制来使用。
- 集成过程 MUST 保留可追溯的学习记录：在代码注释、`docs/` 或 spec.md 中记录该技术解决什么问题、关键配置项、踩坑点。
- 遵循 YAGNI：禁止"为未来可能用到"而引入当前用不上的依赖；引入的依赖 MUST 在 12 个月内有真实业务场景落地。
- 同类技术选型冲突时（如本地缓存 Caffeine vs Guava Cache），优先选择 Spring 生态原生集成方案。
- 版本统一：所有依赖版本 MUST 在父 POM 的 `<properties>` 与 `<dependencyManagement>` 中声明；子模块 POM 禁止覆盖版本。

**为何**：学习型项目最大的浪费是"装一遍就忘"；强制封装下沉与注释留存，把每次集成变成可复用资产。

## 技术栈与架构约束

### 后端技术栈（强制版本基线）

| 类别 | 技术 | 版本 | 用途 |
|------|------|------|------|
| 基座 | Spring Boot | 3.5.0 | 微服务基础 |
| 微服务规范 | Spring Cloud | 2025.0.0 | 微服务规范 |
| 微服务套件 | Spring Cloud Alibaba | 2025.0.0 | Nacos/Sentinel/Seata/Dubbo |
| 注册配置中心 | Nacos | 2.4+ | 服务注册 + 配置中心 |
| 限流熔断 | Sentinel | 1.8.8+ | 限流/熔断/热点参数 |
| 分布式事务 | Seata | 2.2+ | AT/TCC/Saga |
| RPC | Dubbo | 3.3+ | 内部同步调用 |
| 网关 | Spring Cloud Gateway | 4.x | 路由/鉴权/限流 |
| 认证 | Sa-Token | 1.44.0 | 登录/权限/SSO/OAuth2 |
| AI | Spring AI | 1.1.0 | ChatClient/Advisor/VectorStore |
| 工作流 | Warm-Flow | 1.8.8 | 流程定义/审批 |
| ORM | MyBatis-Plus | 3.5.9 | ORM 增强 |
| 多数据源 | dynamic-datasource | 4.3.1 | 多源切换 |
| 缓存 | Redis | 7.4+ | 分布式缓存 |
| 缓存客户端 | Redisson | 4.0.0 | 分布式锁/限流 |
| 缓存 | Caffeine | 3.2+ | 本地缓存 |
| 消息队列 | RabbitMQ | 3.13+ | 事件总线 |
| 搜索 | ElasticSearch | 8.15+ | 全文检索 |
| 文档库 | MongoDB | 7.0+ | 对话/日志 |
| 关系库 | MySQL | 8.4 LTS | 业务主库 |
| 关系库 | PostgreSQL | 16+ | 向量库载体 |
| 时序库 | TDengine | 3.3+ | 服务器监控 |
| 对象存储 | MinIO | latest stable | 文件存储 |
| 实时通信 | Netty | 4.1.x | WebSocket |
| 任务调度 | XXL-JOB | 3.5.0 | 分布式调度 |
| 分库分表 | Apache ShardingSphere | 5.5.2 | 数据分片/加密 |
| 低代码报表 | JimuReport | 2.3.4 | 在线报表 |
| API 文档 | springdoc-openapi | 2.6+ | OpenAPI 3 |
| 监控 | Prometheus | 2.55+ | 指标采集 |
| 可视化 | Grafana | 11.x | 大盘 |
| 工具集 | Hutool | 5.8.27 | 通用工具 |
| JDK | OpenJDK | 21 | 语言版本 |

### 前端技术栈

- Vue 3（Composition API + `<script setup>`）
- TypeScript（strict 模式）
- Naive UI（组件库）
- Pinia（状态管理）
- Axios（HTTP 客户端，统一拦截器处理 `R<T>` 与 401/403/429）
- Vite（构建工具）

### 架构约束

- 顶层 groupId：`com.xytang`；顶层 artifactId：`spring-cloud-alibaba`；version：`1.0-SNAPSHOT`。
- 包命名：`com.xytang.{module}`，全小写、单词无分隔符。
- 服务端口分配：网关 8080、认证 8081、业务服务 8082–8092（详见 `spring-cloud-alibaba/CLAUDE.md` §7）。
- 配置管理：所有服务通过 Nacos 共享 `spring-cloud-shared.yaml` + 各自 `{service}.yaml`；动态刷新字段 MUST 用 `@RefreshScope`。
- 国产化适配：同时兼容 MySQL/PostgreSQL 与人大金仓 KingbaseES、达梦 DM8（通过 ShardingSphere 适配层）。

## 开发工作流与质量门禁

### Spec-Kit 规格驱动开发（SDD）

每个新功能 MUST 先走 Spec-Kit 工作流：

1. `/speckit-specify` 产出 `spec.md`（用户故事/验收标准/功能需求）
2. `/speckit-clarify`（可选）澄清模糊点
3. `/speckit-plan` 产出 `plan.md` + `research.md` + `data-model.md` + `contracts/` + `quickstart.md`
4. `/speckit-tasks` 产出 `tasks.md`（依赖有序、按用户故事分组）
5. `/speckit-implement` 按 `tasks.md` 逐条执行
6. `/speckit-converge` 收敛对齐代码与规格

> 规格文档存放于仓库根 `specs/<NNN-feature>/`，作为项目文档的一部分提交。

### Git 协作

- 分支策略：Trunk-Based（`main` 始终可发布；`feature/{模块}-{功能}`、`fix/{模块}-{问题}`、`release/{版本号}`）。
- 提交信息：Conventional Commits（`<type>(<scope>): <subject>`，type 含 feat/fix/docs/style/refactor/perf/test/chore/ci/build）。
- 提交节奏对齐工作流：constitution/specify/plan/tasks 各 1 个 commit，implement 按任务粒度多 commit。

### 质量门禁

- **异常处理**：业务异常继承 `BusinessException`；`GlobalExceptionHandler` 统一捕获；禁止 try-catch 吞异常。
- **并发**：跨 JVM 共享状态用 `@DistributedLock`（Redisson）；线程池显式配置；异步用 `@Async` + 显式线程池。
- **缓存**：Key 格式 `spring-cloud:{service}:{biz}:{id}`；TTL 加 ±10% 随机；热点用 `@LayeredCache` 多级缓存；穿透用空值或布隆。
- **事务**：Service 层 `@Transactional`；跨数据源 `@DSTransactional`；跨服务 `@GlobalTransactional`（Seata）；长事务拆分 + 消息补偿。
- **安全**：日志禁止打印密码/Token/身份证；SQL 参数化；XSS 过滤；接口加 `@SaCheckPermission`/`@SaCheckRole`；敏感字段加密入库。

### 红线（违反即拒绝合入）

1. 子模块 POM 覆盖父 POM 的依赖版本。
2. `@Autowired` 字段注入（必须构造器注入）。
3. Controller 直接操作数据库（必须经 Service → Mapper）。
4. 非 RESTful API（如 `POST /api/getUser?id=1`）。
5. 用 GET 执行写操作。
6. 日志/响应中泄露密码、Token、身份证号。
7. `System.out.println` / `e.printStackTrace()`。
8. `throw new RuntimeException(...)` 而非具体业务异常。
9. Service 中 `new Thread(...)` / `Executors.newCachedThreadPool()`。
10. SQL 字符串拼接（注入风险）。

## 治理

宪法高于一切其他实践；任何 spec.md/plan.md/tasks.md 与本宪法冲突时，以宪法为准。

- **修订流程**：任何原则的增删改 MUST 通过 `/speckit-constitution` 命令完成，MUST 在文件顶部 Sync Impact Report 中记录变更，MUST 评估对存量 specs 的影响并给出迁移计划。
- **版本策略**：遵循语义化版本 SemVer（MAJOR.MINOR.PATCH）。
  - MAJOR：原则删除或语义不兼容的重定义。
  - MINOR：新增原则或章节、实质性扩展。
  - PATCH：措辞修订、笔误、非语义性精炼。
- **合规审查**：所有 PR/评审 MUST 核对宪法合规性；违反宪法的复杂度决策 MUST 在 `plan.md` 的"复杂度追踪"表中给出正当理由，否则拒绝合入。
- **运行时开发指引**：使用 `spring-cloud-alibaba/CLAUDE.md` 与各子模块 `CLAUDE.md` 作为运行时开发指引，本宪法仅承载"原则"，具体命令、配置、端口表由 CLAUDE.md 维护。
- **学习记录留存**：每次集成新中间件 MUST 在 `docs/` 或对应 spec 中留下可追溯的学习笔记，作为学习型项目的核心产出。

**版本**：1.0.0 | **批准日期**：2026-07-29 | **最后修订**：2026-07-29
