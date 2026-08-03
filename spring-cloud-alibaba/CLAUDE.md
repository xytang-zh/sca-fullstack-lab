# CLAUDE.md — spring-cloud-alibaba 后端聚合项目

> 本文档面向 AI 编码助手（Claude Code / Codex / Cursor），用于在 `spring-cloud-alibaba/` 目录下工作时提供统一的工程约束、技术栈版本、模块结构与开发规范。
> 任何 AI 在本目录（或任意子模块）下生成代码、配置、SQL、文档时，**必须**先读取本文件并严格遵守其中的规范。
> 工作前**必须**先读取仓库根 [`sca-fullstack-lab/CLAUDE.md`](../CLAUDE.md) 了解跨项目契约。

---

## 1. 项目定位

本项目是 `sca-fullstack-lab`（企业级一体化智能管理平台）的 **后端聚合工程**，基于 **Spring Cloud Alibaba** 微服务体系，包含 1 个网关 + 1 个认证中心 + 10 个业务服务 + 15 个公共模块 + 1 个自定义 Starter。

- **顶层 groupId**：`com.xytang`
- **顶层 artifactId**：`spring-cloud-alibaba`
- **当前 version**：`1.0-SNAPSHOT`
- **打包方式**：`pom`（父 POM）
- **JDK 版本**：21（兼容文档要求的 17+，建议保持 21 以使用最新语言特性）
- **编码**：UTF-8

---

## 2. 顶层模块结构

```
spring-cloud-alibaba/
├── pom.xml                              父 POM（packaging=pom）
├── src/                                 仓库级共享资源
│   ├── checkstyle.xml                   Checkstyle 规则（阿里规范 + 项目红线）
│   └── checkstyle-suppressions.xml      规则抑制
├── spring-cloud-common/                 公共模块（15 个子模块，packaging=pom）
│   ├── spring-cloud-common-core         核心工具：异常/响应/常量/枚举
│   ├── spring-cloud-common-web          Web 通用：全局异常/Trace-Id/密码编码器
│   ├── spring-cloud-common-redis        Redis 工具/序列化
│   ├── spring-cloud-common-redisson    分布式锁注解
│   ├── spring-cloud-common-mybatis     MyBatis-Plus 配置/数据权限/雪花 ID
│   ├── spring-cloud-common-datasource  dynamic-datasource 封装（空壳）
│   ├── spring-cloud-common-mq           RabbitMQ 配置/事件基类
│   ├── spring-cloud-common-mongo       MongoDB 配置（空壳）
│   ├── spring-cloud-common-es          ElasticSearch 配置（空壳）
│   ├── spring-cloud-common-satoken     Sa-Token 配置/StpInterface 实现
│   ├── spring-cloud-common-security    网关鉴权过滤器
│   ├── spring-cloud-common-log         日志切面/@OperationLog 注解
│   ├── spring-cloud-common-swagger     springdoc-openapi 配置（空壳）
│   ├── spring-cloud-common-cache       Caffeine + Redis 多级缓存（空壳）
│   └── spring-cloud-common-netty       Netty Server/WebSocket 协议（空壳）
├── spring-cloud-gateway/                网关服务（端口 8080）
├── spring-cloud-auth/                   认证中心（端口 8081）
├── spring-cloud-services/               业务服务聚合（10 个微服务，packaging=pom）
│   ├── spring-cloud-system              系统管理/RBAC 核心（8082）
│   ├── spring-cloud-monitor             服务器监控（8083）
│   ├── spring-cloud-message             消息中心（8086）
│   ├── spring-cloud-search              全文检索（8087）
│   ├── spring-cloud-file                文件服务（8088）
│   ├── spring-cloud-log                 日志服务（8089）
│   ├── spring-cloud-portal              公开门户（8090）
│   ├── spring-cloud-job                 定时任务执行器（8091），可以用来定时保存博客到冷存表中，防止数据丢失
│   ├── spring-cloud-article             博客文章/互动 ★新增（8093）
│   └── spring-cloud-comment             博客评论/审核 ★新增（8094）
└── spring-cloud-starters/               自定义 Starter 聚合（packaging=pom）
    └── spring-cloud-starter-monitor-agent       监控 Agent
```

