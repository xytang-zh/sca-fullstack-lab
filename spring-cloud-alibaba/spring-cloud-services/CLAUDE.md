# CLAUDE.md — spring-cloud-services 业务服务聚合

> 本文档面向 AI 编码助手，用于在 `spring-cloud-services/` 目录下（或任意子服务下）工作时提供模块约束、技术栈版本、服务职责与开发规范。
> 工作前**必须**先读取父目录的 [`spring-cloud-alibaba/CLAUDE.md`](../CLAUDE.md) 与仓库根 [`sca-fullstack-lab/CLAUDE.md`](../../CLAUDE.md) 了解全局规范。

---

## 1. 聚合模块定位

`spring-cloud-services` 是 10 个业务/基础设施微服务的 **Maven 聚合 POM**，本身不含代码，只通过 `<modules>` 声明子服务。其中 `spring-cloud-article`、`spring-cloud-comment` 为按个人博客需求文档新增的博客域服务，其余 8 个为保留服务。

| 维度 | 值 |
|------|-----|
| 父 POM | `com.xytang:spring-cloud-alibaba:1.0-SNAPSHOT` |
| 当前 artifactId | `spring-cloud-services` |
| packaging | `pom` |
| 子服务数量 | 10 |
| 顶级包前缀 | `com.xytang.{服务名}` |

---

## 2. 子服务清单（10 个）

| # | 服务 | 端口 | 类型 | 顶级包 | 启动类                              |
|---|------|------|------|--------|----------------------------------|
| 1 | `spring-cloud-system` | 8082 | 业务 | `com.xytang.system` | `SpringCloudSystemApplication`   |
| 2 | `spring-cloud-monitor` | 8083 | 业务 | `com.xytang.monitor` | `SpringCloudMonitorApplication`  |
| 3 | `spring-cloud-message` | 8086 | 基础设施 | `com.xytang.message` | `SpringCloudMessageApplication`  |
| 4 | `spring-cloud-search` | 8087 | 基础设施 | `com.xytang.search` | `SpringCloudSearchApplication`   |
| 5 | `spring-cloud-file` | 8088 | 基础设施 | `com.xytang.file` | `SpringCloudFileApplication`     |
| 6 | `spring-cloud-log` | 8089 | 基础设施 | `com.xytang.log` | `SpringCloudLogApplication`      |
| 7 | `spring-cloud-portal` | 8090 | 业务 | `com.xytang.portal` | `SpringCloudPortalApplication`   |
| 8 | `spring-cloud-job` | 8091 | 基础设施 | `com.xytang.job` | `SpringCloudJobApplication`      |
| 9 | `spring-cloud-article` | 8093 | 业务 ★新增 | `com.xytang.article` | `SpringCloudArticleApplication`  |
| 10 | `spring-cloud-comment` | 8094 | 业务 ★新增 | `com.xytang.comment` | `SpringCloudCommentApplication`  |

> Dubbo 端口：system 20882 起每服务 +1；article 20893、comment 20894。XXL-JOB 执行器端口 10000 起，article 10011、comment 10012。

---

## 3. 标准服务模块结构

每个业务服务**必须**遵循以下结构：

```
spring-cloud-{服务名}/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/xytang/{服务名}/
    │   │   ├── SpringCloud{服务名}Application.java       启动类
    │   │   ├── config/                              配置类
    │   │   ├── controller/                          Controller（RESTful API）
    │   │   ├── service/                              Service 接口
    │   │   │   └── impl/                            Service 实现
    │   │   ├── mapper/                              MyBatis Mapper 接口
    │   │   ├── entity/                              数据库实体
    │   │   ├── dto/                                  DTO（入参）
    │   │   │   ├── {Biz}CreateDTO.java
    │   │   │   ├── {Biz}UpdateDTO.java
    │   │   │   └── {Biz}PageQuery.java              分页查询入参
    │   │   ├── vo/                                   VO（出参）
    │   │   │   ├── {Biz}VO.java
    │   │   │   └── {Biz}DetailVO.java
    │   │   ├── enums/                                枚举
    │   │   ├── exception/                           业务异常
    │   │   ├── listener/                            MQ 消费者
    │   │   ├── rpc/                                  Dubbo Provider（如有）
    │   │   │   └── {Biz}RpcProvider.java
    │   │   ├── job/                                  XXL-JOB 任务（如有）
    │   │   │   └── {Biz}SyncJob.java
    │   │   ├── ws/                                   WebSocket 处理（如有）
    │   │   └── constant/                            常量
    │   └── resources/
    │       ├── application.yml
    │       ├── application-dev.yml
    │       ├── application-prod.yml
    │       ├── bootstrap.yml                         Nacos 引导
    │       ├── mapper/                                MyBatis XML
    │       │   └── {Biz}Mapper.xml
    │       ├── i18n/                                  国际化
    │       └── db/migration/                         Flyway 脚本
    │           └── V1.0.0__init_{service}_tables.sql
    └── test/
        └── java/com/xytang/{服务名}/
            ├── SpringCloud{服务名}ApplicationTests.java
            └── {Biz}ServiceTest.java
```

