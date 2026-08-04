## Context

当前 Nacos 处于"近乎不可用"状态（详见 proposal.md - Why）：无认证、无持久化、服务级配置 100% 缺失、`log`/`portal` 缺 `spring.config.import` 连不上配置中心。所有服务均保留 `bootstrap.yml` 但 Spring Boot 3 + Spring Cloud 2025.0.0 默认禁用 bootstrap 上下文，故共享配置实际靠 `application.yml` 的 `spring.config.import` 双机制加载（gateway/auth 等 5 个服务），`log`/`portal` 则完全缺失。

已核实的现状约束：
- 7 个服务有配置（gateway/auth/system/article/comment/log/portal），5 个空壳（file/job/message/monitor/search）无 `application.yml`。
- Docker 中 Redis 已设 `requirepass rootpass`，但服务默认 `REDIS_PASSWORD:` 为空 → 服务连不上 Redis（P0-4）。
- 各服务 `spring.cloud.nacos` 只配了 `server-addr` + `file-extension`，namespace 默认 `public`，无 username/password。
- MySQL 初始库 `sca_system`，root 密码 `root`，`docker/compose/init/sql` 已有初始化脚本目录。

## Goals / Non-Goals

**Goals:**
- Nacos 基础设施达到企业级可用：开启认证 + MySQL 持久化，配置不因容器重建丢失。
- 全部 12 个服务（含 5 个空壳）统一通过 `application.yml` 的 `spring.config.import` 加载 Nacos 配置，删除 bootstrap 双机制。
- 建立 `sca-dev` 命名空间，配置按 共享/服务级/环境级 三层组织，业务配置迁出本地 yml。
- 敏感凭据（DB/Redis/JWT/Nacos 令牌）全部走环境变量占位。

**Non-Goals:**
- 配置加密（Nacos 原生加密 / Jasypt）、Sentinel 限流数据源、网关路由表迁移、生产集群、监控/审计/备份/CI/CD —— 均属后续 change。
- 创建 `sca-test`/`sca-prod` 命名空间。
- 修改 Nacos 默认密码、创建只读账号（本次仅记录待办）。

## Decisions

### D1：删除所有 bootstrap.yml，统一走 application.yml 的 spring.config.import

所有 12 个服务的 `bootstrap.yml` 一律删除，不引入 `spring-cloud-starter-bootstrap`。统一 `application.yml` 模板：

```yaml
server:
  port: {port}

spring:
  application:
    name: spring-cloud-{svc}
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}
  config:
    import:
      - optional:nacos:spring-cloud-shared.yaml
      - optional:nacos:${spring.application.name}.yaml
      - optional:nacos:${spring.application.name}-${spring.profiles.active}.yaml
  cloud:
    nacos:
      username: ${NACOS_USERNAME:nacos}
      password: ${NACOS_PASSWORD:nacos}
      discovery:
        server-addr: ${NACOS_ADDR:127.0.0.1:8848}
        namespace: ${NACOS_NAMESPACE:sca-dev}
        group: ${NACOS_GROUP:DEFAULT_GROUP}
      config:
        server-addr: ${NACOS_ADDR:127.0.0.1:8848}
        namespace: ${NACOS_NAMESPACE:sca-dev}
        group: ${NACOS_GROUP:DEFAULT_GROUP}
        file-extension: yaml
        refresh-enabled: true
```

- 替代方案 A：保留 bootstrap.yml 并引入 `spring-cloud-starter-bootstrap`（老 API `shared-configs`）。**否决**：Boot 3 现代方式是 `spring.config.import`，bootstrap 属于历史包袱，且部分服务启用会破坏一致性。
- 替代方案 B：顶层 `spring.cloud.nacos.server-addr` 共享。**否决**：SCA 2025.0.0.0 对顶层共享的完整支持不确定（指南 §4.2.1 标注需降级），在 `discovery`/`config` 下分别声明最稳妥，避免版本兼容问题。
- import 顺序：**共享级 → 服务级 → 环境级**（Spring Boot 中后 import 的优先级更高，故共享最前=最低优先级、环境级最后=最高优先级），与 `NACOS-配置与改造指南.md` §3.3 分层模型一致。**注意**：修正指南 §4.2.1 模板中"服务级→环境级→共享级"的顺序，该顺序会让共享配置优先级最高，与分层意图矛盾。

### D2：命名空间 sca-dev，配置全部迁入该 namespace

仅在 Nacos 创建 `sca-dev` 命名空间，全部 dataId 存放于此。客户端通过 `NACOS_NAMESPACE:sca-dev` 指定。`public` 中现有的 `spring-cloud-shared.yaml` 迁移到 `sca-dev` 后清理。

### D3：Nacos 基础设施改造（docker-compose + MySQL 持久化 + 认证）

- `docker-compose.infra.yml` nacos 段：`NACOS_AUTH_ENABLE=true`、`NACOS_AUTH_TOKEN_SECRET_KEY`（环境变量）、`NACOS_AUTH_IDENTITY_KEY/VALUE`（环境变量）、`SPRING_DATASOURCE_PLATFORM=mysql` + `MYSQL_SERVICE_*` 指向 MySQL。
- 在业务 MySQL 中新建 `nacos` 库与 `nacos` 账号（见 migration 脚本），执行 Nacos 官方 `mysql-schema.sql` 初始化表。
- standalone 模式保留（本阶段），保留集群升级路径。
- 替代方案：Nacos 继续用 Derby 内嵌。**否决**：容器重建即丢配置，P0 级缺陷。

