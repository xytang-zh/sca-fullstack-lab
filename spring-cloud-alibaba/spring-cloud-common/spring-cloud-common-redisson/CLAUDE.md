# CLAUDE.md — spring-cloud-common-redisson 分布式锁/限流

> 本文档面向 AI 编码助手，用于在 `spring-cloud-common-redisson/` 目录下工作时提供模块约束、功能清单与开发规范。
> 工作前**必须**先读取父目录的 [`spring-cloud-common/CLAUDE.md`](../CLAUDE.md) 与 [`spring-cloud-alibaba/CLAUDE.md`](../../CLAUDE.md) 了解全局规范。

---

## 1. 模块定位

`spring-cloud-common-redisson` 把 Redisson 的能力封装成**注解化**的使用方式，业务方加注解即可用分布式锁、限流、延迟队列。

**核心设计原则**：
1. **注解化**：公共能力尽量用注解暴露，业务方零配置可用
2. **SpEL 支持**：注解 key 必须支持 SpEL 表达式（如 `'order:' + #orderId`）
3. **默认值**：注解必须有默认值，避免业务方显式配置

| 维度 | 值 |
|------|-----|
| 顶级包 | `com.xytang.common.redisson` |
| 父 POM | `com.xytang:spring-cloud-alibaba:1.0-SNAPSHOT` |
| 当前 artifactId | `spring-cloud-common-redisson` |
| packaging | `jar` |
| 是否有代码 | 🟡 部分（仅 `@DistributedLock` + 切面） |

---

## 2. 目录结构

```
spring-cloud-common-redisson/
├── pom.xml
└── src/
    └── main/
        └── java/com/xytang/common/redisson/
            ├── annotation/
            │   └── DistributedLock.java
            └── aspect/
                └── DistributedLockAspect.java
```

---

## 3. 包结构

| 包 | 职责 |
|----|------|
| `com.xytang.common.redisson.annotation` | 注解：`@DistributedLock` 等 |
| `com.xytang.common.redisson.aspect` | AOP 切面：`DistributedLockAspect` |

> 计划新增包：`handler`（锁失败处理）、`config`（RedissonAutoConfiguration）。当前未实现。

---

## 4. POM 依赖模板

```xml
<dependencies>
    <!-- 内部依赖 -->
    <dependency>
        <groupId>com.xytang</groupId>
        <artifactId>spring-cloud-common-core</artifactId>
    </dependency>

    <!-- Redisson -->
    <dependency>
        <groupId>org.redisson</groupId>
        <artifactId>redisson-spring-boot-starter</artifactId>
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
| Redisson | 4.0.0 | 分布式锁/限流/延迟队列 |
| Spring AOP | Spring Boot 3.5.0 管理 | 注解切面 |
| SpEL | Spring 管理 | 注解 key 表达式解析 |
| Lombok | 父 POM 全局声明 | 注解简化 |

---

## 6. 功能清单

### 6.1 `@DistributedLock` 注解 + 切面（已实现）

- **职责**：基于 Redisson 的 `RLock` 实现注解化分布式锁
- **注解属性**：
  | 属性 | 默认值 | 说明 |
  |------|--------|------|
  | `name` | 必填 | 锁名（支持 SpEL，如 `'order:' + #orderId`） |
  | `waitTime` | `3` | 获取锁等待时间（秒） |
  | `leaseTime` | `10` | 锁持有时间（秒），超时自动释放 |
  | `lockType` | `REENTRANT` | 锁类型：可重入/公平/读写 |
- **失败行为**：获取锁失败抛 `BusinessException(code=42901, msg="操作过于频繁")`
- **实现技术**：`@Around` AOP + Redisson `RLock` + SpEL `parseExpression`

### 6.2 计划功能（TODO）

| 功能 | 状态 | 说明 |
|------|------|------|
| `@RateLimit` 注解 + 切面 | 未实现 | 令牌桶算法，属性 `rate`/`interval`/`name` |
| `@DelayedQueue` 注解 + 切面 | 未实现 | 延迟队列，基于 Redisson `RDelayedQueue` |
| `LockFailureHandler` 接口 | 未实现 | 锁失败处理策略（抛异常/返回默认值/记录日志） |
| `RedissonAutoConfiguration` | 未实现 | 自动装配类，注册 RedissonClient |
| `AutoConfiguration.imports` | 未实现 | 当前未声明自动装配入口 |

---

## 7. 关键类清单

| 类名 | 包 | 职责 |
|------|----|------|
| `DistributedLock` | `annotation` | `@DistributedLock` 注解定义 |
| `DistributedLockAspect` | `aspect` | AOP 切面，Around 拦截注解方法 |

---

## 8. 配置项

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `spring.redisson.config` | （Nacos 注入） | Redisson YAML 配置字符串 |
| `xytang.redisson.lock.default-wait-time` | `3` | 默认等待时间（秒） |
| `xytang.redisson.lock.default-lease-time` | `10` | 默认持有时间（秒） |

> 子模块内**无** `application.yml`，所有配置在 Nacos `spring-cloud-shared.yaml` 中。

---

## 9. 与其他模块的关系

| 关系 | 模块 |
|------|------|
| 依赖 | `spring-cloud-common-core` |
| 被依赖 | `spring-cloud-auth`、`spring-cloud-system`、`spring-cloud-monitor`、`spring-cloud-message`、`spring-cloud-file` 等需要分布式锁的服务 |
| 不依赖 | `spring-cloud-common-redis`（避免与 Redisson 客户端冲突，Redisson 自带 Redis 客户端） |

---

## 10. 红线

1. ❌ 注解无默认值（业务方必须显式配置才能用）
2. ❌ 注解 key 不支持 SpEL（导致业务方无法用方法参数构造 key）
3. ❌ 用 `synchronized` 跨 JVM 同步（必须用 `@DistributedLock`）
4. ❌ 锁名不加业务前缀（导致跨业务冲突）
5. ❌ 锁持有时间过短（导致业务未完成锁就释放，被并发穿透）
6. ❌ 锁持有时间过长（导致锁释放延迟，吞吐量下降）
7. ❌ AOP 切面不捕获 `InterruptedException`（导致线程中断状态丢失）
8. ❌ AutoConfiguration Bean 不加 `@ConditionalOnMissingBean`
