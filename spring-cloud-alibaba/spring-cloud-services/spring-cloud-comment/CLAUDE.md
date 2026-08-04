# CLAUDE.md — spring-cloud-comment 博客评论服务（★新增）

> 本文档面向 AI 编码助手，用于在 `spring-cloud-comment/` 目录下工作时提供模块约束、技术栈版本、服务职责与开发规范。
> 工作前**必须**先读取父目录 [`spring-cloud-services/CLAUDE.md`](../CLAUDE.md)、后端聚合 [`spring-cloud-alibaba/CLAUDE.md`](../../CLAUDE.md) 与仓库根 [`sca-fullstack-lab/CLAUDE.md`](../../../CLAUDE.md) 了解全局规范。

---

## 1. 模块定位

博客内容域评论服务，按《个人博客项目需求文档 v2.0》规划新增。负责评论全生命周期：发表评论、二级嵌套回复、评论审核、敏感词过滤、评论点赞。

| 维度 | 值 |
|------|-----|
| 父 POM | `com.xytang:spring-cloud-alibaba:1.0-SNAPSHOT` |
| 顶级包 | `com.xytang.comment` |
| 端口 | HTTP 8094 / Dubbo 20894 / XXL-JOB 10012 |
| 服务状态 | 规划中（骨架由后续变更创建，本文档为职责与规范基线） |

> ⚠️ 本服务为新增模块，当前仅存在 CLAUDE.md。服务骨架（pom.xml、启动类、配置）由 `/opsx:apply` 之后的独立变更创建。

---

## 2. 核心功能（对照 blog-domain spec）

| 模块 | 功能点 | 需求来源 |
|------|--------|---------|
| 评论模块 | 发表评论、二级嵌套回复（parent_id 指向一级评论）、被回复者记录（reply_to_id/reply_to_uid，支持 @ 通知） | blog-domain 评论管理 |
| 审核模块 | 状态机 PENDING → APPROVED/REJECTED/DELETED，管理员审核接口 | blog-domain 评论审核 |
| 敏感词模块 | 发表时敏感词过滤（sensitive-word），过滤后存储纯文本 | blog-domain 评论管理 |
| 安全模块 | XSS 过滤（Jsoup）、IP 地址 / User-Agent 记录（反垃圾） | blog-domain 评论管理 |
| 互动模块 | 评论点赞/取消（幂等） | blog-domain 互动 |
| 通知联动 | 评论创建事件发 MQ（`comment.created`）→ message 服务站内通知 | 方案设计 |

### 2.1 审核状态机

```
  发表                   审核
PENDING ───────────────▶ APPROVED（通过，对外可见）
    │                       │
    │                       └──────────▶ DELETED（删除）
    └─────────────────────▶ REJECTED（驳回）
```

- 评论默认状态 PENDING，管理员审核后可见；被驳回/删除的评论不再对外展示
- 文章评论数由 comment 服务维护（+1/-1），经 Dubbo 或事件同步给 article

---

## 3. 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.5.0（父 POM） | 服务基座 |
| MyBatis-Plus | 3.5.9（父 POM） | 评论表 ORM |
| Jsoup | 1.17.2（父 POM） | HTML XSS 过滤 |
| Dubbo | 3.3+（Spring Cloud Alibaba 管理） | 暴露 `CommentRpcService`；调用 article 校验文章存在 |
| Sa-Token | 1.44.0（父 POM） | `@SaCheckLogin`、ADMIN 角色校验 |
| sensitive-word | 0.x（**父 POM 未声明**，落地时补充） | 评论敏感词过滤 |

> 所有依赖版本**必须**在父 POM 声明，禁止在本服务 POM 覆盖。

---

## 4. 关键接口（RESTful 草案）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/articles/{articleId}/comments` | 评论列表（按 parent_id 分组，一级 + 二级） |
| POST | `/api/articles/{articleId}/comments` | 发表评论/回复（需登录） |
| DELETE | `/api/comments/{id}` | 删除（本人或管理员） |
| POST | `/api/comments/{id}/like` | 评论点赞/取消（幂等） |
| GET | `/api/comments/pending` | 待审核评论分页（ADMIN） |
| POST | `/api/comments/{id}/audit` | 审核（APPROVED/REJECTED，ADMIN） |

Dubbo 接口（RPC 契约）：

| 接口 | 方法 | 说明 |
|------|------|------|
| `CommentRpcService` | `countByArticleId(Long articleId)` | 供 article 展示评论数 |

---

## 5. 数据模型

```
t_comment   评论表
  id            雪花 ID
  article_id    所属文章 ID（建索引）
  user_id       评论人 ID（建索引）
  parent_id     父评论 ID（一级评论为 NULL，回复指向一级评论）
  reply_to_id   被回复的评论 ID（@ 通知用）
  reply_to_uid  被回复的用户 ID
  content       评论内容（纯文本，已敏感词/XSS 过滤）
  status        PENDING / APPROVED / REJECTED / DELETED（建索引）
  like_count    点赞数
  ip_address    发表 IP（反垃圾）
  user_agent    浏览器 UA
  create_time / update_time / del_flag
```

---

## 6. 服务间通信

| 方向 | 方式 | 说明 |
|------|------|------|
| comment → article | Dubbo | 发表评论前校验文章存在 |

---

## 7. 开发规范（本服务特有）

### 7.1 安全规范

- 评论内容**必须**先敏感词过滤（sensitive-word），再 XSS 过滤（Jsoup），最后存**纯文本**，**禁止**存 HTML 原文
- **必须**记录 IP 与 UA（反垃圾溯源）

### 7.2 事务与一致性

- 发表评论（过滤 + 入库 + 评论数变更）**必须**在同一事务
- 评论数变更**必须**考虑审核流程：PENDING → APPROVED 时才 +1，REJECTED/DELETED 时 -1

### 7.3 防刷规范

- 同一 IP/用户发表频率**必须**限流（Redis 计数器或 Sentinel）
- 评论内容长度校验（1-1000 字符），**禁止**空白评论

### 7.4 RESTful 规范

- **必须**遵循 `spring-cloud-services/CLAUDE.md` §6 RESTful 强制规范
- 审核/点赞用动词子资源（`POST /api/comments/{id}/audit`）

---

## 8. 红线（违反即拒绝）

1. ❌ 子服务 POM 覆盖父 POM 依赖版本
2. ❌ `@Autowired` 字段注入（必须 `@RequiredArgsConstructor`）
3. ❌ `throw new RuntimeException` / `catch (Exception e)`
4. ❌ `System.out.println` / `e.printStackTrace()`
5. ❌ 评论内容未经敏感词/XSS 过滤直接存储
6. ❌ 评论数在审核流程中计数错误（PENDING 不计数）
7. ❌ 在日志中泄露 IP 之外的敏感信息（手机号/Token 等）
8. ❌ 用 `number` 类型接收/返回雪花 ID（前端必须 string）
9. ❌ 接口未加 `@SaCheckLogin` / `@SaCheckRole("ADMIN")`
10. ❌ 业务配置硬编码（必须放 Nacos）
