## Purpose

定义微服务间 Dubbo RPC 同步调用的行为契约：RPC 接口定义位置、注册与发现机制、暴露与调用规范，以及 article 与 comment 两个服务间的具体调用行为，作为服务间通信环境搭建与验收的基线。

## Requirements

### Requirement: RPC 接口契约定义位置

系统 SHALL 将跨服务 RPC 接口契约（接口定义与其出入参 DTO）统一定义在公共模块 `spring-cloud-common-dubbo` 中，该模块 SHALL 不依赖任何 Dubbo 运行时依赖；所有需要暴露 RPC 或调用 RPC 的服务 SHALL 依赖该模块，禁止在单个服务内私自定义跨服务 RPC 接口。

#### Scenario: 跨服务共享 RPC 契约

- **WHEN** article 与 comment 两个服务需要互相调用 RPC
- **THEN** 双方共享同一份 `spring-cloud-common-dubbo` 模块中的接口定义，调用方与被调方引用的是同一接口类型

### Requirement: 服务注册与发现

系统 SHALL 通过 Nacos 注册中心实现 Dubbo 服务的注册与发现（复用现有 Nacos 实例，地址与项目现有 `NACOS_ADDR` 一致）；Dubbo 协议 SHALL 使用 Triple（`tri`），每个服务使用 CLAUDE.md 端口分配表中规划的 Dubbo 端口（article 20893、comment 20894）。

#### Scenario: 服务注册到 Nacos

- **WHEN** article 服务（或 comment 服务）启动并暴露 RPC 服务
- **THEN** 该服务通过 Triple 协议、指定端口注册到 Nacos 注册中心，可通过 Nacos 控制台看到对应 Dubbo 服务实例

#### Scenario: 调用方自动发现被调方

- **WHEN** 调用方服务启动并持有对某 RPC 接口的引用
- **THEN** 调用方从 Nacos 发现被调方实例并建立连接，无需配置被调方地址

### Requirement: RPC 暴露与调用规范

被调方 SHALL 提供 RPC 接口的实现类并声明为 Dubbo 服务（使 Spring 容器能识别并暴露）；调用方 SHALL 通过注入的 RPC 接口引用发起调用；RPC 返回类型 SHALL 是可序列化的（基本类型或 Serializable DTO），雪花 ID 在 RPC 参数与返回值中 SHALL 保持 `Long` 原始类型。

#### Scenario: 被调方暴露 RPC 实现

- **WHEN** 被调方服务启动、RPC 实现类被 Spring 容器加载
- **THEN** 该 RPC 接口对注册中心可见，调用方可发起远程调用

#### Scenario: 调用方发起远程调用

- **WHEN** 调用方在业务逻辑中调用注入的 RPC 接口方法
- **THEN** 请求经 Dubbo 路由到被调方执行，返回值正确返回调用方

### Requirement: comment 校验文章存在

系统 SHALL 在 comment 服务发表评论前，通过 RPC 调用 article 服务校验目标文章是否存在；文章不存在时，SHALL 拒绝发表并返回业务错误。

#### Scenario: 文章存在时允许评论

- **WHEN** 用户对一篇已存在且可评论的文章发表评论
- **THEN** article 服务返回文章存在，comment 服务正常受理评论

#### Scenario: 文章不存在时拒绝评论

- **WHEN** 用户对一篇不存在的文章发表评论
- **THEN** comment 服务返回业务错误（文章不存在），评论不落库

### Requirement: article 聚合评论数

系统 SHALL 在 article 服务返回文章详情时，通过 RPC 调用 comment 服务获取该文章的评论数，并纳入详情响应中的 `comments` 字段；RPC 调用失败时 SHALL 不阻塞详情返回（评论数降级为 0 或上次缓存值）。

#### Scenario: 详情返回评论数

- **WHEN** 用户请求某篇文章详情，且 comment 服务正常
- **THEN** 详情响应中的 `comments` 字段为 comment 服务返回的真实评论数

#### Scenario: 评论数获取失败降级

- **WHEN** comment 服务不可用或 RPC 调用异常
- **THEN** 文章详情仍正常返回，`comments` 字段降级为 0，不因评论数获取失败而报错