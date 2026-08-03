## Purpose

定义 sca-fullstack-lab 的 CLAUDE.md 文档体系规范，要求仓库内每个模块、服务、应用、共享包及基础设施目录均有对应 CLAUDE.md，父模块文档记录子模块职责与索引，并保证全仓文档索引一致性，作为 AI 助手与开发者在本仓库工作的统一入口。

## ADDED Requirements

### Requirement: 文档覆盖范围

仓库 SHALL 在每个模块/服务目录下提供 CLAUDE.md，至少覆盖：仓库根、后端聚合（spring-cloud-alibaba）、公共层（spring-cloud-common）及其 16 个子模块、网关、认证中心、services 聚合及其全部 13 个业务服务、starters 聚合及其 2 个 Starter、前端 monorepo 根、3 个应用（apps/admin、apps/portal、apps/flow-web）、5 个共享包（packages/*）、docker/ 与 docs/。

#### Scenario: 覆盖率检查

- **WHEN** 遍历仓库所有模块/服务/应用/包目录（排除 node_modules、target、.git 等生成目录）
- **THEN** 每个目录下存在 CLAUDE.md，无缺失

#### Scenario: 新增服务文档同步

- **WHEN** 新增 `spring-cloud-article` 与 `spring-cloud-comment` 服务
- **THEN** 两个服务目录下各有一份 CLAUDE.md

### Requirement: 文档内容要求

每个模块的 CLAUDE.md SHALL 至少包含：模块定位与作用、核心功能清单（主要实现的功能）、所用技术栈（框架/中间件及版本，与父 POM/package.json 已声明版本一致）、关键接口或任务（如适用）、本模块特有的开发规范与红线。父模块（聚合层）CLAUDE.md SHALL 额外包含：聚合定位、子模块清单（名称/端口/作用）、子模块 CLAUDE.md 索引。

#### Scenario: 内容完整性抽查

- **WHEN** 抽查任意模块的 CLAUDE.md
- **THEN** 存在模块定位、核心功能、技术栈、规范/红线四部分；聚合层文档还存在子模块清单与索引链接

#### Scenario: 技术栈版本一致性

- **WHEN** 比对模块 CLAUDE.md 中声明的框架版本与父 POM `<properties>` / package.json 声明
- **THEN** 两者一致，无凭空编造版本号

### Requirement: 父模块子模块索引

父模块 CLAUDE.md SHALL 列出其全部子模块，包含：名称、端口/用途摘要、作用说明与子模块 CLAUDE.md 的相对路径链接；子模块 CLAUDE.md SHALL 声明其父文档的读取顺序（根 → 聚合 → 本模块），形成可导航的文档树。

#### Scenario: 索引链接有效性

- **WHEN** 从仓库根 CLAUDE.md 沿聚合层文档逐级导航至任意叶子模块（如某个业务服务）
- **THEN** 每一级的 CLAUDE.md 索引均可通过相对路径定位到下一级文档，且被索引文件真实存在

#### Scenario: 职责继承

- **WHEN** 修改父模块 CLAUDE.md 中的子模块清单
- **THEN** 对应的子模块文档同步更新，不出现索引到不存在的模块或遗漏新增模块

### Requirement: 文档同步维护

对项目结构、端口、服务职责、依赖版本、跨端契约的任何变更，SHALL 同步更新受影响的全部 CLAUDE.md（仓库根、聚合层、子模块三层），禁止出现文档与代码事实不一致。

#### Scenario: 结构变更后一致性

- **WHEN** 服务数量、端口或模块清单发生变更（如本次新增 2 个服务）
- **THEN** 仓库根、spring-cloud-alibaba 聚合、spring-cloud-services 聚合及新增服务文档同步更新，重新满足覆盖率与索引有效性要求
