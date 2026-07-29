# 网关路由与限流契约：spring-cloud-gateway

**服务**：`spring-cloud-gateway`（端口 8080）
**关联规格**：spec FR-013、FR-015、SC-003、SC-008、边界情况（限流）

> 本文件定义 Gateway 的路由规则、鉴权过滤器、限流策略与跨域配置。所有外部 HTTP 请求经 Gateway 统一拦截后再转发到业务服务。

---

## 1. 路由表

| # | 路径前缀 | 目标服务 | StripPrefix | 鉴权 | 备注 |
|---|----------|----------|-------------|------|------|
| 1 | `/api/auth/**` | `spring-cloud-auth:8081` | 2 | 部分免鉴权（登录/验证码/SSO） | 见 auth-api.md |
| 2 | `/api/system/**` | `spring-cloud-system:8082` | 2 | 全部需鉴权 | 见 system-api.md |
| 3 | `/api/portal/admin/**` | `spring-cloud-portal:8090` | 2 | 全部需鉴权 | 见 portal-api.md §2 |
| 4 | `/api/portal/**` | `spring-cloud-portal:8090` | 2 | 免鉴权 | 公开访客接口（§1） |
| 5 | `/api/log/**` | `spring-cloud-log:8089` | 2 | 全部需鉴权 | 见 log-api.md |
| 6 | `/ws/**` | 各业务服务 | 0 | Token via query 参数 | WebSocket（MVP 后启用） |
| 7 | `/doc.html` | Knife4j 聚合文档 | 0 | 仅内网访问 | API 文档 |

**路由配置示例**（Nacos `gateway-shared.yaml`）：

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: auth-service
          uri: lb://spring-cloud-auth
          predicates:
            - Path=/api/auth/**
          filters:
            - StripPrefix=2
            - name: AuthFilter
              args:
                excludePaths: /login,/captcha,/sso/login,/sso/checkTicket,/oauth2/authorize,/oauth2/token,/oauth2/refresh
        - id: system-service
          uri: lb://spring-cloud-system
          predicates: [ Path=/api/system/** ]
          filters: [ StripPrefix=2 ]
        - id: portal-admin
          uri: lb://spring-cloud-portal
          predicates: [ Path=/api/portal/admin/** ]
          filters: [ StripPrefix=2 ]
        - id: portal-public
          uri: lb://spring-cloud-portal
          predicates: [ Path=/api/portal/** ]
          filters: [ StripPrefix=2, PublicAccessFilter ]
        - id: log-service
          uri: lb://spring-cloud-log
          predicates: [ Path=/api/log/** ]
          filters: [ StripPrefix=2 ]
```

---

## 2. 全局过滤器

### 2.1 AuthFilter（鉴权）

**执行顺序**：`Ordered.HIGHEST_PRECEDENCE + 100`

**逻辑**：
1. 若请求路径匹配 `excludePaths`，直接放行。
2. 从 `Authorization` Header 取 `Bearer {token}`。
3. 调 `StpUtil.checkLoginByToken(token)` 校验。
4. 校验失败：返回 HTTP 401 + `R.code=40101`。
5. 校验通过：将 `X-Login-Id` Header 设为当前用户 ID，透传给业务服务。
6. WebSocket 连接（`/ws/**`）从 query 参数 `?token=` 取 Token。

### 2.2 PublicAccessFilter（公开访问）

仅注册在 `/api/portal/**`（非 admin）路由上，标记为免鉴权，直接放行。

### 2.3 RateLimitFilter（限流）

基于 Redis + Redisson 实现的令牌桶限流：

| 资源 | 维度 | 阈值 | 触发动作 |
|------|------|------|----------|
| `/api/auth/login` POST | IP | 5 次/分钟 | 触发图形验证码 |
| `/api/auth/login` POST | 账号 | 5 次失败/15 分钟 | 锁定账号 15 分钟 |
| `/api/portal/**` GET | IP | 60 次/分钟 | 返回 HTTP 429 + `Retry-After: 30` |
| `/api/**`（其他） | 用户 | 100 次/分钟 | 返回 HTTP 429 + `Retry-After: 30` |
| Gateway 全局 | 全部 | 1000 QPS | 熔断降级，返回 HTTP 503 |

### 2.4 TraceFilter（链路追踪）

为每个请求生成 `X-Trace-Id`（UUID），写入 MDC，透传到业务服务。所有日志输出自动带上 `traceId`。

---

## 3. 跨域（CORS）配置

```yaml
spring:
  cloud:
    gateway:
      globalcors:
        cors-configurations:
          '[/**]':
            allowed-origins:
              - "https://admin.example.com"
              - "https://portal.example.com"
              - "http://localhost:5173"   # 开发环境
              - "http://localhost:5174"
            allowed-methods: [GET, POST, PUT, PATCH, DELETE, OPTIONS]
            allowed-headers: [Authorization, Content-Type, X-API-Version, X-Login-Id, X-Idempotency-Key]
            allow-credentials: true
            max-age: 3600
```

**生产环境禁止 `*` 通配**，必须列出具体域名。

---

## 4. 灰度发布

通过请求头 `X-Gray-Version` 路由到灰度版本实例：

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: system-service-gray
          uri: lb://spring-cloud-system-gray
          predicates:
            - Path=/api/system/**
            - Header=X-Gray-Version, v2
          filters: [ StripPrefix=2 ]
```

MVP 阶段不启用，第 5+ 周扩展。

---

## 5. 熔断与降级

集成 Sentinel，对每个路由配置熔断规则：

| 路由 | 熔断阈值 | 降级响应 |
|------|----------|----------|
| `/api/auth/**` | 慢调用 > 3s，比例 > 50% | 返回 HTTP 503 + `R.code=50301` + "认证服务暂不可用" |
| `/api/system/**` | 异常比例 > 50% | 同上 |
| `/api/portal/**` | 慢调用 > 1s，比例 > 30% | 返回门户兜底静态页 |

降级响应通过 `FallbackHandler` 实现，确保用户看到友好提示而非 500。

---

## 6. 限流维度说明

### 6.1 IP 维度

- 通过 `RemoteAddress` 解析；若经代理，取 `X-Forwarded-For` 第一个 IP。
- IPv4 / IPv6 均支持。

### 6.2 账号维度

- 仅对 `/api/auth/login` POST 生效。
- 触发条件：连续 5 次密码错误。
- 锁定动作：账号状态 1/2→4，`lock_until = now + 15min`。

### 6.3 用户维度

- 对所有 `/api/**` 需鉴权接口生效。
- 触发条件：1 分钟内 > 100 次请求。
- 限流动作：返回 429，`Retry-After: 30`。

---

## 7. 健康检查

### GET /actuator/health

无需鉴权，返回 Gateway 与下游服务健康状态：

```json
{
  "status": "UP",
  "components": {
    "diskSpace": { "status": "UP" },
    "ping": { "status": "UP" },
    "auth-service": { "status": "UP" },
    "system-service": { "status": "UP" }
  }
}
```

供 Prometheus + Grafana 监控使用（MVP 阶段仅采集基础指标，第 8 周扩展）。
