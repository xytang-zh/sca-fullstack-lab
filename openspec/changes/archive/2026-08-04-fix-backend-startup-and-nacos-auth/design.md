## Context

现状约束（见 proposal.md - Why）：项目通过 `spring-cloud-common-mybatis` 引入 `dynamic-datasource-spring-boot3-starter`（4.3.1），但 Nacos `sca-dev` 命名空间下 4 个服务级配置的数据源仍是普通 `spring.datasource` 单源格式，导致 `DynamicRoutingDataSource` 加载 0 个数据源、启动报 `dynamic-datasource can not find primary datasource`。同时 `docker-compose.infra.yml` 中 `NACOS_AUTH_TOKEN` 被赋值为 `NACOS_AUTH_TOKEN_SECRET_KEY` 的 Base64 JWT secret，二者职责混淆，引发 gRPC/鉴权异常，控制台接口返回 401/403 时提示"没有 X 命名空间的访问权限！"。

已核实：Nacos 2.4.3 服务端 `nacos` 用户绑定 `ROLE_ADMIN`，本身具备全部命名空间权限；控制台提示仅是接口 401/403 的兜底文案，非权限清单缺失。客户端 `application.yml` 的 `spring.cloud.nacos.username/password` 与 `namespace: sca-dev` 均已正确，无需改动。

## Goals / Non-Goals

**Goals:**
- 4 个依赖数据源的服务（auth/system/article/comment）启动成功，主数据源被 dynamic-datasource 正确加载。
- 验证 Nacos 认证配置正确（`NACOS_AUTH_TOKEN` 为 JWT secret key），控制台 `nacos/nacos` 登录可正常访问 public 与 sca-dev 命名空间，消除"没有 X 命名空间的访问权限"提示。
- 保持 dynamic-datasource（多数据源扩展能力）不退化。

**Non-Goals:**
- 不引入多数据源实际切换（当前仅 master 单源，`@DS` 能力保留待后续）。
- 不修改服务端 Java 代码。
- 不处理 Nacos 默认密码改密。

## Decisions

### D1：服务级数据源改造为 dynamic-datasource 主数据源契约

将 Nacos 4 个服务级配置（`spring-cloud-auth.yaml` / `spring-cloud-system.yaml` / `spring-cloud-article.yaml` / `spring-cloud-comment.yaml`）中 `spring.datasource` 段替换为：

```yaml
spring:
  datasource:
    dynamic:
      primary: master
      strict: false
      datasource:
        master:
          driver-class-name: com.mysql.cj.jdbc.Driver
          url: jdbc:mysql://${MYSQL_HOST:127.0.0.1}:${MYSQL_PORT:3306}/${MYSQL_DATABASE:sca_system}?useUnicode=true&characterEncoding=UTF-8&connectionCollation=utf8mb4_unicode_ci&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
          username: ${MYSQL_USERNAME:root}
          password: ${MYSQL_PASSWORD:root}
          hikari:
            maximum-pool-size: 20
            minimum-idle: 5
```

- `primary: master` 必须显式声明，否则 dynamic-datasource 仍报找不到主数据源。
- `strict: false` 允许未匹配数据源时回退主数据源，避免误伤。
- 替代方案 A：移除 `dynamic-datasource-spring-boot3-starter` 依赖、回退普通数据源。**否决**：项目技术栈基线明确保留多数据源能力（CLAUDE.md 技术栈矩阵含 dynamic-datasource 4.3.1），且重写依赖链成本高于改配置。
- 替代方案 B：在服务内 `application.yml` 补 dynamic 配置。**否决**：违背"业务配置下沉 Nacos"的既定分层契约，且每服务重复三大段。

### D2：确认 `NACOS_AUTH_TOKEN` 为 JWT secret key，保留原配置

反编译 Nacos 2.4.3 镜像 `application.properties` 确认：

```properties
nacos.core.auth.plugin.nacos.token.secret.key=${NACOS_AUTH_TOKEN:}
```