> ⚠️ `spring-cloud-test/` 与 `spring-cloud-common/spring-cloud-common-test/` 已废弃，从父 POM `<modules>` 中删除。

---

## 3. 技术栈版本矩阵（强制约束）

所有子模块的依赖版本**必须**在父 POM 的 `<properties>` 中统一声明，子模块**不允许**自行指定版本。

### 3.1 已声明的依赖版本（与父 POM 一致）

| 类别 | 技术 | 版本 | 用途 |
|------|------|------|------|
| 基座 | Spring Boot | 3.5.0 | 微服务基础 |
| 微服务规范 | Spring Cloud | 2025.0.0 | 微服务规范 |
| 微服务套件 | Spring Cloud Alibaba | 2025.0.0.0 | Nacos/Sentinel/Seata/Dubbo 集成 |
| 认证 | Sa-Token | 1.44.0 | 登录/权限/SSO/OAuth2 |
| ORM | MyBatis-Plus | 3.5.9 | ORM 增强 |
| 多数据源 | dynamic-datasource | 4.3.1 | 多源切换 |
| 分库分表 | Apache ShardingSphere | 5.5.2 | 数据分片 |
| 分布式锁 | Redisson | 4.0.0 | 分布式锁/限流 |
| 本地缓存 | Caffeine | 3.2.0 | L1 缓存 |
| 工具集 | Hutool | 5.8.27 | 通用工具 |
| HTML 解析 | Jsoup | 1.17.2 | XSS 过滤 |
| 密码哈希 | Bouncy Castle | 1.78.1 | Argon2id 算法 |
| API 文档 | springdoc-openapi | 2.6.0 | OpenAPI 3 |
| API 文档增强 | Knife4j | 4.5.0 | 增强 UI |
| 测试容器 | Testcontainers | 1.20.0 | 集成测试 |
| 测试容器 Redis | testcontainers-redis | 2.2.4 | Redis Testcontainer |
| 架构守护 | ArchUnit | 1.3.0 | 架构规则测试 |
| HTTP Mock | WireMock | 3.9.1 | 集成测试 |
| Maven 插件 | maven-compiler-plugin | 3.13.0 | 编译 |
| Maven 插件 | maven-checkstyle-plugin | 3.5.0 | 阿里规范验证 |
| JDK | OpenJDK | 21 | 语言版本 |

### 3.2 计划引入的依赖版本（父 POM 未声明，落地时补充）

| 技术 | 计划版本 | 用途 | 落地时机 |
|------|---------|------|---------|
| commonmark-java | 0.21.x | Markdown → HTML 渲染 | `spring-cloud-article` 落地时 |
| sensitive-word | 0.x | 评论敏感词过滤 | `spring-cloud-comment` 落地时 |
| XXL-JOB | 3.5.0 | 分布式调度 | `spring-cloud-job` 落地时 |
| Nacos | 2.4+ | 注册配置中心 | 基础设施部署 |
| Sentinel | 1.8.8+ | 限流/熔断 | Spring Cloud Alibaba 自带 |
| Seata | 2.2+ | 分布式事务 | 同上 |
| Dubbo | 3.3+ | 内部 RPC | 同上 |
| MySQL | 8.4 LTS | 业务主库 | 基础设施部署 |
| 人大金仓 KingbaseES | V8 R6 | 国产化适配 | 国产化落地 |
| 达梦 DM8 | DM8 | 国产化适配 | 国产化落地 |
| MinIO | latest stable | 对象存储 | 基础设施部署 |
| Netty | 4.1.x | WebSocket | `spring-cloud-common-netty` 落地时 |
| TDengine | 3.3+ | 服务器监控时序库 | `spring-cloud-monitor` 落地时 |
| RabbitMQ | 3.13+ | 消息队列 | 基础设施部署 |
| Redis | 7.4+ | 分布式缓存 | 基础设施部署 |
| ElasticSearch | 8.15+ | 全文检索 | 基础设施部署 |
| MongoDB | 7.0+ | 日志存储 | 基础设施部署 |
| Prometheus | 2.55+ | 指标采集 | 监控部署 |
| Grafana | 11.x | 大盘 | 同上 |

> 版本升级**必须**通过修改父 POM 的 `<properties>`，**禁止**在子模块 POM 中覆盖。

