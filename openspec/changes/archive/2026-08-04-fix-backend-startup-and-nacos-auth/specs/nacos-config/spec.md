## Delta Spec: nacos-config

针对 `nacos-config` 主 spec 的两处需求修正，修复服务启动失败与 Nacos 鉴权控制台提示。

## MODIFIED Requirements

### Requirement: 配置按共享/服务级/环境级分层

Nacos 配置 MUST 按三层组织：`spring-cloud-shared.yaml`（全服务共享）、`spring-cloud-{svc}.yaml`（服务级）、`spring-cloud-{svc}-{profile}.yaml`（环境级）。环境级覆盖服务级，服务级覆盖共享级。本地 `application.yml` MUST 只保留端口、`spring.config.import` 声明与连接默认值，业务配置（数据源、Redis、RabbitMQ、Sa-Token）落在 Nacos。

**修正**：服务级数据源配置 MUST 采用 dynamic-datasource 契约（`spring.datasource.dynamic.datasource.master` 定义主数据源，`spring.datasource.dynamic.primary` 指定默认数据源），禁止使用普通 `spring.datasource` 单源格式。原因：项目通过 `spring-cloud-common-mybatis` 引入 `dynamic-datasource-spring-boot3-starter`，普通格式会导致启动期 `dynamic-datasource can not find primary datasource`。

#### Scenario: 服务级数据源可被 dynamic-datasource 加载
- **WHEN** 启动依赖数据源的服务（auth/system/article/comment）且其 Nacos 服务级配置声明了 `spring.datasource.dynamic.datasource.master`
- **THEN** 服务成功初始化主数据源，不再报 `dynamic-datasource can not find primary datasource`

#### Scenario: 本地 yml 只留启动必备
- **WHEN** 检查任一服务 `application.yml`
- **THEN** 仅含端口、import 声明、`spring.cloud.nacos.*` 连接配置与 `$VAR` 默认值，不含数据源/Redis/Sa-Token 等业务配置

### Requirement: 敏感凭据外置环境变量

数据库密码、Redis 密码、Sa-Token JWT secret、Nacos 令牌等敏感值 MUST 通过 `${VAR:default}` 占位引用，由环境变量注入，禁止明文写入 Nacos 配置或本地 yml；默认值仅用于本地开发，生产必须显式注入。

**修正**：Nacos 2.4.3 镜像中 `nacos.core.auth.plugin.nacos.token.secret.key` 由环境变量 `NACOS_AUTH_TOKEN` 提供（JWT secret key），`NACOS_AUTH_TOKEN_SECRET_KEY` 环境变量不被镜像读取。因此 `NACOS_AUTH_TOKEN` 必须为 Base64 编码且原始密钥 ≥32 字节（≥256 bits），禁止改为 UUID 或短串格式；`docker-compose.infra.yml` 中 `NACOS_AUTH_TOKEN: ${NACOS_AUTH_TOKEN_SECRET_KEY}` 的映射是正确且必须保留的（同一 secret 的两种引用）。

#### Scenario: JWT secret key 满足强度
- **WHEN** 检查 `.env` 中 `NACOS_AUTH_TOKEN_SECRET_KEY` 与 `docker-compose.infra.yml` 中 `NACOS_AUTH_TOKEN` 映射
- **THEN** `NACOS_AUTH_TOKEN` 为 Base64 值且 Base64 解码后 ≥32 字节，Nacos 容器可正常启动（`docker ps` 显示 healthy）

#### Scenario: 控制台命名空间访问正常
- **WHEN** 以 `nacos/nacos` 登录 Nacos 控制台并访问 public 与 `sca-dev` 命名空间
- **THEN** 不出现"没有 X 命名空间的访问权限"提示，接口返回 200

#### Scenario: 环境变量注入访问 Key
- **WHEN** 服务启动时环境变量提供 `MYSQL_PASSWORD`、`REDIS_PASSWORD`、`SA_TOKEN_JWT_SECRET`
- **THEN** 服务用注入值连接数据源/Redis/签发 Token，配置文件中无明文凭据

#### Scenario: 敏感值不落库
- **WHEN** 用 git 检索代码库中的明文密码/密钥占位
- **THEN** 仓库仅出现 `${VAR:default}` 占位引用，不存在真实凭据明文