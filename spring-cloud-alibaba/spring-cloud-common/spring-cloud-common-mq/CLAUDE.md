# CLAUDE.md — spring-cloud-common-mq RabbitMQ 事件总线

> 本文档面向 AI 编码助手，用于在 `spring-cloud-common-mq/` 目录下工作时提供模块约束、功能清单与开发规范。
> 工作前**必须**先读取父目录的 [`spring-cloud-common/CLAUDE.md`](../CLAUDE.md) 与 [`spring-cloud-alibaba/CLAUDE.md`](../../CLAUDE.md) 了解全局规范。

---

## 1. 模块定位

`spring-cloud-common-mq` 基于 Spring AMQP 提供统一事件基类、幂等消费、死信队列，是项目**异步事件总线**的核心。

**核心设计原则**：
1. **幂等消费**：所有 Listener 必须继承 `AbstractEventListener<T>`，自动用 Redis 去重（eventId）
2. **统一事件模型**：所有事件必须继承 `BaseEvent`，携带 `eventId`/`eventType`/`source`/`timestamp`
3. **死信兜底**：消费失败重试 3 次进死信队列，**禁止**消息丢失

| 维度 | 值 |
|------|-----|
| 顶级包 | `com.xytang.common.mq` |
| 父 POM | `com.xytang:spring-cloud-alibaba:1.0-SNAPSHOT` |
| 当前 artifactId | `spring-cloud-common-mq` |
| packaging | `jar` |
| 是否有代码 | 🟡 部分（`AbstractEventListener` + `EventConfig`） |

---

## 2. 目录结构

```
spring-cloud-common-mq/
├── pom.xml
└── src/
    └── main/
        └── java/com/xytang/common/mq/
            ├── listener/
            │   └── AbstractEventListener.java
            └── config/
                └── EventConfig.java
```

---

## 3. 包结构

| 包 | 职责 |
|----|------|
| `com.xytang.common.mq.listener` | 监听器基类：`AbstractEventListener<T>` |
| `com.xytang.common.mq.config` | 事件配置：`EventConfig`（重试/死信/序列化） |

> 计划新增包：`base`（`BaseEvent` 已在 `common-core`）、`producer`（`EventPublisher`）、`annotation`（`@EventListener`）、`interceptor`（链路追踪）、`constant`（`MqExchange`）。当前未实现。

> ⚠️ 注意：`BaseEvent` 已在 `spring-cloud-common-core` 的 `event` 包定义，本模块**不**重复定义。

---

## 4. POM 依赖模板

```xml
<dependencies>
    <!-- 内部依赖 -->
    <dependency>
        <groupId>com.xytang</groupId>
        <artifactId>spring-cloud-common-core</artifactId>
    </dependency>

    <!-- 内部依赖：Redis（用于幂等去重） -->
    <dependency>
        <groupId>com.xytang</groupId>
        <artifactId>spring-cloud-common-redis</artifactId>
    </dependency>

    <!-- Spring AMQP -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-amqp</artifactId>
    </dependency>
</dependencies>
```

---

## 5. 技术栈选型

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring AMQP | Spring Boot 3.5.0 管理 | RabbitMQ 客户端 |
| Jackson | Spring Boot 管理 | 消息序列化 |
| Redis（内部模块） | Spring Data Redis | 幂等去重 |
| Lombok | 父 POM 全局声明 | 注解简化 |

---

## 6. 功能清单

### 6.1 幂等消费基类 `AbstractEventListener<T>`（已实现）

- **职责**：所有 MQ 监听器的父类，自动用 Redis 去重
- **泛型约束**：`T extends BaseEvent`
- **去重机制**：
  - Redis Key：`mq:consumed:{eventId}`，TTL 24h
  - 收到消息先 `SETNX`，已存在则跳过
- **重试机制**：消费抛异常，Spring AMQP 自动重试 3 次（间隔 1s/3s/5s）
- **死信队列**：重试 3 次仍失败，转发到 `dead.queue`
- **业务方实现**：继承本类，重写 `doConsume(T event)` 方法
- **实现技术**：抽象类 + Template Method 模式 + Redis `setIfAbsent`

### 6.2 事件配置 `EventConfig`（已实现）

- **职责**：配置 RabbitMQ 序列化、重试、死信队列
- **序列化**：Jackson `Jackson2JsonMessageConverter`，消息体为 JSON
- **重试**：`RetryTemplate` 初始 1s、倍数 2、最大间隔 5s、最多 3 次
- **死信**：默认 `dead.queue`，可通过配置覆盖
- **实现技术**：`@Configuration` + `@Bean`

### 6.3 计划功能（TODO）

| 功能 | 状态 | 说明 |
|------|------|------|
| `EventPublisher` 统一发送入口 | 未实现 | 业务方调 `publisher.publish(event)` 发送，自动填充 eventId/timestamp |
| `MqTraceInterceptor` 链路追踪 | 未实现 | 透传 traceId 到 MDC |
| `MqExchange`/`MqConstants` 常量 | 未实现 | Exchange/Queue 名常量集中管理 |
| Exchange/Queue 自动声明 | 未实现 | 基于 `BaseEvent` 子类自动声明 |
| `AutoConfiguration.imports` | 未实现 | 当前未声明自动装配入口 |

---

## 7. 关键类清单

| 类名 | 包 | 职责 |
|------|----|------|
| `AbstractEventListener<T>` | `listener` | 幂等消费基类 |
| `EventConfig` | `config` | 序列化/重试/死信配置 |

---

## 8. 配置项

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `spring.rabbitmq.host` | `127.0.0.1` | RabbitMQ 主机 |
| `spring.rabbitmq.port` | `5672` | AMQP 端口 |
| `spring.rabbitmq.username` | `guest` | 用户名 |
| `spring.rabbitmq.password` | `guest` | 密码 |
| `spring.rabbitmq.listener.direct.acknowledge-mode` | `auto` | 确认模式 |
| `spring.rabbitmq.listener.simple.retry.enabled` | `true` | 开启重试 |
| `spring.rabbitmq.listener.simple.retry.max-attempts` | `3` | 最大重试次数 |
| `xytang.mq.idempotent.ttl` | `24h` | 幂等去重 TTL |
| `xytang.mq.dead.queue` | `dead.queue` | 死信队列名 |

> 子模块内**无** `application.yml`，所有配置在 Nacos `spring-cloud-shared.yaml` 中。

---

## 9. 与其他模块的关系

| 关系 | 模块 |
|------|------|
| 依赖 | `spring-cloud-common-core`（用 `BaseEvent`）、`spring-cloud-common-redis`（用 RedisTemplate 去重） |
| 被依赖 | `spring-cloud-auth`、`spring-cloud-system`、`spring-cloud-workflow`、`spring-cloud-ai`、`spring-cloud-log`、`spring-cloud-message` 等 |

---

## 10. 红线

1. ❌ Listener 不继承 `AbstractEventListener`（导致无幂等去重，可能重复消费）
2. ❌ 事件不继承 `BaseEvent`（导致缺少 eventId/timestamp，幂等失效）
3. ❌ Listener 用 `try-catch` 吞掉异常（必须让异常传播触发重试）
4. ❌ 消息体用 `Serializable` Java 序列化（必须用 Jackson JSON）
5. ❌ 死信队列消息不监控（必须接告警，定期清理）
6. ❌ Exchange/Queue 命名不统一（必须 `spring-cloud.{biz}.{event}` 格式）
7. ❌ 生产端不发 `eventId`（导致消费端无法去重）
8. ❌ AutoConfiguration Bean 不加 `@ConditionalOnMissingBean`
