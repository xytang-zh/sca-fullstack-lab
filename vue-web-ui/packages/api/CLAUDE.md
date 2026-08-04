# CLAUDE.md — packages/api 统一 API 包

> 工作前**必须**先读取 [`vue-web-ui/CLAUDE.md`](../../CLAUDE.md) 与仓库根 [`sca-fullstack-lab/CLAUDE.md`](../../../CLAUDE.md)。

## 定位

`@sca/api`：统一 axios 实例与各服务 API 模块，全项目唯一的 HTTP 出口。

## 核心约定

### request 实例（`src/request.ts`）

- baseURL：`import.meta.env.VITE_API_BASE_URL`
- 请求拦截器：自动携带 `Authorization: Bearer {token}`、`X-Trace-Id`
- 响应拦截器：
  - 业务 `code === 200` → 返回 `data`
  - 业务 `code` 为登录态失效码（`LOGIN_REQUIRED_CODES`：未登录/Token 过期/被禁用/被踢下线）→ 清理登录态并跳转登录页
  - 其余失败 → 显示 `message` 错误提示
- 拦截器**必须**在此包实现，应用层**禁止**重复写拦截逻辑

### R<T> 契约（与后端对齐）

```json
{ "code": 200, "message": "success", "data": { ... }, "timestamp": 1722470400000, "traceId": "..." }
```

- code 语义：200 成功 / 1xxxx 参数 / 2xxxx 用户权限 / 3xxxx 业务 / 4xxxx 第三方 / 5xxxx 系统
- 分页出参：`PageResult<T>`（records/total/page/size/pages/hasPrevious/hasNext），分页入参 `page`/`size`
- 雪花 ID：后端序列化为 String，前端**必须**用 `string` 类型

## 服务模块（`src/services/`）

按后端服务分目录：`auth`、`system`、`article` ★、`comment` ★、`search`、`file`、`monitor`、`message`、`log`、`portal`

## 规范

1. 每个 API 函数**必须**有 TS 入参/出参类型（引用 `@sca/types`）
2. 分页出参**必须**用 `PageResult<T>`
3. API 路径**必须** RESTful：GET 查询 / POST 新增 / PUT 全量 / PATCH 部分 / DELETE 删除

## 红线

1. ❌ 应用层直接 `import axios`（必须经本包）
2. ❌ 用 `number` 类型接收 ID
3. ❌ 硬编码 baseURL
4. ❌ 在拦截器外自行处理业务 code
