## Why

当前项目的统一响应体采用"双码"设计：`code`=HTTP 状态码 + `bizCode`=5 位字符串业务码，并附带 `path`、`devMessage` 等冗余字段，`timestamp` 使用 `Instant`。这与公认的企业级最佳实践（单一业务码 + HTTP 状态码各司其职）存在偏差，增加了前后端协作成本与前端解析成本。本次按《响应体字段设计.md》的设计思路重构响应体契约，使前后端契约更简洁、可观测、前端友好。

## What Changes

- **BREAKING** `R<T>` 字段重构：`code` 改为业务状态码（200 成功；1xxxx 参数校验；2xxxx 用户/权限；3xxxx 业务规则；4xxxx 第三方服务；5xxxx 系统错误），废弃 `bizCode` 字段。
- **BREAKING** 移除 `path`、`devMessage` 字段；`message` 始终为友好文案，后端按环境切换详细程度（dev 给详细、prod 给友好）。
- **BREAKING** `timestamp` 由 `Instant`（ISO-8601 字符串）改为 `Long` 毫秒时间戳。
- **BREAKING** 分页响应 `PageVO` 重构为 `PageResult`，字段 `records / total / page / size / pages / hasPrevious / hasNext`（新增布尔分页标记）。
- 错误码体系重构：`BizCode` 枚举按区段重新设计为 `ErrorCode` 接口 + `BizErrorCode` 枚举，保留模块语义（支持按服务/模块划分错误码段），提供 `userMessage` 与 `formatDevMessage` 能力。
- 前端 `packages/api` 拦截器与 `packages/types` 类型定义同步：成功判定改为 `code === 200`，解包方式（业务层直接拿 `data`）保持不变。
- HTTP 状态码策略保持"各司其职"：业务错误 HTTP 返回 4xx/5xx，`code` 返回业务码，网关/监控可正常告警。

## Capabilities

### New Capabilities

- `api-response-contract`: 定义统一响应体契约（成功/失败/分页结构、业务码区段、错误码枚举、HTTP 状态码映射、前端解包规则），作为前后端对齐的单一事实来源。

### Modified Capabilities

无（现有 specs 均不涉及响应体字段结构，本次行为变更由新 capability 承载）。

## Impact

- **后端**：`spring-cloud-alibaba/spring-cloud-common/spring-cloud-common-core`（`R`、`BizCode`、`PageVO`、`PageQuery`、`BusinessException` 体系）、`spring-cloud-common/spring-cloud-common-web`（`GlobalExceptionHandler`、`RResponseAdvice`、`TraceIdFilter`）、`spring-cloud-gateway`（`TraceIdGatewayFilterFactory`）、全部业务服务 Controller 的返回泛型与分页调用。
- **前端**：`vue-web-ui/packages/types`（`R`/`PageVO` 类型）、`vue-web-ui/packages/api`（`request.ts` 拦截器）、所有调用分页接口的页面组件。
- **文档**：`CLAUDE.md` 跨端契约 §4.1、`docs/` 相关设计文档。

## Non-goals

- 不改变雪花 ID 序列化为 String 的既有约定。
- 不改变 `X-Trace-Id` / `X-Login-Id` / `Authorization` 头透传机制（`traceId` 字段来源不变）。
- 不引入新的网关层响应体包装（网关仍只透传，不包装业务响应）。
- 不对既有业务功能本身做任何逻辑改动，仅调整响应体的结构与字段。