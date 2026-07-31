# CLAUDE.md — spring-cloud-common-security 网关鉴权

> 本文档面向 AI 编码助手，用于在 `spring-cloud-common-security/` 目录下工作时提供模块约束、功能清单与开发规范。
> 工作前**必须**先读取父目录的 [`spring-cloud-common/CLAUDE.md`](../CLAUDE.md) 与 [`spring-cloud-alibaba/CLAUDE.md`](../../CLAUDE.md) 了解全局规范。

---

## 1. 模块定位

`spring-cloud-common-security` 提供 Spring Cloud Gateway 的网关层鉴权过滤器，校验 Token 后透传 `X-Login-Id` 头到下游服务。

**核心设计原则**：
1. **响应式优先**：网关是 WebFlux，**禁止**用 Servlet API（`HttpServletRequest`/`@WebFilter`）
2. **白名单显式**：匿名路径必须显式声明，**禁止**用通配符把鉴权关掉
3. **Token 不透传**：网关只透传 `X-Login-Id`，**禁止**把原始 Token 转发到下游

| 维度 | 值 |
|------|-----|
| 顶级包 | `com.xytang.common.security` |
| 父 POM | `com.xytang:spring-cloud-alibaba:1.0-SNAPSHOT` |
| 当前 artifactId | `spring-cloud-common-security` |
| packaging | `jar` |
| 是否有代码 | 🟡 部分（`SecurityAutoConfiguration` + `AuthGatewayFilterFactory`） |

---

## 2. 目录结构

```
spring-cloud-common-security/
├── pom.xml
└── src/
    └── main/
        └── java/com/xytang/common/security/
            ├── config/
            │   └── SecurityAutoConfiguration.java
            └── filter/
                └── AuthGatewayFilterFactory.java
```

---

## 3. 包结构

| 包 | 职责 |
|----|------|
| `com.xytang.common.security.config` | 自动装配：`SecurityAutoConfiguration` + 白名单配置 |
| `com.xytang.common.security.filter` | 网关过滤器：`AuthGatewayFilterFactory` |

> 计划新增包：`handler`（鉴权成功/失败处理器）、`context`（网关上下文）。当前未实现。

---

## 4. POM 依赖模板

```xml
<dependencies>
    <!-- 内部依赖 -->
    <dependency>
        <groupId>com.xytang</groupId>
        <artifactId>spring-cloud-common-core</artifactId>
    </dependency>

    <!-- Spring Cloud Gateway（响应式） -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-gateway</artifactId>
    </dependency>

    <!-- Sa-Token Reactor 适配 -->
    <dependency>
        <groupId>cn.dev33</groupId>
        <artifactId>sa-token-reactor-spring-boot3-starter</artifactId>
    </dependency>

    <!-- Sa-Token Redis（共享 Token） -->
    <dependency>
        <groupId>cn.dev33</groupId>
        <artifactId>sa-token-redis-jackson</artifactId>
    </dependency>
</dependencies>
```

> ⚠️ **禁止**依赖 `spring-boot-starter-web`（会与 WebFlux 冲突）。

---

## 5. 技术栈选型

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Cloud Gateway | Spring Cloud 2025.0.0 管理 | 响应式网关 |
| Sa-Token Reactor | 1.44.0 | 响应式 Sa-Token 适配 |
| Reactor | Spring Boot 管理 | 响应式编程 |
| Lombok | 父 POM 全局声明 | 注解简化 |

---

## 6. 功能清单

### 6.1 SecurityAutoConfiguration（已实现）

- **职责**：注册网关鉴权相关 Bean，加载白名单配置
- **配置项**：透传 `gateway.security.ignore-paths` 列表
- **实现技术**：`@Configuration` + `@EnableConfigurationProperties`

### 6.2 AuthGatewayFilterFactory（已实现）

- **职责**：自定义网关过滤器工厂，校验 Token → 透传 `X-Login-Id`
- **工作流程**：
  1. 从 `Authorization` 头提取 Token
  2. 调 `StpUtil.getLoginIdByToken(token)` 校验
  3. 白名单路径直接放行
  4. 校验通过，写 `X-Login-Id` 头到下游请求
  5. 校验失败，返回 401 + `R<Void>` JSON
- **配置参数**：`excludePaths`（白名单，可在路由声明时覆盖）
- **实现技术**：继承 `AbstractGatewayFilterFactory<T>` + `Mono` 响应式

### 6.3 计划功能（TODO）

| 功能 | 状态 | 说明 |
|------|------|------|
| `AuthSuccessHandler`/`AuthFailureHandler` | 未实现 | 鉴权成功/失败处理策略 |
| `IgnorePathsConfig` | 未实现 | 白名单配置类 |
| `CorsConfig` | 未实现 | 网关层 CORS |
| `SatokenContext` | 未实现 | 网关上下文 |
| `AutoConfiguration.imports` | 未实现 | 当前未声明自动装配入口 |

---

## 7. 关键类清单

| 类名 | 包 | 职责 |
|------|----|------|
| `SecurityAutoConfiguration` | `config` | 自动装配 |
| `AuthGatewayFilterFactory` | `filter` | 网关鉴权过滤器工厂 |

---

## 8. 配置项

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `gateway.security.ignore-paths` | （Nacos 注入） | 白名单路径列表 |
| `gateway.security.token-header` | `Authorization` | Token 头名 |
| `gateway.security.login-id-header` | `X-Login-Id` | 透传登录 ID 头名 |

> 子模块内**无** `application.yml`，所有配置在 Nacos `spring-cloud-gateway.yaml` 中。

---

## 9. 与其他模块的关系

| 关系 | 模块 |
|------|------|
| 依赖 | `spring-cloud-common-core` |
| 被依赖 | `spring-cloud-gateway`（**仅此一处**，业务服务不依赖） |
| 集成 | `spring-cloud-common-satoken`（共享 Token 持久化） |

---

## 10. 红线

1. ❌ 依赖 `spring-boot-starter-web`（与 WebFlux 冲突，必须用 `spring-cloud-starter-gateway`）
2. ❌ 用 `HttpServletRequest`/`@WebFilter`/`RestTemplate`（必须用响应式 `ServerHttpRequest`/`WebFilter`/`WebClient`）
3. ❌ Token 透传到下游（必须只透传 `X-Login-Id`）
4. ❌ 自己解析 JWT（必须用 Sa-Token 的 `StpUtil.getLoginIdByToken`）
5. ❌ 白名单含管理类接口（如 `/api/system/users/**`）
6. ❌ 鉴权失败返回非结构化 JSON（必须返回 `R<Void>`）
7. ❌ 白名单用通配符 `**` 把鉴权关掉（必须显式声明每条路径）
8. ❌ AutoConfiguration Bean 不加 `@ConditionalOnMissingBean`