---

## 4. 父 POM 配置规范

### 4.1 properties 必须声明

当前父 POM（`spring-cloud-alibaba/pom.xml`）已声明以下 properties：

```xml
<properties>
    <java.version>21</java.version>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>
    <maven.compiler.release>21</maven.compiler.release>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>

    <!-- 基座与微服务 -->
    <spring-boot.version>3.5.0</spring-boot.version>
    <spring-cloud.version>2025.0.0</spring-cloud.version>
    <spring-cloud-alibaba.version>2025.0.0.0</spring-cloud-alibaba.version>

    <!-- 认证与权限 -->
    <sa-token.version>1.44.0</sa-token.version>

    <!-- ORM / 数据 -->
    <mybatis-plus.version>3.5.9</mybatis-plus.version>
    <dynamic-datasource.version>4.3.1</dynamic-datasource.version>
    <shardingsphere.version>5.5.2</shardingsphere.version>

    <!-- 缓存与锁 -->
    <redisson.version>4.0.0</redisson.version>
    <caffeine.version>3.2.0</caffeine.version>

    <!-- 工具集 -->
    <hutool.version>5.8.27</hutool.version>
    <jsoup.version>1.17.2</jsoup.version>
    <bouncycastle.version>1.78.1</bouncycastle.version>

    <!-- API 文档 -->
    <springdoc.version>2.6.0</springdoc.version>
    <knife4j.version>4.5.0</knife4j.version>

    <!-- 测试 -->
    <testcontainers.version>1.20.0</testcontainers.version>
    <testcontainers-redis.version>2.2.4</testcontainers-redis.version>
    <archunit.version>1.3.0</archunit.version>
    <wiremock.version>3.9.1</wiremock.version>

    <!-- Maven 插件 -->
    <maven-compiler-plugin.version>3.13.0</maven-compiler-plugin.version>
    <maven-checkstyle-plugin.version>3.5.0</maven-checkstyle-plugin.version>
    <versions-maven-plugin.version>2.17.1</versions-maven-plugin.version>
</properties>
```

### 4.2 dependencyManagement 必须包含的 BOM

```xml
<dependencyManagement>
    <dependencies>
        <!-- Spring Boot BOM -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>${spring-boot.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
        <!-- Spring Cloud BOM -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>${spring-cloud.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
        <!-- Spring Cloud Alibaba BOM -->
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-alibaba-dependencies</artifactId>
            <version>${spring-cloud-alibaba.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
        <!-- Sa-Token BOM -->
        <dependency>
            <groupId>cn.dev33</groupId>
            <artifactId>sa-token-bom</artifactId>
            <version>${sa-token.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
        <!-- MyBatis-Plus BOM -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-bom</artifactId>
            <version>${mybatis-plus.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
        <!-- Hutool BOM -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-bom</artifactId>
            <version>${hutool.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
        <!-- Testcontainers BOM -->
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>testcontainers-bom</artifactId>
            <version>${testcontainers.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### 4.3 所有子模块 POM 必须声明父 POM

```xml
<parent>
    <groupId>com.xytang</groupId>
    <artifactId>spring-cloud-alibaba</artifactId>
    <version>1.0-SNAPSHOT</version>
    <relativePath>../pom.xml</relativePath>
</parent>
```

### 4.4 Maven Checkstyle 插件（强制）

父 POM 已绑定 `maven-checkstyle-plugin` 到 `validate` 阶段，`failOnViolation=true`：

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-checkstyle-plugin</artifactId>
    <version>${maven-checkstyle-plugin.version}</version>
    <configuration>
        <configLocation>${maven.multiModuleProjectDirectory}/src/checkstyle.xml</configLocation>
        <consoleOutput>true</consoleOutput>
        <failOnViolation>true</failOnViolation>
        <includeTestSourceDirectory>true</includeTestSourceDirectory>
    </configuration>
    <executions>
        <execution>
            <id>checkstyle-validate</id>
            <phase>validate</phase>
            <goals><goal>check</goal></goals>
        </execution>
    </executions>
</plugin>
```

> 任何 `mvn validate` 都会跑 checkstyle，违规直接 build 失败。规则见 `src/checkstyle.xml`，覆盖阿里巴巴 Java 开发规范（泰山版）+ 项目红线。