## 4. 各服务详细说明

### 4.1 spring-cloud-system（系统管理服务）

#### 4.1.1 服务定位

系统管理服务，按个人博客需求文档已**裁剪为 RBAC 核心**：用户/角色/菜单/权限，为博客 USER/AUTHOR/ADMIN 三角色提供数据支撑。其他服务通过 Dubbo 调它的接口。

> ⚠️ 部门/岗位/字典/参数/通知等企业级模块**已移出职责边界**，不再作为本服务核心功能（代码裁剪由后续变更承接）。

#### 4.1.2 核心功能（RBAC 核心）

| 模块 | 功能点 |
|------|--------|
| 用户模块 | 用户 CRUD、重置密码、启用禁用、分配角色（USER/AUTHOR/ADMIN） |
| 角色模块 | 角色 CRUD、菜单分配、权限标识 |
| 菜单模块 | 菜单树 CRUD、按钮权限标识、路由元数据、缓存策略 |
| 权限模块 | 权限标识管理、角色-权限关联、数据权限（MyBatis-Plus 拦截器 + `@DataScope`） |

#### 4.1.3 技术栈

- Spring Boot 3.5
- MyBatis-Plus 3.5.9（含分页、多租户、数据权限拦截器）
- dynamic-datasource 4.3.1（主库 MySQL，备 KingbaseES/DM）
- Caffeine + Redis（多级缓存字典数据）
- Dubbo 3.3（对外暴露 `UserService`、`DeptService` 等 RPC 接口）
- EasyExcel（用户导入导出）

#### 4.1.4 关键接口（RESTful）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/system/users` | 用户分页 |
| GET | `/system/users/{id}` | 用户详情 |
| POST | `/system/users` | 新增用户 |
| PUT | `/system/users/{id}` | 修改用户 |
| PATCH | `/system/users/{id}/password` | 重置密码 |
| PATCH | `/system/users/{id}/status` | 启用/禁用 |
| POST | `/system/users/import` | 导入 Excel |
| GET | `/system/users/export` | 导出 Excel |
| GET | `/system/roles` | 角色分页 |
| GET | `/system/menus/tree` | 菜单树 |
| GET | `/system/menus/routes` | 前端动态路由 |
| GET | `/system/depts/tree` | 部门树 |
| GET | `/system/dicts/data/{type}` | 按类型查字典数据 |
| GET | `/system/notices` | 通知分页 |
| PATCH | `/system/notices/{id}/read` | 标记已读 |

#### 4.1.5 数据模型

```
sys_user, sys_role, sys_menu, sys_dept, sys_post,
sys_user_role, sys_role_menu, sys_role_dept, sys_user_post,
sys_dict_type, sys_dict_data, sys_config, sys_notice, sys_notice_read
```

---

### 4.2 spring-cloud-monitor（服务器监控服务）

#### 4.2.1 服务定位

采集服务器/JVM 指标，存时序库，通过 WebSocket 推送到前端大盘。

#### 4.2.2 核心功能

| 模块 | 功能 |
|------|------|
| 采集模块 | OSHI 采集本机指标；接收 monitor-agent 上报 |
| 存储模块 | TDengine 超级表 `aurora_metrics` |
| 推送模块 | Netty WebSocket Server（端口 9090），按 `userId → Channel` 推送 |
| 历史查询 | 按 service_name + 时间范围查询 |
| 告警模块 | 阈值规则（Nacos 配置）→ 命中发 MQ → message-service 推送 |
| Prometheus 整合 | 暴露 `/actuator/prometheus` |

#### 4.2.3 技术栈

- OSHI 6.6（系统信息采集）
- TDengine 3.3.2.0 + taos-jdbcdriver
- Netty 4.1（WebSocket Server，端口 9090）
- Spring Boot Actuator + micrometer-registry-prometheus
- RabbitMQ（发告警事件）

