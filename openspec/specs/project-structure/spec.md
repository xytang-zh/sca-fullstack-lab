## Purpose

定义 sca-fullstack-lab 在参照个人博客需求文档重构后的后端服务/前端应用/公共模块拆分结构，明确新增、修改、删除清单与各服务的职责边界，作为后续所有开发与文档工作的结构基线。

## Requirements

### Requirement: 后端服务拆分结构

后端聚合工程 SHALL 删除与博客无关的 3 个服务（`spring-cloud-workflow`、`spring-cloud-ai`、`spring-cloud-report`），保留 8 个服务（auth/system/monitor/message/search/file/log/portal/job/gateway），并新增 2 个博客域服务（`spring-cloud-article`、`spring-cloud-comment`），最终业务服务共 10 个（不含网关与认证），SHALL 纳入 `spring-cloud-services` 聚合 POM 与端口分配总表，被删除服务的端口 SHALL 从总表中移除。

#### Scenario: 服务清单与端口

- **WHEN** 查看 `spring-cloud-services/pom.xml` 的 `<modules>` 与端口分配表
- **THEN** 存在 `spring-cloud-article`（HTTP 8093、Dubbo 20893、XXL-JOB 10011）与 `spring-cloud-comment`（HTTP 8094、Dubbo 20894、XXL-JOB 10012）；不存在 workflow(8084)/ai(8085)/report(8092)，对应端口从总表移除；其余服务端口不变

#### Scenario: 公共模块与 Starter 联动删除

- **WHEN** 检查 `spring-cloud-common` 聚合与 `spring-cloud-starters` 聚合的 `<modules>`
- **THEN** 不存在 `spring-cloud-common-ai` 与 `spring-cloud-starter-sso-client`；`spring-cloud-common-mq`/`spring-cloud-common-mongo`/`spring-cloud-common-netty`/`spring-cloud-common-log`/`spring-cloud-starter-monitor-agent` 保留（服务保留需要）

#### Scenario: 服务目录结构合规

- **WHEN** 检查任意业务服务目录（含新增服务）
- **THEN** 其存在 `pom.xml`、`src/main/java/com/xytang/{服务名}/`（含启动类）、`src/main/resources/application.yml` 与 `application-dev.yml`，且 POM 声明父 POM 为 `com.xytang:spring-cloud-alibaba:1.0-SNAPSHOT`

### Requirement: 服务职责边界

每个服务 SHALL 拥有唯一的职责边界，博客域能力按以下分配，不得跨域实现：

- `spring-cloud-article`：文章 CRUD、分类/标签、Markdown 渲染、草稿/发布状态、点赞/收藏、阅读量
- `spring-cloud-comment`：评论/嵌套回复、评论审核、敏感词过滤
- `spring-cloud-portal`：公开门户展示聚合（文章/新闻/产品列表与详情、SEO/GEO），只读为主
- `spring-cloud-search`：全文检索、搜索建议、高亮、RSS 订阅源
- `spring-cloud-system`：裁剪为 RBAC 核心（用户/角色/菜单/权限），含博客 USER/AUTHOR/ADMIN 三角色数据；部门/岗位/字典/参数/通知等企业模块移出职责边界
- `spring-cloud-auth`：注册/登录/登出/Token 刷新/OAuth2（博客用户与后台用户共用）
- `spring-cloud-monitor`：服务器监控（保留，管理员运维大盘）
- `spring-cloud-message`：站内信/评论通知推送（保留）
- `spring-cloud-log`：操作/登录日志审计（保留）

#### Scenario: 博客域能力归属

- **WHEN** 检查文章发布、评论、搜索、RSS 功能所在的实现服务
- **THEN** 文章/互动归属 article、评论归属 comment、搜索/建议/高亮/RSS 归属 search、门户展示归属 portal，无跨域重复实现

#### Scenario: portal 轻量化

- **WHEN** 检查 portal 服务的写操作范围
- **THEN** portal 仅提供文章/新闻/产品等内容的读接口与 SEO/GEO 能力，博客内容的写操作（发布/评论/点赞）由 article/comment 提供

### Requirement: 模块新增/修改/删除清单

本变更 SHALL 提供并执行一份模块变更清单，明确每个模块的状态（新增/修改/删除/保留），且 SHALL 满足以下规则：

- 新增：`spring-cloud-article`、`spring-cloud-comment` 2 个后端服务；如前端新增应用则一并列出
- 修改：`spring-cloud-portal`（轻量化）、`spring-cloud-search`（+建议/高亮/RSS）、`spring-cloud-system`（裁剪为 RBAC 核心）、`spring-cloud-services` 聚合（-3 +2 服务）、后端与仓库根 CLAUDE.md（结构同步）
- 删除：`spring-cloud-workflow`、`spring-cloud-ai`、`spring-cloud-report` 3 个后端服务，`spring-cloud-common-ai` 公共子模块，`spring-cloud-starter-sso-client` 自定义 Starter；已废弃模块（如 `spring-cloud-test`、`spring-cloud-common-test`）不得在聚合中重新引入
- 保留：auth/gateway/system/monitor/message/search/file/log/portal/job 及其公共层 15 子模块、`spring-cloud-starter-monitor-agent`

#### Scenario: 变更清单可验证

- **WHEN** 对照变更清单检查仓库目录与聚合 POM
- **THEN** 新增的 2 个服务存在且已纳入聚合，删除的 3 个服务/1 个子模块/1 个 Starter 不在任何聚合 POM 中，portal/search/system 职责按上述边界调整，无清单外的服务被删除

### Requirement: 前后端结构对应

前端 `vue-web-ui` SHALL 与后端服务结构保持对应关系，门户类功能在 `apps/portal`、管理类功能在 `apps/admin`，共享代码在 `packages/*`；若博客域需要独立前端应用，SHALL 在变更清单中显式声明。

#### Scenario: 前端应用映射

- **WHEN** 查看 `vue-web-ui/apps/` 目录与各应用路由
- **THEN** 每个应用的功能域与后端服务职责一一对应，无功能域悬挂（未映射到任何后端服务）
