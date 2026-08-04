## Purpose

为 sca-fullstack-lab 前后端定义统一响应体契约：单一业务码 `code`、`message`/`data`/`timestamp`/`traceId` 字段、分页响应结构、错误码区段体系与 HTTP 状态码映射，作为前后端对齐的单一事实来源，降低前端解析成本并提升可观测性。

## ADDED Requirements

### Requirement: 统一响应体结构

系统所有 HTTP 接口（除 OAuth2 标准 token 端点与文件下载等二进制响应外）SHALL 返回统一响应体，字段固定为：`code`（Integer，业务状态码）、`message`（String，供前端直接展示的友好文案）、`data`（泛型，业务数据，失败时为 `null`）、`timestamp`（Long，毫秒时间戳）、`traceId`（String，当前请求链路追踪 ID）。结构 SHALL 保持稳定，成功与失败均使用同一结构，仅通过 `code` 区分。

#### Scenario: 成功响应

- **WHEN** 后端接口正常返回业务数据
- **THEN** 响应体为 `{code: 200, message: "success", data: <业务数据>, timestamp: <毫秒时间戳>, traceId: "<链路ID>"}`，`data` 为实际业务数据而非再包一层

#### Scenario: 失败响应

- **WHEN** 后端接口发生业务或系统错误
- **THEN** 响应体为 `{code: <业务错误码>, message: "<友好文案>", data: null, timestamp: <毫秒时间戳>, traceId: "<链路ID>"}`，HTTP 状态码为对应 4xx/5xx

#### Scenario: 数据为空时结构一致

- **WHEN** 成功但无业务数据（如删除操作）或失败
- **THEN** `data` 字段始终存在且为 `null`，前端无需判断字段是否存在

### Requirement: 业务状态码区段

系统响应体 `code` SHALL 使用业务状态码而非 HTTP 状态码，并按区段划分：`200` 表示成功；`1xxxx` 参数校验/请求格式错误；`2xxxx` 用户认证/权限相关；`3xxxx` 业务规则拦截（可恢复）；`4xxxx` 第三方服务错误；`5xxxx` 系统内部错误（不可恢复）。同一区段内 SHALL 允许再细分子错误码，且错误码应能区分所属服务/模块，便于告警与排查。

#### Scenario: 成功用 200

- **WHEN** 业务成功
- **THEN** `code` 为 `200`，与 HTTP 语义对齐

#### Scenario: 参数错误

- **WHEN** 请求参数校验失败（缺参、格式错误、类型不匹配）
- **THEN** `code` 落在 `1xxxx` 区段，HTTP 状态码为 `400`

#### Scenario: 用户/权限错误

- **WHEN** 发生未登录、Token 过期、无权限、账号不存在等
- **THEN** `code` 落在 `2xxxx` 区段，HTTP 状态码为 `401`/`403`/`404` 等对应码

#### Scenario: 业务规则拦截

- **WHEN** 发生可恢复的业务规则冲突（如余额不足、状态不允许操作）
- **THEN** `code` 落在 `3xxxx` 区段，HTTP 状态码为 `422` 或 `409`

#### Scenario: 系统错误

- **WHEN** 发生未捕获的系统异常（数据库故障、RPC 超时等）
- **THEN** `code` 落在 `5xxxx` 区段，HTTP 状态码为 `500`，`message` 为通用友好文案，不暴露内部细节

### Requirement: 分页响应结构

系统列表分页接口 SHALL 将分页数据放入 `data`，结构为 `{records: [...], total: <总记录数>, page: <当前页码>, size: <每页大小>, pages: <总页数>, hasPrevious: <是否有上一页>, hasNext: <是否有下一页>}`。`page` 从 1 开始，`records` 为当前页记录数组。

#### Scenario: 分页查询返回完整分页信息

- **WHEN** 前端请求分页接口并携带 `page`/`size`
- **THEN** 后端返回 `data` 包含 `records`、`total`、`page`、`size`、`pages`、`hasPrevious`、`hasNext`，前端可直接渲染分页组件