#### 4.2.4 关键接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/monitor/realtime` | 当前所有服务最新指标 |
| GET | `/monitor/history` | 按 service_name + 时间范围查历史 |
| GET | `/monitor/services` | 列出所有被监控服务 |
| GET | `/monitor/alerts` | 当前活跃告警 |
| WS | `/ws/monitor/{userId}` | WebSocket 订阅实时指标 |

#### 4.2.5 数据模型

- TDengine 超级表 `aurora_metrics`
- MySQL `biz_alert_rule` 告警规则表
- Redis `monitor:alert:{service}:{metric}` 告警状态去抖

---

### 4.3 spring-cloud-article（博客文章服务）★新增

#### 4.3.1 服务定位

博客内容域核心服务：文章 CRUD、分类/标签、Markdown 渲染、点赞/收藏、阅读量。

#### 4.3.2 核心功能

| 模块 | 功能 |
|------|------|
| 文章模块 | 发布/编辑/删除、草稿/待审核/已发布/已驳回状态流转、slug 唯一标识、置顶、封面图 |
| 分类模块 | 分类 CRUD、排序、URL 别名 |
| 标签模块 | 标签 CRUD、URL 别名、文章-标签关联 |
| Markdown 模块 | Markdown→HTML 渲染（commonmark-java）、XSS 过滤（Jsoup） |
| 互动模块 | 点赞/取消（幂等）、收藏/取消、阅读量计数 |
| 同步模块 | 发布后经 Dubbo 同步 ES 索引（search 服务），失败记本地消息表由 job 补偿 |

#### 4.3.3 技术栈

- MyBatis-Plus 3.5.9（文章/分类/标签/点赞收藏表）
- commonmark-java（计划，**父 POM 未声明**，落地时补充）
- Jsoup 1.17.2（XSS 过滤）
- Redis / Redisson 4.0.0（点赞去重 Set、阅读量计数、多级缓存）
- Dubbo 3.3（暴露 `ArticleService`、调用 search 同步索引）
- Sa-Token 1.44.0（`@SaCheckLogin` / 角色校验）

#### 4.3.4 关键接口（RESTful 草案）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/articles` | 文章分页（?page=&size=&category=&tag=） |
| GET | `/articles/{id}` | 文章详情（Markdown 原文 + HTML） |
| POST | `/articles` | 创建文章（AUTHOR/ADMIN 直接发布，USER 进 AUDIT） |
| PUT | `/articles/{id}` | 更新文章 |
| DELETE | `/articles/{id}` | 软删除 |
| PATCH | `/articles/{id}/status` | 状态流转（草稿→发布→审核） |
| POST | `/articles/{id}/like` | 点赞/取消（幂等） |
| POST | `/articles/{id}/favorite` | 收藏/取消（幂等） |
| GET | `/categories` | 分类列表 |
| GET | `/tags` | 标签列表 |

#### 4.3.5 数据模型

```
t_article, t_category, t_tag, t_article_tag,
t_like_record, t_favorite, t_sync_failed_log（最终一致性补偿）
```

---

### 4.4 spring-cloud-comment（博客评论服务）★新增

#### 4.4.1 服务定位

博客内容域评论服务：评论发表、二级嵌套回复、评论审核、敏感词过滤。

#### 4.4.2 核心功能

| 模块 | 功能 |
|------|------|
| 评论模块 | 发表评论、二级嵌套回复、被回复者记录（@ 通知） |
| 审核模块 | 状态机 PENDING→APPROVED/REJECTED/DELETED，管理员审核 |
| 敏感词模块 | 发表时敏感词过滤（sensitive-word），过滤后存储 |
| 安全模块 | XSS 过滤（Jsoup）、IP/UA 记录（反垃圾） |
| 互动模块 | 评论点赞（幂等） |

#### 4.4.3 技术栈

- MyBatis-Plus 3.5.9（评论表）
- sensitive-word（计划，**父 POM 未声明**，落地时补充）
- Jsoup 1.17.2（XSS 过滤）
- Dubbo 3.3（暴露 `CommentService`、调用 article 校验文章存在）
- Sa-Token 1.44.0（`@SaCheckLogin`）
- RabbitMQ（可选：评论创建事件 → message 服务站内通知）

#### 4.4.4 关键接口（RESTful 草案）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/articles/{articleId}/comments` | 评论列表（按 parent_id 分组） |
| POST | `/articles/{articleId}/comments` | 发表评论/回复 |
| DELETE | `/comments/{id}` | 删除（本人或管理员） |
| POST | `/comments/{id}/like` | 评论点赞/取消（幂等） |
| GET | `/comments/pending` | 待审核评论（管理员） |
| POST | `/comments/{id}/audit` | 审核（APPROVED/REJECTED） |

