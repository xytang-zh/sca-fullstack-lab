## 1. 父 POM 依赖与模块注册

- [x] 1.1 在 `spring-cloud-alibaba/pom.xml` 的 `<properties>` 新增 `<dubbo.version>3.3.6</dubbo.version>`，并在 `<dependencyManagement>` 导入 `org.apache.dubbo:dubbo-bom:${dubbo.version}`（验收：`mvn help:effective-pom -pl spring-cloud-alibaba | grep -i dubbo` 可见 BOM）
- [x] 1.2 在 `spring-cloud-alibaba/spring-cloud-common/pom.xml` 的 `<modules>` 新增 `<module>spring-cloud-common-dubbo</module>`（验收：`mvn validate -pl spring-cloud-common` 通过）

## 2. 新建 spring-cloud-common-dubbo 公共模块

- [x] 2.1 创建 `spring-cloud-alibaba/spring-cloud-common/spring-cloud-common-dubbo/pom.xml`，依赖 `spring-cloud-common-core`（打包 jar，不引入 Dubbo 运行时依赖）（验收：`mvn clean install -pl spring-cloud-common/spring-cloud-common-dubbo -am -DskipTests` 成功）
- [x] 2.2 定义 `com.xytang.common.dubbo.ArticleRpcService`：`boolean existsById(Long articleId)`，纯接口 + `@FunctionalInterface`（可选）（验收：编译通过）
- [x] 2.3 定义 `com.xytang.common.dubbo.CommentRpcService`：`long countByArticleId(Long articleId)`，纯接口（验收：编译通过）

## 3. article 服务实现 Dubbo Provider（暴露 ArticleRpcService）

- [x] 3.1 在 `spring-cloud-alibaba/spring-cloud-services/spring-cloud-article/pom.xml` 新增 `spring-cloud-common-dubbo` 与 `org.apache.dubbo:dubbo-spring-boot-starter` 依赖（不写版本）（验收：`mvn clean install -pl spring-cloud-article -am -DskipTests` 成功）
- [x] 3.2 在 `spring-cloud-article/src/main/resources/application.yml` 新增 `dubbo` 配置：`application.name=${spring.application.name}`、`registry.address=nacos://${NACOS_ADDR:127.0.0.1:8848}`、`protocol.name=tri`、`protocol.port=20893`、`consumer.check=false`（验收：配置语法正确）
- [x] 3.3 在 `SpringCloudArticleApplication` 启动类加 `@EnableDubbo`（验收：编译通过）
- [x] 3.4 新建 `com.xytang.article.rpc.ArticleRpcServiceImpl` 实现 `ArticleRpcService`，`@DubboService` 注解，注入 `ArticleMapper`，`existsById` 用 `selectById` 判空（验收：`mvn clean install -pl spring-cloud-article -am -DskipTests` 成功）

## 4. comment 服务实现 Dubbo Provider（暴露 CommentRpcService）

- [x] 4.1 在 `spring-cloud-alibaba/spring-cloud-services/spring-cloud-comment/pom.xml` 新增 `spring-cloud-common-dubbo` 与 `org.apache.dubbo:dubbo-spring-boot-starter` 依赖（不写版本）（验收：`mvn clean install -pl spring-cloud-comment -am -DskipTests` 成功）
- [x] 4.2 在 `spring-cloud-comment/src/main/resources/application.yml` 新增 `dubbo` 配置：`application.name=${spring.application.name}`、`registry.address=nacos://${NACOS_ADDR:127.0.0.1:8848}`、`protocol.name=tri`、`protocol.port=20894`、`consumer.check=false`（验收：配置语法正确）
- [x] 4.3 在 `SpringCloudCommentApplication` 启动类加 `@EnableDubbo`（验收：编译通过）
- [x] 4.4 新建 `com.xytang.comment.rpc.CommentRpcServiceImpl` 实现 `CommentRpcService`，`@DubboService` 注解，注入 `CommentMapper`，`countByArticleId` 用 `selectCount` 统计（验收：`mvn clean install -pl spring-cloud-comment -am -DskipTests` 成功）

## 5. comment 消费 article（校验文章存在）

- [x] 5.1 在 `CommentServiceImpl` 以 `@DubboReference` 字段注入 `ArticleRpcService`（注：Dubbo 3.3.6 的 `@DubboReference` 不支持构造器参数注入，改用字段注入——Dubbo 官方惯例，非 `@Autowired`，不违反项目红线）（验收：编译通过）
- [x] 5.2 在发表评论方法中，落库前调用 `articleRpcService.existsById(articleId)`，返回 false 时抛 `BusinessException`（文章不存在）（验收：对不存在的 articleId 发评论返回业务错误）

## 6. article 消费 comment（聚合评论数）

- [x] 6.1 在 `ArticleServiceImpl` 以 `@DubboReference` 字段注入 `CommentRpcService`（注：Dubbo 3.3.6 的 `@DubboReference` 不支持构造器参数注入，改用字段注入——Dubbo 官方惯例）（验收：编译通过）
- [x] 6.2 在 `detail` 方法中调用 `commentRpcService.countByArticleId(articleId)` 写入 `ArticleDetailVO.comments`，`catch (RpcException e)` 时降级为 0（精确捕获，不阻塞详情返回）（验收：详情接口返回 `comments` 字段；comment 服务停掉后详情仍返回且 comments=0）

## 7. 构建与端到端验证

- [x] 7.1 全量编译（验收：`mvn clean install -DskipTests` 在 `spring-cloud-alibaba` 根目录成功）
- [x] 7.2 启动 Nacos（`docker compose -f docker/compose/docker-compose.infra.yml up -d nacos`），再启动 article 与 comment 两个服务（验收：两个服务启动无异常，Nacos 控制台服务列表可见 `providers:com.xytang.common.dubbo.ArticleRpcService` 与 `providers:com.xytang.common.dubbo.CommentRpcService`）
- [x] 7.3 双向调用验证（验收：① 对不存在的文章发评论返回"文章不存在"；② 请求文章详情返回真实 `comments` 数；③ 停掉 comment 服务后详情仍返回且 `comments=0`）

## 8. 文档更新

- [x] 8.1 更新 `spring-cloud-alibaba/CLAUDE.md` §10.1：Dubbo 从"接口定义在 common-core rpc 包（计划）"改为"接口定义在 `spring-cloud-common-dubbo`，协议 Triple、端口见端口表"，补充 Dubbo 配置要点（验收：文档与实现一致）
- [x] 8.2 更新 `spring-cloud-alibaba/spring-cloud-common/CLAUDE.md`：新增 `spring-cloud-common-dubbo` 模块说明（职责：RPC 契约定义）（验收：文档与实现一致）