# CLAUDE.md — spring-cloud-article 博客文章服务（★新增）

> 本文档面向 AI 编码助手，用于在 `spring-cloud-article/` 目录下工作时提供模块约束、技术栈版本、服务职责与开发规范。
> 工作前**必须**先读取父目录 [`spring-cloud-services/CLAUDE.md`](../CLAUDE.md)、后端聚合 [`spring-cloud-alibaba/CLAUDE.md`](../../CLAUDE.md) 与仓库根 [`sca-fullstack-lab/CLAUDE.md`](../../../CLAUDE.md) 了解全局规范。

---

## 1. 模块定位

博客内容域核心服务，按《个人博客项目需求文档 v2.0》规划新增。负责博客文章的全生命周期：发布/编辑/删除、分类/标签、Markdown 渲染、点赞/收藏、阅读量，并负责与 search 服务的 ES 索引同步。

| 维度 | 值 |
|------|-----|
| 父 POM | `com.xytang:spring-cloud-alibaba:1.0-SNAPSHOT` |
| 顶级包 | `com.xytang.article` |
| 端口 | HTTP 8093 / Dubbo 20893 / XXL-JOB 10011 |
| 服务状态 | 规划中（骨架由后续变更创建，本文档为职责与规范基线） |

> ⚠️ 本服务为新增模块，当前仅存在 CLAUDE.md。服务骨架（pom.xml、启动类、配置）由 `/opsx:apply` 之后的独立变更创建。

---

## 2. 核心功能（对照 blog-domain spec）

| 模块 | 功能点 | 需求来源 |
|------|--------|---------|
| 文章模块 | 发布/编辑/删除（软删）、草稿/待审核/已发布/已驳回状态流转、slug 唯一标识、置顶、封面图（经 file 服务） | blog-domain 文章管理 |
| 分类模块 | 分类 CRUD、唯一名称、URL 别名、排序、描述 | blog-domain 分类与标签 |
| 标签模块 | 标签 CRUD、唯一名称、URL 别名、文章-标签多对多关联 | 同上 |
| Markdown 模块 | Markdown → HTML 渲染、XSS 过滤后存储、渲染结果缓存 | blog-domain 文章管理 |
| 互动模块 | 点赞/取消（幂等）、收藏/取消（幂等）、阅读量计数 | blog-domain 互动 |
| 搜索同步 | 发布/更新/删除后经 Dubbo 同步 ES 索引（search 服务），失败记本地消息表由 job 定时补偿 | blog-domain 全文搜索 |
| 审核支持 | 提供审核所需状态查询与变更接口（供 admin 前端调用） | blog-domain 文章状态审核 |

### 2.1 状态机

```
       提交              审核
DRAFT ──────▶ AUDIT ────────▶ PUBLISHED（通过，触发 ES 同步）
                │
                └────────────▶ REJECTED（驳回，作者可见原因）
```

- USER 角色发布 → AUDIT；AUTHOR/ADMIN 角色发布 → PUBLISHED

### 2.2 互动幂等规则

- 点赞：Redis Set（`spring-cloud:article:like:{targetType}:{targetId}`）去重 + MySQL `t_like_record` 持久化，重复点赞 = 取消
- 收藏：同点赞规则，表 `t_favorite`
- 计数：先改 Redis 计数，异步回写 MySQL

---

## 3. 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.5.0（父 POM） | 服务基座 |
| MyBatis-Plus | 3.5.9（父 POM） | 文章/分类/标签/互动表 ORM |
| Jsoup | 1.17.2（父 POM） | HTML XSS 过滤 |
| Redis / Redisson | 4.0.0（父 POM） | 点赞去重、阅读量计数、文章详情多级缓存、分布式锁 |
| Dubbo | 3.3+（Spring Cloud Alibaba 管理） | 暴露 `ArticleRpcService`；调用 search 同步索引 |
| Sa-Token | 1.44.0（父 POM） | `@SaCheckLogin`、角色/权限校验 |
| commonmark-java | 0.21.x（**父 POM 未声明**，落地时补充） | Markdown → HTML 渲染 |
| 本地消息表 | — | ES 同步失败的最终一致性补偿（`t_sync_failed_log`） |

