# CLAUDE.md — spring-cloud-common-satoken Sa-Token 集成

> 本文档面向 AI 编码助手，用于在 `spring-cloud-common-satoken/` 目录下工作时提供模块约束、功能清单与开发规范。
> 工作前**必须**先读取父目录的 [`spring-cloud-common/CLAUDE.md`](../CLAUDE.md) 与 [`spring-cloud-alibaba/CLAUDE.md`](../../CLAUDE.md) 了解全局规范。

---

## 1. 模块定位

`spring-cloud-common-satoken` 提供 Sa-Token 在业务服务侧的配置与上下文透传能力。**网关已经做了鉴权**，本模块只负责"从 `X-Login-Id` 头还原登录态"。

**核心设计原则**：
1. **不在业务服务里调 `StpUtil.login()`**：登录在 `spring-cloud-auth` 完成，业务服务只读登录态
2. **`StpInterface` 实现统一**：所有业务服务共享同一套权限查询逻辑
3. **网关鉴权透传**：网关校验 Token 后写 `X-Login-Id` 头，下游直接读

| 维度 | 值 |
|------|-----|
| 顶级包 | `com.xytang.common.satoken` |
| 父 POM | `com.xytang:spring-cloud-alibaba:1.0-SNAPSHOT` |
| 当前 artifactId | `spring-cloud-common-satoken` |
| packaging | `jar` |
| 是否有代码 | 🟡 部分（`SaTokenAutoConfiguration` + `StpInterfaceImpl`） |

---

## 2. 目录结构

```
spring-cloud-common-satoken/
├── pom.xml
└── src/
    └── main/
        └── java/com/xytang/common/satoken/
            ├── config/
            │   └── SaTokenAutoConfiguration.java
            └── stp/
                └── StpInterfaceImpl.java
```

---

## 3. 包结构

| 包 | 职责 |
|----|------|
| `com.xytang.common.satoken.config` | 自动装配：`SaTokenAutoConfiguration` |
| `com.xytang.common.satoken.stp` | Sa-Token StpInterface 实现：`StpInterfaceImpl`（权限/角色查询） |

> 计划新增包：`filter`（`SaTokenContextFilter` 从 X-Login-Id 还原）、`processor`（SSO Client）、`annotation`（重导出）、`context`（LoginUserContext）。当前未实现。

---

## 4. POM 依赖模板

```xml
<dependencies>
    <!-- 内部依赖 -->
    <dependency>
        <groupId>com.xytang</groupId>
        <artifactId>spring-cloud-common-core</artifactId>
    </dependency>

    <!-- Sa-Token Spring Boot 3 Starter -->
    <dependency>
        <groupId>cn.dev33</groupId>
        <artifactId>sa-token-spring-boot3-starter</artifactId>
    </dependency>

    <!-- Sa-Token Redis Jackson（持久化 Token） -->
    <dependency>
        <groupId>cn.dev33</groupId>
        <artifactId>sa-token-redis-jackson</artifactId>
    </dependency>
</dependencies>
```

---

## 5. 技术栈选型

| 技术 | 版本 | 用途 |
|------|------|------|
| Sa-Token | 1.44.0 | 登录/权限/SSO/OAuth2 |
| sa-token-spring-boot3-starter | 1.44.0 | Spring Boot 3 集成 |
| sa-token-redis-jackson | 1.44.0 | Token 持久化到 Redis |
| Lombok | 父 POM 全局声明 | 注解简化 |

---

## 6. 功能清单

### 6.1 SaTokenAutoConfiguration（已实现）

- **职责**：注册 Sa-Token 必要 Bean，加载配置
- **配置项**：透传 `sa-token.*` 官方配置
- **实现技术**：`@Configuration` + `@EnableConfigurationProperties`

### 6.2 StpInterfaceImpl（已实现）

- **职责**：实现 Sa-Token 的 `StpInterface` 接口，提供权限和角色查询
- **方法**：
  | 方法 | 用途 |
  |------|------|
  | `getPermissionList(loginId, loginType)` | 返回当前用户的权限码列表 |
  | `getRoleList(loginId, loginType)` | 返回当前用户的角色列表 |
- **数据来源**：通过 Dubbo 调用 `spring-cloud-system` 的 `UserRpcService`（计划，未实现）
- **缓存**：权限/角色列表缓存到 Redis（TTL 30min）
- **实现技术**：实现 `StpInterface` + Redis 缓存

### 6.3 计划功能（TODO）

| 功能 | 状态 | 说明 |
|------|------|------|
| `SaTokenContextFilter` | 未实现 | 从 `X-Login-Id` 头还原登录态到 ThreadLocal |
| `LoginUserContext` | 未实现 | ThreadLocal 当前用户，方便 `@LoginUser` 注解取 |
| `SaSsoClientAutoConfiguration` | 未实现 | SSO Client 配置（独立在 `spring-cloud-starter-sso-client`） |
| 重导出 `@SaCheckPermission`/`@SaCheckRole` | 未实现 | 集中暴露注解 |
| `AutoConfiguration.imports` | 未实现 | 当前未声明自动装配入口 |

---

## 7. 关键类清单

| 类名 | 包 | 职责 |
|------|----|------|
| `SaTokenAutoConfiguration` | `config` | Sa-Token 自动装配 |
| `StpInterfaceImpl` | `stp` | 权限/角色查询实现 |

---

## 8. 配置项

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `sa-token.token-name` | `Authorization` | Token 名 |
| `sa-token.token-prefix` | `Bearer` | Token 前缀 |
| `sa-token.timeout` | `7200` | Token 有效期（秒） |
| `sa-token.is-concurrent` | `false` | 不允许同账号并发登录 |
| `sa-token.is-kickout` | `true` | 启用踢人下线 |
| `sa-token.is-read-header` | `true` | 从 Header 读 Token |
| `sa-token.jwt-secret-key` | （Nacos 注入） | JWT 密钥 |

> 子模块内**无** `application.yml`，所有配置在 Nacos 中。

---

## 9. 与其他模块的关系

| 关系 | 模块 |
|------|------|
| 依赖 | `spring-cloud-common-core` |
| 被依赖 | `spring-cloud-auth`、所有业务服务（除网关）、`spring-cloud-starter-sso-client` |
| 集成 | `spring-cloud-common-redis`（Token 持久化） |

---

## 10. 红线

1. ❌ 在业务服务调 `StpUtil.login()`（登录只在 `spring-cloud-auth`，业务服务只读登录态）
2. ❌ `StpInterfaceImpl` 直接查数据库（必须走 Dubbo RPC 或 Redis 缓存）
3. ❌ 权限码不一致（必须用 `module:action` 格式，如 `system:user:create`）
4. ❌ Token 校验在业务服务再做一遍（网关已校验，业务服务只从 `X-Login-Id` 读）
5. ❌ `SaTokenContextFilter` 不清理 ThreadLocal（导致内存泄漏，必须用 try-finally）
6. ❌ AutoConfiguration Bean 不加 `@ConditionalOnMissingBean`