---

## 5. 包命名规范（强制约束）

```
com.xytang
  ├── common          公共模块根包
  │   ├── core        spring-cloud-common-core
  │   ├── web         spring-cloud-common-web
  │   ├── redis       spring-cloud-common-redis
  │   ├── redisson    spring-cloud-common-redisson
  │   ├── mybatis     spring-cloud-common-mybatis
  │   ├── datasource  spring-cloud-common-datasource
  │   ├── mq          spring-cloud-common-mq
  │   ├── mongo       spring-cloud-common-mongo
  │   ├── es          spring-cloud-common-es
  │   ├── satoken     spring-cloud-common-satoken
  │   ├── security    spring-cloud-common-security
  │   ├── log         spring-cloud-common-log
  │   ├── swagger     spring-cloud-common-swagger
  │   ├── cache       spring-cloud-common-cache
  │   └── netty       spring-cloud-common-netty
  ├── gateway         网关
  ├── auth            认证中心
  ├── system          系统管理
  ├── monitor         监控
  ├── message         消息
  ├── search          搜索
  ├── file            文件
  ├── log             日志
  ├── portal          公开门户
  ├── job             定时任务
  ├── article         博客文章
  ├── comment         博客评论
  └── starter         自定义 Starter 根包
      └── monitoragent Monitor Agent
```

> 命名风格：**全小写、单词之间无分隔符**（不用 `com.xy.tang` 或 `com.xytang.platform`）。

---

## 6. 类命名规范

| 类型 | 后缀/前缀 | 示例                             |
|------|----------|--------------------------------|
| 启动类 | `Application` 后缀 | `SpringCloudSystemApplication` |
| Controller | `Controller` 后缀 | `UserController`               |
| Service 接口 | `Service` 后缀 | `UserService`                  |
| Service 实现 | `ServiceImpl` 后缀 | `UserServiceImpl`              |
| Mapper | `Mapper` 后缀 | `UserMapper`                   |
| Entity | 无后缀 | `User`                         |
| DTO（入参） | `DTO` 后缀 | `UserCreateDTO`                |
| VO（出参） | `VO` 后缀 | `UserVO`                       |
| Query（查询） | `Query` 后缀 | `UserPageQuery`                |
| Config | `Config` 后缀 | `RedisConfig`                  |
| Exception | `Exception` 后缀 | `BusinessException`            |
| Listener | `Listener` 后缀 | `UserLoginListener`            |
| Dubbo Provider | `RpcProvider` 后缀 | `UserRpcProvider`              |
| Dubbo Consumer | `RpcConsumer` 后缀 | `UserRpcConsumer`              |
| 注解 | `Annotation` 后缀 | `@OperationLog`                |
| 切面 | `Aspect` 后缀 | `OperationLogAspect`           |
| AutoConfiguration | `AutoConfiguration` 后缀 | `SaTokenAutoConfiguration`     |

---

## 7. 服务端口分配表

| 服务 | HTTP 端口 | Dubbo 端口 | WebSocket | XXL-JOB 执行器 |
|------|----------|-----------|-----------|---------------|
| spring-cloud-gateway | 8080 | - | - | - |
| spring-cloud-auth | 8081 | 20881 | - | 9999 |
| spring-cloud-system | 8082 | 20882 | - | 10000 |
| spring-cloud-monitor | 8083 | 20883 | 9090 | 10001 |
| spring-cloud-message | 8086 | 20886 | 9091 | 10004 |
| spring-cloud-search | 8087 | 20887 | - | 10005 |
| spring-cloud-file | 8088 | 20888 | - | 10006 |
| spring-cloud-log | 8089 | 20889 | - | 10007 |
| spring-cloud-portal | 8090 | 20890 | - | 10008 |
| spring-cloud-job | 8091 | 20891 | - | 10009 |
| spring-cloud-article | 8093 | 20893 | - | 10011 |
| spring-cloud-comment | 8094 | 20894 | - | 10012 |

> **XXL-JOB Admin** 独立部署，端口 8099（避免与 Gateway 8080 冲突）。

---

## 8. RESTful API 强制规范（所有 HTTP 接口必须遵守）

