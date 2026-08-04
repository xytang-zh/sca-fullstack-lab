## Purpose

定义 sca-fullstack-lab 在参照个人博客需求文档重构后的后端服务/前端应用/公共模块拆分结构，明确新增、修改、删除清单与各服务的职责边界，作为后续所有开发与文档工作的结构基线。

## Requirements

### Requirement: 后端服务拆分结构

后端聚合工程 SHALL 精简为 3 个业务服务（`spring-cloud-system`、`spring-cloud-article`、`spring-cloud-comment`）+ 1 个网关（`spring-cloud-gateway`）+ 1 个认证中心（`spring-cloud-auth`），SHALL 删除全部 7 个空壳服务（`spring-cloud-monitor`、`spring-cloud-message`、`spring-cloud-search`、`spring-cloud-file`、`spring-cloud-log`、`spring-cloud-portal`、`spring-cloud-job`），SHALL 删除整个 `spring-cloud-starters` 聚合；公共层 SHALL 精简为 6 个子模块（`core`/`web`/`mybatis`/`redis`/`satoken`/`dubbo`），其余 10 个子模块（`cache`/`datasource`/`es`/`log`/`mongo`/`mq`/`netty`/`redisson`/`security`/`swagger`）SHALL 删除或并入保留模块；被删除服务的端口 SHALL 从端口分配总表中移除。

#### Scenario: 服务清单与端口

- **WHEN** 查看 `spring-cloud-services/pom.xml` 的 `<modules>` 与端口分配总表
- **THEN** 存在 `spring-cloud-system`（HTTP 8082）、`spring-cloud-article`（HTTP 8093）、`spring-cloud-comment`（HTTP 8094）；不存在 monitor(8083)/message(8086)/search(8087)/file(8088)/log(8089)/portal(8090)/job(8091) 及其对应端口

#### Scenario: 公共模块与 Starter 精简

- **WHEN** 检查 `spring-cloud-common` 聚合与父 POM 的 `<modules>`
- **THEN** `spring-cloud-common` 仅含 `core`/`web`/`mybatis`/`redis`/`satoken`/`dubbo` 6 个子模块；`spring-cloud-starters` 聚合不存在于父 POM `<modules>`；已删除的 common 子模块不在任何聚合 POM 中

#### Scenario: 服务目录结构合规

- **WHEN** 检查任一保留业务服务目录
- **THEN** 其存在 `pom.xml`、`src/main/java/com/xytang/{服务名}/`（含启动类）、`src/main/resources/application.yml` 与 `application-dev.yml`，且 POM 声明父 POM 为 `com.xytang:spring-cloud-alibaba:1.0-SNAPSHOT`

### Requirement: 服务职责边界

每个服务 SHALL 拥有唯一的职责边界，博客域能力按以下分配，不得跨域实现：

- `spring-cloud-article`：文章 CRUD、分类/标签、Markdown 渲染、草稿/发布状态、点赞/收藏、阅读量
- `spring-cloud-comment`：评论/嵌套回复、评论审核、敏感词过滤
- `spring-cloud-system`：RBAC 核心（用户/角色/菜单/权限），含博客 USER/AUTHOR/ADMIN 三角色数据
- `spring-cloud-auth`：注册/登录/登出/Token 刷新/验证码（博客用户与后台用户共用）
- `spring-cloud-gateway`：路由、鉴权、CORS、TraceId 透传

#### Scenario: 博客域能力归属

- **WHEN** 检查文章发布、评论、登录、权限功能所在的实现服务
- **THEN** 文章/互动归属 article、评论归属 comment、登录认证归属 auth、RBAC 归属 system、请求入口归属 gateway，无跨域重复实现

#### Scenario: 已删除服务职责不存在

- **WHEN** 在仓库中检索 monitor/message/search/file/log/portal/job 相关领域代码
- **THEN** 不存在对应的业务服务模块，其职责（监控、站内信、全文检索、文件、日志、门户、定时任务）不属于任何保留服务

### Requirement: 模块新增/修改/删除清单

本变更 SHALL 提供并执行一份模块变更清单，明确每个模块的状态（新增/修改/删除/保留），且 SHALL 满足以下规则：

