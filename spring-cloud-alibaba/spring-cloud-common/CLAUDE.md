# CLAUDE.md — spring-cloud-common 公共模块聚合

> 本文档面向 AI 编码助手，用于在 `spring-cloud-common/` 目录下（或任意子模块下）工作时提供模块约束、技术栈版本、对外暴露能力与开发规范。
> 工作前**必须**先读取父目录的 [`spring-cloud-alibaba/CLAUDE.md`](../CLAUDE.md) 与仓库根 [`sca-fullstack-lab/CLAUDE.md`](../../CLAUDE.md) 了解全局规范。

---

## 1. 模块定位

`spring-cloud-common` 是整个项目的 **公共能力下沉层**，由 **16 个子模块**组成，每个子模块聚焦一个垂直能力（Redis、MyBatis、MQ、Sa-Token、Netty 等）。

> ⚠️ 原规划的 `spring-cloud-common-test` 子模块已废弃，从父 POM `<modules>` 中删除。当前实际只有 16 个子模块。

**核心设计原则**：
1. **单一职责**：每个 common 子模块只做一件事，不交叉依赖
2. **开箱即用**：所有子模块都通过 `@AutoConfiguration` 自动装配，业务方加依赖即可用
3. **依赖最小化**：common 子模块**禁止**依赖具体业务模块
4. **禁止循环依赖**：common 子模块之间如有依赖**必须**通过 `core` 间接解耦
5. **API 与实现分离**：注解、接口、DTO 放 `core`，实现放各能力模块

| 维度 | 值 |
|------|-----|
| 顶级包 | `com.xytang.common` |
| 父 POM | `com.xytang:spring-cloud-alibaba:1.0-SNAPSHOT` |
| 当前 artifactId | `spring-cloud-common` |
| packaging | `pom` |
| 子模块数量 | 16 |

---

## 2. 子模块结构

```
spring-cloud-common/
├── pom.xml                              packaging=pom，声明 16 个子模块
├── spring-cloud-common-core/             核心工具：异常/响应/常量/事件基类（完整代码）
├── spring-cloud-common-web/              Web 通用：全局异常/Trace-Id/密码编码器（完整代码）
├── spring-cloud-common-redis/            Redis 工具：序列化/RedisTemplate（部分代码）
├── spring-cloud-common-redisson/         Redisson：分布式锁注解（部分代码）
├── spring-cloud-common-mybatis/          MyBatis-Plus：分页/数据权限/雪花 ID（完整代码）
├── spring-cloud-common-datasource/       dynamic-datasource：多数据源切换（空壳）
├── spring-cloud-common-mq/               RabbitMQ：事件基类/幂等消费（部分代码）
├── spring-cloud-common-mongo/            MongoDB：配置/通用 DAO（空壳）
├── spring-cloud-common-es/               ElasticSearch：配置/索引管理（空壳）
├── spring-cloud-common-ai/               Spring AI：ChatClient/Advisor/VectorStore（空壳）
├── spring-cloud-common-satoken/          Sa-Token：StpInterface 实现（部分代码）
├── spring-cloud-common-security/         网关鉴权：过滤器/路由元数据（部分代码）
├── spring-cloud-common-log/              日志：@OperationLog 注解/AOP 切面（部分代码）
├── spring-cloud-common-swagger/          OpenAPI：springdoc 聚合/Knife4j（空壳）
├── spring-cloud-common-cache/            多级缓存：Caffeine + Redis（空壳）
└── spring-cloud-common-netty/            Netty：WebSocket Server/心跳/路由（空壳）
```