#### 4.4.5 数据模型

```
t_comment（parent_id 一级评论、reply_to_id 被回复评论、status PENDING/APPROVED/REJECTED/DELETED）
```

---

### 4.4 spring-cloud-comment（博客评论服务）★新增

#### 4.4.1 服务定位

博客内容域评论服务：评论发表、二级嵌套回复、评论审核、敏感词过滤。

#### 4.4.2 核心功能

| 模块 | 功能 |
|------|------|
| 评论模块 | 发表评论、二级嵌套回复、被回复者记录（@ 通知） |
| 审核模块 | 状态机 PENDING→APPROVED/REJECTED/DELETED，管理员审核 |
| 敏感词模块 | 发表时敏感词过滤（sensitive-word），过滤后存储 |
| 安全模块 | XSS 过滤（Jsoup）、IP/UA 记录（反垃圾） |
| 互动模块 | 评论点赞（幂等） |

#### 4.4.3 技术栈

- MyBatis-Plus 3.5.9（评论表）
- sensitive-word（计划，**父 POM 未声明**，落地时补充）
- Jsoup 1.17.2（XSS 过滤）
- Dubbo 3.3（暴露 `CommentService`、调用 article 校验文章存在）
- Sa-Token 1.44.0（`@SaCheckLogin`）
- RabbitMQ（可选：评论创建事件 → message 服务站内通知）

#### 4.4.4 关键接口（RESTful 草案）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/articles/{articleId}/comments` | 评论列表（按 parent_id 分组） |
| POST | `/articles/{articleId}/comments` | 发表评论/回复 |
| DELETE | `/comments/{id}` | 删除（本人或管理员） |
| POST | `/comments/{id}/like` | 评论点赞/取消（幂等） |
| GET | `/comments/pending` | 待审核评论（管理员） |
| POST | `/comments/{id}/audit` | 审核（APPROVED/REJECTED） |

#### 4.4.5 数据模型

```
t_comment（parent_id 一级评论、reply_to_id 被回复评论、status PENDING/APPROVED/REJECTED/DELETED）
```

---

### 4.5 spring-cloud-message（消息中心服务）

#### 4.5.1 服务定位

统一的消息出口：MQ 消费 → WebSocket 推送 → 站内信/邮件/短信。

#### 4.5.2 核心功能

| 模块 | 功能 |
|------|------|
| MQ 消费模块 | 监听 `task.todo`、`alert.trigger`、`user.kickout` 等事件 |
| WebSocket 模块 | Netty Server（端口 9091），`userId → Channel` 路由 |
| 站内信模块 | 通知 CRUD、已读未读、批量删除 |
| 邮件模块 | Thymeleaf 模板 + spring-boot-starter-mail |
| 短信模块 | 阿里云/腾讯云 SDK，限流 |
| 客服模块 | 访客排队、客服分配、消息路由 |

#### 4.5.3 技术栈

- Spring Boot AMQP（RabbitMQ 消费）
- Netty 4.1（WebSocket Server，端口 9091）
- spring-boot-starter-mail
- Aliyun/Tencent SMS SDK
- Thymeleaf（邮件模板）
- Redisson（客服分配分布式锁）

#### 4.5.4 关键接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/messages` | 站内信分页 |
| PATCH | `/messages/{id}/read` | 标记已读 |
| POST | `/messages/read-all` | 全部已读 |
| DELETE | `/messages/{id}` | 删除 |
| GET | `/messages/unread-count` | 未读数 |
| WS | `/ws/messages/{userId}` | WebSocket 订阅消息推送 |
| POST | `/messages/customer-service/connect` | 访客接入客服 |
| POST | `/messages/customer-service/send` | 发送客服消息 |

---

### 4.6 spring-cloud-search（全文检索服务）

#### 4.6.1 服务定位

基于 ElasticSearch 提供文章、知识库、日志等多维度搜索。

#### 4.6.2 核心功能

| 模块 | 功能 |
|------|------|
| 索引模块 | 创建、删除、更新 Mapping |
| 同步模块 | 监听 MySQL 变更 → MQ → ES 写入；定时全量重建 |
| 搜索模块 | 多字段搜索、高亮、分页 |
| 聚合模块 | 按分类、标签聚合统计 |

#### 4.6.3 技术栈

- Spring Boot Data Elasticsearch（或 ES Java Client 8.x）
- ElasticSearch 8.15 + ik_max_word 分词器
- RabbitMQ（增量同步事件）

