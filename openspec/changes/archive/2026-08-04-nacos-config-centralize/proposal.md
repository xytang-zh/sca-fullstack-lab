## Why

当前 Nacos 配置处于"近乎不可用"状态：Nacos 无认证（任意外部可读写）、无 MySQL 持久化（容器重建即丢失全部配置）、服务级配置 100% 缺失（数据源/Redis/Sa-Token/路由表硬编码在本地 application.yml，违背配置中心价值）、`log`/`portal` 两个服务因缺少 `spring.config.import` 连不上配置中心。业务配置无法热更新、改配置需改代码重新部署，与企业级 Nacos 使用规范差距大。

## What Changes

- **BREAKING**（基础设施）：`docker-compose.infra.yml` 中 Nacos 段改造——开启认证（`NACOS_AUTH_ENABLE=true` + 强 token secret）、接入 MySQL 持久化（新建 `nacos` 库与账号）、同步初始化表结构。
- 客户端配置统一：删除 7 个现有服务（gateway/auth/system/article/comment/log/portal）的 `bootstrap.yml`，统一走 `application.yml` 的 `spring.config.import`；为 `log`/`portal` 补齐 import 三行（修复 P0-5）；为 5 个空壳服务（file/job/message/monitor/search）补齐统一模板。
- 新建 `sca-dev` 命名空间，在 `sca-dev` 下创建全部 dataId：`spring-cloud-shared.yaml`（共享配置）、各服务级 `*.yaml`、环境级 `-dev.yaml`。
- 将本地 application.yml 中的业务配置（数据源、Redis、RabbitMQ、Sa-Token、日志级别、actuator）迁移到 Nacos dataId；本地 yml 只保留端口、import 声明、连接默认值。
- 共享配置提取：Redis、RabbitMQ、日志级别、actuator 监控端点等全服务通用项收敛到 `spring-cloud-shared.yaml`。

## Capabilities

### New Capabilities
- `nacos-config`: Nacos 配置中心的全链路治理——基础设施持久化与认证、客户端配置加载统一、多环境命名空间隔离、共享/服务级/环境级配置分层与敏感配置外置。

### Modified Capabilities
- 无（现有 specs 均为业务功能，不涉及配置管理行为变更）。

## Non-goals

- 配置加密（Nacos 原生加密 / Jasypt，指南 P1-6）——后续单独 change。
- Sentinel 限流规则接入 Nacos 与网关路由表迁移（指南 P1-7/P1-8）——后续单独 change。
- 生产集群（3 节点）、可观测性（监控/审计/备份/CI/CD，指南 P2 系列）——后续单独 change。
- 创建 `sca-test`/`sca-prod` 命名空间（当前无对应环境，仅建 `sca-dev`）。
- Nacos 默认密码改密与只读账号创建（属认证后续运维动作，本次仅开启认证并记录）。

## Impact

- **受影响模块（后端）**：`spring-cloud-alibaba/spring-cloud-gateway/`、`spring-cloud-alibaba/spring-cloud-auth/`、`spring-cloud-alibaba/spring-cloud-services/spring-cloud-{system,article,comment,log,portal,file,job,message,monitor,search}/` 的 `src/main/resources/`（application.yml、bootstrap.yml）。
- **基础设施**：`docker/compose/docker-compose.infra.yml`（nacos 段）、`docker/compose/init/`（MySQL 初始化脚本）。
- **不需要代码改动**：无 Java 代码、依赖、API 变更；仅配置与基础设施编排变更。
- **运维影响**：开启认证后所有客户端连接需账号密码；`NACOS_AUTH_TOKEN_SECRET_KEY`、`NACOS_DB_PASSWORD` 等敏感值走环境变量（`.env`，不提交 git）。