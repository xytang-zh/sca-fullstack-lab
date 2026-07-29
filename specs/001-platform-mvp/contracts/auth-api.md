# 认证中心契约：spring-cloud-auth

**服务**：`spring-cloud-auth`（端口 8081）
**前缀**：`/api/auth/`（由 Gateway `StripPrefix=2` 剥离）
**关联规格**：spec FR-001 ~ FR-008、FR-024 ~ FR-029

---

## 1. 登录

### POST /api/auth/login

**鉴权**：无需 Token

**请求体** `LoginDTO`：

```json
{
  "username": "admin",
  "password": "Pass@1234",
  "captcha": "a1b2",
  "captchaKey": "uuid-from-captcha-endpoint",
  "rememberMe": true
}
```

| 字段 | 类型 | 必填 | 校验 |
|------|------|------|------|
| `username` | string | ✅ | 3-64 字符 |
| `password` | string | ✅ | 8-32 字符 |
| `captcha` | string | ✅ | 4 字符 |
| `captchaKey` | string | ✅ | UUID |
| `rememberMe` | boolean | 否 | 默认 false |

**响应** `LoginVO`：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "tokenName": "Authorization",
    "tokenValue": "Bearer eyJ...",
    "expiresIn": 1800,
    "userId": 10001,
    "username": "admin",
    "nickname": "超级管理员",
    "avatar": "/avatar/admin.png",
    "roles": ["super_admin"],
    "perms": ["*:*:*"]
  },
  "timestamp": "..."
}
```

**业务规则**：
- 验证码先校验（Redis 取 `captcha:{captchaKey}` 对比，校验后删除）。
- 密码 BCrypt 校验。
- 连续 5 次失败后锁定账号 15 分钟（FR-003）。
- 登录成功后清除 `fail_count`，记录登录日志（FR-020）。
- `rememberMe=true` 时 `expiresIn` 延长至 7 天。

---

### GET /api/auth/captcha

**鉴权**：无需 Token

**响应** `CaptchaVO`：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "captchaKey": "550e8400-e29b-41d4-a716-446655440000",
    "captchaImg": "data:image/png;base64,iVBORw0..."
  }
}
```

**业务规则**：
- 4 位字母数字混合，忽略大小写。
- 验证码存 Redis，TTL 5 分钟，校验后删除。

---

## 2. 登出

### POST /api/auth/logout

**鉴权**：需 Token

**请求体**：空

**响应**：

```json
{
  "code": 200,
  "msg": "success",
  "data": null
}
```

**业务规则**：
- 销毁当前 Token 对应会话。
- 若为 SSO 主会话，触发 SLO 单点注销，通知所有子系统下线（FR-006）。
- 记录登录日志（`login_type=2` 登出）。

---

## 3. 踢人下线

### POST /api/auth/kickout

**鉴权**：`@SaCheckPermission("auth:kickout")`

**请求体** `KickoutDTO`：

```json
{
  "userId": 10077
}
```

**响应**：

```json
{
  "code": 200,
  "msg": "已将用户踢下线",
  "data": null
}
```

**业务规则**：
- 调用 `StpUtil.kickout(userId)`（Sa-Token 核心 API，FR-004）。
- 通过 Redis Pub/Sub 通知所有子系统清除本地会话。
- 目标用户在 5 秒内从所有子系统页面跳回登录中心（SC-008）。
- 记录登录日志（`login_type=3` 踢人下线）。
- **禁止踢下线自己**（边界情况）。

---

## 4. SSO 单点登录

### GET /api/auth/sso/login

**鉴权**：无需 Token

**Query 参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `redirect` | string | 是 | 子系统回调 URL |
| `back` | string | 否 | 登录后回跳路径 |

**流程**：
1. 用户未登录 → 返回登录页（HTML 或 JSON 视前端类型）。
2. 用户已登录 → 生成 Ticket，重定向到 `redirect?ticket={ticket}`。

**响应**（已登录场景，HTTP 302）：
```
Location: https://子系统域名/sso/login?ticket=abc123def456
```

---

### POST /api/auth/sso/checkTicket

**鉴权**：无需 Token（子系统服务端调用）

**请求体**：

```json
{
  "ticket": "abc123def456"
}
```

**响应**：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "userId": 10001,
    "username": "admin",
    "nickname": "超级管理员"
  }
}
```

**业务规则**：
- Ticket 一次性使用，TTL 60 秒（Redis `sa:sso:ticket:{ticket}`）。
- 校验通过后子系统调 `StpUtil.login(userId)` 建立本地会话。

---

### POST /api/auth/sso/logout

**鉴权**：需 Token

**请求体** `SsoLogoutDTO`：

```json
{
  "userId": 10001,
  "ignoreClient": "client-app-id"
}
```

**响应**：

```json
{
  "code": 200,
  "msg": "success",
  "data": null
}
```

**业务规则**：
- 调用 `SaSsoServerUtil.ssoLogout(loginId, SaLogoutParameter, ignoreClient)` 实现单点注销（FR-006）。
- 通过 Sa-Token SSO 通知所有注册的子系统下线。

---

## 5. OAuth2 Server（第三方接入）

### GET /api/auth/oauth2/authorize

**鉴权**：需登录（Resource Owner）

**Query 参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `client_id` | string | 是 | 客户端 ID |
| `redirect_uri` | string | 是 | 回调 URL（需与注册一致） |
| `response_type` | string | 是 | 固定 `code` |
| `scope` | string | 否 | 申请权限范围 |
| `state` | string | 否 | CSRF 防护 |

**响应**：
- 用户已授权 → HTTP 302 重定向到 `redirect_uri?code={code}&state={state}`
- 用户未授权 → 返回授权确认页面

---

### POST /api/auth/oauth2/token

**鉴权**：HTTP Basic（client_id:client_secret）

**请求体**（`application/x-www-form-urlencoded`）：

```
grant_type=authorization_code
code={code}
redirect_uri={redirect_uri}
```

**响应**：

```json
{
  "access_token": "eyJ...",
  "token_type": "Bearer",
  "expires_in": 7200,
  "refresh_token": "def...",
  "scope": "read"
}
```

---

### POST /api/auth/oauth2/refresh

**请求体**：

```
grant_type=refresh_token
refresh_token={refresh_token}
```

**响应**：同 `/token`，返回新的 access_token。

---

## 6. 当前用户信息

### GET /api/auth/me

**鉴权**：需 Token

**响应** `UserVO`（脱敏）：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": 10001,
    "username": "admin",
    "nickname": "超级管理员",
    "email": "a****@example.com",
    "phone": "138****8888",
    "avatar": "/avatar/admin.png",
    "deptId": 1,
    "deptName": "总部",
    "roles": [
      { "id": 1, "code": "super_admin", "name": "超级管理员", "dataScope": 1 }
    ],
    "perms": ["*:*:*"]
  }
}
```

---

## 7. 修改密码

### PATCH /api/auth/me/password

**鉴权**：需 Token

**请求体** `PasswordUpdateDTO`：

```json
{
  "oldPassword": "Old@1234",
  "newPassword": "New@1234",
  "confirmPassword": "New@1234"
}
```

**业务规则**：
- 校验旧密码（BCrypt 比对）。
- 新密码 8-32 字符，需含字母+数字。
- `confirmPassword` 必须等于 `newPassword`。
- 修改成功后销毁当前 Token，要求重新登录。