- 删除：7 个空壳业务服务（`spring-cloud-monitor`/`spring-cloud-message`/`spring-cloud-search`/`spring-cloud-file`/`spring-cloud-log`/`spring-cloud-portal`/`spring-cloud-job`）、整个 `spring-cloud-starters` 聚合、10 个 common 子模块（`cache`/`datasource`/`es`/`log`/`mongo`/`mq`/`netty`/`redisson`/`security`/`swagger`）
- 修改：`spring-cloud-common-web`（纳入 `@OperationLog` 切面与 springdoc/Knife4j 配置）、`spring-cloud-common-mybatis`（纳入 dynamic-datasource 配置）、`spring-cloud-common-redis`（纳入 `@DistributedLock` 切面与 Caffeine 配置）、`spring-cloud-auth`（移除 MQ 登录日志）、gateway/auth/system/article/comment 的 POM 依赖、`spring-cloud-services` 聚合（10 服务 → 3 服务）、`spring-cloud-common` 聚合（16 子模块 → 6 子模块）、父 POM（移除 starters 模块）、仓库根与聚合层 CLAUDE.md
- 保留：`spring-cloud-gateway`、`spring-cloud-auth`、`spring-cloud-system`、`spring-cloud-article`、`spring-cloud-comment`、`spring-cloud-common-core`/`web`/`mybatis`/`redis`/`satoken`/`dubbo`

#### Scenario: 变更清单可验证

- **WHEN** 对照变更清单检查仓库目录与聚合 POM
- **THEN** 保留的 5 个服务与 6 个 common 子模块存在且已纳入聚合；已删除的 7 个服务、1 个 Starter 聚合、10 个 common 子模块不在任何聚合 POM 中，目录与配置文件同步移除；无清单外的模块被删除

### Requirement: 技术栈到模块映射

系统 SHALL 提供一份"技术栈 → 模块"映射表，使开发者能快速定位任一技术栈（框架/中间件）归属于哪个后端模块；映射表 SHALL 覆盖所有保留模块所承载的技术栈，并随模块结构变更同步维护。

#### Scenario: 技术栈可定位

- **WHEN** 开发者需要确认某个技术栈（如 Redis、Sa-Token、Dubbo、MyBatis-Plus）在哪个模块
- **THEN** 映射表能给出唯一归属模块，且映射表与模块实际 POM 依赖一致

#### Scenario: 映射表随结构变更同步

- **WHEN** 模块结构发生变更（如删除或合并模块）
- **THEN** 映射表同步更新，不出现指向已删除模块或遗漏新模块的技术栈条目

### Requirement: 前端根级单应用结构

前端 `vue-web-ui` SHALL 为单一根级应用：应用代码位于 `vue-web-ui/src`，根 `package.json` 即为应用本体（包名 `@sca/web`），dev 端口 SHALL 为 5173；`vue-web-ui/apps` 目录 SHALL 被删除，不再存在 `apps/portal` 与 `apps/admin`；共享代码 SHALL 保留在 `packages/*`（api/types/utils/ui/uno-preset）。

#### Scenario: 应用代码位于根级 src

- **WHEN** 查看 `vue-web-ui/` 目录结构
- **THEN** 存在 `src/`（含 main.ts、App.vue、router、store、views、layouts、components、api 等）与 `packages/`，不存在 `apps/` 目录

#### Scenario: 根 package.json 为应用本体

- **WHEN** 查看 `vue-web-ui/package.json`
- **THEN** `name` 为 `@sca/web`，包含 `dev/build/typecheck/lint` 等应用脚本，并声明 `packages/*` 为 workspace 依赖

#### Scenario: dev 端口为 5173

- **WHEN** 运行 `pnpm dev`
- **THEN** 应用在 `http://localhost:5173` 启动，网关 CORS 白名单包含该端口

#### Scenario: 公共包保留

- **WHEN** 查看 `vue-web-ui/packages/`
- **THEN** `api`、`types`、`utils`、`ui`、`uno-preset` 等公共包保留，且应用通过 `@sca/*` 引用它们
