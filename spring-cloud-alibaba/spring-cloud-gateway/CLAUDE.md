# CLAUDE.md — spring-cloud-gateway 网关服务

> 本文档面向 AI 编码助手，用于在 `spring-cloud-gateway/` 目录下工作时提供模块约束、技术栈版本、功能清单与开发规范。
> 工作前**必须**先读取父目录的 [`spring-cloud-alibaba/CLAUDE.md`](../CLAUDE.md) 与仓库根 [`sca-fullstack-lab/CLAUDE.md`](../../CLAUDE.md) 了解全局规范。

---

## 1. 服务定位

`spring-cloud-gateway` 是整个 `sca-fullstack-lab` 项目的 **统一 API 入口**，所有 `/api/**` 请求先经过网关，由网关统一完成：

1. **路由转发**：`/api/system/**` → `spring-cloud-system`，`/api/ai/**` → `spring-cloud-ai`...
2. **统一鉴权**：基于 Sa-Token 校验 Token，把 `loginId` 透传到下游
3. **限流熔断**：集成 Sentinel，按路由/IP/用户三维度限流
4. **跨域处理**：CORS 全局白名单
5. **日志记录**：请求/响应日志、慢请求标记（>1s）
6. **灰度发布**：基于请求头 `X-Gray-Version` 路由到 v2 实例
7. **API 文档聚合**：聚合所有服务的 OpenAPI 文档

| 维度 | 值                                                  |
|------|----------------------------------------------------|
| 服务名 | `spring-cloud-gateway`                             |
| HTTP 端口 | 8080                                               |
| Dubbo 端口 | 不用（网关只做 HTTP 转发）                                   |
| 顶级包 | `com.xytang.gateway`                               |
| 启动类 | `com.xytang.gateway.SpringCloudGatewayApplication` |
| 运行模式 | 响应式（基于 Netty + Spring WebFlux）                     |
| 依赖 | Nacos、Redis（Sa-Token）、Sentinel Dashboard           |

> ⚠️ **网关是响应式的，禁止使用传统 Servlet API**。`@WebFilter`、`HttpServletRequest`、`Spring MVC` 全部不可用，必须用 `WebFilter`、`ServerHttpRequest`、`WebFlux`。

---

## 2. 模块结构

```
spring-cloud-gateway/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/xytang/gateway/
    │   │   ├── SpringCloudGatewayApplication.java       启动类
    │   │   ├── config/
    │   │   │   ├── GatewayConfig.java               路由配置（从 Nacos 加载）
    │   │   │   ├── CorsConfig.java                  全局 CORS
    │   │   │   ├── SentinelConfig.java              限流规则
    │   │   │   ├── SaTokenConfig.java               Sa-Token 适配
    │   │   │   ├── OpenApiAggConfig.java            文档聚合
    │   │   │   └── GrayReleaseConfig.java           灰度路由
    │   │   ├── filter/
    │   │   │   ├── AuthGatewayFilterFactory.java    鉴权过滤器工厂
    │   │   │   ├── LogGatewayFilterFactory.java      日志过滤器
    │   │   │   ├── TraceIdFilterFactory.java          链路追踪
    │   │   │   ├── RateLimitFilterFactory.java         限流过滤器
    │   │   │   └── GrayReleaseFilterFactory.java      灰度过滤器
    │   │   ├── handler/
    │   │   │   ├── GatewayExceptionHandler.java      全局异常
    │   │   │   ├── SentinelFallbackHandler.java     限流降级
    │   │   │   └── SaTokenExceptionHandler.java
    │   │   ├── service/
    │   │   │   ├── RouteService.java                  动态路由管理
    │   │   │   ├── GrayReleaseService.java            灰度策略
    │   │   │   └── impl/
    │   │   ├── controller/
    │   │   │   ├── HealthController.java              健康检查
    │   │   │   └── FallbackController.java             降级响应
    │   │   ├── dto/
    │   │   │   ├── RouteDTO.java                       路由配置 DTO
    │   │   │   └── GrayRuleDTO.java
    │   │   ├── vo/
    │   │   │   └── RouteVO.java
    │   │   ├── enums/
    │   │   │   ├── FilterOrderEnum.java               过滤器顺序
    │   │   │   └── GrayStrategyEnum.java
    │   │   └── constant/
    │   │       ├── GatewayConstants.java
    │   │       └── SaTokenConstants.java
    │   └── resources/
    │       ├── application.yml
    │       ├── application-dev.yml
    │       ├── application-prod.yml
    │       ├── bootstrap.yml                          Nacos 引导
    │       └── logback-spring.xml
    └── test/
        └── java/com/xytang/gateway/
            ├── SpringCloudGatewayApplicationTests.java
            └── GatewayRoutingTest.java
```