#### 4.6.4 关键接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/search/indices` | 创建索引 |
| POST | `/search/indices/sync` | 全量重建 |
| GET | `/search/articles` | 搜索文章 |
| GET | `/search/knowledge` | 搜索知识库 |
| GET | `/search/logs` | 搜索操作日志 |
| GET | `/search/suggest` | 搜索建议（自动补全） |

---

### 4.7 spring-cloud-file（文件服务）

#### 4.7.1 服务定位

基于 MinIO 的对象存储服务，支持大文件分片上传、断点续传、预签名 URL。

#### 4.7.2 核心功能

| 模块 | 功能 |
|------|------|
| 上传模块 | 单文件、批量、分片（init/upload/complete 三步） |
| 下载模块 | 普通下载、断点续传、临时链接 |
| 预览模块 | 图片/PDF 直预览，Office 走 KKFileView |
| 管理模块 | 文件 CRUD、回收站、配额 |
| 预签名模块 | 前端直传 MinIO 的签名接口 |
| 秒传模块 | 上传前查 MD5，已存在复用 |

#### 4.7.3 技术栈

- MinIO Java SDK
- Spring Boot Web
- Redisson（分片上传状态管理）
- KKFileView（独立容器，文件预览代理）

#### 4.7.4 关键接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/files/upload/init` | 初始化分片上传，返回 uploadId |
| POST | `/files/upload/chunks` | 上传一片 |
| POST | `/files/upload/complete` | 完成上传，合并文件 |
| POST | `/files` | 单文件上传 |
| GET | `/files/{id}` | 文件元数据 |
| GET | `/files/{id}/download` | 下载 |
| GET | `/files/{id}/preview` | 预览（返回 KKFileView URL） |
| GET | `/files/presign` | 预签名上传 URL |
| POST | `/files/check-md5` | 秒传检查 |
| DELETE | `/files/{id}` | 删除（进回收站） |
| POST | `/files/{id}/restore` | 从回收站恢复 |

---

### 4.8 spring-cloud-log（日志服务）

#### 4.8.1 服务定位

统一记录操作日志、登录日志、审计日志，存 MongoDB（按月分表）。

#### 4.8.2 核心功能

| 模块 | 功能 |
|------|------|
| 操作日志模块 | `@OperationLog` 注解 + AOP，自动记录接口调用 |
| 登录日志模块 | 监听 auth 服务的 `user.login` 事件 |
| 审计日志模块 | 监听敏感操作（delete、update permission） |
| 查询模块 | 按用户、时间、类型查询 |

#### 4.8.3 技术栈

- Spring AOP（切面）
- MongoDB（存储）
- RabbitMQ（异步写入）
- ShardingSphere 5.5.2（按月分表，可选）

#### 4.8.4 关键接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/logs/operations` | 操作日志分页 |
| GET | `/logs/logins` | 登录日志分页 |
| GET | `/logs/audits` | 审计日志分页 |
| POST | `/logs/operations` | 内部接口：接收日志事件 |

---

### 4.9 spring-cloud-portal（公开门户服务）

#### 4.9.1 服务定位

对外公开的内容门户（博客、新闻、产品），SEO/GEO 友好。

#### 4.9.2 核心功能

| 模块 | 功能 |
|------|------|
| 博客模块 | 文章 CRUD、标签、分类、置顶、阅读量 |
| 新闻模块 | 新闻发布、栏目管理 |
| 产品模块 | 产品 CRUD、规格 |
| SEO 模块 | Sitemap、Meta 自动生成、robots.txt |
| GEO 模块 | 地理位置内容（基于用户 IP 定位） |

#### 4.9.3 技术栈

- Spring Boot
- MyBatis-Plus（文章、新闻表）
- ElasticSearch（全文搜索调 aurora-search）
- Redis（文章缓存、阅读量计数）

#### 4.9.4 关键接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/portal/articles` | 文章分页 |
| GET | `/portal/articles/{id}` | 文章详情 |
| GET | `/portal/articles/hot` | 热门文章 |
| GET | `/portal/news` | 新闻分页 |
| GET | `/portal/products` | 产品分页 |
| GET | `/portal/sitemap.xml` | Sitemap |
| GET | `/portal/robots.txt` | Robots |

> portal 服务**不需要登录**，但写操作（POST/PUT/DELETE）必须 @SaCheckRole(ADMIN)。

---

### 4.10 spring-cloud-job（定时任务服务）

#### 4.10.1 服务定位

XXL-JOB 执行器，所有定时任务集中管理。

#### 4.10.2 核心功能

