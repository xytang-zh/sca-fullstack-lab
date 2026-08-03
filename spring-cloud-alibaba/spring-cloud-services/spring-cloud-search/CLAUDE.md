# CLAUDE.md — spring-cloud-search 全文检索服务

> 本文档面向 AI 编码助手，用于在 `spring-cloud-search/` 目录下工作时提供模块约束、技术栈版本、服务职责与开发规范。
> 工作前**必须**先读取父目录 [`spring-cloud-services/CLAUDE.md`](../CLAUDE.md)、后端聚合 [`spring-cloud-alibaba/CLAUDE.md`](../../CLAUDE.md) 与仓库根 [`sca-fullstack-lab/CLAUDE.md`](../../../CLAUDE.md) 了解全局规范。

---

## 1. 模块定位

全文检索服务，按个人博客需求文档在原有"索引/同步/搜索/聚合"基础上**新增搜索建议、结果高亮、RSS 订阅源**。基于 ElasticSearch 提供文章多维度检索，博客系统的搜索入口。

| 维度 | 值 |
|------|-----|
| 父 POM | `com.xytang:spring-cloud-alibaba:1.0-SNAPSHOT` |
| 顶级包 | `com.xytang.search` |
| 端口 | HTTP 8087 / Dubbo 20887 / XXL-JOB 10005 |

---

## 2. 核心功能

| 模块 | 功能 | 需求来源 |
|------|------|---------|
| 索引模块 | 创建、删除、更新 Mapping | 原职责 |
| 同步模块 | 监听 article 发布事件 → ES 写入；定时全量重建（job 服务触发） | blog-domain 全文搜索 |
| 搜索模块 | 多字段搜索、**结果高亮**、分页、中文分词（ik_max_word） | blog-domain 全文搜索 |
| 建议模块 | **搜索建议（前缀自动补全）**，防抖输入联想 | blog-domain 搜索建议 |
| RSS 模块 | **RSS 2.0 订阅源**（最新已发布文章，Redis 缓存） | blog-domain RSS 订阅 |
| 聚合模块 | 按分类、标签聚合统计 | 原职责 |

### 2.1 索引一致性

- 增量：article 服务 Dubbo 调用 `SearchRpcService.syncArticleIndex` 同步；失败记 article 本地消息表
- 兜底：job 服务定时调用 `rebuildAllIndex` 全量重建（幂等，基于 article_id 覆盖）
- 删除：`deleteArticleIndex(articleId)`

---

## 3. 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Data Elasticsearch | 3.5.0（父 POM） | ES 操作 |
| ElasticSearch | 8.15+（基础设施） | 全文检索，ik_max_word 分词器（计划） |
| Redis | 7.4+（父 POM） | RSS 缓存、搜索建议缓存 |
| Dubbo | 3.3+（Spring Cloud Alibaba 管理） | 暴露 `SearchRpcService`（syncArticleIndex/deleteArticleIndex/rebuildAllIndex） |
| RabbitMQ | Spring Boot AMQP（父 POM） | 文章发布事件增量同步（可选） |

---

## 4. 关键接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/search` | 全文检索 `{ "keyword": "Spring", "page": 1, "size": 10 }` |
| GET | `/api/search/suggest?q=Spr` | 搜索建议（前缀自动补全） |
| GET | `/api/rss` | RSS 2.0 XML（Redis 缓存） |
| GET | `/api/search/indices` | 索引列表 |
| POST | `/api/search/indices/sync` | 全量重建（ADMIN，job 调用） |

Dubbo 接口（RPC 契约）：

| 接口 | 方法 | 说明 |
|------|------|------|
| `SearchRpcService` | `syncArticleIndex(ArticleIndexDTO)` | article 发布时增量同步（幂等） |
| `SearchRpcService` | `deleteArticleIndex(Long articleId)` | 文章删除时移除索引 |
| `SearchRpcService` | `rebuildAllIndex(List<ArticleIndexDTO>)` | job 全量重建（Bulk 写入） |

---

## 5. 索引文档

```
article_index
  id / title / summary / content / category / tags / author
  published_at / like_count / comment_count
```

---

## 6. 服务间通信

| 方向 | 方式 | 说明 |
|------|------|------|
| article → search | Dubbo | 文章索引增量同步 |
| job → search | Dubbo | 定时全量重建 |

---

## 7. 开发规范（本服务特有）

- 搜索接口**必须**分页（pageNum/pageSize），**禁止**全量返回
- RSS 响应**必须** Redis 缓存（TTL 随机化），**禁止**每次实时生成
- 搜索建议词**必须**防抖 + 限制长度（≥2 字符）
- 高亮**必须**转义用户输入，**禁止** XSS 注入
- 索引 Mapping 变更**必须**走重建流程（alias + reindex）

---

## 8. 红线（违反即拒绝）

1. ❌ 子服务 POM 覆盖父 POM 依赖版本
2. ❌ `@Autowired` 字段注入
3. ❌ `throw new RuntimeException` / `catch (Exception e)`
4. ❌ `System.out.println` / `e.printStackTrace()`
5. ❌ 搜索/建议结果不转义导致 XSS
6. ❌ RSS 每次实时生成不打缓存
7. ❌ 全量重建不幂等（重复数据）
8. ❌ 业务配置硬编码（必须放 Nacos）