---

## 3. 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.5.0 | 基座（WebFlux 而非 Web MVC） |
| Spring Cloud | 2025.0.0 | 微服务规范 |
| Spring Cloud Alibaba | 2025.0.0.0 | Nacos/Sentinel |
| Spring Cloud Gateway | Spring Cloud 2025.0.0 管理 | 响应式网关 |
| Sa-Token | 1.44.0 | Token 校验（Reactor 适配） |
| Sentinel | Spring Cloud Alibaba 管理 | 限流/熔断 |
| springdoc-openapi | 2.6.0 | 文档聚合 |
| Knife4j | 4.5.0 | 增强文档 UI |
| micrometer-registry-prometheus | Spring Boot 管理 | 监控指标 |

> 所有依赖**必须**通过父 POM 的 dependencyManagement 管理版本。

---

## 4. POM 依赖清单

```xml
<dependencies>
    <!-- Spring Cloud Gateway（响应式） -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-gateway</artifactId>
    </dependency>

    <!-- Spring Cloud Alibaba -->
    <dependency>
        <groupId>com.alibaba.cloud</groupId>
        <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
    </dependency>
    <dependency>
        <groupId>com.alibaba.cloud</groupId>
        <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
    </dependency>
    <dependency>
        <groupId>com.alibaba.cloud</groupId>
        <artifactId>spring-cloud-starter-alibaba-sentinel</artifactId>
    </dependency>
    <dependency>
        <groupId>com.alibaba.csp</groupId>
        <artifactId>spring-cloud-starter-alibaba-sentinel-gateway</artifactId>
    </dependency>

    <!-- Sa-Token（Gateway 适配） -->
    <dependency>
        <groupId>cn.dev33</groupId>
        <artifactId>sa-token-spring-boot3-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>cn.dev33</groupId>
        <artifactId>sa-token-reactor-spring-boot3-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>cn.dev33</groupId>
        <artifactId>sa-token-redis-jackson</artifactId>
    </dependency>

    <!-- 内部 common 模块 -->
    <dependency>
        <groupId>com.xytang</groupId>
        <artifactId>spring-cloud-common-core</artifactId>
    </dependency>
    <dependency>
        <groupId>com.xytang</groupId>
        <artifactId>spring-cloud-common-security</artifactId>
    </dependency>
    <dependency>
        <groupId>com.xytang</groupId>
        <artifactId>spring-cloud-common-swagger</artifactId>
    </dependency>
    <dependency>
        <groupId>com.xytang</groupId>
        <artifactId>spring-cloud-common-redis</artifactId>
    </dependency>

    <!-- 文档聚合 -->
    <dependency>
        <groupId>com.github.xiaoymin</groupId>
        <artifactId>knife4j-openapi3-jakarta-spring-boot-starter</artifactId>
    </dependency>

    <!-- 监控 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-registry-prometheus</artifactId>
    </dependency>
</dependencies>
```

> ⚠️ **禁止依赖 `spring-boot-starter-web`**！会与 WebFlux 冲突。

---

## 5. 路由配置规范

### 5.1 路由表（在 Nacos `spring-cloud-gateway.yaml`）

