# 设计：依据博客需求文档重新规划模块与文档体系（方案 B：博客域拆分）

## Context

动机见 proposal.md。当前状态（方案设计的输入约束）：

- 后端现有 13 个服务：gateway(8080)、auth(8081)、system(8082)、monitor(8083)、workflow(8084)、ai(8085)、message(8086)、search(8087)、file(8088)、log(8089)、portal(8090)、job(8091)、report(8092)，公共层 16 子模块、2 个 Starter
- portal 已含博客模块雏形（文章 CRUD/分类/标签/置顶/阅读量），但缺评论、互动、RSS；search 缺建议/高亮/RSS
- 需求文档（博客 v2.0）核心能力：文章(Markdown)、评论(嵌套/审核/敏感词)、互动(点赞/收藏)、搜索(建议/高亮)、RSS、RBAC 三角色
- 需求文档明确：审核流程用单步状态机（2.6 节不用工作流引擎）、ES 同步用 Dubbo+本地消息表+定时补偿（2.5 节方案 A）、无 AI 功能
- 文档缺口：11 个业务服务、前端 apps/packages、docker/、docs/ 均无 CLAUDE.md
- 技术栈与需求文档同源（Spring Cloud Alibaba + Sa-Token + Dubbo + Vue3），版本以父 POM 为准

## Goals / Non-Goals

**Goals:**
- 以需求文档为参照，落地方案 B：新增 article/comment 2 个服务，保留 8 个相关服务，删除与博客无关的 workflow/ai/report
- 建立"每模块一份 CLAUDE.md"的完整文档体系，父模块含子模块职责与索引
- 每份文档记录：模块定位、核心功能、技术栈（与父 POM 版本一致）、关键接口、规范红线

**Non-Goals:**
- 不实现 article/comment 业务代码（本变更交付规划 + 文档；代码由后续变更承接）
- 不迁移 portal 现有博客代码到新服务
- 不修改技术栈版本矩阵、跨端契约、全局红线
- 不删除与博客相关或经用户确认保留的服务（monitor/message/log/job/file/search/auth/gateway/portal/system）

## Decisions

### D1：新增服务边界 — article（文章+互动）与 comment（评论）拆分

```
┌────────────────────── 博客化服务全景（12 服务）───────────────────┐
│                                                                  │
│  gateway(8080) ── auth(8081) ── system(8082)                     │
│      │                │              │ RBAC 核心：用户/角色/权限  │
│      │                └── Dubbo ────┘   (裁剪部门/岗位/字典等)    │
│      ▼                                                            │
│  ★ article(8093)  ★ comment(8094)    portal(8090)                │
│  文章/分类/标签    评论/嵌套回复/      门户展示聚合(读为主)         │
│  点赞/收藏/阅读量   审核/敏感词过滤     +SEO/GEO                   │
│  Markdown→HTML     Dubbo:CommentSvc   （博客写操作已移出）         │
│  Dubbo:ArticleSvc                                                │
│      │ Dubbo 索引同步         ▲ 文章校验 Dubbo                    │
│      ▼                        │                                  │
│  search(8087) ←──────┬────────┘                                  │
│  ES检索/建议/高亮/RSS  │                                           │
│      ▲                ▼                                          │
│  file(8088)  monitor(8083)  message(8086)  log(8089)             │
│              服务器监控      站内信/评论通知    操作/登录日志       │
│  job(8091) 定时任务：ES 补偿同步 / 统计聚合 / RSS 定时生成          │
│                                                                  │
│  ✂ 已删除：workflow(8084)  ai(8085)  report(8092)                │
│    （工作流/AI/低代码报表与博客无关，随其依赖 common-ai 删除）     │
└──────────────────────────────────────────────────────────────────┘
```

- **方案选择**：A（并入 portal）→ portal 承担"文章+评论+互动+RSS"过重，且与现有企业级功能耦合，违背博客文档的微服务边界；C（领域重构 9 服务）→ 需要拆 system/合并企业级服务，迁移风险大。B 增量最小且边界清晰。
- **互动归属**：点赞/收藏与文章同生命周期（幂等、计数聚合），入 article；评论点赞复用 comment 的表结构（与需求文档 `t_like_record` 的 `target_type` 设计一致，归属 target 所属服务）。

### D2：RSS 归属 search

需求文档将 `/api/rss` 置于 search 服务（8085）。RSS 本质是"最新已发布文章"的只读查询，search 已有 ES 索引与缓存能力，归 search 避免 article 提供额外 HTTP 接口。备选：归 article（更贴近数据源），但需新增 HTTP 出口；**选择归 search**，与需求文档对齐。