### 8.1 URI 设计

- 资源名**必须**用复数名词、全小写、短横线分隔：`/api/system/users`、`/api/workflow/instances`
- 路径**必须**包含 `/api/{服务名}/` 前缀，由 Gateway 通过 `StripPrefix=2` 剥离
- 版本化通过 Header `X-API-Version: 1` 实现，**禁止**在 URI 中加 `/v1/`
- 业务动作（非 CRUD）使用动词子资源：`POST /api/system/users/{id}/disable`、`POST /api/workflow/tasks/{id}/approve`

### 8.2 HTTP 方法语义

| 方法 | 语义 | 示例 | 是否幂等 |
|------|------|------|----------|
| GET | 查询（无副作用） | `GET /api/system/users/{id}` | ✅ |
| POST | 新增（非幂等） | `POST /api/system/users` | ❌ |
| PUT | 全量更新（幂等） | `PUT /api/system/users/{id}` | ✅ |
| PATCH | 部分更新（幂等） | `PATCH /api/system/users/{id}/password` | ✅ |
| DELETE | 删除（幂等） | `DELETE /api/system/users/{id}` | ✅ |

> **禁止**用 GET 执行写操作，**禁止**用 POST 同时承担新增和更新（更新必须用 PUT/PATCH）。

### 8.3 统一响应格式

所有 HTTP 接口**必须**返回 `R<T>` 包装类（来自 `spring-cloud-common-core`）：

```json
{
  "code": 200,
  "msg": "success",
  "data": { ... },
  "timestamp": 1722470400000
}
```

| code | 含义 |
|------|------|
| 200 | 成功 |
| 400 | 参数错误 |
| 401 | 未登录 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 409 | 资源冲突（如账号已存在） |
| 429 | 限流 |
| 500 | 服务器内部错误 |
| 503 | 服务不可用（熔断） |

### 8.4 分页查询规范

- 入参：`pageNum`（从 1 开始）、`pageSize`（默认 10，最大 100）、`orderBy`（字段名 + ASC/DESC）
- 出参：`PageVO<T>`，包含 `list`、`total`、`pageNum`、`pageSize`、`pages`

```
GET /api/system/users?pageNum=1&pageSize=10&orderBy=createTime DESC&keyword=admin
```

### 8.5 状态码使用

- **禁止**用 200 返回业务错误（如"用户名已存在"），必须返回对应 HTTP 状态码（409 Conflict）
- 异常**必须**由 `spring-cloud-common-web` 的 `GlobalExceptionHandler` 统一捕获
- 业务异常类**必须**继承 `BusinessException`（来自 `spring-cloud-common-core`）

### 8.6 接口文档

- 所有 Controller **必须**使用 `@Tag`、`@Operation` 注解（springdoc-openapi）
- 所有 DTO/VO 字段**必须**使用 `@Schema` 注解描述
- Knife4j 聚合文档地址：`http://localhost:8080/doc.html`（Gateway 端聚合）

---

## 9. 前后端契约（跨项目）

### 9.1 雪花 ID Long → String 序列化

后端 `spring-cloud-common-web` 计划在 `JacksonConfig` 中把所有 `Long` 类型序列化为 String，避免前端 JS Number 精度丢失（最大安全整数 `2^53 - 1`）。

**前端约定**：所有 ID 字段（`userId`、`orderId`、`id` 等）**必须**用 TypeScript `string` 类型接收，**禁止**用 `number`。

### 9.2 HTTP 头透传

| 头 | 方向 | 用途 |
|----|------|------|
| `Authorization: Bearer {token}` | 前端 → 网关 | Sa-Token 访问令牌 |
| `X-Login-Id` | 网关 → 下游服务 | 透传当前登录用户 ID（网关鉴权后写入） |
| `X-Trace-Id` | 网关 → 下游服务 | 全链路追踪 ID（UUID） |
| `X-Gray-Version` | 前端 → 网关（可选） | 灰度路由标识 |

**禁止**：网关把原始 Token 透传到下游服务（只透传 `X-Login-Id`）。

### 9.3 前后端协作

- 前端 axios 响应拦截器**必须**根据 `code !== 200` 显示 `msg` 错误提示
- 前端 ID 字段用 `string` 接收，避免精度丢失
- CORS 白名单：开发 `http://localhost:5173`，生产 `https://*.example.com`