| 任务 | 触发时机 |
|------|----------|
| `metricsAggregateJob` | 每分钟 |
| `aiMessageArchiveJob` | 每天凌晨 |
| `articleSyncToEsJob` | 每天 2 点 |
| `workflowRemindJob` | 每 10 分钟 |
| `logCleanupJob` | 每天凌晨清理 30 天前 |
| `createNextMonthTablesJob` | 每月 25 号 |

#### 4.10.3 技术栈

- XXL-JOB Core 3.5.0（**父 POM 未声明**，落地时补充）
- Spring Boot
- Dubbo（调用其他服务执行任务）

#### 4.10.4 关键接口

无对外 HTTP 接口，任务由 XXL-JOB Admin 调度触发。

---


## 5. 服务间通信规范

### 5.1 同步调用（Dubbo）

| 场景 | 调用方 | 被调方 | 方法 |
|------|--------|--------|------|
| 文件元数据 | portal | file | `FileRpcService.getMeta` |
| 用户角色 | monitor | system | `UserRpcService.getRoles` |
| 文章索引同步 | article | search | `SearchRpcService.syncArticleIndex` |
| 文章存在校验 | comment | article | `ArticleRpcService.existsById` |

> Dubbo 接口定义在 `spring-cloud-common-core` 的 `rpc` 包（计划，未实现），由被调方实现 `*RpcProvider`。

### 5.2 异步事件（RabbitMQ）

| 事件 | Exchange | 生产者 | 消费者 |
|------|----------|--------|--------|
| 用户注册 | `user.register` | auth | message, log |
| 用户更新 | `user.update` | system | message, log, search |
| 告警 | `alert.trigger` | monitor | message |
| 评论通知 | `comment.created` | comment | message |
| 操作日志 | `log.operation` | 所有 | log |
| 文件上传 | `file.uploaded` | file | log |
| 文章发布 | `article.published` | article | search |

> 所有事件**必须**继承 `BaseEvent`，**必须**用 `AbstractEventListener<T>` 消费（自动幂等）。

### 5.3 实时推送（WebSocket）

| 端点 | 服务 | 用途 |
|------|------|------|
| `/ws/monitor/{userId}` | monitor | 实时指标推送 |
| `/ws/message/{userId}` | message | 站内信/任务提醒 |

---

## 6. RESTful API 强制规范（所有服务必须遵守）

### 6.1 URI 设计

- 资源名**必须**用复数名词、全小写、短横线分隔：`/system/users`、`/workflow/instances`
- 业务动作（非 CRUD）使用动词子资源：`POST /system/users/{id}/disable`、`POST /workflow/tasks/{id}/approve`
- **禁止**把动词放在路径里：`/getUser`、`/createOrder`、`/deleteUserById`

### 6.2 HTTP 方法语义

| 方法 | 语义 | 是否幂等 | 示例 |
|------|------|----------|------|
| GET | 查询 | ✅ | `GET /system/users/{id}` |
| POST | 新增 | ❌ | `POST /system/users` |
| PUT | 全量更新 | ✅ | `PUT /system/users/{id}` |
| PATCH | 部分更新 | ✅ | `PATCH /system/users/{id}/password` |
| DELETE | 删除 | ✅ | `DELETE /system/users/{id}` |

> **禁止**用 GET 执行写操作。**禁止**用 POST 同时承担新增和更新。

### 6.3 统一响应格式

```json
{
  "code": 200,
  "msg": "success",
  "data": { ... },
  "timestamp": 1722470400000
}
```

| code | 含义 |
|------|------|
| 200 | 成功 |
| 400 | 参数错误 |
| 401 | 未登录 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 409 | 资源冲突 |
| 429 | 限流 |
| 500 | 服务器内部错误 |

### 6.4 分页规范

入参：
```
GET /system/users?pageNum=1&pageSize=10&orderBy=createTime DESC&keyword=admin
```

出参（`PageVO<T>`）：
```json
{
  "code": 200,
  "data": {
    "list": [...],
    "total": 100,
    "pageNum": 1,
    "pageSize": 10,
    "pages": 10
  }
}
```

### 6.5 状态码使用

- **禁止**用 200 返回业务错误（如"用户名已存在"），必须返回对应 HTTP 状态码（409 Conflict）
- 异常**必须**由 `GlobalExceptionHandler` 统一捕获
- 业务异常类**必须**继承 `BusinessException`

### 6.6 接口文档

- 所有 Controller **必须**使用 `@Tag`、`@Operation` 注解
- 所有 DTO/VO 字段**必须**使用 `@Schema` 注解描述
- 文档地址：`http://localhost:8080/doc.html`（Gateway 聚合）

