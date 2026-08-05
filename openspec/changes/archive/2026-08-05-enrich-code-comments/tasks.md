# Tasks: 全仓库代码与配置注释补充

> 依据 design.md D1 分层顺序执行：公共层 → 网关 → 认证 → 业务 → 配置 → 前端 → 基础设施。
> 每层完成后跑对应验收命令（后端 `mvn validate`、前端 `pnpm typecheck`），全部通过再进下一层。
> 每层一个 `docs(注释): ...` commit，与代码改动隔离。注释质量按 §7 目标 ★★★~★★★★。

## 1. Layer 1 后端公共层（spring-cloud-common）

- [x] 1.1 `spring-cloud-common-core`：为 R<T> 响应/PageResult/业务状态码类补 Javadoc 与字段注释；验收 `mvn validate -pl spring-cloud-common/spring-cloud-common-core -am`
- [x] 1.2 `spring-cloud-common-web`：JacksonConfig（Long→String 雪花 ID 序列化）、全局异常处理、日志/常量类补 Javadoc 与"为什么"说明；验收同上模块
- [x] 1.3 `spring-cloud-common-mybatis`：MyBatis-Plus 配置类、字段填充、分页插件补 Javadoc；验收 `mvn validate -pl spring-cloud-common/spring-cloud-common-mybatis -am`
- [x] 1.4 `spring-cloud-common-redis`：Redis 配置/工具类、缓存 Key 约定补 Javadoc 与行尾注释；验收同上模块
- [x] 1.5 `spring-cloud-common-satoken`：Sa-Token 配置与 StpInterface 集成补 Javadoc（含"为什么这样集成"）；验收 `mvn validate -pl spring-cloud-common/spring-cloud-common-satoken -am`
- [x] 1.6 `spring-cloud-common-dubbo`：Dubbo 配置/接口补 Javadoc；验收 `mvn validate -pl spring-cloud-common/spring-cloud-common-dubbo -am`

## 2. Layer 2 网关（spring-cloud-gateway）

- [x] 2.1 网关认证过滤器/降级 Handler/路由配置类补 Javadoc，重点注释"网关鉴权→透传 X-Login-Id/X-Trace-Id"链路；验收 `mvn validate -pl spring-cloud-gateway -am`

## 3. Layer 3 认证中心（spring-cloud-auth）

- [x] 3.1 `config`/`filter`/`handler`：Sa-Token 过滤器链、登录/踢人/单点登录集成点补 Javadoc 与安全约束说明；验收 `mvn validate -pl spring-cloud-auth -am`
- [x] 3.2 `controller`/`service`/`service.impl`：登录接口复杂逻辑补 `// 1.`/`// 2.` 步骤注释与 Javadoc（含 @param/@return/@throws）；验收同上模块
- [x] 3.3 `dto`/`vo`/`entity`/`enums`/`constant`/`mapper`/`exception`：字段注释、枚举值 `/** 中文说明 */`、常量行尾注释（消除魔法数字）；验收 `mvn validate -pl spring-cloud-auth -am`

## 4. Layer 4 业务服务（spring-cloud-services）

- [x] 4.1 `spring-cloud-system`：用户/角色/菜单相关 Controller/Service/Impl 补 Javadoc 与多步逻辑注释；验收 `mvn validate -pl spring-cloud-services/spring-cloud-system -am`
- [x] 4.2 `spring-cloud-article`：文章 CRUD/点赞/收藏逻辑补 Javadoc 与步骤注释；验收 `mvn validate -pl spring-cloud-services/spring-cloud-article -am`
- [x] 4.3 `spring-cloud-comment`：评论列表/审核/删除逻辑补 Javadoc，含 XSS/敏感词过滤点的安全约束注释；验收 `mvn validate -pl spring-cloud-services/spring-cloud-comment -am`

## 5. Layer 5 后端配置（pom.xml / application*.yml / MyBatis XML）

- [x] 5.1 父 `spring-cloud-alibaba/pom.xml`：`<properties>` 版本属性补行尾注释，`<dependencies>` 按用途分组加 `<!-- 分组名 -->`，plugin 补用途；验收 `mvn validate`
- [x] 5.2 各服务 `bootstrap.yml`/`application*.yml`（auth/gateway/system/article/comment）：每个非自解释键补行尾注释（单位/默认值/开关后果），长配置按 `# ====` 分组，`${VAR:default}` 占位符补用途注释；验收 `mvn validate`
- [x] 5.3 MyBatis Mapper XML：每个 `<select>/<insert>/<update>/<delete>` 补 `<!-- 说明 -->`，复杂 `<where>`/`<if>` 补条件分支注释，`<resultMap>` 非直观映射补注释；验收 `mvn validate`

## 6. Layer 6 前端（vue-web-ui）

- [x] 6.1 `packages/types`：类型/接口/枚举补 JSDoc（含雪花 ID 为 string 的说明）；验收 `pnpm typecheck`
- [x] 6.2 `packages/api`：请求封装/拦截器补 JSDoc（含 code 处理逻辑与登录态失效跳转的"为什么"）；验收 `pnpm typecheck`
- [x] 6.3 `packages/utils` + `packages/ui`：工具函数/UI 封装补 JSDoc，常量与正则补注释（正则必须注释匹配规则）；验收 `pnpm typecheck`
- [x] 6.4 `src/store` + `src/router`：Pinia Store 与路由守卫补 JSDoc（含守卫"为什么这样拦截"）；验收 `pnpm typecheck`
- [x] 6.5 `src/views` + `src/components` 页面组件：`<script setup>` 首行补组件职责注释，`<template>` 区块补 `<!-- 区块说明 -->`，scoped 样式按区块注释；验收 `pnpm typecheck`
- [x] 6.6 `vite.config.ts` + `package.json`：插件/代理/构建配置补注释；验收 `pnpm typecheck`

## 7. Layer 7 基础设施与脚本

- [x] 7.1 `docker/compose/docker-compose.infra.yml`：每个服务补用途与端口注释；验收 `docker compose -f docker/compose/docker-compose.infra.yml config -q`
- [x] 7.2 仓库内 `*.sql`：每个表/字段补 `-- 说明` / COMMENT（含状态位含义）；验收 人工核对
- [x] 7.3 Dockerfile / Shell 脚本：每个构建阶段/脚本头部补用途注释；验收 人工核对

## 8. 全局收尾

- [x] 8.1 全仓对照注释规范 §6 检查清单自检：无废话/无过时/无敏感信息/注释在代码上方或行尾/中英不混排
- [x] 8.2 后端 `mvn validate` 与前端 `pnpm typecheck` 全绿
- [x] 8.3 按层提交 `docs(注释): ...` commit，确认无代码改动混入