> 所有依赖版本**必须**在父 POM 声明，禁止在本服务 POM 覆盖。

---

## 4. 关键接口（RESTful 草案）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/articles` | 文章分页（?page=&size=&category=&tag=&sort=） |
| GET | `/api/articles/{id}` | 文章详情（Markdown 原文 + HTML） |
| POST | `/api/articles` | 创建文章 |
| PUT | `/api/articles/{id}` | 更新文章（全量） |
| PATCH | `/api/articles/{id}/status` | 状态流转 |
| DELETE | `/api/articles/{id}` | 软删除 |
| POST | `/api/articles/{id}/like` | 点赞/取消（幂等） |
| POST | `/api/articles/{id}/favorite` | 收藏/取消（幂等） |
| GET | `/api/categories` | 分类列表 |
| GET | `/api/tags` | 标签列表 |

Dubbo 接口（RPC 契约）：

| 接口 | 方法 | 说明 |
|------|------|------|
| `ArticleRpcService` | `existsById(Long id)` | 供 comment 校验文章存在 |
| `ArticleRpcService` | `listPublishedForSync(...)` | 供 job 全量索引重建拉取 |

---

## 5. 数据模型

```
t_article         文章表（title/slug/summary/content_md/content_html/cover_url/
                  category_id/status/view_count/like_count/comment_count/is_top/
                  is_original/published_at）
t_category        分类表（name/slug/description/sort_order）
t_tag             标签表（name/slug）
t_article_tag     文章-标签关联表（article_id/tag_id 联合主键）
t_like_record     点赞记录表（user_id/target_type/target_id 唯一键）
t_favorite        收藏表（user_id/article_id 唯一键）
t_sync_failed_log 同步失败日志（target_id/sync_type/status/retry_count，最终一致性补偿）
```

---

## 6. 服务间通信

| 方向 | 方式 | 说明 |
|------|------|------|
| article → search | Dubbo | 文章发布/更新/删除时同步 ES 索引（失败记本地消息表） |
| comment → article | Dubbo | comment 校验文章是否存在 |
| article → file | Dubbo | 封面图上传（文件服务） |
| article → system | Dubbo | 作者信息查询 |

---

## 7. 开发规范（本服务特有）

### 7.1 缓存规范

- 文章详情热点数据**必须**多级缓存（Caffeine L1 + Redis L2），Key：`spring-cloud:article:detail:{id}`
- 缓存 TTL **必须**加 ±10% 随机数
- 发布/更新后**必须**主动失效对应缓存

### 7.2 事务规范

- 发布文章（保存 + 渲染 HTML + 状态变更）**必须**在同一事务（Service 层 `@Transactional`）
- ES 同步**禁止**放在事务内：Dubbo 调用失败仅记本地消息表，不回滚文章发布

### 7.3 并发规范

- 点赞/收藏幂等**必须**用 Redis Set 原子操作 + 唯一键约束兜底
- 阅读量高并发更新**必须**走 Redis 计数 + 定时回写，**禁止**直接 UPDATE MySQL

### 7.4 RESTful 规范

- **必须**遵循 `spring-cloud-services/CLAUDE.md` §6 RESTful 强制规范
- 业务动作（点赞/收藏/审核）用动词子资源（`POST /api/articles/{id}/like`）
- **禁止**用 GET 执行写操作

---

## 8. 红线（违反即拒绝）

1. ❌ 子服务 POM 覆盖父 POM 依赖版本
2. ❌ `@Autowired` 字段注入（必须 `@RequiredArgsConstructor`）
3. ❌ `throw new RuntimeException` / `catch (Exception e)`
4. ❌ `System.out.println` / `e.printStackTrace()`
5. ❌ ES 同步放在事务内（必须事务外调用 + 本地消息表补偿）
6. ❌ 点赞/收藏不做幂等（必须 Redis Set 去重）
7. ❌ 文章 HTML 未经 XSS 过滤直接存储
8. ❌ 用 `number` 类型接收/返回雪花 ID（Long 序列化为 String，前端用 string）
9. ❌ 接口未加 `@SaCheckPermission`/`@SaCheckRole`
10. ❌ 业务配置硬编码（必须放 Nacos）
