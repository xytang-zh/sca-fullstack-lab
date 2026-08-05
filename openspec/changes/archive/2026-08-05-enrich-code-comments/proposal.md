# Proposal: 全仓库代码与配置注释补充（按注释规范）

## Why

本项目是**学习向项目**，目的是在实战中理解 Spring Cloud Alibaba、Vue 3 等框架原理。但当前代码注释稀少，公开类/方法缺 Javadoc、配置项缺含义说明，读者无法理解设计意图与框架集成点，达不到"看代码即学习"的目标。仓库存档的 `docs/12-注释规范.md` 已定义完整注释体系，但尚未落地到代码。

## What Changes

本变更**只补充注释，不改变任何运行行为**（纯 docs 性质，`skip_specs: true`）：

- **后端 Java（spring-cloud-alibaba/，166 个 .java）**
  - 公开类/接口/枚举补 Javadoc（含类职责、安全约束、协作关系）
  - 公开方法补 Javadoc（含 `@param`/`@return`/`@throws`）
  - 字段/常量补注释，消除魔法数字
  - 复杂业务逻辑补 `// 1.`、`// 2.` 步骤化注释
  - 框架集成点（配置类、Sa-Token 过滤器、网关过滤器）补"为什么这样集成"说明
- **后端配置文件**
  - `application*.yml`/`bootstrap*.yml`：每个非自解释配置项补行尾注释（含单位/默认值/开关后果），长配置按 `# ====` 分组，`${VAR:default}` 占位符补用途注释
  - `pom.xml`：`<dependencies>` 按用途分组加 `<!-- 分组名 -->`，`<properties>` 版本属性补行尾注释，`<plugin>` 补用途说明
  - MyBatis Mapper XML：每个 SQL 补 `<!-- 说明 -->`，复杂 `<where>`/`<if>` 补条件分支注释
- **前端（vue-web-ui/，约 46 个 TS/Vue + 配置）**
  - 类型/接口/组合式函数补 JSDoc（含 `@param`/`@returns`）
  - Vue 组件 `<script setup>` 首行补组件职责注释，`<template>` 区块补 `<!-- 说明 -->`，scoped 样式按区块注释
  - 常量/魔法值/正则补注释
  - `vite.config.ts`、`package.json` 关键配置补注释
- **基础设施与脚本**
  - `docker-compose*.yml`：每个服务补用途与端口注释
  - `*.sql`：每个表/字段补 `-- 说明` / `COMMENT`
  - Dockerfile / Shell 脚本：每个构建阶段/脚本头部补注释

## Capabilities

### New Capabilities

（无 —— 本变更不改变任何系统行为，仅补充注释，故按 `skip_specs: true` 处理，不新增 spec）

### Modified Capabilities

（无 —— 无 spec 级需求变更）

## Impact

- **后端模块**：`spring-cloud-alibaba/spring-cloud-common/*`（6 公共子模块）、`spring-cloud-gateway`、`spring-cloud-auth`、`spring-cloud-services/spring-cloud-system|article|comment`
- **后端配置**：`spring-cloud-alibaba/pom.xml`、各服务 `src/main/resources/application*.yml`/`bootstrap*.yml`、MyBatis XML
- **前端**：`vue-web-ui/src/**`、`vue-web-ui/packages/{api,types,ui,utils,uno-preset}/**`、`vue-web-ui/vite.config.ts`、`vue-web-ui/package.json`
- **基础设施**：`docker/compose/docker-compose.infra.yml`、仓库内 `*.sql`、Dockerfile、Shell 脚本
- **风险**：纯注释改动不影响编译与运行；需保证注释与代码一致（不产生过时注释），Java 侧不破坏 checkstyle（`mvn validate` 通过）

## Non-goals

- 不改动任何业务逻辑、接口、数据结构、配置值（注释是对现有代码的解释，不是重构）
- 不新增/删除任何文件（除注释规范本身指引外）
- 不处理 `docs/04/09/10` 中已删除服务（workflow/ai/report）的历史遗留内容
- 不为自解释代码添加废话注释（"翻译式"注释，规范 §4.3 禁止）
- 单个 commit 不混入代码改动；注释 commit 统一用 `docs` 类型