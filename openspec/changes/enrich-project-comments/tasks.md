# Tasks: 全仓库注释丰富与完善

> 执行原则（见 design.md）：
> - 每批完成 → `git diff` 逐行审查（仅允许注释行）→ 验收命令通过 → `docs(注释): ...` 提交
> - 注释规则严格遵循 `docs/12-注释规范.md`，禁废话/翻译式注释，禁改业务逻辑
> - 粒度 ≤2 小时，一条任务只做一件事

## 1. 前置与收尾

- [ ] 1.1 创建特性分支 `docs/comment-enrichment`（不从 main 直接改）
- [ ] 1.2 收尾验证：全仓库 `mvn validate` 与 `pnpm typecheck` 全部通过

## 2. 批次 0：基础设施与配置脚本

- [ ] 2.1 后端 14 个 `pom.xml`（`spring-cloud-alibaba/pom.xml` 及所有子模块）按用途分组注释依赖、`<properties>` 版本属性补行尾用途说明 —— 验收：`mvn validate`
- [ ] 2.2 `docker/compose/docker-compose.infra.yml` 每个服务补用途/端口/依赖注释 —— 验收：人工 `git diff` 审查
- [ ] 2.3 Flyway 迁移 SQL（`spring-cloud-services/{system,article,comment}/src/main/resources/db/migration/*.sql`）补表/索引/字段 COMMENT 与文件头说明
- [ ] 2.4 初始化 SQL 与脚本（`docker/compose/init/sql/*`、`scripts/*`）补表/字段 COMMENT 与脚本头说明
- [ ] 2.5 其余配置脚本：`Dockerfile`、`.env*` 示例、`vue-web-ui/vite.config.ts`、后端 `bootstrap`/`application*.yml`（5 个）按分组加分隔线、占位符说明 —— 验收：`pnpm typecheck`

## 3. 批次 1：spring-cloud-common 公共层（44 个 Java）

- [ ] 3.1 `spring-cloud-common-core`：响应 `R`/`PageResult`、异常体系、常量/枚举、工具类补 Javadoc —— 验收：`mvn checkstyle:check -pl spring-cloud-common/spring-cloud-common-core -am`
- [ ] 3.2 `spring-cloud-common-web`：`JacksonConfig`（Long→String 序列化）、CORS、全局异常处理 —— 验收：同上 `-pl spring-cloud-common/spring-cloud-common-web`
- [ ] 3.3 `spring-cloud-common-redis`：`RedisConfig`、`DistributedLockAspect` 分布式锁切面「为什么这样集成」说明 —— 验收：`-pl spring-cloud-common/spring-cloud-common-redis`
- [ ] 3.4 `spring-cloud-common-mybatis`：`DataPermissionInnerInterceptor` 数据权限拦截器、`RbacContext`、MyBatis-Plus 配置 —— 验收：`-pl spring-cloud-common/spring-cloud-common-mybatis`
- [ ] 3.5 `spring-cloud-common-satoken`：`StpInterfaceImpl` 权限适配、Sa-Token 配置类 —— 验收：`-pl spring-cloud-common/spring-cloud-common-satoken`
- [ ] 3.6 `spring-cloud-common-dubbo`：Dubbo 相关配置/接口 —— 验收：`-pl spring-cloud-common/spring-cloud-common-dubbo`

## 4. 批次 2：网关与认证中心

- [ ] 4.1 `spring-cloud-gateway`（6 个 Java）：鉴权过滤器链、`X-Login-Id`/`X-Trace-Id` 透传、CORS、路由配置 —— 验收：`mvn checkstyle:check -pl spring-cloud-gateway -am`
- [ ] 4.2 `spring-cloud-auth` 的 controller/service/impl（登录、验证码、风控）：安全约束说明、多步逻辑 `// 1.` 步骤化 —— 验收：`mvn checkstyle:check -pl spring-cloud-auth -am`
- [ ] 4.3 `spring-cloud-auth` 的 entity/dto/vo/enums/constant/exception/config：枚举值、常量、字段 100% 注释 —— 验收：同上

## 5. 批次 3：业务服务

- [ ] 5.1 `spring-cloud-system`（51 个 Java）：entity/dto/vo/exception/常量/枚举与 Service 业务逻辑注释 —— 验收：`mvn checkstyle:check -pl spring-cloud-services/spring-cloud-system -am`
- [ ] 5.2 `spring-cloud-article`（28 个 Java + `ArticleMapper.xml`）：同上 + SQL 意图与 `<where>`/`<if>` 分支注释校准 —— 验收：`mvn checkstyle:check -pl spring-cloud-services/spring-cloud-article -am`
- [ ] 5.3 `spring-cloud-comment`（15 个 Java）：同上 —— 验收：`mvn checkstyle:check -pl spring-cloud-services/spring-cloud-comment -am`

## 6. 批次 4：前端 packages

- [ ] 6.1 `vue-web-ui/packages/api`：`request` 实例、各 service 的 JSDoc（`@param`/`@returns`）、拦截器逻辑注释 —— 验收：`pnpm typecheck`
- [ ] 6.2 `vue-web-ui/packages/types`：全部类型/接口/枚举字段注释，雪花 ID string 约束说明
- [ ] 6.3 `vue-web-ui/packages/utils` + `ui` + `uno-preset`：工具函数 JSDoc、组件封装用途、UnoCSS 预设说明 —— 验收：`pnpm typecheck`

## 7. 批次 5：前端 src

- [ ] 7.1 `vue-web-ui/src/views`（15 个 .vue）：组件顶部职责注释、模板区块 `<!-- -->`、`<script setup>` 逻辑、scoped 样式分组 —— 验收：`pnpm typecheck`
- [ ] 7.2 `vue-web-ui/src/components`（4 个）+ `layouts`（4 个）：复用组件用途注释、插槽/布局说明
- [ ] 7.3 `vue-web-ui/src/hooks` + `router` + `store` + `main.ts` + `App.vue`：组合式函数 JSDoc、路由守卫、Pinia store 注释 —— 验收：`pnpm typecheck`