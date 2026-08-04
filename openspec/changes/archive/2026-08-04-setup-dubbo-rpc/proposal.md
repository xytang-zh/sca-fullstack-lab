# Proposal: 搭建 Dubbo 服务间调用环境（Triple 协议 + Nacos 注册）

## Why

article 与 comment 是独立微服务，博客详情页需要同时展示文章内容与用户评论。当前服务间**没有任何 RPC 通信能力**：`spring-cloud-alibaba` 虽在 CLAUDE.md 规划了 Dubbo（端口 20893/20894、RPC 契约 `ArticleRpcService.existsById` 等），但**父 POM 未引入 Dubbo 依赖、无 RPC 接口定义、application.yml 无 Dubbo 配置**，服务间只能通过 HTTP 网关间接交互。本变更搭建 Dubbo 通讯环境并用双向调用链路验证，为后续服务间同步调用（comment 校验文章、article 聚合评论数等）奠定基础。

## What Changes

**依赖与版本（spring-cloud-alibaba/pom.xml）**
- 新增 `<dubbo.version>3.3.6</dubbo.version>` 属性，在 `<dependencyManagement>` 导入 `org.apache.dubbo:dubbo-bom:3.3.6`
- 说明：SCA BOM 2025.0.0.0 仅管理 Nacos/Sentinel/Seata，**不管理 Dubbo 版本**，需自行导入 Dubbo BOM；`dubbo-spring-boot-starter` 已内置 Triple 协议与 Nacos 注册中心

**新增公共模块 `spring-cloud-common/spring-cloud-common-dubbo`**
- 存放跨服务 RPC 接口契约（纯接口 + 可序列化 DTO，不引入 Dubbo 依赖，保持轻量）
- `ArticleRpcService`：`existsById(Long id)`（供 comment 校验文章存在）
- `CommentRpcService`：`countByArticleId(Long articleId)`（供 article 聚合评论数）

**article 服务（Dubbo Provider + Consumer）**
- POM 引入 `spring-cloud-common-dubbo` + `dubbo-spring-boot-starter`
- application.yml 配置 Dubbo：注册中心 Nacos（复用现有 127.0.0.1:8848）、协议 `tri`、端口 20893
- 启动类加 `@EnableDubbo`
- 新增 `rpc/ArticleRpcServiceImpl`（`@DubboService` 实现 `ArticleRpcService`）
- 文章详情接口经 `@DubboReference` 调用 `CommentRpcService.countByArticleId` 聚合评论数（纳入 `ArticleDetailVO.comments`）

**comment 服务（Dubbo Provider + Consumer）**
- POM 引入 `spring-cloud-common-dubbo` + `dubbo-spring-boot-starter`
- application.yml 配置 Dubbo：注册中心 Nacos、协议 `tri`、端口 20894
- 启动类加 `@EnableDubbo`
- 新增 `rpc/CommentRpcServiceImpl`（`@DubboService` 实现 `CommentRpcService`）
- 发表评论前经 `@DubboReference` 调用 `ArticleRpcService.existsById` 校验文章存在

**打通 `spring-cloud-common-dubbo` 模块**：在 `spring-cloud-common/pom.xml` 的 `<modules>` 中注册，article/comment 服务 POM 依赖之

## Capabilities

### New Capabilities

- `dubbo-rpc`: 定义服务间 Dubbo RPC 通信契约——RPC 接口定义位置、注册中心（Nacos）、协议（Triple）与端口、Provider/Consumer 注解规范，以及 `ArticleRpcService` / `CommentRpcService` 两个具体接口的调用行为

### Modified Capabilities

（无）

## Impact

- 后端模块：`spring-cloud-alibaba/pom.xml`、`spring-cloud-alibaba/spring-cloud-common/pom.xml`、`spring-cloud-alibaba/spring-cloud-common/spring-cloud-common-dubbo/`（新增）、`spring-cloud-alibaba/spring-cloud-services/spring-cloud-article/`（pom、application.yml、启动类、新增 rpc 包、ArticleServiceImpl 聚合评论数）、`spring-cloud-alibaba/spring-cloud-services/spring-cloud-comment/`（pom、application.yml、启动类、新增 rpc 包、CommentServiceImpl 校验文章存在）
- 依赖：新增 `org.apache.dubbo:dubbo-bom`（3.3.6）、`dubbo-spring-boot-starter`（3.3.6）、`spring-cloud-common-dubbo`
- 基础设施：Nacos 注册中心（复用现有 8848，无需新增容器）
- 前端：无改动（兼容 `CommentPanel` 现有评论列表接口）

## Non-goals

- 不做 search/file/system 等其他服务的 Dubbo 接入（仅 article↔comment 双向验证）
- 不做 Dubbo 服务治理功能（限流、熔断、灰度、可观测性）——仅打通基础通讯链路
- 不改变前端评论数据获取方式（详情页评论仍由前端直接调 comment 服务，非经 article 聚合）
- 不引入 gRPC 外部调用方（仅 Dubbo 内部调用）
- 不做多注册中心 / 多协议等高级配置