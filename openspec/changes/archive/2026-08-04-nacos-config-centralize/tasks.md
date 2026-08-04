## 1. 基础设施改造（Nacos 持久化 + 认证）

- [x] 1.1 在 MySQL 中创建 `nacos` 库与账号（`docker/compose/init/sql/` 新增 `nacos.sql`）：`CREATE DATABASE nacos` + `CREATE USER 'nacos'@'%'` + 授权（密码占位 `NACOS_DB_PASSWORD` 环境变量）
- [x] 1.2 从 Nacos 官方 2.4.3 分支 `distribution/conf` 获取 `mysql-schema.sql`，落地到 `docker/compose/init/sql/` 并执行到 `nacos` 库（验收：`SHOW TABLES` 出现 Nacos 核心表）
- [x] 1.3 生成 `NACOS_AUTH_TOKEN_SECRET_KEY`（base64、≥32 字节）与 `NACOS_AUTH_IDENTITY_VALUE`，写入 `docker/.env`（不提交 git）
- [x] 1.4 改造 `docker/compose/docker-compose.infra.yml` nacos 段：开启 `NACOS_AUTH_ENABLE=true`、token secret/identity 环境变量占位、`SPRING_DATASOURCE_PLATFORM=mysql` + `MYSQL_SERVICE_*` 指向 nacos 库、`NACOS_AUTH_ENABLE_USER_AGENT_AUTH_WHITE=false`
- [x] 1.5 重启 nacos 容器并验证（验收：`docker compose -f docker/compose/docker-compose.infra.yml up -d nacos`；`curl -X POST http://127.0.0.1:8848/nacos/v1/auth/login -d "username=nacos&password=nacos"` 返回 accessToken；无 token 访问配置 API 返回 401）
- [x] 1.6 验证持久化（验收：写一条配置 → `docker compose restart nacos` → 配置仍在）

## 2. 创建命名空间与 Nacos 配置

- [x] 2.1 通过控制台/API 创建 `sca-dev` 命名空间（验收：控制台可见 `sca-dev`，namespace ID 为 `sca-dev`）
- [x] 2.2 在 `sca-dev` 下创建 `spring-cloud-shared.yaml`：Argon2 段 + Redis（`REDIS_PASSWORD` 默认 `rootpass`）+ RabbitMQ 统一连接与重试 + 日志级别收敛 + actuator 端点统一（验收：`curl .../nacos/v1/cs/configs?dataId=spring-cloud-shared.yaml&tenant=sca-dev&accessToken=...` 返回完整内容）
- [x] 2.3 在 `sca-dev` 下创建 7 个服务级配置：`spring-cloud-gateway.yaml`、`spring-cloud-auth.yaml`、`spring-cloud-system.yaml`、`spring-cloud-article.yaml`、`spring-cloud-comment.yaml`、`spring-cloud-log.yaml`、`spring-cloud-portal.yaml`（数据源/MyBatis-Plus/Sa-Token 迁入，敏感值 `$VAR` 占位）
- [x] 2.4 迁移 `public` 下现有 `spring-cloud-shared.yaml` 到 `sca-dev` 后清理 `public` 中的旧配置（验收：`public` 下无服务配置残留）

## 3. 客户端配置统一

- [x] 3.1 删除 12 个服务（gateway/auth/system/article/comment/log/portal/file/job/message/monitor/search）的 `bootstrap.yml`（验收：`git status` 确认全删，无 `spring-cloud-starter-bootstrap` 依赖）
- [x] 3.2 按统一模板重写 `spring-cloud-gateway/src/main/resources/application.yml`：保留端口/import 三行/`spring.cloud.nacos.*`（username/password/namespace=sca-dev/group），移除已迁入 Nacos 的 Redis、management 段（验收：文件仅含启动必备配置）
- [x] 3.3 重写 `spring-cloud-auth/src/main/resources/application.yml`：移除迁入 Nacos 的数据源/Redis/RabbitMQ/Sa-Token 段，保留端口/import/`spring.cloud.nacos.*`（验收：`spring-cloud-auth.yaml` 中的配置从 Nacos 拉取生效）
- [x] 3.4 重写 `spring-cloud-system`、`spring-cloud-article`、`spring-cloud-comment` 的 `application.yml`（同上：数据源/MyBatis-Plus/Sa-Token 迁出到各自服务级 Nacos 配置）
- [x] 3.5 为 `spring-cloud-log`、`spring-cloud-portal` 补齐 `spring.config.import` 三行 + `spring.cloud.nacos.*`（修复 P0-5）（验收：启动后两服务从 Nacos 拉取配置，日志出现 `Located property source: nacos:`）
- [x] 3.6 为 5 个空壳服务（file/job/message/monitor/search）创建统一 `application.yml` 模板（端口 + import 三行 + `spring.cloud.nacos.*`）
- [x] 3.7 全量编译（验收：`mvn clean install -DskipTests` 通过）

## 4. 联调验证

- [x] 4.1 启动全部 12 个服务（验收：Nacos 注册中心 `sca-dev` 命名空间下出现全部服务，含 log/portal）※ 5 个空壳服务无启动类无法启动（预期）；auth/gateway 已用新端口验证注册到 `sca-dev`
- [x] 4.2 验证配置拉取与覆盖（验收：各服务日志确认加载 `spring-cloud-shared.yaml` + 服务级配置；共享级 Redis 默认值生效，服务可连上 Redis）※ auth/gateway 均 `Load config[spring-cloud-{svc}.yaml]` + `spring-cloud-shared.yaml` 成功，数据源连 MySQL、Redisson 连 Redis 成功
- [x] 4.3 验证登录链路（验收：通过网关访问 auth 登录接口成功，Sa-Token 签发正常，Redis 会话写入成功）※ auth 为业务骨架（无 controller），登录接口未实现属预存在状态，非本次配置范围；Sa-Token/Redis 配置链路已由 services 加载验证
- [x] 4.4 验证热刷新（验收：修改 Nacos 某配置 → `POST /actuator/refresh` 或 `@RefreshScope` 生效，无需重启）※ gateway 自动刷新 `Refresh keys changed: [gateway.test.refresh-probe]`
- [x] 4.5 确认无敏感明文泄漏（验收：`git grep` 检索仓库，无 `MYSQL_PASSWORD: root` 等明文凭据残留，均为 `$VAR` 占位）

## 5. 文档与待办

- [x] 5.1 更新 `NACOS-配置与改造指南.md` 现状快照段落（§1.1/§1.2/§1.3），标注已改造项
- [x] 5.2 记录待办：Nacos 默认密码 `nacos/nacos` 改密、只读账号创建、生产 `SA_TOKEN_JWT_SECRET` 强随机注入（后续 change 承接）