## Context

现状（见 proposal.md Why）：后端聚合工程 33 个 Maven 模块中，7 个业务服务（monitor/message/search/file/log/portal/job）与 6 个 common 子模块（cache/datasource/es/mongo/netty/swagger）是空壳，starters 聚合（monitor-agent）无使用方。项目已转型为个人博客平台，需求只有一个前端应用（@sca/web）。

已核实的关键事实：
- 真实服务仅 5 个：gateway(6)/auth(23)/system(51)/article(28)/comment(15) 个 Java 文件
- gateway 的 Java 代码只 import `com.xytang.common.core`，其 POM 对 security/redis/redisson/swagger 的依赖是多余的
- `common-security` 是 WebFlux 环境（网关专用），`common-satoken` 是 Servlet 环境，二者无法合并；`AuthGatewayFilterFactory` 无任何服务引用
- `common-log` 的 `@OperationLog` 切面依赖 `common-mq`（RabbitTemplate 发 `log.operation`），无服务使用
- `common-redisson` 的 `@DistributedLock` 注解无服务使用
- `spring-cloud-auth` 的 `AuthServiceImpl` 用 RabbitTemplate 发登录日志事件（`EXCHANGE_LOG_LOGIN`），log 服务删除后无消费者
- `common-web` 已声明 `spring-boot-starter-aop`，可直接容纳 `@OperationLog` 切面

## Goals / Non-Goals

**Goals:**
- 后端模块从 33 个精简为 5 服务 + 6 common + 0 Starter，删除全部空壳与无使用方模块
- 保留模块无功能回归：gateway/auth/system/article/comment 现有行为不变
- 建立"技术栈 → 模块"映射表，全仓文档同步，满足 module-docs 覆盖要求
- 构建（`mvn clean install`）通过

**Non-Goals:**
- 不新增任何业务功能（评论通知、全文检索等博客扩展能力不在本变更范围）
- 不重构保留服务的内部代码（system/article/comment 的包结构、RESTful 接口不变）
- 不迁移空壳服务中不存在的业务（空壳本无代码，直接删除）
- 不处理 gateway 现存的 knife4j Servlet/WebFlux 兼容问题（非本变更引入）

## Decisions

### 决策 1：目标结构 = 5 服务 + 6 common + 0 Starter

```
spring-cloud-alibaba/
├── pom.xml                         父 POM（modules: common/gateway/auth/services）
├── spring-cloud-common/            6 个子模块
│   ├── spring-cloud-common-core     R<T>/BusinessException/事件基类/常量
│   ├── spring-cloud-common-web      Web 通用 + springdoc + @OperationLog
│   ├── spring-cloud-common-mybatis  MyBatis-Plus + dynamic-datasource
│   ├── spring-cloud-common-redis    Redis + Redisson + Caffeine
│   ├── spring-cloud-common-satoken  Sa-Token
│   └── spring-cloud-common-dubbo    Dubbo RPC 契约
├── spring-cloud-gateway/            网关（8080）
├── spring-cloud-auth/               认证中心（8081）
└── spring-cloud-services/           3 个业务服务
    ├── spring-cloud-system          RBAC（8082）
    ├── spring-cloud-article         博客文章（8093）
    └── spring-cloud-comment         博客评论（8094）
```

删除清单：7 空壳服务 + starters 聚合 + 10 个 common 子模块（cache/datasource/es/log/mongo/mq/netty/redisson/security/swagger）。

**替代方案**：按参考文档保留 4 服务 + search（可延后）。被否：search 是空壳无代码，前端搜索页用 MySQL 模糊搜索即可达标，保留占位违背"精简"目标。

### 决策 2：common 合并策略（代码迁移 + 依赖归并）

| 保留模块 | 迁入内容 | 新增依赖 |
|---------|---------|---------|
| `web` | `@OperationLog` 注解 + `OperationLogAspect`（自 `log` 迁入，**去掉 RabbitTemplate**，改 `@Slf4j` 本地日志）；springdoc/Knife4j 配置（原 `swagger` 空壳职责） | `springdoc-openapi-starter-webmvc-ui`、`knife4j-openapi3-jakarta-spring-boot-starter` |
| `mybatis` | dynamic-datasource 配置（原 `datasource` 空壳职责） | `dynamic-datasource-spring-boot3-starter` |
| `redis` | `@DistributedLock` 注解 + `DistributedLockAspect`（自 `redisson` 迁入）；Caffeine 多级缓存配置（原 `cache` 空壳职责） | `redisson-spring-boot-starter`、`caffeine` |
| `satoken` | 不变（`security` 直接删除，不并入） | — |

**关键点**：`OperationLogAspect` 依赖 `HttpServletRequest`（Servlet API），`web` 是 Servlet 环境，兼容；`DistributedLockAspect` 是 AOP，`redis` 增加 `spring-boot-starter-aop` 依赖即可。

**替代方案**：`common-security` 强行并入 satoken，用 Maven profile 区分 WebFlux/Servlet。被否：成本高、易出依赖冲突，且 `AuthGatewayFilterFactory` 无使用方，删除无损失。

### 决策 3：gateway 依赖瘦身