```yaml
spring:
  cloud:
    gateway:
      discovery:
        locator:
          enabled: false           # 关闭基于服务名的自动路由（强制用显式路由）
      routes:
        # === 业务服务 ===
        - id: system-service
          uri: lb://spring-cloud-system
          predicates:
            - Path=/api/system/**
          filters:
            - StripPrefix=2          # 剥离 /api/system，转发为 /**
            - name: SaToken
              args:
                excludePaths: /api/system/public/**
            - name: Log
            - name: TraceId
        - id: auth-service
          uri: lb://spring-cloud-auth
          predicates: [Path=/api/auth/**, Method=GET,POST]
          filters: [StripPrefix=2, name: SaToken, name: Log]
        - id: monitor-service
          uri: lb://spring-cloud-monitor
          predicates: [Path=/api/monitor/**]
          filters: [StripPrefix=2, name: SaToken, name: Log]
        - id: workflow-service
          uri: lb://spring-cloud-workflow
          predicates: [Path=/api/workflow/**]
          filters: [StripPrefix=2, name: SaToken, name: Log]
        - id: ai-service
          uri: lb://spring-cloud-ai
          predicates: [Path=/api/ai/**]
          filters: [StripPrefix=2, name: SaToken, name: Log]
        - id: message-service
          uri: lb://spring-cloud-message
          predicates: [Path=/api/message/**]
          filters: [StripPrefix=2, name: SaToken, name: Log]
        - id: search-service
          uri: lb://spring-cloud-search
          predicates: [Path=/api/search/**]
          filters: [StripPrefix=2, name: SaToken, name: Log]
        - id: file-service
          uri: lb://spring-cloud-file
          predicates: [Path=/api/file/**]
          filters: [StripPrefix=2, name: SaToken, name: Log]
        - id: log-service
          uri: lb://spring-cloud-log
          predicates: [Path=/api/log/**]
          filters: [StripPrefix=2, name: SaToken, name: Log]
        - id: portal-service
          uri: lb://spring-cloud-portal
          predicates: [Path=/api/portal/**]
          filters: [StripPrefix=2]    # 公开门户不需要鉴权
        - id: report-service
          uri: lb://spring-cloud-report
          predicates: [Path=/api/report/**]
          filters: [StripPrefix=2, name: SaToken, name: Log]
        # === WebSocket ===
        - id: monitor-ws
          uri: lb:ws://spring-cloud-monitor
          predicates: [Path=/ws/monitor/**]
          filters: [StripPrefix=2]
        - id: message-ws
          uri: lb:ws://spring-cloud-message
          predicates: [Path=/ws/message/**]
          filters: [StripPrefix=2]
        # === XXL-JOB Admin ===
        - id: xxl-job-admin
          uri: lb://xxl-job-admin
          predicates: [Path=/xxl-job/**]
          filters: [StripPrefix=1]
```

### 5.2 路由命名规范

- **id**：`{服务名}-service` 或 `{服务名}-ws`（WebSocket）
- **路径前缀**：`/api/{服务名}/`（业务）、`/ws/{服务名}/`（WebSocket）
- **filters 顺序**：`StripPrefix → SaToken → Log → TraceId`（顺序很重要，否则鉴权失效）

---

## 6. 鉴权过滤器实现规范

### 6.1 `SaTokenGatewayFilterFactory`

- **职责**：自定义网关过滤器工厂，校验 Token → 透传 `X-Login-Id`
- **工作流程**：
  1. 从 `Authorization` 头提取 Token
  2. 白名单路径直接放行（基于 `IgnorePathsConfig`）
  3. 调 `StpUtil.getLoginIdByToken(token)` 校验 Token
  4. 校验通过：写 `X-Login-Id` 头到下游请求
  5. 校验失败：返回 401 + `R<Void>` JSON
- **实现技术**：继承 `AbstractGatewayFilterFactory<Config>` + `Mono` 响应式
- **配置参数**：`excludePaths`（白名单，可在路由声明时覆盖）

### 6.2 白名单配置（Nacos）

```yaml
gateway:
  security:
    ignore-paths:
      - /api/auth/sso/login          # 登录
      - /api/auth/sso/captcha         # 验证码
      - /api/auth/sso/auth            # SSO 登录入口
      - /api/auth/oauth2/**           # OAuth2 公开端点
      - /api/system/public/**          # 公开 API
      - /api/portal/**                 # 公开门户
      - /actuator/health
      - /actuator/prometheus
```

---

## 7. 跨域处理（CORS）

### 7.1 `CorsConfig`

- **职责**：注册 `CorsWebFilter`，全局 CORS 白名单
- **配置项**：
  | 项 | 值 |
  |----|----|
  | `allowedOriginPatterns` | `https://*.example.com`, `http://localhost:5173` |
  | `allowedMethods` | `GET, POST, PUT, PATCH, DELETE, OPTIONS` |
  | `allowedHeaders` | `*` |
  | `exposedHeaders` | `X-Trace-Id`, `Authorization` |
  | `allowCredentials` | `true` |
  | `maxAge` | `3600` |
- **实现技术**：`@Configuration` + `@Bean CorsWebFilter` + `UrlBasedCorsConfigurationSource`

### 7.2 CORS 规范

- **禁止** `Access-Control-Allow-Origin: *`（必须显式白名单）
- `Allow-Credentials: true` 时**必须**用 `Origin-Patterns` 而非 `Origins`
- 预检请求缓存 1 小时（`maxAge=3600`）

---

## 8. 限流熔断（Sentinel）

### 8.1 限流规则（Nacos `spring-cloud-gateway.yaml`）

```yaml
spring:
  cloud:
    sentinel:
      transport:
        dashboard: ${SENTINEL_DASHBOARD:127.0.0.1:8858}
        port: 8719
      scg:
        fallback:
          mode: response
          response-status: 429
          response-body: '{"code":42901,"msg":"操作过于频繁，请稍后重试"}'
      datasource:
        ds1:
          nacos:
            server-addr: ${NACOS_ADDR:127.0.0.1:8848}
            data-id: ${spring.application.name}-sentinel-rules
            group-id: SENTINEL_GROUP
            data-type: json
            rule-type: gw-flow
```

