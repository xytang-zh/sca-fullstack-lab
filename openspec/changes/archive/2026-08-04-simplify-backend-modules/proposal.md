## Why

后端聚合工程当前 33 个 Maven 模块中，7 个业务服务（monitor/message/search/file/log/portal/job）、6 个 common 子模块（cache/datasource/es/mongo/netty/swagger）与整个 starters 聚合均为空壳，无业务代码、无法启动，却持续增加维护成本与认知负担。项目需求已从"企业级一体化管理平台"转型为"个人博客平台"（前端已整合为单一应用），后端结构急需同步精简，并让开发者能**快速定位某个技术栈归属于哪个模块**。

## What Changes

- **删除 7 个空壳业务服务**：`spring-cloud-monitor`、`spring-cloud-message`、`spring-cloud-search`、`spring-cloud-file`、`spring-cloud-log`、`spring-cloud-portal`、`spring-cloud-job`（均无实际业务代码，无迁移成本）
- **删除整个 starters 聚合**：`spring-cloud-starters`（monitor-agent 仅 1 个类，monitor 服务删除后无使用方）
- **保留 5 个真实服务**：`spring-cloud-gateway`、`spring-cloud-auth`、`spring-cloud-system`、`spring-cloud-article`、`spring-cloud-comment`
- **common 从 16 个精简到 6 个**：`core`、`web`、`mybatis`、`redis`、`satoken`、`dubbo`
  - 合并入保留模块：`swagger`→`web`（springdoc/Knife4j 配置）、`log`→`web`（`@OperationLog` 切面迁入并**去掉 MQ 依赖**，改为本地日志）、`datasource`→`mybatis`（dynamic-datasource 依赖）、`redisson`/`cache`→`redis`（`@DistributedLock` 切面与 Caffeine 依赖）
  - 直接删除：`security`（`AuthGatewayFilterFactory` 无使用方，且 WebFlux 与 satoken 的 Servlet 环境冲突）、`mq`、`netty`、`mongo`、`es`
- **BREAKING**：`spring-cloud-auth` 移除 RabbitMQ 登录日志事件发送（`EXCHANGE_LOG_LOGIN`，已在删除的 log 服务中无消费者），相关 `RabbitTemplate` 依赖与常量一并移除
- **各服务 POM 依赖调整**：gateway/auth/system/article/comment 的 POM 改为引用精简后的 6 个 common 模块
- **CLAUDE.md 文档体系同步精简**：删除对应模块的文档目录，更新根/聚合/服务三层文档，**新增"技术栈 → 模块"映射表**（如 Redis → `spring-cloud-common-redis`、Sa-Token → `spring-cloud-common-satoken`）
- **端口分配表、网关路由、Nacos 配置同步精简**：移除已删除服务的端口与路由

## Capabilities

### New Capabilities

（无新增能力；技术栈→模块映射作为 `module-docs` 的增强要求）

### Modified Capabilities

- `project-structure`: 后端服务拆分结构从 10 业务服务 + 16 common 子模块 + 1 Starter 精简为 3 业务服务（system/article/comment）+ 6 个 common 子模块 + 0 个 Starter；删除服务与模块的端口、路由、Nacos 配置同步移除
- `module-docs`: CLAUDE.md 覆盖范围从 16 个 common 子模块、13 个业务服务、2 个 Starter 精简为 6 个 common 子模块、3 个业务服务、0 个 Starter；新增"技术栈 → 模块"映射表要求，确保全仓文档与结构一致性

## Impact

- **后端结构**：`spring-cloud-alibaba/` 下的模块目录、聚合 POM（父 POM、common/services 聚合 POM）大范围精简
- **后端代码**：`spring-cloud-auth` 移除 MQ 登录日志代码；`spring-cloud-common-web` 迁入 `@OperationLog` 切面（去 MQ）；`spring-cloud-common-redis` 迁入 `@DistributedLock` 切面；gateway/auth/system/article/comment 的 POM 依赖调整
- **配置**：`.nacos` 相关配置（`spring-cloud-*.yaml`）、`docker-compose` 中已删除服务（如 RabbitMQ 若不再被需要）待评估
- **文档**：仓库根、`spring-cloud-alibaba/`、`spring-cloud-common/`、`spring-cloud-services/` 及其子模块的 CLAUDE.md 全部更新
- **前端**：不受影响（前端已整合为单一应用，仅依赖 system/article/comment/auth 的 API）