---

## 10. 服务间通信规范

### 10.1 同步调用 — Dubbo

| 场景 | 调用方 | 被调方 | 方法 |
|------|--------|--------|------|
| 文件元数据 | portal | file | `FileService.getMeta` |
| 文章索引同步 | article | search | `SearchService.syncArticleIndex` |
| 文章存在校验 | comment | article | `ArticleService.existsById` |

> Dubbo 接口定义在 `spring-cloud-common-core` 的 `rpc` 包（计划），由被调方实现 `*RpcProvider`。

### 10.2 异步事件 — RabbitMQ

| 事件 | Exchange | 生产者 | 消费者 |
|------|----------|--------|--------|
| 用户注册 | `user.register` | auth | message、log |
| 告警 | `alert.trigger` | monitor | message |
| 评论通知 | `comment.created` | comment | message |
| 操作日志 | `log.operation` | 所有 | log |

> 事件基类 `BaseEvent` 在 `spring-cloud-common-core`，所有事件**必须**继承它。事件 Listener **必须**继承 `AbstractEventListener<T>`（来自 `spring-cloud-common-mq`）以实现幂等消费。

### 10.3 实时推送 — WebSocket

| 端点 | 服务 | 用途 |
|------|------|------|
| `/ws/monitor/{userId}` | monitor | 实时指标推送 |
| `/ws/message/{userId}` | message | 站内信/任务提醒 |

---

## 11. 配置管理（Nacos）

### 11.1 配置文件命名

```
nacos:/
├── spring-cloud-shared.yaml          所有服务共享（Redis、MQ、Sa-Token）
├── spring-cloud-gateway.yaml
├── spring-cloud-auth.yaml
├── spring-cloud-system.yaml
├── spring-cloud-system-dev.yaml     dev 环境
├── spring-cloud-system-prod.yaml    prod 环境
└── ...
```

### 11.2 bootstrap.yml 必须包含

```yaml
spring:
  application:
    name: spring-cloud-system
  profiles:
    active: dev
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_ADDR:127.0.0.1:8848}
        namespace: ${NACOS_NAMESPACE:public}
      config:
        server-addr: ${NACOS_ADDR:127.0.0.1:8848}
        file-extension: yaml
        shared-configs:
          - data-id: spring-cloud-shared.yaml
            refresh: true
```

### 11.3 动态刷新

- 配置变更需要实时生效的字段**必须**用 `@RefreshScope` 注解
- 业务配置（如阈值、限流规则）**禁止**硬编码

---

## 12. 开发规范

### 12.1 编码规范（阿里巴巴 Java 开发规范 + 项目强制）

1. **遵循阿里巴巴 Java 开发规范（泰山版）**
2. **强制规则由 `src/checkstyle.xml` 落实**，绑定到 Maven `validate` 阶段，`failOnViolation=true`
3. **强制规则清单**：
   - `if`/`else`/`for`/`while`/`do` **必须**加大括号，即使只有一句
   - 缩进 4 空格，**禁止** Tab
   - 行宽 ≤ 120 字符
   - 方法 ≤ 150 行，参数 ≤ 7 个
   - if 嵌套深度 ≤ 3，try 嵌套深度 ≤ 2
   - **禁止**魔法数字（除 -1/0/1/2）
   - 常量在 `equals` 左侧避免空指针
   - 一个 `.java` 文件只能有一个顶层类
   - 包/import/类/方法之间必须有空行
   - **禁止** `import xxx.*`
   - **禁止** `sun.*`
   - **禁止**抛 `RuntimeException`/`Error`（必须具体业务异常）
   - **禁止** catch `Exception`/`Throwable`（必须精确捕获）
   - **禁止** `System.out/err.println`
   - **禁止** `e.printStackTrace()`
   - **禁止** `@Autowired` 字段注入（必须构造器注入）
   - **禁止** public 字段（除 final）

### 12.2 异常处理规范

1. 业务异常**必须**继承 `BusinessException`，包含 `code` 和 `msg`
2. **禁止**用 `try-catch` 吞掉异常（必须有日志或重新抛出）
3. **禁止**用 `throw new RuntimeException("xxx")`，必须用具体业务异常
4. 边界校验**必须**用 `@Validated` + Hibernate Validator，**禁止**手动 if-else 校验

