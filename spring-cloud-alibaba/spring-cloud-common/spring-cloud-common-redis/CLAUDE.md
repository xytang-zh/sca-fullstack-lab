# CLAUDE.md — spring-cloud-common-redis Redis 工具

> 本文档面向 AI 编码助手，用于在 `spring-cloud-common-redis/` 目录下工作时提供模块约束、功能清单与开发规范。
> 工作前**必须**先读取父目录的 [`spring-cloud-common/CLAUDE.md`](../CLAUDE.md) 与 [`spring-cloud-alibaba/CLAUDE.md`](../../CLAUDE.md) 了解全局规范。

---

## 1. 模块定位

`spring-cloud-common-redis` 提供 Redis 客户端配置与序列化方案，**所有依赖 Redis 的服务**都会引入此模块。

**核心设计原则**：
1. **统一序列化**：Key 用 String，Value 用 `GenericJackson2JsonRedisSerializer`（带 Java 8 时间模块）
2. **不重复造轮子**：直接用 Spring Data Redis，**禁止**自己封装 RedisTemplate 行为
3. **集成 Redisson 与 Caffeine**：本模块自 `spring-cloud-common-redisson`/`spring-cloud-common-cache` 并入 `@DistributedLock` 分布式锁与多级缓存能力

| 维度 | 值 |
|------|-----|
| 顶级包 | `com.xytang.common.redis` |
| 父 POM | `com.xytang:spring-cloud-alibaba:1.0-SNAPSHOT` |
| 当前 artifactId | `spring-cloud-common-redis` |
| packaging | `jar` |
| 是否有代码 | 🟡 部分（仅 RedisConfig） |

---

## 2. 目录结构

```
spring-cloud-common-redis/
├── pom.xml
└── src/
    └── main/
        └── java/com/xytang/common/redis/
            └── config/
                └── RedisConfig.java
```

---

## 3. 包结构

| 包 | 职责 |
|----|------|
| `com.xytang.common.redis.config` | Redis 配置类：`RedisConfig` |

> 计划新增包：`util`（RedisUtils 静态封装）、`serializer`（自定义序列化器）、`lock`（锁回调）。当前未实现。

---

## 4. POM 依赖模板

```xml
<dependencies>
    <!-- 内部依赖 -->
    <dependency>
        <groupId>com.xytang</groupId>
        <artifactId>spring-cloud-common-core</artifactId>
    </dependency>

    <!-- Spring Data Redis -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
</dependencies>
```

---

## 5. 技术栈选型

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Data Redis | Spring Boot 3.5.0 管理 | Redis 客户端 |
| Jackson | Spring Boot 管理 | Value 序列化 |
| Lombok | 父 POM 全局声明 | 注解简化 |

---

## 6. 功能清单

### 6.1 RedisTemplate 配置 `RedisConfig`（已实现）

- **职责**：注册 `RedisTemplate<Object, Object>` 和 `StringRedisTemplate` Bean
- **序列化方案**：
  | Bean | Key 序列化 | Value 序列化 |
  |------|-----------|-------------|
  | `RedisTemplate` | `StringRedisSerializer` | `GenericJackson2JsonRedisSerializer`（带 Java 8 时间模块） |
  | `StringRedisTemplate` | `StringRedisSerializer` | `StringRedisSerializer` |
- **关键配置**：`GenericJackson2JsonRedisSerializer` 启用默认类型信息（`activateDefaultTyping`），避免反序列化失败
- **实现技术**：`@Configuration` + `@Bean`

### 6.2 计划功能（TODO）

| 功能 | 状态 | 说明 |
|------|------|------|
| `RedisUtils` 静态封装 | 未实现 | 常用操作 get/set/del/incr/expire 的静态包装 |
| `RateLimitUtils` | 未实现 | 基于 Redis 的限速器 |
| `CacheBatchUtils` | 未实现 | 批量写入/管道 |
| `LockCallback` 函数式锁回调 | 未实现 | 配合 Redisson 使用 |
| `ProtobufRedisSerializer` | 未实现 | 高性能序列化方案 |
| `AutoConfiguration.imports` | 未实现 | 当前未声明自动装配入口 |

---

## 7. 关键类清单

| 类名 | 包 | 职责 |
|------|----|------|
| `RedisConfig` | `config` | RedisTemplate + StringRedisTemplate 注册与序列化配置 |

---

## 8. 配置项

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `spring.data.redis.host` | `127.0.0.1` | Redis 主机 |
| `spring.data.redis.port` | `6379` | Redis 端口 |
| `spring.data.redis.password` | （空） | Redis 密码 |
| `spring.data.redis.database` | `0` | Redis 数据库 |
| `spring.data.redis.timeout` | `5s` | 连接超时 |
| `spring.data.redis.lettuce.pool.max-active` | `8` | 连接池最大活跃数 |

> 子模块内**无** `application.yml`，所有配置在 Nacos `spring-cloud-shared.yaml` 中。

---

## 9. 与其他模块的关系

| 关系 | 模块 |
|------|------|
| 依赖 | `spring-cloud-common-core` |
| 被依赖 | `spring-cloud-auth`、`spring-cloud-system`、`spring-cloud-article`、`spring-cloud-comment` 等业务服务 |
| 集成 | Redisson（`@DistributedLock`）、Caffeine（多级缓存），自 `spring-cloud-common-redisson`/`spring-cloud-common-cache` 并入 |

---

## 10. 红线

1. ❌ 缓存对象不实现 `Serializable`（会导致 `GenericJackson2JsonRedisSerializer` 反序列化失败）
2. ❌ 缓存对象不声明 `serialVersionUID`（版本不兼容时反序列化失败）
3. ❌ 业务服务自己注册 `RedisTemplate` Bean（必须用本模块的）
4. ❌ 用 `JdkSerializationRedisSerializer`（性能差、跨语言不友好）
5. ❌ Redis Key 不加前缀（必须 `spring-cloud:{service}:{biz}:{id}` 格式，避免冲突）
6. ❌ Redis Key 包含中文或特殊字符（必须 ASCII）
7. ❌ 缓存大 Value（>10KB）不压缩（占用带宽）
8. ❌ AutoConfiguration Bean 不加 `@ConditionalOnMissingBean`