`spring-cloud-gateway/pom.xml` 移除 `common-security`、`common-redis`、`common-redisson`、`common-swagger` 四个内部依赖，仅保留 `common-core`。gateway 为 WebFlux 环境，**禁止**依赖 `common-web`（Servlet）与 `common-satoken`（Servlet）。

**理由**：gateway 代码仅 import `common-core` 的 `HeaderConstants`/`BizCode`/`R`，其余依赖无实际使用；网关鉴权过滤器（common-security）已删除，网关鉴权能力后续在 gateway 服务内自行实现（不在本变更范围）。

### 决策 4：auth 移除 MQ 登录日志

删除 `AuthServiceImpl` 中 `rabbitTemplate.convertAndSend(AuthConstants.EXCHANGE_LOG_LOGIN, "", event)` 调用与 `UserLoginEvent` 事件类、`AuthConstants` 中相关 Exchange 常量、`RabbitTemplate` 注入依赖；同步更新 `AuthServiceImplTest`（移除 RabbitTemplate mock）。

**理由**：log 服务已删除，MQ 事件无消费者；独立日志服务在职责任界上已移除。

### 决策 5：技术栈 → 模块映射表（写入 CLAUDE.md）

映射表草案（落实于 `spring-cloud-alibaba/CLAUDE.md` 与 `spring-cloud-common/CLAUDE.md`）：

| 技术栈 | 归属模块 |
|--------|---------|
| 统一响应 `R<T>` / `BusinessException` / 事件基类 | `spring-cloud-common-core` |
| 全局异常 / TraceId / `R` 包装 / Argon2id / 操作日志 / springdoc+Knife4j | `spring-cloud-common-web` |
| MyBatis-Plus / 分页 / 数据权限 / dynamic-datasource | `spring-cloud-common-mybatis` |
| Redis / RedisTemplate / Redisson 分布式锁 / Caffeine 多级缓存 | `spring-cloud-common-redis` |
| Sa-Token 登录鉴权 / StpInterface | `spring-cloud-common-satoken` |
| Dubbo RPC 契约（article↔comment） | `spring-cloud-common-dubbo` |
| Spring Cloud Gateway / 路由 / CORS / Sentinel 限流 | `spring-cloud-gateway` |
| Sa-Token 登录 / 注册 / 验证码 | `spring-cloud-auth` |
| MyBatis-Plus / RBAC 用户角色菜单 | `spring-cloud-system` |
| 文章 / 分类 / 标签 / Markdown / 点赞收藏 | `spring-cloud-article` |
| 评论 / 审核 / 敏感词 | `spring-cloud-comment` |

## Risks / Trade-offs

- **[删除 common-security 后网关鉴权空白]** → 当前 `AuthGatewayFilterFactory` 本就无任何服务引用，网关鉴权实际未生效；本变更不引入新鉴权，保持现状，后续单独变更在 gateway 内实现鉴权过滤器
- **[gateway 现存的 knife4j Servlet/WebFlux 兼容问题]** → 非本变更引入，不处理；仅移除 common-swagger 依赖，gateway 直接依赖的 knife4j 保留
- **[auth MQ 移除后登录日志丢失]** → log 服务已删，日志本无落点；如后续需要，可在 auth 内用 @OperationLog 本地日志替代
- **[`@OperationLog` 切面迁入 web 后无使用方]** → 保留学习价值，web 模块下未来业务可直接 `@OperationLog` 标注；不强制任何服务使用
- **[构建失败风险]** → 删除模块后 POM 引用残留会导致编译失败；tasks 中每一步删除后即跑 `mvn validate` 校验
- **[Nacos 远程配置残留已删服务路由]** → 仓库内无法改 Nacos，tasks 中标注为运维步骤，需手动删除 `spring-cloud-gateway.yaml` 中 monitor/message/search/file/log/portal 等路由

## Migration Plan

1. **删除模块**：`git rm -r` 删除 7 空壳服务、starters 聚合、10 个 common 子模块目录
2. **更新聚合 POM**：父 POM 移除 starters module；`spring-cloud-common/pom.xml` 精简为 6 子模块；`spring-cloud-services/pom.xml` 精简为 3 服务
3. **common 代码迁移**：`@OperationLog` 迁入 web（去 MQ）；`@DistributedLock` 迁入 redis；web/mybatis/redis 补依赖
4. **服务 POM 调整**：gateway/auth/system/article/comment 依赖改为精简后的 6 个 common
5. **auth 代码清理**：移除 MQ 登录日志相关代码与测试
6. **删除模块的 CLAUDE.md 与索引同步移除**；更新受影响"服务间通信"（作者/文章/评论 RPC 不受影响）
7. **文档更新**：根/聚合/服务三层 CLAUDE.md 精简 + 新增"技术栈 → 模块"映射表
8. **构建验证**：`mvn clean install -DskipTests` 全量编译通过
9. **运维核对**：Nacos 中移除已删服务配置与路由；清理 docker-compose 中不再需要的中间件（RabbitMQ 等，待确认）

**回滚策略**：全部步骤为删除/精简，无数据迁移；回滚即 `git checkout` 恢复删除目录，POM 还原即可。