即 **JWT secret key 由 `NACOS_AUTH_TOKEN` 环境变量提供**，`NACOS_AUTH_TOKEN_SECRET_KEY` 环境变量不被镜像读取。故 `docker-compose.infra.yml` 中 `NACOS_AUTH_TOKEN: ${NACOS_AUTH_TOKEN_SECRET_KEY}` 的映射是**正确且必须保留**的——`.env` 中的 `NACOS_AUTH_TOKEN_SECRET_KEY` 是唯一 secret 来源，经 compose 注入为 `NACOS_AUTH_TOKEN` 供镜像使用。

- 验证：`zHpo3u3SQji1YUhvzrmuPhJ95Fg/skbeHTSff/qFJic=` Base64 解码后 32 字节（256 bits），满足 JWT HMAC-SHA256 要求；若误改为 UUID（32 hex 字符）当 Base64 解码仅 24 字节（192 bits），Nacos 启动即抛 `IllegalArgumentException`。
- 控制台"没有 X 命名空间的访问权限"提示：该文案仅存在于控制台前端 `main.js`（`getNamespace403`），在接口返回 401/403 时显示。已核实 `nacos` 用户绑定 `ROLE_ADMIN`（`roles` 表），对全部命名空间具备权限；当前状态控制台访问正常。历史出现该提示，根因是 Nacos 之前认证状态异常（如 16:52 启动期 `User nacos not found`），非权限清单缺失，Nacos 恢复并验证后消除。
- 替代方案 A：把 `NACOS_AUTH_TOKEN` 改为独立 UUID。**否决**：镜像会把它当 JWT secret 的 Base64 解码，192 bits 不满足要求，Nacos 无法启动（已实测复现并回滚）。
- 替代方案 B：删除 `NACOS_AUTH_TOKEN` 变量。**否决**：镜像需要它作为 JWT secret key，缺失即认证失效。

## Risks / Trade-offs

- [Nacos 重启后旧 token 失效] → 服务会自动重新登录，无需干预；控制台需重新登录。
- [dynamic-datasource 配置段改错导致多数据源加载失败] → 严格按 D1 模板，`primary`/`datasource.master` 结构一致；改后逐个服务启动验证。
- [NACOS_AUTH_TOKEN 改为 UUID 后，若存在依赖旧 Base64 值的客户端] → 仓库内仅本项目客户端，测试后无影响；外部工具需同步更新。
- [排查归因：控制台提示可能还叠加会话过期] → 修正令牌配置后，用 `nacos/nacos` 登录验证控制台即可排除。

## Migration Plan

按依赖顺序执行，每步可独立验证：

1. **基础设施确认**：`docker-compose.infra.yml` 的 `NACOS_AUTH_TOKEN: ${NACOS_AUTH_TOKEN_SECRET_KEY}` 映射保持不变（Nacos 2.4.3 镜像用 `NACOS_AUTH_TOKEN` 作为 JWT secret key）；`.env` 的 `NACOS_AUTH_TOKEN_SECRET_KEY` 为 Base64 ≥32 字节。重启 Nacos 后以 `nacos/nacos` 登录验证控制台无权限提示。
2. **Nacos 配置**：通过控制台/API 更新 4 个服务级配置的 `spring.datasource` 段为 dynamic-datasource 格式。
3. **启动验证**：`mvn spring-boot:run -pl spring-cloud-auth`（及 system/article/comment）逐一启动，确认 `dynamic-datasource initial loaded [1] datasource` 且 `Started`。
4. **回归**：gateway 启动不受影响；检查 Nacos 注册中心 4 个服务均在线。

**回滚**：若 Nacos 认证配置被误改导致无法启动，回退 `.env`/compose 为 `NACOS_AUTH_TOKEN` 指向 Base64 ≥32 字节 secret 并重启；Nacos 配置保留旧版（普通 datasource）可回滚；若认证导致大面积连不上，临时将 `NACOS_AUTH_ENABLE` 置 false 并重启 nacos。

## Open Questions

- 无（诊断与方案均已核实，无需延后决策项）。