---

## 7. 数据库规范

### 7.1 命名规范

| 类型 | 前缀 | 示例 |
|------|------|------|
| 系统表 | `sys_` | `sys_user`、`sys_role` |
| 业务表 | `biz_` | `biz_leave`、`biz_expense` |
| 工作流表 | `flow_` | `flow_definition` |
| AI 表 | `ai_` | `ai_knowledge_base` |
| 分表后缀 | `_YYYYMM` | `biz_operation_log_202501` |

字段命名：
- 主键：`id`（雪花 ID）
- 外键：`{表名}_id`，如 `user_id`
- 时间字段：`create_time`、`update_time`、`delete_time`
- 操作人：`create_by`、`update_by`
- 软删除：`del_flag`（0=未删，1=已删）
- 状态：`status`（用 `TINYINT`）
- 排序：`sort`（INT）

### 7.2 字段类型规范

| Java 类型 | 数据库类型 | 说明 |
|-----------|-----------|------|
| Long | `BIGINT` | 主键、外键 |
| String | `VARCHAR(n)` | 短文本 |
| String | `TEXT` / `CLOB` | 长文本 |
| LocalDateTime | `DATETIME` | MySQL |
| LocalDateTime | `TIMESTAMP` | PG/KingbaseES/DM（标准 SQL） |
| Integer | `INT` | 数值 |
| Boolean | `TINYINT` / `SMALLINT` | 0/1 |
| BigDecimal | `DECIMAL(p, s)` | 金额（**禁止**用 DOUBLE/FLOAT） |

### 7.3 SQL 兼容性（多数据库）

**必须**用标准 SQL，避免 MySQL 私有函数：

| 场景 | ❌ MySQL 私有 | ✅ 通用写法 |
|------|---------------|-------------|
| 判空 | `IFNULL(a, b)` | `COALESCE(a, b)` |
| 字符串拼接 | `CONCAT(a, b)` | `a || b` |
| 日期格式化 | `DATE_FORMAT(t, '%Y-%m-%d')` | `TO_CHAR(t, 'YYYY-MM-DD')` |
| 当前时间 | `NOW()` | `CURRENT_TIMESTAMP` |
| 自增主键 | `AUTO_INCREMENT` | 用雪花 ID |
| 布尔 | `TINYINT(1)` | `SMALLINT` + 0/1 |

### 7.4 索引规范

- 主键**必须**有索引（默认）
- 外键**必须**建索引
- 唯一约束字段**必须**建唯一索引
- 组合索引字段顺序：**高选择性在前**，**等值在前，范围在后**
- 单表索引数**建议** ≤ 5
- **禁止**在大字段（TEXT/BLOB）上建索引

### 7.5 分库分表

需要分表的表：

| 表 | 分表策略 | 理由 |
|----|----------|------|
| `biz_operation_log` | 按月分表 `biz_operation_log_YYYYMM` | 写多查少 |
| `biz_login_log` | 按月分表 | 同上 |
| `biz_ai_message` | 按月分表 | AI 对话量大 |

集成方式：Apache ShardingSphere 5.5.2 JDBC 模式。

---

## 8. 必须遵守的开发规范

### 8.1 编码规范（阿里巴巴 Java 开发规范 + 项目强制）

1. **必须**遵循阿里巴巴 Java 开发规范（泰山版）
2. **强制规则由 `spring-cloud-alibaba/src/checkstyle.xml` 落实**，绑定到 Maven `validate` 阶段
3. 缩进 4 空格，行宽 ≤ 120 字符
4. **禁止**用 `@Autowired` 字段注入，必须用 `@RequiredArgsConstructor` 构造器注入
5. **禁止**用 `System.out.println` / `e.printStackTrace()`，必须用 `@Slf4j`
6. **禁止**在 Controller 写业务逻辑
7. **禁止**在 Service 直接操作 `HttpServletRequest`
8. **禁止**在 Service 直接拼接 SQL
9. **必须**用 `@Transactional` 标注事务方法（Service 层）
10. 跨库事务**必须**用 `@DSTransactional` 或 `@GlobalTransactional`
11. **禁止**用 `new Thread(...)` / `Executors.newCachedThreadPool()`
12. **禁止**用 `synchronized` 跨 JVM 同步
13. **if/else/for/while 必须加大括号**，即使只有一句

### 8.2 异常处理规范

1. 业务异常**必须**继承 `BusinessException`
2. **禁止**用 `try-catch` 吞掉异常
3. **禁止**用 `throw new RuntimeException("xxx")`
4. **禁止** catch `Exception`/`Throwable`（必须精确捕获）
5. 边界校验**必须**用 `@Validated` + Hibernate Validator

