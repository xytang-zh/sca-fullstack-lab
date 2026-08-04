## 1. 删除空壳模块与目录

- [x] 1.1 删除 7 个空壳服务目录：`spring-cloud-services/spring-cloud-monitor`、`spring-cloud-message`、`spring-cloud-search`、`spring-cloud-file`、`spring-cloud-log`、`spring-cloud-portal`、`spring-cloud-job`（`git rm -r`）
- [x] 1.2 删除 starters 聚合：`spring-cloud-starters/` 整个目录（含 monitor-agent）
- [x] 1.3 删除 10 个 common 子模块目录：`spring-cloud-common/spring-cloud-common-{cache,datasource,es,log,mongo,mq,netty,redisson,security,swagger}`
- [x] 1.4 删除被删模块的 `target/` 残留产物（`mvn clean` 一并清理）

## 2. 更新聚合 POM

- [x] 2.1 父 POM `spring-cloud-alibaba/pom.xml`：`<modules>` 移除 `spring-cloud-starters`
- [x] 2.2 `spring-cloud-common/pom.xml`：`<modules>` 精简为 `core`/`web`/`mybatis`/`redis`/`satoken`/`dubbo` 6 个
- [x] 2.3 `spring-cloud-services/pom.xml`：`<modules>` 精简为 `system`/`article`/`comment` 3 个
- [x] 2.4 验收：`mvn validate` 通过（checkstyle 不因结构变更报错）

## 3. common 代码迁移与依赖补充

- [x] 3.1 `spring-cloud-common-web`：迁入 `@OperationLog` 注解与 `OperationLogAspect`（自 `common-log`，去掉 `RabbitTemplate` 依赖，改 `@Slf4j` 本地日志），保留"切面不吞异常、脱敏"既定行为
- [x] 3.2 `spring-cloud-common-web/pom.xml`：新增 `springdoc-openapi-starter-webmvc-ui` 与 `knife4j-openapi3-jakarta-spring-boot-starter` 依赖（承接原 `swagger` 职责）
- [x] 3.3 `spring-cloud-common-mybatis/pom.xml`：新增 `dynamic-datasource-spring-boot3-starter` 依赖（承接原 `datasource` 职责）
- [x] 3.4 `spring-cloud-common-redis`：迁入 `@DistributedLock` 注解与 `DistributedLockAspect`（自 `common-redisson`）
- [x] 3.5 `spring-cloud-common-redis/pom.xml`：新增 `redisson-spring-boot-starter`、`caffeine`、`spring-boot-starter-aop` 依赖（承接原 `redisson`/`cache` 职责）
- [x] 3.6 验收：`mvn clean install -pl spring-cloud-common -am -DskipTests` 通过

## 4. 服务 POM 依赖调整

- [x] 4.1 `spring-cloud-gateway/pom.xml`：移除 `common-security`、`common-redis`、`common-redisson`、`common-swagger` 依赖，仅保留 `common-core`（gateway 为 WebFlux，禁止依赖 `common-web`/`common-satoken`）
- [x] 4.2 `spring-cloud-auth/pom.xml`：移除 `common-cache`、`common-mq`、`common-redisson`、`common-log`、`common-swagger` 依赖，保留 `core`/`web`/`mybatis`/`redis`/`satoken`
- [x] 4.3 `spring-cloud-system/pom.xml`：移除 `common-cache`、`common-mq`、`common-redisson`、`common-log`、`common-swagger` 依赖，保留 `core`/`web`/`mybatis`/`redis`/`satoken`
- [x] 4.4 `spring-cloud-article/pom.xml`：移除 `common-swagger` 依赖，保留 `core`/`web`/`mybatis`/`redis`/`satoken`/`dubbo`
- [x] 4.5 `spring-cloud-comment/pom.xml`：移除 `common-swagger` 依赖，保留 `core`/`web`/`mybatis`/`redis`/`satoken`/`dubbo`
- [x] 4.6 验收：`mvn clean install -DskipTests` 各服务编译通过

## 5. auth 移除 MQ 登录日志

- [x] 5.1 `spring-cloud-auth/.../service/impl/AuthServiceImpl.java`：删除 `RabbitTemplate` 注入与 `EXCHANGE_LOG_LOGIN` 事件发送逻辑
- [x] 5.2 `spring-cloud-auth/.../constant/AuthConstants.java`：删除 `EXCHANGE_LOG_LOGIN` 等登录日志相关常量
- [x] 5.3 删除 `spring-cloud-auth/.../event/UserLoginEvent.java` 事件类
- [x] 5.4 `spring-cloud-auth/.../AuthServiceImplTest.java`：移除 `RabbitTemplate` mock 与相关断言
- [x] 5.5 验收：`mvn clean test -pl spring-cloud-auth -am` 通过

## 6. 文档同步更新

- [x] 6.1 `spring-cloud-alibaba/CLAUDE.md`：更新模块结构、端口分配表、服务间通信表，新增"技术栈 → 模块"映射表
- [x] 6.2 `spring-cloud-common/CLAUDE.md`：子模块清单精简为 6 个，更新依赖映射表，新增"技术栈 → 模块"映射表
- [x] 6.3 `spring-cloud-services/CLAUDE.md`：子服务清单精简为 3 个，删除已删服务职责与文档
- [x] 6.4 仓库根 `sca-fullstack-lab/CLAUDE.md`：更新端口分配表、子项目索引、技术栈矩阵
- [x] 6.5 保留模块 CLAUDE.md（gateway/auth/system/article/comment）`pom.xml` 依赖清单与 common 引用同步
- [x] 6.6 删除已删模块目录下的 CLAUDE.md（随目录删除），父层文档索引不再引用
- [x] 6.7 同步更新 `spring-cloud-auth/CLAUDE.md` 中 MQ 相关描述（移除登录日志事件）

## 7. 配置核对

- [x] 7.1 检查 `spring-cloud-gateway/src/main/resources/` 下 application.yml/bootstrap.yml 是否引用已删服务（如路由/白名单），有则清理
- [x] 7.2 检查 `docker/compose/docker-compose.infra.yml` 是否需移除 RabbitMQ（mq 模块删除后如无使用方，与用户确认）
- [x] 7.3 核对 Nacos 远程配置（`spring-cloud-gateway.yaml` 等）中已删服务路由/白名单，输出运维清理清单（仓库内不改远程配置）

## 8. 全量构建验证

- [x] 8.1 `mvn clean install -DskipTests` 全量聚合工程编译通过
- [x] 8.2 `mvn checkstyle:check` 全量阿里规范验证通过
- [x] 8.3 抽查 `spring-cloud-auth`、`spring-cloud-system` 单测通过（`mvn clean test -pl spring-cloud-auth -am`、`-pl spring-cloud-system -am`）
- [x] 8.4 核对技术栈映射表与最终 POM 依赖一致（无指向已删模块的条目）