### 12.3 并发规范

1. 共享可变状态**必须**用 `@DistributedLock` 注解（来自 `spring-cloud-common-redisson`）
2. **禁止**用 `synchronized` 跨 JVM 同步
3. 线程池**必须**通过 `ThreadPoolTaskExecutor` 显式配置，**禁止**用 `Executors.newXxx`（避免 OOM）
4. 异步任务**必须**用 `@Async` + 显式线程池

### 12.4 缓存规范

1. 缓存 Key**必须**以 `spring-cloud:{service}:{biz}:{id}` 格式，避免冲突
2. 缓存 TTL**必须**加 ±10% 随机数，防止雪崩
3. 热点数据**必须**用 `@LayeredCache`（计划在 `spring-cloud-common-cache`）多级缓存
4. 缓存穿透用空值缓存或布隆过滤器，**禁止**直接打到 DB

### 12.5 事务规范

1. `@Transactional` **必须**标注在 Service 方法上，**禁止**标注在 Controller 上
2. 跨数据源事务**必须**用 `@DSTransactional`（dynamic-datasource）
3. 跨服务事务**必须**用 `@GlobalTransactional`（Seata AT）
4. 长事务**禁止**用声明式事务，必须拆分为多个小事务 + 消息补偿

### 12.6 安全规范

1. **禁止**在日志中打印密码、Token、身份证号
2. SQL **必须**参数化查询（MyBatis-Plus 自动），**禁止**字符串拼接 SQL
3. 用户输入**必须**经过 XSS 过滤，富文本用 Jsoup
4. 接口**必须**加 `@SaCheckPermission` 或 `@SaCheckRole` 注解
5. 敏感字段（手机号、身份证）入库前**必须**加密（ShardingSphere 加密或 AES）
6. 密码哈希**必须**用 Argon2id（Bouncy Castle），**禁止**用 MD5/SHA1/BCrypt

---

## 13. Git 提交规范

### 13.1 分支策略

采用 Trunk-Based：
- `main` — 主干（始终可发布）
- `feature/{模块}-{功能}` — 功能分支，如 `feature/auth-sso`
- `fix/{模块}-{问题}` — 修复分支
- `release/{版本号}` — 发布分支

### 13.2 Conventional Commits

```
<type>(<scope>): <subject>

<body>

<footer>
```

| type | 用途 |
|------|------|
| feat | 新功能 |
| fix | 修复 bug |
| docs | 文档变更 |
| style | 代码格式（不影响逻辑） |
| refactor | 重构 |
| perf | 性能优化 |
| test | 测试 |
| chore | 构建/工具 |
| ci | CI 配置 |
| build | 构建系统或外部依赖 |

示例：`feat(auth): 实现 SSO 模式二授权码颁发与校验`

---

## 14. 常用命令

```bash
# 编译整个聚合工程
mvn clean install -DskipTests

# 编译并测试单个模块
mvn clean test -pl spring-cloud-auth -am

# 启动单个服务（开发模式）
mvn spring-boot:run -pl spring-cloud-auth -Dspring-boot.run.profiles=dev

# 仅跑 checkstyle（阿里规范验证）
mvn checkstyle:check -pl spring-cloud-auth

# 查看依赖树
mvn dependency:tree -pl spring-cloud-system

# 生成可执行 JAR
mvn clean package -pl spring-cloud-system -am -DskipTests

# 启动基础设施容器
docker compose -f docker/compose/docker-compose.infra.yml up -d
```

---

## 15. 子模块 CLAUDE.md 索引

每个子模块都有自己的 `CLAUDE.md`，提供更细粒度的约束：

### 15.1 一级子模块

- [`spring-cloud-common/CLAUDE.md`](./spring-cloud-common/CLAUDE.md) — 公共模块聚合（16 个子模块）
- [`spring-cloud-gateway/CLAUDE.md`](./spring-cloud-gateway/CLAUDE.md) — 网关
- [`spring-cloud-auth/CLAUDE.md`](./spring-cloud-auth/CLAUDE.md) — 认证中心
- [`spring-cloud-services/CLAUDE.md`](./spring-cloud-services/CLAUDE.md) — 业务服务聚合
- [`spring-cloud-starters/CLAUDE.md`](./spring-cloud-starters/CLAUDE.md) — 自定义 Starter