### D4：共享配置 spring-cloud-shared.yaml 统一 Redis/RabbitMQ/日志/actuator

在 `sca-dev` 下创建 `spring-cloud-shared.yaml`，内容：现有 Argon2 段 + Redis（`REDIS_PASSWORD` 默认值对齐 `rootpass`，修复 P0-4）+ RabbitMQ（统一连接 + 重试）+ 日志级别收敛 + actuator 端点统一暴露。各服务本地 yml 中重复的 Redis/RabbitMQ/management 段删除。

### D5：服务级配置按服务拆分，本地 yml 只留启动必备

为 7 个有配置的服务创建 `spring-cloud-{svc}.yaml`，把数据源、MyBatis-Plus、Sa-Token（含 `SA_TOKEN_JWT_SECRET` 环境变量占位）等业务配置迁入。本地 `application.yml` 只保留端口、import 声明、`spring.cloud.nacos.*` 连接配置与 `$VAR` 默认值。

### D6：dev 环境不创建 -dev.yaml，import 声明保留

`spring.config.import` 中声明 `-{profile}.yaml` 一行，但 dev 环境不实际创建该文件（`optional:` 前缀静默跳过），为将来 test/prod 预留分层能力。分层覆盖机制通过共享级 vs 服务级验证即可。

### D7：5 个空壳服务补齐统一模板

file/job/message/monitor/search 五个空壳服务补 `application.yml`（统一模板），不创建服务级 Nacos 配置（无业务配置可迁），保证后续启用时连接能力即用。**不需要** Java 代码改动。

### D8：敏感凭据外置

所有敏感值用 `${VAR:default}` 占位：`MYSQL_PASSWORD`、`REDIS_PASSWORD`、`SA_TOKEN_JWT_SECRET`、`NACOS_USERNAME`、`NACOS_PASSWORD`、`NACOS_AUTH_TOKEN_SECRET_KEY`、`NACOS_DB_PASSWORD`。生产默认值必须显式注入；`SA_TOKEN_JWT_SECRET` 默认值改为非生产强占位并记录待办。

## Risks / Trade-offs

- 开启认证后若客户端未配 username/password → 服务启动报 NacosException。**缓解**：统一模板内置 `NACOS_USERNAME/NACOS_PASSWORD` 占位，默认 `nacos/nacos`；实施时先改 docker-compose 再改客户端，按序验证。
- 删除 bootstrap.yml 后若某服务依赖 bootstrap 优先加载 → 启动异常。**缓解**：当前无服务有此类依赖（日志初始化如需，后续统一引入 bootstrap starter，见指南 §4.2.2）。
- 共享配置 REDIS_PASSWORD 默认值改为 `rootpass` 后，若本地 Redis 无密码 → 连接报错。**缓解**：默认值与 docker-compose 对齐（`rootpass`），本地自定义 Redis 可通过环境变量覆盖。
- `spring.cloud.nacos.username/password` 若在 discovery/config 下重复声明，两处需一致。**缓解**：统一模板保证一致，避免顶层共享的不确定性。
- mysql-schema.sql 版本需与 Nacos 2.4.3 匹配。**缓解**：从官方 2.4.3 分支 `distribution/conf` 获取，落地前复核。

## Migration Plan

按依赖顺序执行，每步可独立验证：

1. **基础设施**：MySQL 建 `nacos` 库/账号 → 执行 `mysql-schema.sql` → 改 `docker-compose.infra.yml` nacos 段 → `.env` 写入 `NACOS_AUTH_TOKEN_SECRET_KEY`/`NACOS_DB_PASSWORD` → 重启 nacos → 验证登录接口 + 配置持久化。
2. **创建命名空间与配置**：Nacos 控制台/API 创建 `sca-dev` → 创建 `spring-cloud-shared.yaml` 及 7 个服务级 `*.yaml`（含 Argon2 段）。
3. **客户端改造**：删除 12 个服务 bootstrap.yml → 按统一模板重写 12 个 application.yml（log/portal 补 import，空壳补模板）→ 本地 yml 移除迁移到 Nacos 的业务配置段。
4. **联调验证**：`mvn clean install -DskipTests` → 启动全部服务 → 验证注册到 `sca-dev`、配置可拉取、服务可登录。

**回滚**：git 回退配置改动 + docker-compose 还原 nacos 段（保留 MySQL 持久化库无害）；若认证导致服务大面积连不上，优先回退 `NACOS_AUTH_ENABLE` 为 false 并恢复客户端 username/password 占位。

## Open Questions

- `NACOS_CONFIG_ENCRYPT_ENABLED` 环境变量名准确性与加密方案（属 Non-goals，落地时复核）。
- `mysql-schema.sql` 具体文件路径与版本核对（实施时从官方 2.4.3 分支获取）。
- 生产环境 `SA_TOKEN_JWT_SECRET` 的强随机值生成方式（本次仅去默认占位，生产实施时通过 CI 注入）。