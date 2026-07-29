# 日志查询契约：spring-cloud-log

**服务**：`spring-cloud-log`（端口 8089）
**前缀**：`/api/log/`（由 Gateway `StripPrefix=2` 剥离）
**关联规格**：spec FR-019 ~ FR-022、FR-024、SC-007、SC-009

> 本服务的日志由切面异步落盘（`spring-cloud-common-log` 模块），MVP 阶段仅提供查询接口；写入由业务服务通过 RabbitMQ 事件 `log.operation.create` / `log.login.create` 异步触发。

---

## 1. 操作日志查询

### 1.1 分页查询

#### GET /api/log/operations

**鉴权**：`@SaCheckPermission("log:operation:list")` + `@DataScope(userAlias="l")`

**Query 参数** `OperationLogPageQuery`：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `pageNum` | int | 否 | 默认 1 |
| `pageSize` | int | 否 | 默认 10，最大 100 |
| `userId` | long | 否 | 按操作人筛选 |
| `username` | string | 否 | 模糊 |
| `module` | string | 否 | 模块（如 `system/user`） |
| `operation` | string | 否 | 操作类型 |
| `status` | int | 否 | 1=成功 0=失败 |
| `startTime` | string | 否 | ISO-8601 开始时间 |
| `endTime` | string | 否 | ISO-8601 结束时间 |
| `orderBy` | string | 否 | 默认 `create_time desc` |

**响应** `PageVO<OperationLogListVO>`：

```json
{
  "code": 200, "msg": "success", "timestamp": "...",
  "data": {
    "list": [
      {
        "id": "20260701-10001",
        "userId": 10001, "username": "admin",
        "module": "system/user", "operation": "CREATE",
        "method": "UserController.create",
        "requestMethod": "POST", "requestUrl": "/api/system/users",
        "ip": "192.168.1.1", "location": "局域网",
        "costMs": 45, "status": 1,
        "createTime": "2026-07-30T10:00:00+08:00"
      }
    ],
    "total": 12345, "pageNum": 1, "pageSize": 10, "pages": 1235
  }
}
```

**业务规则**：
- ShardingSphere 按 `create_time` 月份路由到对应分表（FR-024）。
- 跨月查询走归并查询，P95 ≤ 3 秒（SC-009）。
- 数据权限：操作人仅可见本人或本部门及以下的操作日志（`@DataScope(userAlias="l")` 按 `user_id` 维度）。

---

### 1.2 详情

#### GET /api/log/operations/{id}

**鉴权**：`@SaCheckPermission("log:operation:query")`

**响应** `OperationLogVO`：含 `requestParams` 与 `responseResult`（敏感字段脱敏）：

```json
{
  "code": 200, "msg": "success", "timestamp": "...",
  "data": {
    "id": "20260701-10001",
    "userId": 10001, "username": "admin",
    "module": "system/user", "operation": "CREATE",
    "method": "UserController.create",
    "requestUrl": "/api/system/users",
    "requestMethod": "POST",
    "requestParams": "{\"username\":\"zhangsan\",\"phone\":\"138****8888\"}",
    "responseResult": "{\"code\":200}",
    "ip": "192.168.1.1", "location": "局域网",
    "costMs": 45, "status": 1,
    "errorMsg": null,
    "createTime": "2026-07-30T10:00:00+08:00"
  }
}
```

**脱敏规则**（FR-022）：
- 密码、Token、身份证号、手机号等敏感字段在 `requestParams` JSON 序列化时由 `LogParamFilter` 自动脱敏。
- 脱敏后无法通过任何接口还原原值。

---

## 2. 登录日志查询

### 2.1 分页查询

#### GET /api/log/logins

**鉴权**：`@SaCheckPermission("log:login:list")`

**Query 参数** `LoginLogPageQuery`：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `pageNum` | int | 否 | |
| `pageSize` | int | 否 | |
| `username` | string | 否 | 模糊 |
| `loginType` | int | 否 | 1=登录 2=登出 3=踢人下线 |
| `result` | int | 否 | 1=成功 0=失败 |
| `startTime` / `endTime` | string | 否 | |
| `orderBy` | string | 否 | 默认 `create_time desc` |

**响应** `PageVO<LoginLogListVO>`：

```json
{
  "code": 200, "msg": "success", "timestamp": "...",
  "data": {
    "list": [
      {
        "id": "...",
        "username": "admin", "userId": 10001,
        "loginTime": "2026-07-30T10:00:00+08:00",
        "ip": "192.168.1.1", "location": "局域网",
        "browser": "Chrome 126", "os": "Windows 11",
        "device": "PC", "loginType": 1, "result": 1
      }
    ],
    "total": 5432, "pageNum": 1, "pageSize": 10, "pages": 544
  }
}
```

---

### 2.2 快速跳转

#### GET /api/log/logins/by-user/{userId}

**鉴权**：`@SaCheckPermission("log:login:list")`

**业务规则**：从某条操作日志快速跳转到该用户的所有登录尝试（FR-021）。响应同分页查询。

---

## 3. 异步写入契约

业务服务通过 RabbitMQ 事件异步落盘日志（`spring-cloud-common-log` 切面自动发送）：

### 3.1 操作日志事件

**Exchange**：`log.exchange`
**Routing Key**：`log.operation.create`
**Payload**：

```json
{
  "userId": 10001,
  "username": "admin",
  "module": "system/user",
  "operation": "CREATE",
  "method": "UserController.create",
  "requestUrl": "/api/system/users",
  "requestMethod": "POST",
  "requestParams": "{\"username\":\"zhangsan\",\"phone\":\"138****8888\"}",
  "responseResult": "{\"code\":200}",
  "ip": "192.168.1.1",
  "costMs": 45,
  "status": 1,
  "errorMsg": null,
  "createTime": "2026-07-30T10:00:00+08:00"
}
```

### 3.2 登录日志事件

**Exchange**：`log.exchange`
**Routing Key**：`log.login.create`
**Payload**：

```json
{
  "username": "admin",
  "userId": 10001,
  "loginTime": "2026-07-30T10:00:00+08:00",
  "ip": "192.168.1.1",
  "browser": "Chrome 126",
  "os": "Windows 11",
  "loginType": 1,
  "result": 1,
  "failReason": null
}
```

**消费者幂等**：Listener 继承 `AbstractEventListener<T>`，通过 `eventId` 字段去重（FR-019）。

**MQ 不可用兜底**：日志切面有本地兜底落盘（写入 `logs/operation-fallback.log`），并触发告警（边界情况）。
