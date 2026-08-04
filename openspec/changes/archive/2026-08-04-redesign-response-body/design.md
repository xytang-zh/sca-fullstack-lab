## Context

当前响应体为"双码"体系（见 proposal.md Why）：`R<T>` 的 `code` 是 HTTP 状态码、`bizCode` 是 5 位字符串业务码，并带 `timestamp(Instant)`、`path`、`devMessage` 字段；分页出参 `PageVO`（list/total/pageNum/pageSize/pages）；错误码枚举 `BizCode(httpCode, code, message)`。

本次改造遵循《响应体字段设计.md》的单业务码思路，且已与用户确认四个决策：单一业务码（废弃 `bizCode`）、分页按 `records/total/page/size/pages/hasPrevious/hasNext`、`timestamp` 用 Long 毫秒、移除 `path`/`devMessage` 且 `message` 恒为友好文案。

改造涉及 core（R/错误码/分页/异常）、web（异常处理/Advice/过滤器）、security（网关鉴权）、gateway（异常处理）、全部业务 Controller，以及前端 types/api/页面。

## Goals / Non-Goals

**Goals:**
- 响应体收敛为 `{code, message, data, timestamp, traceId}` 五字段，业务码区段化，前后端零解析成本。
- 错误码体系支持区段划分 + 模块归属，保留当前全部错误语义，避免信息丢失。
- 分页出参 `PageResult` 与入参 `PageQuery` 字段名对齐（page/size），消除前后端认知混乱。
- 前端拦截器改为 `code === 200` 判定，`data` 直接解包，跳登录逻辑收敛为精确码集合。
- 网关（WebFlux）与服务端（Servlet MVC）响应体结构保持一致。

**Non-Goals:**
- 不改变雪花 ID Long→String 序列化、`X-Trace-Id`/`X-Login-Id`/`Authorization` 头透传机制。
- 不引入网关层对业务响应的二次包装（网关仍只透传）。
- 不改变业务功能逻辑，仅调整响应结构与字段。
- 不实现文档中的 `/export` 等 `@IgnoreResponseAdvice` 豁免场景（当前无此需求）。

## Decisions

### D1: 保留 `BizCode` 类名，改造为实现 `ErrorCode` 接口

新建 `ErrorCode` 接口（`getCode()/getUserMessage()/getDevMessage()/getHttpStatus()`），将现有 `BizCode` 枚举改造为该接口实现，字段从 `(httpCode, code, message)` 改为 `(code, httpStatus, userMessage, devMessage)`。

- **理由**：`BizCode` 被 29 个文件引用，保留类名可把改动收敛到方法签名（`bizCode.code()` → `bizCode.getCode()`），避免全量替换类名；同时 `ErrorCode` 接口满足"接口 + 枚举"的可扩展设计，支持自定义错误码。
- **替代方案**：新建 `BizErrorCode` 枚举并删除 `BizCode` —— 更贴近文档命名，但破坏面大、风险高，且文档命名非强制要求，故不采用。

### D2: 错误码区段 + 模块归属（5 位数字）

```
┌──────────────────────────────────────────────────────────────┐
│  code = 区段(1位) + 模块(1位) + 序号(3位)                      │
│  区段:  1 参数 / 2 用户权限 / 3 业务规则 / 4 第三方 / 5 系统    │
│  模块:  0 通用 / 1 auth / 2 system / 3 article / 4 comment    │
│         5 portal / 6 message / 7 search / 8 file / 99 gateway │
│  例:    20004 = 2(用户权限) + 0(通用) + 004(Token 过期)        │
└──────────────────────────────────────────────────────────────┘
```

成功码固定 `200`；通用错误模块号取 `0`，使文档示例 `20001`（用户不存在）、`30001`（业务规则）等码值成立；具体服务错误通过模块号区分（如 `1 + 1 + 001` = auth 参数错误）。

- **理由**：区段位满足"运维按 5xxxx 告警、前端按区段处理"的告警/排查诉求；模块位保留多服务语义，兼容现有 `BizCode` 的模块归属能力。
- **替代方案**：纯区段 + 序号（如文档示例 `20001`）—— 更简单但丢失模块维度；考虑项目为多服务架构，保留模块位价值更高。

### D3: 新 `R<T>` 五字段结构

```java
public class R<T> implements Serializable {
    private Integer code;       // 业务状态码（200 成功）
    private String message;     // 友好文案，恒为用户可读
    private T data;             // 业务数据，失败为 null
    private Long timestamp;     // 毫秒时间戳
    private String traceId;     // 链路追踪 ID
    // ok() / ok(data) / ok(message, data) / fail(ErrorCode) / fail(ErrorCode, message) / fail(code, message)
    // isSuccess()  → code == 200
}
```

移除 `bizCode`/`path`/`devMessage`；`timestamp` 由 `Instant.now()` 改为 `System.currentTimeMillis()`；`traceId` 仍由 `RResponseAdvice` 从 MDC 回填。

- **理由**：与《响应体字段设计.md》§1.2 代码一致；`timestamp` 用 Long 毫秒时区无关、前端按需格式化。
- **替代方案**：保留 `Instant` —— 前端解析成本高，且与用户确认相悖，不采用。

### D4: 分页出参 `PageResult` + 入参 `PageQuery` 字段对齐

