# CLAUDE.md — spring-cloud-portal 公开门户服务

> 本文档面向 AI 编码助手，用于在 `spring-cloud-portal/` 目录下工作时提供模块约束、技术栈版本、服务职责与开发规范。
> 工作前**必须**先读取父目录 [`spring-cloud-services/CLAUDE.md`](../CLAUDE.md)、后端聚合 [`spring-cloud-alibaba/CLAUDE.md`](../../CLAUDE.md) 与仓库根 [`sca-fullstack-lab/CLAUDE.md`](../../../CLAUDE.md) 了解全局规范。

---

## 1. 模块定位

公开门户服务，按个人博客需求文档已**轻量化**：只读门户展示聚合（文章/新闻/产品列表与详情 + SEO/GEO）。**博客写操作（发布/评论/点赞）已移出本服务**，由 `spring-cloud-article` / `spring-cloud-comment` 承接（代码迁移由后续变更执行）。

| 维度 | 值 |
|------|-----|
| 父 POM | `com.xytang:spring-cloud-alibaba:1.0-SNAPSHOT` |
| 顶级包 | `com.xytang.portal` |
| 端口 | HTTP 8090 / Dubbo 20890 / XXL-JOB 10008 |
| 鉴权 | 读接口公开（GET），写操作必须 `@SaCheckRole("ADMIN")`（如新闻/产品维护） |

---

## 2. 核心功能（读为主）

| 模块 | 功能 |
|------|------|
| 文章展示模块 | 文章分页/详情（读，数据源对齐 article 服务，兼容期可读本地表） |
| 新闻模块 | 新闻发布、栏目管理 |
| 产品模块 | 产品 CRUD、规格 |
| SEO 模块 | Sitemap、Meta 自动生成、robots.txt |
| GEO 模块 | 地理位置内容（基于用户 IP 定位） |

> ⚠️ 博客内容写操作（发布/审核/点赞）**不在本服务**——见 `spring-cloud-article/CLAUDE.md` 与 `spring-cloud-comment/CLAUDE.md`。

---

## 3. 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.5.0（父 POM） | 服务基座 |
| MyBatis-Plus | 3.5.9（父 POM） | 新闻/产品表 |
| Redis | 7.4+（父 POM） | 文章缓存、阅读量计数 |
| ElasticSearch | 8.15+（基础设施） | 全文搜索（调 search 服务） |
| Dubbo | 3.3+（Spring Cloud Alibaba 管理） | 调用 file 获取文件元数据 |

---

## 4. 关键接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/portal/articles` | 文章分页（公开） |
| GET | `/api/portal/articles/{id}` | 文章详情（公开） |
| GET | `/api/portal/articles/hot` | 热门文章 |
| GET | `/api/portal/news` | 新闻分页 |
| GET | `/api/portal/products` | 产品分页 |
| GET | `/api/portal/sitemap.xml` | Sitemap |
| GET | `/api/portal/robots.txt` | Robots |

---

## 5. 开发规范（本服务特有）

- 公开读接口**必须**做缓存（Redis），防缓存穿透
- SEO 元数据**必须**随内容更新自动失效
- 与 article 服务的数据一致性：兼容期由后续迁移变更统一，**禁止**新写业务逻辑
- 阅读量计数走 Redis，**禁止**直接 UPDATE

---

## 6. 红线（违反即拒绝）

1. ❌ 子服务 POM 覆盖父 POM 依赖版本
2. ❌ `@Autowired` 字段注入
3. ❌ `throw new RuntimeException` / `catch (Exception e)`
4. ❌ `System.out.println` / `e.printStackTrace()`
5. ❌ 在本服务新增博客写接口（必须走 article/comment）
6. ❌ 公开接口无缓存/防穿透
7. ❌ 业务配置硬编码（必须放 Nacos）
