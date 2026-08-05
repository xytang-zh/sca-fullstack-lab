# Proposal: 全仓库注释丰富与完善

## Why

本项目是**学习向**个人博客项目，目的是在实战中理解各个技术栈与框架原理，因此注释是**项目资产的一部分**而非可有可无的装饰。当前核心/新写文件（如 Spring Cloud Auth 登录链路、登录页、`application.yml`、初始化 SQL、`ArticleMapper.xml`、`docker-compose.infra.yml`）注释已相当完善，但覆盖面不均：`spring-cloud-common` 公共层 44 个 Java 文件、业务服务的 entity/DTO/VO/exception/常量类、前端 packages 的 TS 类型与工具函数、部分配置文件与脚本仍存在注释缺失或仅翻译代码的低质量注释，不利于学习者通读。本变更按 `docs/12-注释规范.md` 的标准，分层补齐并校准全仓库注释，使注释密度与质量达到 **★★★ 及以上**（关键业务/安全代码 ★★★★）。

## What Changes

纯注释/文档改动，**不改变任何运行行为、接口契约、依赖版本**。具体：

- **后端 Java（`spring-cloud-alibaba/`，约 166 个源文件）**
  - `spring-cloud-common/*`（44 个）：补齐公共响应/异常/工具/常量/配置类与枚举的 Javadoc，重点解释框架集成点（AOP 切面、MyBatis 拦截器、Jackson 配置、Sa-Token 适配）的「为什么这样集成」
  - `spring-cloud-auth`（22 个）：补充登录风控、验证码、Sa-Token 会话等安全敏感代码的约束说明
  - `spring-cloud-services/{system,article,comment}`（51+28+15 个）：补齐 entity/DTO/VO/exception/常量/枚举的字段与类注释，业务 Service 多步逻辑补 `// 1.` 步骤注释
  - `spring-cloud-gateway`（6 个）：网关过滤器链、鉴权与透传逻辑注释
- **前端（`vue-web-ui/`，27 个 `.vue` + 23 个 `.ts`）**
  - 页面/组件：按 `docs/12-注释规范.md` §3.3 补齐组件顶部职责注释、模板区块注释、`<script setup>` 逻辑注释、scoped 样式区块注释
  - `packages/api`、`packages/types`、`packages/utils`：补齐 JSDoc（`@param`/`@returns`）与类型/工具函数说明
- **配置与脚本**
  - 5 个 `application*.yml`：按分组加 `# ====` 分隔线，非自解释配置项与 `${VAR:default}` 占位符补行尾注释
  - 后端与前端 `pom.xml`（共 14 个）：依赖按用途分组注释，`<properties>` 版本属性补用途说明
  - 1 个 MyBatis XML（`ArticleMapper.xml`）：补 SQL 意图与复杂 `<where>`/`<if>` 分支注释（抽查已较完善，仅校准）
  - 10 个 SQL 脚本（Flyway 迁移 + 初始化脚本）：补表/索引/字段 COMMENT 与复杂语句意图
  - `docker/compose/docker-compose.infra.yml`：补服务用途、端口、依赖关系注释
  - 其它：`Dockerfile`、`vite.config.ts`、Shell 脚本、`.env*` 示例等按 §3.9 补充
- **校准**：修正与代码不一致的过时注释、删除孤儿注释；不新增废话注释

## Capabilities

### New Capabilities

（无 —— 纯注释/文档改动，不引入新能力）

### Modified Capabilities

（无 —— 不改变任何既有能力的 REQUIREMENTS，代码行为不变，采用 `skip_specs: true`）

## Impact

- **affected modules（后端）**：`spring-cloud-alibaba/spring-cloud-common`（6 个子模块）、`spring-cloud-gateway`、`spring-cloud-auth`、`spring-cloud-services/{system,article,comment}`、父 POM 与各子模块 `pom.xml`
- **affected modules（前端）**：`vue-web-ui/src`（views/components/layouts/hooks/router/store）、`vue-web-ui/packages/{api,types,utils,ui,uno-preset}`、`vite.config.ts`
- **affected modules（基础设施/脚本）**：`docker/compose/docker-compose.infra.yml`、`docker/compose/init/sql/*`、`scripts/*`、Flyway 迁移脚本（`spring-cloud-services/*/src/main/resources/db/migration/*.sql`）
- **行为影响**：无。仅注释文本变更，不触碰业务逻辑、接口签名、依赖版本
- **验证方式**：后端 `mvn checkstyle:check`（注释不得引入 checkstyle 违规）、前端 `pnpm typecheck` 不受影响

## Non-goals

- 不改变任何代码行为、接口、依赖版本（纯注释）
- 不新增业务功能、不重构代码结构
- 不写废话/翻译式注释（`// 声明变量` 等），严格按 §4.3 禁止项执行
- 不处理 `target/`、`node_modules/`、`dist/` 等构建产物目录
- 不修改 `docs/12-注释规范.md` 本身（除非发现规范冲突，另行提案）
- 不为已自解释的命名（如 `getUserId()`）强行加注释
- 不遗留 `// TODO`/`// FIXME` 不处理（如有必须注明负责人与截止时间）