> ⚠️ 当前 common 子模块下**均未声明** `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 文件，自动装配机制尚未启用，业务方需手动 `@Import` 配置类。落地时需补充。

---

## 3. 子模块清单（含代码状态 + 链接）

| # | 子模块 | 代码状态 | 一句话职责 | 详见 |
|---|--------|---------|----------|------|
| 1 | spring-cloud-common-core | ✅ 完整 | 纯 POJO + 工具类，被所有模块依赖，无 Spring 依赖 | [CLAUDE.md](./spring-cloud-common-core/CLAUDE.md) |
| 2 | spring-cloud-common-web | ✅ 完整 | Spring MVC 全局配置：全局异常、Trace-Id、密码编码器（Argon2id） | [CLAUDE.md](./spring-cloud-common-web/CLAUDE.md) |
| 3 | spring-cloud-common-redis | 🟡 部分 | RedisTemplate 配置 + 序列化方案 | [CLAUDE.md](./spring-cloud-common-redis/CLAUDE.md) |
| 4 | spring-cloud-common-redisson | 🟡 部分 | `@DistributedLock` 注解 + AOP 切面 | [CLAUDE.md](./spring-cloud-common-redisson/CLAUDE.md) |
| 5 | spring-cloud-common-mybatis | ✅ 完整 | 拦截器链 + 数据权限（`@DataScope`）+ 自动填充 + RBAC 上下文 | [CLAUDE.md](./spring-cloud-common-mybatis/CLAUDE.md) |
| 6 | spring-cloud-common-datasource | ❌ 空壳 | dynamic-datasource 多数据源切换 + 国产库适配 + ShardingSphere 加密 | [CLAUDE.md](./spring-cloud-common-datasource/CLAUDE.md) |
| 7 | spring-cloud-common-mq | 🟡 部分 | `AbstractEventListener<T>` 幂等消费基类 + 事件配置 | [CLAUDE.md](./spring-cloud-common-mq/CLAUDE.md) |
| 8 | spring-cloud-common-mongo | ❌ 空壳 | MongoDB 配置 + 通用 DAO + 分页 | [CLAUDE.md](./spring-cloud-common-mongo/CLAUDE.md) |
| 9 | spring-cloud-common-es | ❌ 空壳 | ES 8 客户端 + 索引管理 + 批量操作 | [CLAUDE.md](./spring-cloud-common-es/CLAUDE.md) |
| 10 | spring-cloud-common-ai | ❌ 空壳 | Spring AI ChatClient + Advisor 链 + pgvector | [CLAUDE.md](./spring-cloud-common-ai/CLAUDE.md) |
| 11 | spring-cloud-common-satoken | 🟡 部分 | Sa-Token 配置 + `StpInterfaceImpl` 权限查询 | [CLAUDE.md](./spring-cloud-common-satoken/CLAUDE.md) |
| 12 | spring-cloud-common-security | 🟡 部分 | 网关鉴权过滤器 `AuthGatewayFilterFactory` | [CLAUDE.md](./spring-cloud-common-security/CLAUDE.md) |
| 13 | spring-cloud-common-log | 🟡 部分 | `@OperationLog` 注解 + AOP 切面 + MQ 异步发送 | [CLAUDE.md](./spring-cloud-common-log/CLAUDE.md) |
| 14 | spring-cloud-common-swagger | ❌ 空壳 | springdoc-openapi + Knife4j 配置 | [CLAUDE.md](./spring-cloud-common-swagger/CLAUDE.md) |
| 15 | spring-cloud-common-cache | ❌ 空壳 | Caffeine + Redis 多级缓存 + `@LayeredCache` 注解 | [CLAUDE.md](./spring-cloud-common-cache/CLAUDE.md) |
| 16 | spring-cloud-common-netty | ❌ 空壳 | Netty WebSocket Server + 心跳 + 路由 | [CLAUDE.md](./spring-cloud-common-netty/CLAUDE.md) |

> **代码状态说明**：
> - ✅ 完整：核心功能已实现
> - 🟡 部分：部分功能已实现，仍有 TODO
> - ❌ 空壳：仅 `pom.xml`，无任何 .java 代码

---

## 4. 技术栈版本矩阵

### 4.1 父 POM 已声明的版本（强制）

| 子模块 | 关键依赖 | 版本 |
|--------|----------|------|
| core | Hutool | 5.8.27 |
| core | Jackson | Spring Boot 3.5 管理 |
| web | spring-boot-starter-web | 3.5.0 |
| web | spring-boot-starter-validation | 3.5.0 |
| web | Jsoup | 1.17.2 |
| web | Bouncy Castle | 1.78.1 |
| redis | spring-boot-starter-data-redis | 3.5.0 |
| redisson | redisson-spring-boot-starter | 4.0.0 |
| mybatis | mybatis-plus-spring-boot3-starter | 3.5.9 |
| datasource | dynamic-datasource-spring-boot3-starter | 4.3.1 |
| datasource | shardingsphere-jdbc | 5.5.2 |
| mq | spring-boot-starter-amqp | 3.5.0 |
| mongo | spring-boot-starter-data-mongodb | 3.5.0 |
| es | spring-boot-starter-data-elasticsearch | 3.5.0 |
| ai | spring-ai-* | 1.1.0（**父 POM 未声明**，落地时补充） |
| satoken | sa-token-spring-boot3-starter | 1.44.0 |
| satoken | sa-token-redis-jackson | 1.44.0 |
| security | spring-cloud-starter-gateway | Spring Cloud 2025.0.0 管理 |
| log | spring-boot-starter-aop | 3.5.0 |
| swagger | springdoc-openapi-starter-webmvc-ui | 2.6.0 |
| swagger | knife4j-openapi3-jakarta-spring-boot-starter | 4.5.0 |
| cache | caffeine | 3.2.0 |
| netty | netty-all | 4.1.x（Spring Boot 管理） |

### 4.2 测试依赖

| 技术 | 版本 | 用途 |
|------|------|------|
| Testcontainers | 1.20.0 | 集成测试容器 |
| testcontainers-redis | 2.2.4 | Redis Testcontainer（社区版） |
| ArchUnit | 1.3.0 | 架构守护测试 |
| WireMock | 3.9.1 | HTTP Mock |

> 所有版本**必须**在父 POM 的 `<properties>` 声明，common 子模块**禁止**覆盖。

---

## 5. 公共能力 → 业务模块的依赖映射

| 业务模块 | 依赖的 common 子模块 |
|----------|---------------------|
| spring-cloud-gateway | security、swagger、core、redis |
| spring-cloud-auth | core、web、redis、redisson、satoken、mq、cache、log、swagger、mybatis |
| spring-cloud-system | core、web、mybatis、datasource、redis、redisson、satoken、mq、log、swagger、cache |
| spring-cloud-monitor | core、web、mybatis、redis、redisson、netty、mq、swagger |
| spring-cloud-workflow | core、web、mybatis、redis、satoken、mq、log、swagger、cache |
| spring-cloud-ai | core、web、ai、mongo、redis、satoken、mq、swagger |
| spring-cloud-message | core、web、netty、mq、redis、redisson、swagger |
| spring-cloud-search | core、web、es、mq、swagger |
| spring-cloud-file | core、web、redis、redisson、swagger |
| spring-cloud-log | core、web、mongo、mq、mybatis、swagger |
| spring-cloud-portal | core、web、mybatis、redis、es、swagger、cache |
| spring-cloud-job | core、web、redis、swagger |
| spring-cloud-report | core、web、datasource、swagger、satoken |

---

## 6. 开发规范

### 6.1 子模块命名规范

- artifactId：`spring-cloud-common-{能力名}`，如 `spring-cloud-common-redis`
- 顶级包：`com.xytang.common.{能力名}`，如 `com.xytang.common.redis`

### 6.2 自动装配规范

每个 common 子模块**必须**在 `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 中声明自动装配类：