```
PageResult<T> { records: List<T>, total: long, page: int, size: int, pages: int, hasPrevious: boolean, hasNext: boolean }
PageQuery     { page: int = 1, size: int = 10, orderBy: String, keyword: String }
```

`PageQuery` 入参由 `pageNum/pageSize` 改为 `page/size`，与出参对齐；`PageResult.of(list, total, page, size)` 自动计算 `pages` 与 `hasPrevious/hasNext`。

- **理由**：前后端字段名一致，避免"传 pageNum、收 page"的认知错位；本次本就是破坏性重构，一并处理更干净。
- **替代方案**：仅改出参、保留入参 `pageNum/pageSize` —— 破坏面小但前后端不对齐，长期埋坑，不采用。

### D5: 网关与业务服务错误码统一

网关错误码并入区段体系，不再使用 `bizCode` 字符串：
- `GW_TOKEN_MISSING` → `TOKEN_MISSING(20005, 401)`、`GW_TOKEN_INVALID` → `TOKEN_INVALID(20006, 401)`
- `GW_ROUTE_NOT_FOUND` → `50004`（404）、`GW_DOWNSTREAM_UNAVAILABLE` → `50005`（503）、`GW_DOWNSTREAM_TIMEOUT` → `50006`（504）
- `RATE_LIMIT` → `30001`（429）

- **理由**：网关异常处理（`GatewayExceptionHandler`/`AuthGatewayFilterFactory`）是 WebFlux，需手动构造 R 响应；统一错误码后前端对网关错误与业务错误用同一套 `code` 判断逻辑。
- **替代方案**：网关单独维护一套码 —— 前端需两套判断，不采用。

### D6: 前端跳登录逻辑收敛为精确码集合

前端不再按"模块号前缀"分派，改为 `code === 200` 成功判定 + 精确码集合触发登录态清理：

```ts
const LOGIN_REQUIRED_CODES = [20004, 20005, 20006, 20008, 20009] // Token过期/未登录/失效/禁用/锁定
if (code === 200) return data
if (LOGIN_REQUIRED_CODES.includes(code)) { clearToken(); redirectToLogin() }
handleMessage(message)
```

- **理由**：`2xxxx` 区段含 `20001`（用户不存在）等非登录态错误，整区段跳登录会误伤；精确码集合语义清晰、可维护。
- **替代方案**：按区段 `code >= 20000 && code < 30000` 跳登录 —— 实现简单但逻辑错误（用户不存在也跳登录），不采用。

### D7: `message` 恒为友好文案，开发详情仅走日志

删除 `DevMessageHolder`/`PathHolder` 及 `R` 的 `devMessage` 字段；`GlobalExceptionHandler` 不再把异常堆栈写入响应，而是打在日志（WARN/ERROR），`message` 恒为 `userMessage` 友好文案。

- **理由**：用户确认"message 始终为友好文案"；生产环境天然不泄露内部细节，回归文档"生产给用户、开发走日志"的分层意图。
- **替代方案**：保留 `devMessage` 字段仅 dev 返回 —— 与用户确认相悖，不采用。

## Risks / Trade-offs

- **破坏性契约变更** → 后端 `R`/`BizCode`/`PageVO`/`PageQuery` 全部 core 类改动，影响所有服务与前端。缓解：本 change 一次性完成前后端同步改造，并提供 `mvn -pl spring-cloud-common-core -am` 编译验收 + 前端 `pnpm typecheck` 验收。
- **`BizCode` 枚举值语义迁移** → 5 位字符串映射为 5 位数字，若映射错误会导致错误码漂移。缓解：design 中给出完整映射表，迁移动态涉及的业务异常一并核对。
- **测试存量** → `RTest`/`BizCodeTest`/`GlobalExceptionHandlerTest`/`AuthServiceImplTest` 依赖旧结构。缓解：tasks 中单列测试更新任务，作为验收门禁。
- **网关 404 归 5xxxx** → 路由不存在被归为"系统错误"区段，可能与告警语义冲突。缓解：HTTP 状态码仍为 404，网关/监控按 HTTP 码告警，`code` 仅供前端区分，语义冲突可控。

## Migration Plan

1. 后端 core 先行：`ErrorCode` 接口 → `BizCode` 改造 → `R` 改造 → `PageResult`/`PageQuery` → `BusinessException` 体系 → 删除 `DevMessageHolder`/`PathHolder`，同步更新单测。
2. 后端 web/security/gateway：`GlobalExceptionHandler`/`RResponseAdvice`/`TraceIdFilter`/`AuthGatewayFilterFactory`/`GatewayExceptionHandler` 改造并更新测试。
3. 业务服务：全量 Controller 返回泛型与分页调用适配（`R<PageVO<...>>` → `R<PageResult<...>>`，`bizCode` 引用替换）。
4. 前端：`types` 更新 → `api/request.ts` 拦截器 → `api/services` 分页类型 → 页面组件适配。
5. 文档：根/后端/前端 CLAUDE.md 契约同步更新。
6. 验收：后端 `mvn clean install -DskipTests`（含 checkstyle）、前端 `pnpm typecheck && pnpm lint`，前后端联调验证。

## Open Questions

无（关键决策已与用户确认，错误码映射与字段命名均在 design 中定稿）。