### D3：portal 轻量化边界

portal 保留：文章/新闻/产品列表与详情（读）、SEO（sitemap/robots/meta）、GEO（IP 定位）；移除：博客写操作（发布/审核/点赞）职责（代码迁移为后续变更）。写操作由 article/comment 承接。备选：完全删除 portal → 博客前台无聚合层，门户数据源散落；**保留**。

### D4：新增服务端口与聚合

| 服务 | HTTP | Dubbo | XXL-JOB | 顶级包 |
|------|------|-------|---------|--------|
| spring-cloud-article | 8093 | 20893 | 10011 | com.xytang.article |
| spring-cloud-comment | 8094 | 20894 | 10012 | com.xytang.comment |

端口延续现有序列（8081→8092 中保留服务占用 8080-8083/8086-8091，workflow 8084/ai 8085/report 8092 释放，新服务顺延 8093/8094）。两者纳入 `spring-cloud-services/pom.xml` `<modules>` 与父 POM 端口总表；被删除服务的端口、Dubbo 端口、XXL-JOB 端口从总表移除。

### D5：文档体系 — 三层导航树

```
sca-fullstack-lab/CLAUDE.md                     （根：跨端契约/端口总表/子项目索引）
├── spring-cloud-alibaba/CLAUDE.md              （后端聚合：模块结构/版本矩阵/红线）
│   ├── spring-cloud-common/CLAUDE.md           （聚合：15 子模块索引，common-ai 已删）
│   │   └── spring-cloud-common-{x}/CLAUDE.md   （15 叶子）
│   ├── spring-cloud-gateway/CLAUDE.md
│   ├── spring-cloud-auth/CLAUDE.md
│   ├── spring-cloud-services/CLAUDE.md         （聚合：10 服务清单+索引）
│   │   └── spring-cloud-{system|monitor|message|search|file|log|
│   │       portal|job|★article|★comment}/CLAUDE.md
│   └── spring-cloud-starters/CLAUDE.md         （聚合：1 Starter 索引）
│       └── spring-cloud-starter-monitor-agent/CLAUDE.md
└── vue-web-ui/CLAUDE.md                        （前端聚合：2 应用+5 包索引）
    ├── apps/{admin|portal}/CLAUDE.md
    └── packages/{ui|api|utils|types|uno-preset}/CLAUDE.md
```

- 每份文档模板（叶子）：定位 → 核心功能 → 技术栈 → 关键接口/任务 → 规范红线
- 聚合文档额外：子模块清单（名称/端口/作用）+ 相对路径索引链接
- 每份文档开头声明读取顺序：根 CLAUDE.md → 本聚合层 → 本模块

### D6：各服务功能与技术栈映射（写入对应 CLAUDE.md）

| 服务 | 核心功能 | 技术栈（父 POM 已声明/计划） |
|------|---------|------------------------------|
| ★ article | 文章 CRUD、Markdown→HTML、XSS 过滤、草稿/发布/审核状态、分类/标签、slug、点赞/收藏（Redis Set 去重+计数）、阅读量、封面图（经 file）、ES 索引同步（经 search Dubbo） | MyBatis-Plus 3.5.9、commonmark-java（计划）、Jsoup 1.17.2、Redis/Redisson 4.0.0、Dubbo 3.3、Sa-Token 1.44.0 |
| ★ comment | 评论/二级嵌套回复、审核状态机（PENDING→APPROVED/REJECTED）、敏感词过滤、IP/UA 记录、评论点赞 | MyBatis-Plus、sensitive-word（计划）、Jsoup、Dubbo、Sa-Token |
| search 修改 | +搜索建议（前缀补全）、+结果高亮、+RSS 2.0（Redis 缓存） | Spring Data ES（计划 ik 分词）、Redis、Dubbo |
| portal 修改 | 轻量化为门户展示聚合：文章/新闻/产品读接口、SEO（sitemap/robots）、GEO | MyBatis-Plus、Redis（阅读量/缓存）、ElasticSearch（调 search） |
| system 裁剪 | 收敛为 RBAC 核心：用户/角色/菜单/权限（博客 USER/AUTHOR/ADMIN 复用）；部门/岗位/字典/参数/通知移出职责边界 | 现状不变（MyBatis-Plus/dynamic-datasource/Dubbo） |
| auth | 注册/登录/登出/刷新、OAuth2（sa-token-oauth2 计划） | Sa-Token 1.44.0、现状不变 |
| monitor 保留 | 服务器监控（OSHI 采集/时序存储/WebSocket 推送/告警），管理员运维大盘 | 现状不变（OSHI/TDengine/Netty） |
| message 保留 | 站内信/评论通知/待办提醒、WebSocket 推送、客服 | 现状不变（AMQP/Netty/mail/短信） |
| log 保留 | 操作/登录/审计日志（MongoDB 按月分表） | 现状不变（AOP/MongoDB/RabbitMQ） |
| job | 定时任务：ES 补偿同步/统计聚合/RSS 定时生成（保留，职责微调） | XXL-JOB 计划、Dubbo |
| file | 文件上传/分片/预签名（封面图等） | 现状不变（MinIO SDK） |
| ✂ workflow | 删除 — 需求文档 2.6 明确审核用状态机不用工作流引擎 | — |
| ✂ ai | 删除 — 需求文档无 AI 功能 | — |
| ✂ report | 删除 — 统计面板用 admin 前端 ECharts 即可 | — |