### 8.3 缓存规范

1. 缓存 Key**必须**以 `spring-cloud:{service}:{biz}:{id}` 格式
2. 缓存 TTL**必须**加 ±10% 随机数
3. 热点数据**必须**用 `@LayeredCache` 多级缓存
4. 缓存穿透用空值缓存或布隆过滤器

### 8.4 安全规范

1. **禁止**在日志中打印密码、Token、身份证号
2. SQL **必须**参数化查询
3. 用户输入**必须**经过 XSS 过滤
4. 接口**必须**加 `@SaCheckPermission` 或 `@SaCheckRole`
5. 敏感字段**必须**加密
6. 密码哈希**必须**用 Argon2id

### 8.5 测试规范

- **必须**写单元测试：`ServiceTest` 覆盖率 ≥ 70%
- **必须**写 Controller 集成测试：用 `MockMvc` + `@WebMvcTest`
- 公共测试基类在 `spring-cloud-common-test`（**已废弃**，落地时重新规划）

---

## 9. 配置文件规范

### 9.1 bootstrap.yml 模板

```yaml
spring:
  application:
    name: spring-cloud-{服务名}
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_ADDR:127.0.0.1:8848}
        namespace: ${NACOS_NAMESPACE:public}
      config:
        server-addr: ${NACOS_ADDR:127.0.0.1:8848}
        file-extension: yaml
        shared-configs:
          - data-id: spring-cloud-shared.yaml
            refresh: true
```

### 9.2 application.yml 模板

```yaml
server:
  port: 808X

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  metrics:
    tags:
      application: ${spring.application.name}
```

### 9.3 业务配置（Nacos）

业务配置（数据库连接、Redis、限流规则）**必须**在 Nacos，**禁止**写死在 application.yml。

---

## 10. 红线（违反即拒绝）

1. ❌ 在子服务 POM 中覆盖父 POM 的依赖版本
2. ❌ Controller 直接操作 DB（必须经 Service → Mapper）
3. ❌ 非 RESTful API（如 `POST /api/getUser?id=1`）
4. ❌ 用 GET 执行写操作
5. ❌ 在日志/响应中泄露密码、Token、身份证号
6. ❌ 用 `System.out.println` / `e.printStackTrace()`
7. ❌ 用 `throw new RuntimeException(...)` 而非 BusinessException
8. ❌ 用 `catch (Exception e)` 而非具体异常类型
9. ❌ 在 Service 直接 `new Thread(...)` / `Executors.newXxx()`
10. ❌ SQL 字符串拼接（SQL 注入风险）
11. ❌ 用 MySQL 私有函数（必须用标准 SQL 适配多库）
12. ❌ 用 `@Autowired` 字段注入（必须用构造器注入）
13. ❌ `if`/`else`/`for`/`while` 不加大括号
14. ❌ 业务配置硬编码（必须放 Nacos）
15. ❌ 接口未加 `@SaCheckPermission`/`@SaCheckRole`
16. ❌ Controller 不加 `@Tag` / `@Operation` 注解

---

## 11. 子服务 CLAUDE.md 索引

每个子服务目录下都有独立的 `CLAUDE.md`，包含定位、核心功能、技术栈、关键接口与红线：

| # | 服务 | 文档 |
|---|------|------|
| 1 | spring-cloud-system | [CLAUDE.md](./spring-cloud-system/CLAUDE.md) |
| 2 | spring-cloud-monitor | [CLAUDE.md](./spring-cloud-monitor/CLAUDE.md) |
| 3 | spring-cloud-message | [CLAUDE.md](./spring-cloud-message/CLAUDE.md) |
| 4 | spring-cloud-search | [CLAUDE.md](./spring-cloud-search/CLAUDE.md) |
| 5 | spring-cloud-file | [CLAUDE.md](./spring-cloud-file/CLAUDE.md) |
| 6 | spring-cloud-log | [CLAUDE.md](./spring-cloud-log/CLAUDE.md) |
| 7 | spring-cloud-portal | [CLAUDE.md](./spring-cloud-portal/CLAUDE.md) |
| 8 | spring-cloud-job | [CLAUDE.md](./spring-cloud-job/CLAUDE.md) |
| 9 | spring-cloud-article ★ | [CLAUDE.md](./spring-cloud-article/CLAUDE.md) |
| 10 | spring-cloud-comment ★ | [CLAUDE.md](./spring-cloud-comment/CLAUDE.md) |
