# 公开门户契约：spring-cloud-portal

**服务**：`spring-cloud-portal`（端口 8090）
**前缀**：`/api/portal/`（由 Gateway `StripPrefix=2` 剥离）
**关联规格**：spec FR-016 ~ FR-018、FR-029、SC-004、SC-005

> 本服务对外提供两类接口：**管理员管理公开内容**（需登录 + `@SaCheckPermission`）与**公开访客浏览内容**（无需登录）。

---

## 1. 公开访客接口（无需登录）

### 1.1 内容列表

#### GET /api/portal/contents

**鉴权**：无需 Token

**Query 参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `type` | int | 是 | 1=博客 2=新闻 3=产品介绍 |
| `category` | string | 否 | 分类 |
| `tag` | string | 否 | 标签 |
| `pageNum` | int | 否 | 默认 1 |
| `pageSize` | int | 否 | 默认 10，最大 50 |
| `lang` | string | 否 | `zh` / `en`，默认按 `Accept-Language` 自动判断 |

**响应** `PageVO<PortalContentListVO>`：

```json
{
  "code": 200, "msg": "success", "timestamp": "...",
  "data": {
    "list": [
      {
        "id": 1,
        "title": "Spring Cloud Alibaba 2025.0.0 发布",
        "slug": "sca-2025-0-0-released",
        "summary": "...",
        "coverImage": "/covers/sca.jpg",
        "category": "Release", "tags": ["Spring Cloud", "Release"],
        "authorName": "张三",
        "publishTime": "2026-07-30T10:00:00+08:00"
      }
    ],
    "total": 42, "pageNum": 1, "pageSize": 10, "pages": 5
  }
}
```

**业务规则**：
- 仅返回 `status=3` 已发布的内容（FR-029）。
- 缓存：Redis 缓存 5 分钟，TTL 加 ±10% 随机；变更时主动清除。
- 数据来源：admin 后台发布的内容；门户前端 SSG 预渲染通过此接口获取数据。

---

### 1.2 内容详情

#### GET /api/portal/contents/{slug}

**鉴权**：无需 Token

**响应** `PortalContentVO`：

```json
{
  "code": 200, "msg": "success", "timestamp": "...",
  "data": {
    "id": 1,
    "title": "Spring Cloud Alibaba 2025.0.0 发布",
    "slug": "sca-2025-0-0-released",
    "contentType": 1, "contentTypeName": "博客",
    "body": "<p>正文 HTML（已 XSS 清洗）</p>",
    "summary": "...",
    "coverImage": "/covers/sca.jpg",
    "category": "Release", "tags": ["Spring Cloud", "Release"],
    "seoTitle": "...", "seoDescription": "...", "seoKeywords": "...",
    "ogImage": "/covers/sca.jpg",
    "authorName": "张三",
    "publishTime": "2026-07-30T10:00:00+08:00"
  }
}
```

**HTTP 语义**（FR-029）：
- `status=3` 已发布：返回 200 + 内容。
- `status=4` 已下架：返回 HTTP 410 Gone + `Retry-After: 86400`，让爬虫从索引中移除。
- 不存在：返回 HTTP 404 + `R.code=40401`。

---

### 1.3 站点地图

#### GET /api/portal/sitemap.xml

**鉴权**：无需 Token

**响应**：XML 格式 sitemap，包含所有 `status=3` 已发布内容的 URL 与 `lastmod`（发布时间），供搜索引擎抓取。

---

## 2. 管理员接口（需登录）

### 2.1 内容列表（管理）

#### GET /api/portal/admin/contents

**鉴权**：`@SaCheckPermission("portal:content:list")`

**Query 参数**：与公开列表相同，但额外支持：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `status` | int | 否 | 1=草稿 2=待审核 3=已发布 4=已下架 |
| `authorId` | long | 否 | 按作者筛选 |

**响应**：返回所有状态的内容（不只已发布）。

---

### 2.2 创建

#### POST /api/portal/admin/contents

**鉴权**：`@SaCheckPermission("portal:content:create")`

**请求体** `PortalContentCreateDTO`：

```json
{
  "title": "Spring Cloud Alibaba 2025.0.0 发布",
  "slug": "sca-2025-0-0-released",
  "contentType": 1,
  "body": "<p>...</p>",
  "summary": "...",
  "coverImage": "/covers/sca.jpg",
  "category": "Release",
  "tags": ["Spring Cloud", "Release"],
  "seoTitle": "...", "seoDescription": "...", "seoKeywords": "...",
  "ogImage": "/covers/sca.jpg",
  "status": 1
}
```

**校验**：
- `title`：1-255 字符
- `slug`：1-255 字符，仅字母/数字/短横线，全局唯一
- `body`：非空，HTML 入库前 jsoup 清洗
- `contentType`：1/2/3

**业务规则**：
- 初始状态 `status=1` 草稿（FR-029）。
- `authorId` 自动填充为当前登录用户。
- 创建后写入操作日志（FR-019）。
- 使用 `X-Idempotency-Key` 实现幂等。

---

### 2.3 更新

#### PUT /api/portal/admin/contents/{id}

**鉴权**：`@SaCheckPermission("portal:content:update")`

**业务规则**：乐观锁校验（FR-027）。

---

### 2.4 状态迁移（业务动作）

#### POST /api/portal/admin/contents/{id}/submit

**鉴权**：`@SaCheckPermission("portal:content:submit")`

**业务规则**：状态 1=草稿→2=待审核。普通员工可调用。

#### POST /api/portal/admin/contents/{id}/approve

**鉴权**：`@SaCheckPermission("portal:content:approve")` + 业务审批人角色

**请求体**：

```json
{ "version": 1 }
```

**业务规则**：
- 状态 2=待审核→3=已发布。
- 记录 `reviewer_id` 为当前用户，`publish_time` 为当前时间。
- 发送 RabbitMQ 事件 `portal.content.published` 触发 SSG 重新构建。
- 写入操作日志。

#### POST /api/portal/admin/contents/{id}/reject

**请求体**：

```json
{ "reason": "内容需要修订", "version": 1 }
```

**业务规则**：状态 2=待审核→1=草稿，记录驳回原因。

#### POST /api/portal/admin/contents/{id}/unpublish

**鉴权**：`@SaCheckPermission("portal:content:unpublish")` + 超级管理员

**业务规则**：
- 状态 3=已发布→4=已下架。
- 发送 `portal.content.unpublished` 事件，触发 SSG 删除对应静态页。
- 后续访问该内容返回 410 Gone。

#### POST /api/portal/admin/contents/{id}/republish

**业务规则**：状态 4=已下架→1=草稿，重新进入工作流。

---

## 3. 删除

#### DELETE /api/portal/admin/contents/{id}

**鉴权**：`@SaCheckPermission("portal:content:delete")` + 超级管理员

**业务规则**：软删除（`deleted=1`）；已发布内容删除前需先下架。