### D7：前端结构

- apps/portal：博客前台（文章列表/详情/Markdown 渲染/评论/搜索/点赞收藏交互），对接 article/comment/search/portal 服务
- apps/admin：管理后台（博客审核、用户/角色管理、数据统计），对接 system/article/comment
- 不新增前端应用；packages 5 个共享包职责不变

## Risks / Trade-offs

- [删除 workflow/ai/report 后存在残留引用（如 message 消费 workflow 的 `task.todo` 事件、job 的 workflow 提醒任务）] → 本变更删除服务目录与聚合引用；MQ 队列清理、job 任务调整列为后续收尾，实施时全局 grep 残留引用（`git grep -l "workflow\|spring-cloud-ai\|report"`）
- [portal 现有博客代码与新服务职责重叠，文档边界与代码事实短暂不一致] → 本变更明确职责边界并写入文档；代码迁移列为后续独立变更，迁移完成后删除 portal 博客代码
- [新增服务无实现，"文档先行"可能导致文档内容超前] → CLAUDE.md 技术栈标注"计划引入/落地时补充"（沿用现状做法），与 services/CLAUDE.md 中 Warm-Flow 等条目一致
- [文档漂移：未来结构变更后 CLAUDE.md 过期] → module-docs spec 强制同步规则；可在实施时提供目录覆盖率检查命令（列入 tasks）
- [flow-web 应用目录实际不存在，但根 CLAUDE.md 声明 3 个应用] → 按"计划中"处理：文档保留声明但标注未创建，不为其编写 CLAUDE.md，避免对不存在目录建文档（见 Open Questions）

## Migration Plan

1. 删除：`spring-cloud-services` 下 workflow/ai/report 3 个服务目录，并从聚合 POM `<modules>` 移除；释放端口 8084/8085/8092、Dubbo 20884/20885/20892、XXL-JOB 10002/10003/10010
2. 删除：`spring-cloud-common-ai` 子模块、`spring-cloud-starter-sso-client`（待确认），并从各自聚合 POM 移除
3. 更新仓库根 CLAUDE.md：端口总表 +2 -3、服务数量 13→10、子项目索引同步
4. 更新 spring-cloud-alibaba/CLAUDE.md 与 spring-cloud-services/CLAUDE.md：10 服务清单、删除服务标注、system 裁剪边界
5. 新增 spring-cloud-article/CLAUDE.md、spring-cloud-comment/CLAUDE.md（服务骨架 pom 由后续变更创建）
6. 补齐 8 个既有业务服务 CLAUDE.md（按 D5 模板）
7. 补齐前端 apps/packages CLAUDE.md，更新 vue-web-ui/CLAUDE.md
8. 补齐 docker/、docs/ CLAUDE.md
9. 校验：目录覆盖率 + 索引链接有效性 + 版本一致性 + 残留引用检查
10. 回滚策略：删除为 `git rm` + POM 修改，git 回滚即可；无数据/代码风险

## Open Questions

- flow-web 应用是否保留规划？（当前目录不存在；若保留则后续需创建应用骨架与 CLAUDE.md，若不保留则根文档删除其声明）— 不影响本次文档主体工作，可在实施时确认
- `spring-cloud-starter-sso-client` 删除为建议项（博客无多系统 SSO 需求），用户未单独确认；实施时默认执行，如有异议可保留
