## MODIFIED Requirements

### Requirement: 文档覆盖范围

仓库 SHALL 在每个模块/服务目录下提供 CLAUDE.md，至少覆盖：仓库根、后端聚合（spring-cloud-alibaba）、公共层（spring-cloud-common）及其 6 个子模块（core/web/mybatis/redis/satoken/dubbo）、网关、认证中心、services 聚合及其 3 个业务服务（system/article/comment）、前端 monorepo 根、单一根级应用、共享包（packages/*）、docker/ 与 docs/。

#### Scenario: 覆盖率检查

- **WHEN** 遍历仓库所有模块/服务/应用/包目录（排除 node_modules、target、.git 等生成目录）
- **THEN** 每个目录下存在 CLAUDE.md，无缺失；已删除模块（7 个空壳服务、10 个 common 子模块、starters 聚合）的目录与文档不存在

#### Scenario: 删除模块文档同步移除

- **WHEN** 删除 `spring-cloud-monitor` 等模块
- **THEN** 对应模块目录及其 CLAUDE.md 一并移除，父层文档索引不再引用

### Requirement: 文档内容要求

每个模块的 CLAUDE.md SHALL 至少包含：模块定位与作用、核心功能清单（主要实现的功能）、所用技术栈（框架/中间件及版本，与父 POM/package.json 已声明版本一致）、关键接口或任务（如适用）、本模块特有的开发规范与红线。父模块（聚合层）CLAUDE.md SHALL 额外包含：聚合定位、子模块清单（名称/端口/作用）、子模块 CLAUDE.md 索引，以及**"技术栈 → 模块"映射表**（每个技术栈唯一归属到承载它的模块，便于快速定位）。

#### Scenario: 内容完整性抽查

- **WHEN** 抽查任意模块的 CLAUDE.md
- **THEN** 存在模块定位、核心功能、技术栈、规范/红线四部分；聚合层文档还存在子模块清单与索引链接

#### Scenario: 技术栈版本一致性

- **WHEN** 比对模块 CLAUDE.md 中声明的框架版本与父 POM `<properties>` / package.json 声明
- **THEN** 两者一致，无凭空编造版本号

#### Scenario: 技术栈映射表存在且唯一

- **WHEN** 查看后端聚合层（`spring-cloud-alibaba/CLAUDE.md`）或公共层（`spring-cloud-common/CLAUDE.md`）的"技术栈 → 模块"映射表
- **THEN** 每个技术栈条目（如 Redis、Sa-Token、MyBatis-Plus、Dubbo、Redisson、dynamic-datasource）唯一指向一个保留模块，且映射对应关系与模块实际 POM 依赖一致

#### Scenario: 映射表不含已删除模块

- **WHEN** 检查"技术栈 → 模块"映射表
- **THEN** 不存在指向已删除模块（如 `spring-cloud-common-mq`、`spring-cloud-common-netty`）的条目