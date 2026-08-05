## Why

服务启动与 Nacos 访问存在两类阻断问题：① auth/system/article/comment 四个依赖数据源的服务启动即失败，报 `dynamic-datasource can not find primary datasource`；② Nacos 控制台访问时提示"没有 public 命名空间的访问权限！"。根因：项目引入了 dynamic-datasource，但 Nacos 服务级配置仍是普通 `spring.datasource` 格式，导致动态数据源加载 0 个数据源；Nacos 认证配置曾因镜像变量映射理解偏差被误改，需确认 `NACOS_AUTH_TOKEN` 即 JWT secret key 的正确语义。

## What Changes

- **BREAKING**（Nacos 配置）：`sca-dev` 命名空间下 4 个服务级配置（`spring-cloud-auth.yaml`、`spring-cloud-system.yaml`、`spring-cloud-article.yaml`、`spring-cloud-comment.yaml`）的数据源从普通 `spring.datasource` 改为 dynamic-datasource 格式（`spring.datasource.dynamic.datasource.master` + `primary: master`），使 dynamic-datasource 能加载主数据源。
- **确认**（docker-compose）：`NACOS_AUTH_TOKEN` 保持映射 `NACOS_AUTH_TOKEN_SECRET_KEY`（Nacos 2.4.3 镜像 `nacos.core.auth.plugin.nacos.token.secret.key=${NACOS_AUTH_TOKEN}`，即 JWT secret key），`.env` 的 secret 为 Base64 ≥32 字节；验证 Nacos 正常启动且控制台 `nacos/nacos` 可访问 public 与 sca-dev 命名空间。
- 客户端 `application.yml` 无需改动（连接配置已具备），仅 Nacos 侧配置变更。

## Capabilities

### Modified Capabilities
- `nacos-config`：修正"配置按共享/服务级/环境级分层"中服务级数据源须为 dynamic-datasource 契约；修正"敏感凭据外置环境变量"中 `NACOS_AUTH_TOKEN` 为 JWT secret key（Base64 ≥32 字节）的约束。

## Non-goals

- 不创建 `sca-test`/`sca-prod` 命名空间。
- 不改服务端代码（Java 无变更，仅配置与编排）。
- 不引入配置加密（Nacos 原生加密 / Jasypt）。
- 不处理 Nacos 默认密码改密与只读账号创建（属后续运维动作）。

## Impact

- **受影响模块（后端）**：`spring-cloud-alibaba/spring-cloud-auth/`、`spring-cloud-alibaba/spring-cloud-services/spring-cloud-{system,article,comment}/` 的 Nacos 服务级配置（不在仓库内，位于 Nacos 服务端 `sca-dev` 命名空间）。
- **基础设施**：`docker/compose/docker-compose.infra.yml`（nacos 段 `NACOS_AUTH_TOKEN` 映射注释澄清）、`.env`（`NACOS_AUTH_TOKEN_SECRET_KEY` 注释澄清，值不变）。
- **不需要代码改动**：无 Java 代码、依赖、API 变更。
- **验证途径**：`mvn spring-boot:run -pl spring-cloud-auth` 等 4 个服务启动成功且 `dynamic-datasource initial loaded [1] datasource`；Nacos 控制台以 `nacos/nacos` 登录可正常访问 public 与 sca-dev 命名空间。