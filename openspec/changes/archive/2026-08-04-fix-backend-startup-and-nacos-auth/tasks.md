## 1. 基础设施：确认 Nacos 认证配置与控制台访问

- [x] 1.1 确认 `docker/.env` 的 `NACOS_AUTH_TOKEN_SECRET_KEY` 为 Base64 且解码后 ≥32 字节（验收：`python -c "import base64;print(len(base64.b64decode('<值>')))"` 输出 ≥32）
- [x] 1.2 确认 `docker/compose/docker-compose.infra.yml` nacos 段 `NACOS_AUTH_TOKEN: ${NACOS_AUTH_TOKEN_SECRET_KEY:?必须在 .env 设置}` 映射正确（Nacos 2.4.3 镜像用 `NACOS_AUTH_TOKEN` 作为 JWT secret key；验收：`grep NACOS_AUTH docker/compose/docker-compose.infra.yml` 确认映射）
- [x] 1.3 重启 Nacos 容器并验证控制台 `nacos/nacos` 登录可访问 public 与 sca-dev 命名空间（验收：`curl -s -X POST http://127.0.0.1:8848/nacos/v1/auth/login -d "username=nacos&password=nacos"` 返回 accessToken，且 `docker ps` 显示 sca-nacos healthy，控制台无"没有 X 命名空间的访问权限"提示）

## 2. Nacos 服务级数据源改为 dynamic-datasource 格式

- [x] 2.1 更新 Nacos `sca-dev` 命名空间 `spring-cloud-auth.yaml` 的 `spring.datasource` 段为 dynamic-datasource 格式（`spring.datasource.dynamic.datasource.master` + `primary: master`）
- [x] 2.2 更新 Nacos `sca-dev` 命名空间 `spring-cloud-system.yaml` 的 `spring.datasource` 段（同上）
- [x] 2.3 更新 Nacos `sca-dev` 命名空间 `spring-cloud-article.yaml` 的 `spring.datasource` 段（同上）
- [x] 2.4 更新 Nacos `sca-dev` 命名空间 `spring-cloud-comment.yaml` 的 `spring.datasource` 段（同上）

## 3. 启动验证与回归

- [x] 3.1 启动 auth 服务验证 `dynamic-datasource initial loaded [1] datasource` 且 `Started`（验收：`cd spring-cloud-alibaba && mvn -q spring-boot:run -pl spring-cloud-auth` 启动成功，无 `dynamic-datasource can not find primary datasource`）
- [x] 3.2 启动 system 服务验证（验收：同上，`-pl spring-cloud-services/spring-cloud-system`）
- [x] 3.3 启动 article 服务验证（验收：同上，`-pl spring-cloud-services/spring-cloud-article`）
- [x] 3.4 启动 comment 服务验证（验收：同上，`-pl spring-cloud-services/spring-cloud-comment`）
- [x] 3.5 回归 gateway 启动不受影响（验收：`cd spring-cloud-alibaba && mvn -q spring-boot:run -pl spring-cloud-gateway` 启动成功）
- [x] 3.6 确认 Nacos 注册中心 4 个服务均在线（验收：Nacos 控制台服务列表显示 auth/system/article/comment 健康实例）