#### Scenario: 首页与末页的分页标记

- **WHEN** 当前页为第一页时
- **THEN** `hasPrevious` 为 `false`；当当前页为最后一页时 `hasNext` 为 `false`

#### Scenario: 空数据分页

- **WHEN** 查询结果为空
- **THEN** 返回 `records: []`、`total: 0`、`page` 与 `size` 为请求值，`pages: 0`、`hasPrevious`/`hasNext` 均为 `false`

### Requirement: 错误码枚举体系

系统 SHALL 提供结构化的错误码定义能力：每个错误码 SHALL 包含业务码、用户友好文案（`userMessage`）与可选开发详情模板（`devMessage`，支持占位符填充）。`message` 字段 SHALL 始终为用户友好文案；开发详情 SHALL 仅在开发/测试环境通过日志或非生产响应暴露，生产环境不暴露内部错误细节。

#### Scenario: 用户友好文案

- **WHEN** 错误发生时
- **THEN** 响应体 `message` 为用户可读的友好文案（如"用户不存在"），前端可直接 toast 展示，不做额外加工

#### Scenario: 开发详情

- **WHEN** 开发/测试环境发生错误且启用了开发详情
- **THEN** 系统可提供带参数占位符填充的开发详情（如"用户不存在: userId=10086"），生产环境不返回该详情

### Requirement: 链路追踪

系统响应体 `traceId` SHALL 与全链路追踪头 `X-Trace-Id` 一致：网关接收或生成 `X-Trace-Id` 并透传下游，下游服务响应时 SHALL 将 `traceId` 回填到响应体，同时写回 `X-Trace-Id` 响应头。前端 SHALL 在请求头缺失时生成 `traceId` 并通过 `X-Trace-Id` 传递。

#### Scenario: 响应体携带 traceId

- **WHEN** 任意接口返回响应体
- **THEN** `traceId` 非空，且与 `X-Trace-Id` 响应头一致，可用于日志串联完整调用链

#### Scenario: 前端生成 traceId

- **WHEN** 前端未持有 `traceId`（如首次请求）
- **THEN** 前端生成一个 `traceId` 放入 `X-Trace-Id` 请求头，同一调用链内保持贯穿

### Requirement: 前端解包与错误处理

前端统一请求层 SHALL 以 `code === 200` 判定业务成功，成功时返回 `data` 供业务代码直接消费（无需 `.data.data`）；失败时对登录态失效类业务码（如未登录、Token 过期、被禁用、被踢下线）触发登录态清理与跳转登录页，其余失败仅展示 `message` 提示。HTTP 非 2xx 或网络错误 SHALL 走独立错误分支处理。

#### Scenario: 业务成功直接取 data

- **WHEN** 接口返回 `code === 200`
- **THEN** 请求层返回 `data`，业务代码拿到的是 `data` 本身（含类型推断），无需再解包外层

#### Scenario: 登录态失效跳转

- **WHEN** 请求返回登录态失效类业务码（如 Token 过期、未登录、被禁用、被踢下线）
- **THEN** 请求层清理本地登录态并跳转登录页

#### Scenario: 其他业务失败提示

- **WHEN** 请求返回非登录态失效类的失败业务码（如用户不存在、参数错误、系统繁忙）
- **THEN** 请求层展示 `message` 提示并拒绝该请求，业务代码获知失败

#### Scenario: HTTP 层错误

- **WHEN** 响应 HTTP 状态码为 4xx/5xx 或网络异常
- **THEN** 请求层按 HTTP 状态码处理（401 清理登录态），其余情况展示通用错误提示

### Requirement: 雪花 ID 序列化保持

系统响应体中所有 `Long` 类型雪花 ID 字段 SHALL 继续序列化为 String，前端 SHALL 用 TypeScript `string` 类型接收，禁止 `number`，避免 JS 精度丢失。

#### Scenario: 长 ID 序列化为字符串

- **WHEN** 响应体包含雪花 ID（如 `userId`、`articleId`）
- **THEN** 该字段以字符串形式传输，前端以其为 `string` 类型直接使用