### 8.2 限流维度

| 维度 | 资源名 | 阈值 | 说明 |
|------|--------|------|------|
| 路由 | `system-service` | 100 QPS | 系统服务整体 |
| 路由 | `auth-service` | 50 QPS | 登录服务限制更严 |
| 接口 | `/api/system/users` | 200 QPS | 单接口 |
| IP | `${remote_addr}` | 10 QPS | 单 IP |
| 用户 | `${X-Login-Id}` | 30 QPS | 单用户 |

### 8.3 降级响应

限流命中后返回：
```json
{
  "code": 42901,
  "msg": "操作过于频繁，请稍后重试",
  "data": null,
  "timestamp": 1722470400000
}
```

---

## 9. 日志与链路追踪

### 9.1 `LogGatewayFilterFactory`

- **职责**：记录请求日志，标记慢请求，透传 `X-Trace-Id`
- **工作流程**：
  1. 请求开始：从 `X-Trace-Id` 头读 traceId，缺失则生成 UUID
  2. 透传 traceId 到下游
  3. 请求结束后：计算耗时，>1s 打 WARN（标记 `[SLOW]`），否则 INFO（标记 `[REQ]`）
  4. 日志格式：`[SLOW] traceId={} {} {} {}ms status={}`
- **实现技术**：`AbstractGatewayFilterFactory` + `Mono.doFinally()`

### 9.2 Trace-Id 透传

- 网关生成 `X-Trace-Id`（UUID），透传到下游服务
- 下游服务从 `X-Trace-Id` 头读取并打印到 MDC（通过 `spring-cloud-common-web` 的 `TraceIdFilter`）

---

## 10. 灰度发布

### 10.1 灰度策略

```yaml
gateway:
  gray:
    enabled: true
    rules:
      - service: spring-cloud-system
        strategy: HEADER       # 基于 Header 路由
        header: X-Gray-Version
        value: v2
        target-version: v2
      - service: spring-cloud-ai
        strategy: WEIGHT       # 基于权重
        weight: 30             # 30% 流量到 v2
        target-version: v2
```

### 10.2 实现要点

- 基于 `lb:gray://service-name` 自定义负载均衡器
- 服务实例元数据带 `version=v1`/`version=v2`（Nacos metadata）
- 测试请求带 `X-Gray-Version: v2` Header 路由到 v2 实例

---

## 11. API 文档聚合（Knife4j）

### 11.1 配置

```yaml
knife4j:
  gateway:
    enabled: true
    discover:
      enabled: true               # 从 Nacos 自动发现服务
      version: openapi3
    strategy: discover
  aggregation:
    enabled: true
    routes:
      - name: 认证中心
        service-name: spring-cloud-auth
        url: /spring-cloud-auth/v3/api-docs
      - name: 系统管理
        service-name: spring-cloud-system
        url: /spring-cloud-system/v3/api-docs
```

### 11.2 文档地址

- 开发环境：`http://localhost:8080/doc.html`
- 生产环境：**必须关闭**，`knife4j.production=true`

---

## 12. 必须遵守的开发规范

### 12.1 响应式编程规范

1. **禁止** `spring-boot-starter-web`，必须 `spring-cloud-starter-gateway`（基于 WebFlux）
2. **禁止** `HttpServletRequest`，必须用 `ServerHttpRequest`
3. **禁止** `@WebFilter`，必须用 `WebFilter` 或 `GatewayFilter`
4. **禁止** `ServletRequestAttributes`，必须用响应式 `ServerWebExchange`
5. **禁止** 在网关写阻塞 IO（如同步 RPC、`Thread.sleep`）
6. **必须** 用 `Mono<T>` / `Flux<T>` 包装返回值
7. **必须** 用 `reactor.core.publisher.Mono.fromCallable` 包装阻塞调用
8. **必须** 在 `WebClient` 而非 `RestTemplate` 调用外部服务

### 12.2 鉴权规范

1. **必须** 在白名单中显式声明匿名路径，**禁止**用通配符 `**` 把鉴权关掉
2. Token 校验**必须**通过 Sa-Token 的 `StpUtil.getLoginIdByToken`，**禁止**自己解析 JWT
3. **禁止**把 Token 透传到下游（透传的是 `X-Login-Id`，不是 Token 本身）
4. Token 校验失败**必须**返回 401 + `R<Void>` 包装
5. 白名单**禁止**包含管理类接口（如 `/api/system/users/**`）

