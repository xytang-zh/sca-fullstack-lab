## Purpose

Nacos 作为本仓库唯一的注册配置中心，需建立企业级可用的配置治理能力：基础设施开启认证与持久化、客户端配置加载统一、多环境命名空间隔离、按共享/服务级/环境级分层管理配置，并将敏感凭据外置到环境变量，实现"改配置不重启、不泄露凭据"。

## Requirements

### Requirement: Nacos 服务端开启认证与 MySQL 持久化

Nacos 服务端（docker-compose 编排）MUST 开启认证（`NACOS_AUTH_ENABLE=true`），且 MUST 使用独立 MySQL 库（`nacos` 库）持久化配置数据，禁止使用 Derby 内嵌存储。认证 token secret 与数据库密码 MUST 通过环境变量注入，禁止硬编码在编排文件中。

#### Scenario: 未认证请求被拒绝
- **WHEN** 任一客户端不带 token 访问 Nacos 配置/服务 API
- **THEN** Nacos 返回 401 未授权，且拒绝读写操作

#### Scenario: 容器重建后配置保留
- **WHEN** 写入一条 Nacos 配置后重启 Nacos 容器
- **THEN** 该配置仍存在且可读取，数据不丢失（持久化于 MySQL）

#### Scenario: 认证凭据可注入
- **WHEN** 通过环境变量提供 `NACOS_AUTH_TOKEN_SECRET_KEY` 与 `NACOS_DB_PASSWORD`
- **THEN** 编排文件中的占位引用被替换为实际值，且仓库中不出现明文密钥

### Requirement: 客户端配置加载统一走 spring.config.import

所有微服务 MUST 通过 `application.yml` 的 `spring.config.import` 加载 Nacos 配置，MUST 删除各服务的 `bootstrap.yml`，禁止同时混用 bootstrap 与 config.import 双机制。每个服务 MUST 依次声明三条 import：服务级配置、环境级配置、共享配置。

#### Scenario: 服务从 Nacos 加载配置
- **WHEN** 服务启动且本地 `application.yml` 声明了 `spring.config.import`
- **THEN** 服务依次从 Nacos 拉取服务级、环境级、共享配置，且共享配置优先级最低可被覆盖

#### Scenario: 无 bootstrap 上下文
- **WHEN** 检查任一服务模块的 `src/main/resources/`
- **THEN** 不存在 `bootstrap.yml` 文件，且未引入 `spring-cloud-starter-bootstrap`

### Requirement: 全部服务接入 Nacos 配置中心

所有含配置的 7 个服务（gateway/auth/system/article/comment/log/portal）以及 5 个空壳服务（file/job/message/monitor/search）MUST 具备从 Nacos 加载配置的能力，禁止任何服务缺失 `spring.config.import` 而无法拉取配置。

#### Scenario: log/portal 修复配置加载
- **WHEN** 启动 `log` 或 `portal` 服务
- **THEN** 该服务能成功从 Nacos 拉取服务级与共享配置，不再静默缺失

#### Scenario: 空壳服务具备统一模板
- **WHEN** 检查空壳服务（file/job/message/monitor/search）的 `application.yml`
- **THEN** 已包含统一的 `spring.config.import` 与 Nacos 连接配置，后续启用时配置能力即用

### Requirement: 命名空间隔离环境

Nacos 中 MUST 存在独立的 `sca-dev` 命名空间，全部服务配置数据 MUST 存放于该命名空间下，客户端通过 `spring.cloud.nacos.{discovery,config}.namespace` 指定命名空间 ID，禁止把服务配置写入 `public`。

#### Scenario: 配置存放于 sca-dev
- **WHEN** 在 Nacos 控制台按 `sca-dev` 命名空间查询配置列表
- **THEN** 能看到全部共享/服务级/环境级 dataId，且 `public` 下不再承载服务配置

#### Scenario: 客户端连接到 sca-dev
- **WHEN** 任一服务启动并注册到 Nacos
- **THEN** 其注册与配置读取均落在 `sca-dev` 命名空间，与 `public` 隔离

### Requirement: 配置按共享/服务级/环境级分层

Nacos 配置 MUST 按三层组织：`spring-cloud-shared.yaml`（全服务共享）、`spring-cloud-{svc}.yaml`（服务级）、`spring-cloud-{svc}-{profile}.yaml`（环境级）。环境级覆盖服务级，服务级覆盖共享级。本地 `application.yml` MUST 只保留端口、`spring.config.import` 声明与连接默认值，业务配置（数据源、Redis、RabbitMQ、Sa-Token）落在 Nacos。

#### Scenario: 三层配置覆盖生效
- **WHEN** 同一键在共享、服务级、环境级均存在
- **THEN** 环境级值生效，其次服务级，共享级为兜底

#### Scenario: 本地 yml 只留启动必备
- **WHEN** 检查任一服务 `application.yml`
- **THEN** 仅含端口、import 声明、`spring.cloud.nacos.*` 连接配置与 `$VAR` 默认值，不含数据源/Redis/Sa-Token 等业务配置

### Requirement: 共享配置提取全服务通用项

Redis、RabbitMQ、日志级别、management 监控端点等全服务通用配置 MUST 收敛到 `spring-cloud-shared.yaml`，各服务不再各自重复声明。

#### Scenario: 共享配置被各服务继承
- **WHEN** 任一服务启动并加载共享配置
- **THEN** 该服务获得统一的 Redis/RabbitMQ/日志/actuator 配置，且可在服务级覆盖

### Requirement: 敏感凭据外置环境变量

数据库密码、Redis 密码、Sa-Token JWT secret、Nacos 令牌等敏感值 MUST 通过 `${VAR:default}` 占位引用，由环境变量注入，禁止明文写入 Nacos 配置或本地 yml；默认值仅用于本地开发，生产必须显式注入。

#### Scenario: 环境变量注入访问 Key
- **WHEN** 服务启动时环境变量提供 `MYSQL_PASSWORD`、`REDIS_PASSWORD`、`SA_TOKEN_JWT_SECRET`
- **THEN** 服务用注入值连接数据源/Redis/签发 Token，配置文件中无明文凭据

#### Scenario: 敏感值不落库
- **WHEN** 用 git 检索代码库中的明文密码/密钥占位
- **THEN** 仓库仅出现 `${VAR:default}` 占位引用，不存在真实凭据明文