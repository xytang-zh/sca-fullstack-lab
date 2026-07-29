# 公共契约模式：Platform MVP Foundation

**功能分支**：`001-platform-mvp`
**创建日期**：2026-07-30
**关联规格**：[spec.md](../spec.md) | [plan.md](../plan.md)

> 本文定义所有接口共享的契约模式：统一响应、异常、分页、校验、版本化。所有 `auth-api.md` / `system-api.md` / `log-api.md` / `portal-api.md` 中的接口均遵循本文约定。

---

## 1. 统一响应格式 `R<T>`

所有 HTTP 接口必须返回 `R<T>` 包装结构：

```json
{
  "code": 200,
  "msg": "success",
  "data": { ... },
  "timestamp": "2026-07-30T10:30:00.000+08:00"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `code` | int | ✅ | 业务码：200 成功；4xx 客户端错误；5xx 服务端错误；具体见 §2 |
| `msg` | string | ✅ | 简短消息（中文） |
| `data` | T \| null | ✅ | 业务数据；列表为 `PageVO<T>`，详情为对象，无数据返回时为 `null` |
| `timestamp` | string | ✅ | ISO-8601 时间戳，含时区 |

**业务码表**：

| 段位 | 含义 | 示例 |
|------|------|------|
| 200 | 成功 | 200 |
| 400 | 参数校验失败 | 40001 |
| 401 | 未登录 | 40101 |
| 403 | 权限不足 | 40301 |
| 404 | 资源不存在 | 40401 |
| 409 | 冲突（乐观锁） | 40901 |
| 429 | 限流 | 42901 |
| 500 | 系统异常 | 50000 |
| 5xx | 业务异常 | 50xxx |

---

## 2. 异常响应

### 2.1 业务异常

业务异常通过 `GlobalExceptionHandler` 统一捕获，HTTP 200 + `R.code` 业务码：

```json
{
  "code": 40101,
  "msg": "账号或密码错误",
  "data": null,
  "timestamp": "2026-07-30T10:30:00.000+08:00"
}
```

### 2.2 参数校验失败

Hibernate Validator 校验失败时 HTTP 200 + `R.code=40001` + `data` 为字段级错误列表：

```json
{
  "code": 40001,
  "msg": "参数校验失败",
  "data": [
    { "field": "username", "message": "账号长度必须在 3-64 之间" },
    { "field": "email", "message": "邮箱格式不正确" }
  ],
  "timestamp": "..."
}
```

### 2.3 限流

限流触发时返回 HTTP 429 + `R.code=42901` + `Retry-After` 头：

```
HTTP/1.1 429 Too Many Requests
Retry-After: 30

{
  "code": 42901,
  "msg": "请求过于频繁，请稍后再试",
  "data": null,
  "timestamp": "..."
}
```

### 2.4 乐观锁冲突

并发编辑冲突时返回 HTTP 409 + `R.code=40901`：

```json
{
  "code": 40901,
  "msg": "该资源已被他人修改，请刷新后重试",
  "data": null,
  "timestamp": "..."
}
```

---

## 3. 分页约定

### 3.1 入参（PageQuery 基类）

| 参数 | 类型 | 必填 | 默认 | 说明 |
|------|------|------|------|------|
| `pageNum` | int | 否 | 1 | 页码，从 1 起 |
| `pageSize` | int | 否 | 10 | 每页大小，最大 100 |
| `orderBy` | string | 否 | null | 排序字段，格式 `field asc,field2 desc` |

### 3.2 出参（PageVO<T>）

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "list": [ ... ],
    "total": 1234,
    "pageNum": 1,
    "pageSize": 10,
    "pages": 124
  },
  "timestamp": "..."
}
```

---

## 4. 校验注解

所有 DTO 入参使用 `@Validated` + Hibernate Validator 注解：

| 注解 | 用途 |
|------|------|
| `@NotBlank` | 字符串非 null/空 |
| `@Length(min, max)` | 字符串长度范围 |
| `@Email` | 邮箱格式 |
| `@Pattern(regexp)` | 正则匹配 |
| `@Min` / `@Max` | 数值范围 |
| `@NotNull` | 非 null |
| `@Size(min, max)` | 集合大小 |

---

## 5. 版本化

- 通过 HTTP Header `X-API-Version: 1` 传递版本号；省略时默认当前稳定版本。
- **禁止在 URI 中嵌入 `/v1/`**。
- 版本升级策略：旧版本至少保留 6 个月兼容期。

---

## 6. 鉴权约定

### 6.1 Token 传递

- 登录成功后，前端将 Token 存入 localStorage（admin）或 cookie（portal）。
- 后续请求通过 HTTP Header `Authorization: Bearer {tokenValue}` 传递。
- WebSocket 连接通过 query 参数 `?token={tokenValue}` 传递（MVP 后启用）。

### 6.2 网关拦截

- Gateway 全局过滤器校验 Token（Sa-Token `StpUtil.checkLogin()`）。
- 校验通过后透传 `X-Login-Id` Header 到业务服务。
- 业务服务通过 `StpUtil.getLoginIdAsLong()` 获取当前用户 ID。

### 6.3 权限校验

- 接口级：`@SaCheckPermission("system:user:list")` / `@SaCheckRole("super_admin")`。
- 数据级：`@DataScope(deptAlias="d", userAlias="u")`（自研注解）。

---

## 7. 跨域（CORS）

Gateway 配置全局 CORS：

```
Access-Control-Allow-Origin: https://admin.example.com, https://portal.example.com
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Authorization, Content-Type, X-API-Version, X-Login-Id
Access-Control-Allow-Credentials: true
Access-Control-Max-Age: 3600
```

- **生产环境禁止 `*` 通配**，必须列出具体域名。
- 预检请求（OPTIONS）由 Gateway 直接返回 204。

---

## 8. 限流规则

| 资源 | 限流策略 | 触发动作 |
|------|----------|----------|
| `/api/auth/login` POST | IP 维度，5 次/分钟 | 触发图形验证码 |
| `/api/auth/login` POST | 账号维度，5 次失败/15 分钟 | 锁定账号 |
| `/api/portal/**` GET | IP 维度，60 次/分钟 | 返回 429 + `Retry-After` |
| `/api/**`（其他） | 用户维度，100 次/分钟 | 返回 429 + `Retry-After` |
| Gateway 全局 | 1000 QPS | 熔断降级 |

---

## 9. 国际化

- MVP 阶段：错误消息仅简体中文。
- 错误消息通过 `MessageSource` 管理，为后续 i18n 预留接口。
- 公开门户支持中/英双语（基于 Vite SSG 多语言路由 `/zh/`、`/en/`）。

---

## 10. 幂等性

- `GET` / `PUT` / `DELETE` 天然幂等。
- `POST` 非幂等；对关键写操作（如创建用户、发布内容）通过 `X-Idempotency-Key` Header 实现幂等：
  - 前端生成 UUID 作为幂等键，5 分钟内同 Key 同入参返回首次结果。
  - 幂等键存储于 Redis，TTL 5 分钟。