### 12.3 路由规范

1. 路由**必须**显式声明（`discovery.locator.enabled=false`），**禁止**自动路由
2. 路由 ID**必须**有意义（`{service}-service`），**禁止**用 `route1`、`route2`
3. `StripPrefix` **必须**为 2（剥离 `/api/{service}`），下游服务**禁止**感知前缀
4. 新增服务**必须**先在路由表声明，再写业务代码

### 12.4 限流规范

1. 限流规则**必须**从 Nacos 拉取，**禁止**硬编码
2. 限流降级**必须**返回结构化 JSON（`R<Void>`），**禁止**返回纯文本
3. 限流**必须**分维度：路由、接口、IP、用户
4. **禁止**把限流阈值设过大（导致限流失效）或过小（导致误杀）

### 12.5 日志规范

1. **禁止**打印 Token、密码
2. 慢请求（>1s）**必须** WARN 日志
3. **必须**打印 `traceId`，方便全链路追踪
4. **禁止**打印请求 body（可能含敏感数据），只能打印 method/uri/cost/status

### 12.6 配置规范

1. 路由配置**必须**在 Nacos，**禁止**写死在 application.yml
2. CORS 白名单**必须**用环境变量：`gateway.cors.allowed-origins=${CORS_ORIGINS:http://localhost:5173}`
3. 生产环境**必须**关闭 Knife4j 文档：`knife4j.production=true`

### 12.7 测试规范

1. 网关测试**必须**用 `WebTestClient`（响应式），**禁止** `MockMvc`
2. 路由测试**必须**用 `RouteLocatorTest`
3. 鉴权测试**必须**覆盖：白名单放行、Token 有效、Token 缺失、Token 过期、Token 被踢

---

## 13. 配置文件

### 13.1 bootstrap.yml

```yaml
spring:
  application:
    name: spring-cloud-gateway
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_ADDR:127.0.0.1:8848}
        namespace: ${NACOS_NAMESPACE:public}
      config:
        server-addr: ${NACOS_ADDR:127.0.0.1:8848}
        file-extension: yaml
        shared-configs:
          - data-id: spring-cloud-shared.yaml
            refresh: true
```

### 13.2 application.yml

```yaml
server:
  port: 8080

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics,gateway
  metrics:
    tags:
      application: ${spring.application.name}

spring:
  cloud:
    gateway:
      metrics:
        enabled: true       # 网关指标（路由维度 QPS/P99）
```

---

## 14. 必须实现的接口清单

| # | 方法 | 路径 | 说明 | 鉴权 |
|---|------|------|------|------|
| 1 | - | `/api/{service}/**` | 业务请求转发 | Sa-Token |
| 2 | - | `/ws/{service}/**` | WebSocket 转发 | Token（query 参数） |
| 3 | - | `/xxl-job/**` | XXL-JOB Admin 代理 | Basic Auth |
| 4 | GET | `/actuator/health` | 健康检查 | 匿名 |
| 5 | GET | `/actuator/prometheus` | Prometheus 指标 | 内网 |
| 6 | GET | `/actuator/gateway/routes` | 路由列表（运维） | @SaCheckRole(ADMIN) |
| 7 | GET | `/actuator/gateway/refresh` | 路由刷新 | @SaCheckRole(ADMIN) |
| 8 | GET | `/doc.html` | Knife4j 文档（仅 dev/test） | 匿名 |
| 9 | GET | `/v3/api-docs` | OpenAPI 聚合文档 | 匿名 |
| 10 | GET | `/fallback` | 限流降级响应 | - |

---

## 15. 红线（违反即拒绝）

1. ❌ 依赖 `spring-boot-starter-web`（必须用 WebFlux 响应式）
2. ❌ 在网关写阻塞 IO / 同步 RPC
3. ❌ 用 `HttpServletRequest` / `@WebFilter` / `RestTemplate`
4. ❌ 路由用自动发现（`discovery.locator.enabled=true`）
5. ❌ `StripPrefix` 与下游不匹配（下游感知 `/api/system/` 前缀）
6. ❌ Token 透传到下游（必须只透传 `X-Login-Id`）
7. ❌ 自己解析 JWT（必须用 Sa-Token 的 API）
8. ❌ `Access-Control-Allow-Origin: *`（必须显式白名单）
9. ❌ 限流降级返回非结构化 JSON
10. ❌ 路由配置硬编码（必须放 Nacos）
11. ❌ 生产环境开放 Knife4j 文档
12. ❌ 在白名单中放管理类接口
13. ❌ 打印 Token / 密码 / 请求 body