```
com.xytang.common.redis.config.RedisAutoConfiguration
com.xytang.common.redis.config.RedissonAutoConfiguration
```

> ⚠️ **当前所有 common 子模块都未声明此文件**，自动装配机制尚未启用。落地时需补充。

### 6.3 注解化原则

- 公共能力**尽量**用注解暴露（如 `@DistributedLock`、`@OperationLog`）
- 注解**必须**有默认值，业务方零配置可用
- 注解**必须**支持 SpEL 表达式（如 `key = "'user:' + #id"`）

### 6.4 异常处理规范

- common 模块**禁止**吞掉异常，**必须**抛出 `BusinessException` 或子类
- common 模块的异常**必须**有清晰的 `ResultCode`，便于业务方区分
- 跨模块调用失败**必须**返回 `R<Void>` 而非 null

### 6.5 序列化规范

- Redis 缓存对象**必须**实现 `Serializable`，**必须**有 `serialVersionUID`
- MongoDB 文档**必须**用 `@Document` 注解，**禁止**裸 POJO
- ES 文档**必须**用 `@Document` + `@Field` 注解，类型显式声明

### 6.6 配置规范

- 所有配置**必须**支持环境变量注入：`${REDIS_HOST:127.0.0.1}`
- 默认值**必须**是开发环境友好的（如 `127.0.0.1`、`8080`）
- 生产环境配置**必须**在 Nacos 中覆盖
- 子模块内**无** `application.yml`，所有配置在 Nacos `spring-cloud-shared.yaml` 中

