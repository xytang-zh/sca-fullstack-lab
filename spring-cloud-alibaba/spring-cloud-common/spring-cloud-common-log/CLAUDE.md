# CLAUDE.md — spring-cloud-common-log 日志切面

> 本文档面向 AI 编码助手，用于在 `spring-cloud-common-log/` 目录下工作时提供模块约束、功能清单与开发规范。
> 工作前**必须**先读取父目录的 [`spring-cloud-common/CLAUDE.md`](../CLAUDE.md) 与 [`spring-cloud-alibaba/CLAUDE.md`](../../CLAUDE.md) 了解全局规范。

---

## 1. 模块定位

`spring-cloud-common-log` 基于 AOP 提供 `@OperationLog` 注解，自动记录接口调用的入参/出参/异常到 MQ，由 `spring-cloud-log` 服务消费落库。

**核心设计原则**：
1. **注解声明**：业务方加 `@OperationLog` 即可，零配置可用
2. **异步发送**：日志写入走 MQ 异步，**禁止**阻塞业务请求
3. **不打印敏感数据**：密码、Token、身份证号自动脱敏

| 维度 | 值 |
|------|-----|
| 顶级包 | `com.xytang.common.log` |
| 父 POM | `com.xytang:spring-cloud-alibaba:1.0-SNAPSHOT` |
| 当前 artifactId | `spring-cloud-common-log` |
| packaging | `jar` |
| 是否有代码 | 🟡 部分（`@OperationLog` + `OperationLogAspect`） |

---

## 2. 目录结构

```
spring-cloud-common-log/
├── pom.xml
└── src/
    └── main/
        └── java/com/xytang/common/log/
            ├── annotation/
            │   └── OperationLog.java
            └── aspect/
                └── OperationLogAspect.java
```

---

## 3. 包结构

| 包 | 职责 |
|----|------|
| `com.xytang.common.log.annotation` | 注解：`@OperationLog` |
| `com.xytang.common.log.aspect` | AOP 切面：`OperationLogAspect` |

> 计划新增包：`enums`（`BusinessType`）、`model`（`OperationLogEvent`）、`producer`（`LogEventPublisher`）。当前未实现。

---

## 4. POM 依赖模板

```xml
<dependencies>
    <!-- 内部依赖 -->
    <dependency>
        <groupId>com.xytang</groupId>
        <artifactId>spring-cloud-common-core</artifactId>
    </dependency>

    <!-- 内部依赖：MQ（异步发送日志） -->
    <dependency>
        <groupId>com.xytang</groupId>
        <artifactId>spring-cloud-common-mq</artifactId>
    </dependency>

    <!-- AOP -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-aop</artifactId>
    </dependency>
</dependencies>
```

---

## 5. 技术栈选型

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring AOP | Spring Boot 3.5.0 管理 | 注解切面 |
| RabbitMQ（内部模块） | Spring AMQP | 异步发送 |
| Hutool | 5.8.27 | JSON 序列化 + 工具 |
| Lombok | 父 POM 全局声明 | 注解简化 |

---

## 6. 功能清单

### 6.1 `@OperationLog` 注解（已实现）

- **职责**：标注在 Controller 方法上，声明操作日志
- **属性**：
  | 属性 | 默认值 | 说明 |
  |------|--------|------|
  | `title` | `""` | 模块标题（如"用户管理"） |
  | `businessType` | `OTHER` | 业务类型（INSERT/UPDATE/DELETE/EXPORT/IMPORT/OTHER） |
  | `saveParam` | `true` | 是否记录入参 |
  | `saveResult` | `false` | 是否记录出参 |
- **实现技术**：注解 + 元注解 `@Target(METHOD)`/`@Retention(RUNTIME)`

### 6.2 `OperationLogAspect` AOP 切面（已实现）

- **职责**：Around 拦截 `@OperationLog` 方法，捕获入参/出参/异常，异步发到 MQ
- **执行流程**：
  1. 方法前：记录入参（脱敏）、操作人、IP、Trace-Id
  2. 方法执行：调 `joinPoint.proceed()`
  3. 方法后：记录出参（脱敏）、耗时、状态（成功/失败）
  4. 异常时：记录异常信息（不吞掉，重新抛出）
  5. 异步：通过 RabbitTemplate 发送到 `log.operation` Exchange
- **脱敏**：自动识别字段名（password/token/idCard/phone），用 `*` 替换
- **实现技术**：`@Around` + `RabbitTemplate.convertAndSend` + Hutool JSON

### 6.3 计划功能（TODO）

| 功能 | 状态 | 说明 |
|------|------|------|
| `BusinessType` 枚举 | 未实现 | INSERT/UPDATE/DELETE/EXPORT/IMPORT/OTHER |
| `OperationLogEvent` 模型 | 未实现 | MQ 事件模型 |
| `LogEventPublisher` | 未实现 | 统一发送入口 |
| `MqTraceInterceptor` | 未实现 | 链路追踪 |
| `AutoConfiguration.imports` | 未实现 | 当前未声明自动装配入口 |

---

## 7. 关键类清单

| 类名 | 包 | 职责 |
|------|----|------|
| `OperationLog` | `annotation` | `@OperationLog` 注解定义 |
| `OperationLogAspect` | `aspect` | Around AOP 切面 |

---

## 8. 配置项

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `xytang.log.mq.exchange` | `log.operation` | 日志 MQ Exchange 名 |
| `xytang.log.async` | `true` | 是否异步发送 |
| `xytang.log.save-param` | `true` | 默认是否记录入参 |
| `xytang.log.save-result` | `false` | 默认是否记录出参 |
| `xytang.log.sensitive-fields` | `password,token,idCard,phone,idCard` | 脱敏字段名 |

> 子模块内**无** `application.yml`，所有配置在 Nacos `spring-cloud-shared.yaml` 中。

---

## 9. 与其他模块的关系

| 关系 | 模块 |
|------|------|
| 依赖 | `spring-cloud-common-core`、`spring-cloud-common-mq`（异步发送） |
| 被依赖 | `spring-cloud-auth`、`spring-cloud-system`、`spring-cloud-workflow` 等所有业务服务 |

---

## 10. 红线

1. ❌ 日志打印密码/Token/身份证号（必须脱敏，字段名识别 + `*` 替换）
2. ❌ 日志同步写库（必须异步走 MQ，避免阻塞业务）
3. ❌ 切面不重新抛出异常（必须 try-finally，异常传播）
4. ❌ 切面在 Controller 写业务逻辑（必须只做日志记录）
5. ❌ 记录大字段（如文件二进制）入参（导致 MQ 消息过大）
6. ❌ MQ 发送失败导致业务失败（必须 catch + 降级为本地日志）
7. ❌ AutoConfiguration Bean 不加 `@ConditionalOnMissingBean`
