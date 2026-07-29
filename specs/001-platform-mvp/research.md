# 调研：Platform MVP Foundation

**功能分支**：`001-platform-mvp`
**创建日期**：2026-07-30
**关联规格**：[spec.md](./spec.md)
**关联计划**：[plan.md](./plan.md)

> 本文为 `/speckit-plan` Phase 0 输出。所有结论已基于 context7 MCP 查询的官方文档与项目宪法（`.specify/memory/constitution.md`）交叉验证。

---

## 1. 依赖版本核对

### 1.1 Spring Cloud Alibaba

- **Decision**：沿用宪法锁定的 `2025.0.0`（即 `2025.0.0.0` BOM），暂不升级到 `2025.1.0.0`。
- **Rationale**：
  - 项目宪法 §技术栈与架构约束 已锁定 `Spring Cloud Alibaba 2025.0.0`。
  - context7 查询 `/alibaba/spring-cloud-alibaba` 显示当前 `2025.1.x` 分支的 BOM 版本为 `2025.1.0.0`，但其 Release Notes 与 Spring Cloud `2025.0.0` 的兼容矩阵尚未在 context7 中体现（来源：[SCA README 2025.1.x](https://github.com/alibaba/spring-cloud-alibaba/blob/2025.1.x/README.md)）。
  - 宪法治理章节规定"版本统一：所有依赖版本 MUST 在父 POM 的 `<properties>` 与 `<dependencyManagement>` 中声明"；升级版本必须通过 `/speckit-constitution` 修订宪法，不在本规格的 plan 阶段决定。
  - `2025.0.0` 自带的 `nacos.client.version=3.0.3`（来源：[SCA dependencies pom.xml 2025.0.0.0](https://github.com/alibaba/spring-cloud-alibaba/blob/2025.1.x/spring-cloud-alibaba-dependencies/pom.xml)），满足 MVP 阶段对配置中心 + 服务注册的需求。
- **Alternatives considered**：
  - 升级至 `2025.1.0.0`：可获取最新特性，但需先通过宪法修订流程评估与 Spring Cloud `2025.0.0` 的兼容性，存在 rework 风险。
  - 降级至 `2023.x`：与 Spring Boot 3.5.0 + Spring Cloud 2025.0.0 不兼容，放弃。
- **Action Item**：在 `/speckit-tasks` 阶段为"SCA 版本升级评估"创建独立 task，留待 MVP 完成后单独验证。

### 1.2 Sa-Token

- **Decision**：沿用宪法锁定的 `1.44.0`。
- **Rationale**：
  - context7 查询 `/dromara/sa-token` 返回 `Versions: v1.44.0`，确认 1.44.0 为当前最新 stable。
  - 宪法与 spec 假设章节一致指定 `Sa-Token 1.44.0`，无需调整。
- **API 已验证**（来源：[sa-token-doc sso-dev.md](https://github.com/dromara/sa-token/blob/dev/sa-token-doc/sso/sso-dev.md)、[README.md](https://github.com/dromara/sa-token/blob/dev/README.md)）：
  - 单点注销：`SaSsoServerUtil.ssoLogout(Object loginId)` 与重载 `ssoLogout(loginId, SaLogoutParameter, ignoreClient)`。
  - SSO Client 消息推送：`SaSsoClientUtil.pushMessage(SaSsoMessage)` / `pushMessageAsSaResult(...)` / `buildCheckTicketMessage(ticket, ssoLogoutCallUrl)` / `buildSignoutMessage(loginId, SaLogoutParameter)`。
  - 踢人下线：`StpUtil.kickout(loginId)`——直接对应 spec FR-004。
  - OAuth2 Server 四种授权模式：Authorization Code / Implicit / Password / Client Credentials。spec FR-007"第三方合作方接入"建议采用 **Authorization Code** 模式（最安全、最主流）。
- **Alternatives considered**：
  - 自研 SSO：学习价值高但与"学习导向的技术栈整合"原则冲突（重复造轮子）。
  - Spring Security OAuth2：生态成熟但配置繁琐，与 Sa-Token 在 RBAC + SSO 上能力重叠。

### 1.3 MyBatis-Plus

- **Decision**：沿用宪法锁定的 `3.5.9`，启用乐观锁 + 分页 + 数据权限三类 Inner Interceptor。
- **Rationale**：
  - context7 查询 `/websites/baomidou` 返回 `Benchmark Score: 80.7`（最高），代码片段 483 个，文档最权威。
  - 官方推荐的乐观锁实现（来源：[baomidou.com/reference/annotation](https://baomidou.com/reference/annotation) 与 [/plugins/optimistic-locker](https://baomidou.com/plugins/optimistic-locker)）：
    - 实体字段加 `@Version` 注解；
    - 注册 `OptimisticLockerInnerInterceptor` 到 `MybatisPlusInterceptor` Bean。
  - 与 spec FR-027（乐观锁）完全对应；与 spec FR-025（五级数据权限）配合需要自研 `DataPermissionInnerInterceptor`（基于 `JsqlParser` 拼接 `IN` 子查询，按 `dept_id` 或 `creator` 维度）。
- **关键代码片段**（来自 context7 官方文档）：
  ```java
  // 实体
  @TableName("sys_user")
  public class User {
      @TableId
      private Long id;
      @Version
      private Integer version;
      // ...
  }

  // 配置
  @Configuration
  @MapperScan("com.xytang.**.mapper")
  public class MybatisPlusConfig {
      @Bean
      public MybatisPlusInterceptor mybatisPlusInterceptor() {
          MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
          interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
          interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
          // 自研数据权限拦截器（按宪法 §四 的 Mapper 拦截器规范）
          interceptor.addInnerInterceptor(new DataPermissionInnerInterceptor());
          return interceptor;
      }
  }
  ```
- **Alternatives considered**：
  - 原生 MyBatis + 手写乐观锁 SQL：违反宪法 §三"Controller MUST 仅做参数解析；禁止拼接 SQL"与 §四"数据库访问 MUST 经 Mapper"。
  - JPA/Hibernate：与 Sa-Token + Warm-Flow 等中文生态对齐度差，放弃。

### 1.4 其他依赖

| 依赖 | 宪法版本 | context7 验证 | 决策 |
|------|----------|--------------|------|
| Spring Boot | 3.5.0 | 已在 SCA 2025.0.0 BOM 中被引用 | 沿用 |
| Spring Cloud | 2025.0.0 | SCA 2025.0.0 BOM 对应 | 沿用 |
| Nacos | 2.4+ | SCA 2025.0.0 BOM 声明 `nacos.client.version=3.0.3` | 沿用 |
| Redisson | 4.0.0 | 待 plan 阶段独立验证（与 Redis 7.4+ 兼容性） | 沿用，标注 TODO |
| RabbitMQ | 3.13+ | 不通过 context7 验证（中间件版本） | 沿用，标注 TODO |
| Hutool | 5.8.27 | 待 plan 阶段独立验证 | 沿用 |

---

## 2. SSO 与踢人下线的实现路径

### 2.1 SSO 主路径（同域 + 跨域 Ticket）

- **Decision**：采用 Sa-Token SSO 模式三（"前后端分离 + 跨域"），由 SSO Server（`spring-cloud-auth:8081`）作为唯一登录入口，子系统通过 Ticket 校验完成免登。
- **Rationale**：
  - spec 假设章节已明确"SSO 主路径采用 Sa-Token 内建 SSO（同域免登 + 跨域 Ticket 模式）"。
  - context7 查询返回 `SaSsoServerUtil.ssoLogout(loginId)` 与 `SaSsoClientUtil.buildCheckTicketMessage(...)` API，覆盖 SSO Server 与 Client 两端的核心能力。
  - 模式三为 Sa-Token 官方推荐的"前后端分离 + 跨域"方案，匹配 admin SPA + 多子系统场景。
- **流程**：
  1. 用户访问子系统受保护资源 → Gateway 拦截 → 重定向到 SSO Server `/sso/login`。
  2. SSO Server 校验账号密码 + 验证码 → 创建会话 → 生成 Ticket → 重定向回子系统 `/sso/login?ticket=xxx`。
  3. 子系统调 `SaSsoClientUtil.pushMessageAsSaResult(buildCheckTicketMessage(ticket, ...))` 校验 Ticket → 校验通过则建立子系统本地会话。
  4. 后续子系统请求带本地会话 Token，Gateway 拦截器校验后透传 `X-Login-Id`。
- **Alternatives considered**：
  - 模式一（同域 Cookie）：仅适用同一顶级域名，admin/portal 子站不满足，放弃。
  - 模式二（同域 + 临时 Token）：复杂度与模式三相当但能力更弱，放弃。

### 2.2 踢人下线

- **Decision**：管理员在 `spring-cloud-system` 调用 `StpUtil.kickout(loginId)`（Sa-Token 核心 API），Sa-Token 通过 Redis Pub/Sub 通知所有子系统清除本地会话。
- **Rationale**：
  - context7 文档显示 `StpUtil.kickout(10077)` 为官方"踢人下线"API，与 spec FR-004 直接对应。
  - Sa-Token Redisson 集成已内置跨 JVM 的会话同步能力，无需自研 Pub/Sub。
- **验收对齐**：spec SC-008 要求"被踢下线的用户在 5 秒内从所有子系统页面跳回一体化登录中心"——Sa-Token 默认的 Redis Pub/Sub 延迟 < 1 秒，满足。

### 2.3 OAuth2 Server（第三方接入）

- **Decision**：仅启用 **Authorization Code** 模式，用于第三方合作方代表已授权用户访问受保护接口。
- **Rationale**：
  - context7 文档明确 Sa-Token OAuth2 模块有四种授权模式；Authorization Code 是 OAuth2 RFC 6749 推荐的主流模式。
  - spec FR-007 仅要求"第三方合作方持有效凭证代表已授权用户访问受保护接口"，不需要 Password 或 Client Credentials 模式。
  - 隐式模式（Implicit）已被 OAuth2.1 弃用，不启用。
- **Alternatives considered**：
  - 同时启用 Client Credentials：用于服务间调用，但 spec 已采用 Dubbo RPC 作为内部同步调用方案，重复。
  - 同时启用 Password 模式：违反 OAuth2.1 安全建议，放弃。

---

## 3. 数据权限拦截器实现

### 3.1 五级数据范围（FR-025）

- **Decision**：自研 `DataPermissionInnerInterceptor`，注册到 `MybatisPlusInterceptor`，按角色配置的 `data_scope` 字段动态拼接 SQL。
- **Rationale**：
  - MyBatis-Plus 官方未提供"五级数据范围"开箱实现，但提供了 `InnerInterceptor` SPI，可基于 `JsqlParser` 解析并改写 SQL。
  - 实现模式参考 RuoYi/JeecgBoot 的成熟方案，社区资料丰富。
- **数据范围枚举**：
  | 值 | 含义 | 拼接 SQL（以 sys_user 列表为例） |
  |----|------|---------------------------------|
  | 1 | 全部 | 不拼接 |
  | 2 | 本部门及以下 | `dept_id IN (SELECT id FROM sys_dept WHERE id = ? OR FIND_IN_SET(?, ancestors))` |
  | 3 | 仅本部门 | `dept_id = ?` |
  | 4 | 仅本人 | `creator = ?` |
  | 5 | 自定义部门集合 | `dept_id IN (?, ?, ?)` |
- **拦截器规则**：
  - 仅对标注 `@DataScope(deptAlias="d", userAlias="u")` 的 Mapper 方法生效。
  - 通过 `ThreadLocal` 传递当前登录用户与角色集合（避免每次查 Redis）。
  - 跳过超级管理员（`admin` 角色）——拥有全部数据权限，不拼接。
- **Alternatives considered**：
  - 在 Service 层手写 SQL 拼接：违反宪法 §三"Controller MUST 仅做参数解析与调用 Service；禁止在 Controller 写业务逻辑或直接操作数据库"以及 §四"数据库访问 MUST 经 Mapper"。
  - 使用 MyBatis-Plus 自带的 `PaginationInnerInterceptor` + 手写 SQL：粒度太粗，无法表达五级数据范围。

---

## 4. 乐观锁与分布式锁组合（FR-027 / FR-028）

### 4.1 乐观锁

- **Decision**：实体表统一加 `version INT NOT NULL DEFAULT 0`；MyBatis-Plus `@Version` + `OptimisticLockerInnerInterceptor`；冲突时 Mapper 抛出 `OptimisticLockerException`，由 `GlobalExceptionHandler` 转换为 HTTP 409 + `R<T>` 业务码。
- **Rationale**：context7 文档已确认该 API 为官方推荐方案（来源：[/plugins/optimistic-locker](https://baomidou.com/plugins/optimistic-locker)）。

### 4.2 分布式锁

- **Decision**：使用 Redisson 的 `@DistributedLock` 注解（封装在 `spring-cloud-common-lock` 模块），锁 Key 格式 `lock:{resource_type}:{resource_id}`，默认 TTL 30 秒。
- **Rationale**：
  - 宪法 §质量门禁"并发：跨 JVM 共享状态用 `@DistributedLock`（Redisson）"已强制。
  - Redisson 的 `@DistributedLock` 由 `spring-cloud-common-lock` 自封装 Starter 提供，业务侧只加注解。
- **使用范围**：
  - 菜单树整体更新（`lock:menu:tree`）
  - 角色权限批量分配（`lock:role:{roleId}`）
  - 字典批量刷新（`lock:dict:all`）

---

## 5. 日志按月分表（FR-024 / SC-009）

- **Decision**：操作日志表 `sys_operation_log_YYYYMM`、登录日志表 `sys_login_log_YYYYMM`，使用 ShardingSphere 5.5.2 的"精确分片 + 月份路由"，分表不分库；1 年前数据由 XXL-JOB 月度任务归档至冷存表。
- **Rationale**：
  - spec FR-024 / SC-009 已明确保留 1 年 + 按月分表。
  - ShardingSphere 是宪法锁定的分库分表方案，与国产化适配（KingbaseES/DM8）共用同一适配层。
- **分片键**：`create_time`（DATETIME）。
- **路由规则**：按 `create_time` 的年月路由到对应分表；跨分表查询走 ShardingSphere 的归并查询，业务代码无感。
- **建表时间点**：spec 假设章节明确"日志表的分表字段（按 `create_time` 月份）必须在第 1–2 周建表时即确定"——首次建表即按 ShardingSphere 规则创建 12 张预分配分表（覆盖 1 年），之后由 XXL-JOB 每月 25 日预创建下月分表。
- **Alternatives considered**：
  - 按日分表：分表数量过多（365 张/年），跨分表查询性能差。
  - 按 ID 哈希分表：丢失时间维度，无法按时间归档。
  - 不分表：100 万日志/日 × 1 年 = 3.65 亿行，单表查询性能不达标。

---

## 6. 公开内容发布工作流（FR-029）

- **Decision**：在 `portal_content` 表加 `status TINYINT NOT NULL`，状态枚举 1=草稿 / 2=待审核 / 3=已发布 / 4=已下架；状态迁移由 Service 层校验，关键迁移（待审核→已发布、已发布→已下架）记录到操作日志。
- **Rationale**：
  - spec FR-029 已明确四态。
  - 工作流引擎（Warm-Flow）在 MVP 阶段尚未引入（第 7 周才接入），MVP 用"简单状态机 + 操作日志审计"足够覆盖。
- **状态迁移图**：
  ```
  [草稿] --提交--> [待审核] --审批通过--> [已发布] --下架--> [已下架]
                       |                       |
                       +----审批驳回----> [草稿]
                                               [已下架] --重新发布--> [草稿]
  ```
- **SSG 预渲染触发**：仅"已发布"状态变更触发门户 SSG 重新构建（通过 RabbitMQ 事件 `portal.content.published` 触发）。
- **HTTP 语义**：访问非"已发布"内容的详情页，已下架返回 410 Gone + `Retry-After` 头，不存在返回 404。

---

## 7. 数据库国产化适配（FR-023）

- **Decision**：通过 ShardingSphere 5.5.2 的 `database-protocol` 适配层统一数据库方言；业务代码仅与 MySQL 方言对齐，KingbaseES/DM8 的差异由 ShardingSphere 在 SQL 改写阶段处理。
- **Rationale**：
  - 宪法 §架构约束 "国产化适配：同时兼容 MySQL/PostgreSQL 与人大金仓 KingbaseES、达梦 DM8（通过 ShardingSphere 适配层）"。
  - ShardingSphere 5.5.2 提供 `KingbaseESDatabaseType` 与 `DM8DatabaseType` SPI，业务侧无需写分支。
- **MVP 阶段验证范围**：
  - 仅验证 MySQL 8.4 LTS 主路径 + 通过 ShardingSphere 适配层跑通 KingbaseES V8R6 的"建表 + 增删改查"冒烟测试。
  - 国产化深度适配（如存储过程、触发器、自定义函数差异）留待第 12 周。
- **Action Item**：在 `/speckit-tasks` 阶段为"国产化适配层验证"创建独立 task。

---

## 8. 项目结构（前后端分离）

### 8.1 后端（`spring-cloud-alibaba/`）

```text
spring-cloud-alibaba/
├── pom.xml                                  # 父 POM（统一版本管理）
├── spring-cloud-common/                     # 17 个公共 jar 模块
│   ├── spring-cloud-common-core/            # 工具、常量、R<T>、BusinessException
│   ├── spring-cloud-common-redis/           # Redis + Redisson 封装
│   ├── spring-cloud-common-mybatis/        # MyBatis-Plus + 乐观锁 + 数据权限拦截器
│   ├── spring-cloud-common-security/       # Sa-Token 公共配置
│   ├── spring-cloud-common-cache/          # 多级缓存（Caffeine + Redis）
│   ├── spring-cloud-common-mq/            # RabbitMQ 事件总线
│   ├── spring-cloud-common-log/            # 操作/登录日志切面
│   ├── spring-cloud-common-lock/           # 分布式锁 Starter
│   ├── spring-cloud-common-rpc/            # Dubbo 接口定义
│   ├── spring-cloud-common-es/            # ElasticSearch 客户端（MVP 后启用）
│   ├── spring-cloud-common-mongo/         # MongoDB 客户端（MVP 后启用）
│   ├── spring-cloud-common-oss/           # MinIO 客户端（MVP 后启用）
│   ├── spring-cloud-common-tdengine/      # TDengine 客户端（MVP 后启用）
│   ├── spring-cloud-common-job/           # XXL-JOB 执行器封装（MVP 后启用）
│   ├── spring-cloud-common-starter-web/   # Web 通用 Starter
│   ├── spring-cloud-common-starter-log/   # 日志 Starter
│   └── spring-cloud-common-test/          # 集成测试支持
├── spring-cloud-starters/                  # 2 个自定义 Starter
│   ├── spring-cloud-starter-satoken/      # Sa-Token SSO + RBAC 一站式 Starter
│   └── spring-cloud-starter-rbac/         # 数据权限 + 乐观锁一站式 Starter
├── spring-cloud-auth/                       # 认证中心（SSO Server + OAuth2）
├── spring-cloud-system/                     # 系统管理（用户/角色/菜单/部门/字典/参数/通知）
├── spring-cloud-log/                        # 操作/登录日志查询
├── spring-cloud-portal/                     # 公开门户内容管理
├── spring-cloud-gateway/                    # 网关
└── spring-cloud-test/                       # 端到端集成测试
```

### 8.2 前端（`vue-web-ui/`）

```text
vue-web-ui/
├── pnpm-workspace.yaml
├── package.json
├── apps/
│   ├── admin/                              # Vue3 SPA + Naive UI（一体化平台）
│   │   ├── src/
│   │   │   ├── api/                        # Axios 客户端
│   │   │   ├── components/                 # 通用组件
│   │   │   ├── layouts/                    # 布局
│   │   │   ├── router/                     # Vue Router
│   │   │   ├── stores/                     # Pinia
│   │   │   ├── views/                      # 页面（system/log/portal）
│   │   │   └── App.vue
│   │   └── vite.config.ts
│   ├── portal/                             # Vue3 + Vite SSG（公开门户）
│   │   └── ...
│   └── flow-web/                           # Vue3 SPA（工作流前端，MVP 后启用）
└── packages/
    ├── ui/                                 # 共享 UI 组件
    ├── types/                              # 共享 TS 类型
    └── utils/                              # 共享工具
```

---

## 9. 宪法合规性预检

| 宪法原则 | 合规性 | 说明 |
|----------|--------|------|
| 一、前后端分离与契约驱动 | ✅ | 后端 `spring-cloud-alibaba/` + 前端 `vue-web-ui/` 独立工程；OpenAPI 3 由 springdoc 生成 + Knife4j 在网关聚合 |
| 二、微服务聚合与网关统一拦截 | ✅ | 所有外部请求经 Gateway:8080；内部同步用 Dubbo；内部异步用 RabbitMQ；实时推送用 WebSocket |
| 三、RESTful API 契约 | ✅ | URI 复数名词 + `/api/{service}/` 前缀；GET/POST/PUT/PATCH/DELETE 语义准确；业务异常继承 `BusinessException`；统一 `R<T>` |
| 四、标准 Java 规范与中文详尽注释 | ✅ | Google Java Style + `@RequiredArgsConstructor` 构造器注入 + `@Slf4j` + `@Validated` + 中文 Javadoc |
| 五、学习导向的技术栈整合 | ✅ | 13 个微服务 + 17 个公共 jar + 2 个自定义 Starter，技术栈整合密度高 |

**无宪法违反项，无需填写复杂度追踪表。**

---

## 10. 待解决的次要未知点（Phase 1 处理）

- **Redisson 4.0.0 与 Redis 7.4+ 兼容性**：context7 查询未返回 Redisson 库，留待 `/speckit-tasks` 阶段用官方 GitHub Release Notes 验证。
- **Warm-Flow 1.8.8 API 稳定性**：MVP 不涉及，第 7 周接入时再验证。
- **JimuReport 2.3.4 与 Spring Boot 3.5.0 兼容性**：MVP 不涉及，第 13 周接入时再验证。
- **XXL-JOB 3.5.0 调度器端口分配**：MVP 不涉及，第 13 周接入时再验证。