### 15.2 common 子模块（16 个）

- [`spring-cloud-common-core/CLAUDE.md`](./spring-cloud-common/spring-cloud-common-core/CLAUDE.md) — 核心工具
- [`spring-cloud-common-web/CLAUDE.md`](./spring-cloud-common/spring-cloud-common-web/CLAUDE.md) — Web 通用
- [`spring-cloud-common-redis/CLAUDE.md`](./spring-cloud-common/spring-cloud-common-redis/CLAUDE.md) — Redis 工具
- [`spring-cloud-common-redisson/CLAUDE.md`](./spring-cloud-common/spring-cloud-common-redisson/CLAUDE.md) — 分布式锁
- [`spring-cloud-common-mybatis/CLAUDE.md`](./spring-cloud-common/spring-cloud-common-mybatis/CLAUDE.md) — MyBatis-Plus
- [`spring-cloud-common-datasource/CLAUDE.md`](./spring-cloud-common/spring-cloud-common-datasource/CLAUDE.md) — 多数据源（空壳）
- [`spring-cloud-common-mq/CLAUDE.md`](./spring-cloud-common/spring-cloud-common-mq/CLAUDE.md) — 消息队列
- [`spring-cloud-common-mongo/CLAUDE.md`](./spring-cloud-common/spring-cloud-common-mongo/CLAUDE.md) — MongoDB（空壳）
- [`spring-cloud-common-es/CLAUDE.md`](./spring-cloud-common/spring-cloud-common-es/CLAUDE.md) — ElasticSearch（空壳）
- [`spring-cloud-common-satoken/CLAUDE.md`](./spring-cloud-common/spring-cloud-common-satoken/CLAUDE.md) — Sa-Token 集成
- [`spring-cloud-common-security/CLAUDE.md`](./spring-cloud-common/spring-cloud-common-security/CLAUDE.md) — 网关鉴权
- [`spring-cloud-common-log/CLAUDE.md`](./spring-cloud-common/spring-cloud-common-log/CLAUDE.md) — 日志切面
- [`spring-cloud-common-swagger/CLAUDE.md`](./spring-cloud-common/spring-cloud-common-swagger/CLAUDE.md) — OpenAPI 文档（空壳）
- [`spring-cloud-common-cache/CLAUDE.md`](./spring-cloud-common/spring-cloud-common-cache/CLAUDE.md) — 多级缓存（空壳）
- [`spring-cloud-common-netty/CLAUDE.md`](./spring-cloud-common/spring-cloud-common-netty/CLAUDE.md) — WebSocket（空壳）

> **AI 在任意子模块下工作时，必须先读取仓库根 `sca-fullstack-lab/CLAUDE.md`，再读取本文件，再读取对应子模块的 CLAUDE.md。**

---

## 16. 红线（违反即拒绝）

1. ❌ 在子模块 POM 中覆盖父 POM 的依赖版本
2. ❌ 用 `@Autowired` 字段注入（必须用 `@RequiredArgsConstructor` 构造器注入）
3. ❌ `if`/`else`/`for`/`while` 不加大括号（即使只有一句）
4. ❌ Controller 直接操作 DB（必须经 Service → Mapper）
5. ❌ 非 RESTful API（如 `POST /api/getUser?id=1`）
6. ❌ 用 GET 执行写操作
7. ❌ 在日志/响应中泄露密码、Token、身份证号
8. ❌ 用 `System.out.println` / `e.printStackTrace()`
9. ❌ 用 `throw new RuntimeException(...)` 而非具体业务异常
10. ❌ 用 `catch (Exception e)` 而非具体异常类型
11. ❌ 在 Service 直接 `new Thread(...)` / `Executors.newCachedThreadPool()`
12. ❌ SQL 字符串拼接（SQL 注入风险）
13. ❌ 用 BCrypt 存密码（必须 Argon2id）
14. ❌ 用 MySQL 私有函数（`IFNULL`/`CONCAT`/`DATE_FORMAT`，必须用标准 SQL）