### 6.7 日志规范

- **必须**用 `@Slf4j`（Lombok），**禁止**手动 `LoggerFactory.getLogger(...)`
- **禁止**打印密码、Token、身份证号
- 关键操作**必须**用 `@OperationLog` 注解
- 慢操作（>500ms）**必须**打 WARN 日志

### 6.8 测试规范

- 每个注解**必须**有对应的切面测试
- 每个 AutoConfiguration**必须**有 `@EnabledAutoConfiguration` 测试
- 公共工具类**必须**有 100% 覆盖率测试
- 集成测试用 Testcontainers，**禁止**用 mock 容器

---

## 7. common 模块不允许的事项

1. ❌ 在 common 子模块依赖具体业务模块（如 `spring-cloud-system`）
2. ❌ 在 common 子模块依赖业务数据库表
3. ❌ 在 core 中加 Spring 依赖
4. ❌ 在 common 子模块中写 Controller（**例外**：security 可写网关过滤器）
5. ❌ 在 common 子模块中直接操作业务库
6. ❌ 在 common 子模块的配置类中读 Nacos 配置（用 `@Value` + `${VAR:default}`）
7. ❌ 在 common 子模块中使用 `System.out.println`
8. ❌ 在 common 子模块的代码中硬编码业务路径（如 `/api/system/users`）
9. ❌ 在 common 子模块中暴露 `R<T>` 之外的响应格式
10. ❌ common 子模块的 AutoConfiguration 直接暴露 `@Bean`，**必须**加 `@ConditionalOnXxx` 防止冲突

---

## 8. 子模块 POM 模板

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.xytang</groupId>
        <artifactId>spring-cloud-alibaba</artifactId>
        <version>1.0-SNAPSHOT</version>
        <relativePath>../../pom.xml</relativePath>
    </parent>

    <artifactId>spring-cloud-common-redis</artifactId>
    <packaging>jar</packaging>

    <dependencies>
        <!-- 内部依赖 -->
        <dependency>
            <groupId>com.xytang</groupId>
            <artifactId>spring-cloud-common-core</artifactId>
        </dependency>

        <!-- 第三方依赖（不指定版本，由父 POM 管理） -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>org.redisson</groupId>
            <artifactId>redisson-spring-boot-starter</artifactId>
        </dependency>
    </dependencies>
</project>
```

---

## 9. 红线（违反即拒绝）

1. ❌ 在 common 子模块覆盖父 POM 的依赖版本
2. ❌ 在 core 中加 Spring/MyBatis/Servlet 依赖
3. ❌ 在 common 中写 Controller（除 security 的网关过滤器）
4. ❌ common 子模块依赖具体业务模块
5. ❌ 在 common 中暴露 `R<T>` 之外的响应格式
6. ❌ 在 common 中用 `System.out.println`
7. ❌ 在 common 中硬编码业务路径
8. ❌ AutoConfiguration 不加 `@ConditionalOnXxx`（导致冲突）
9. ❌ 注解无默认值（业务方必须显式配置才能用）
10. ❌ Redis 缓存对象不实现 `Serializable`
