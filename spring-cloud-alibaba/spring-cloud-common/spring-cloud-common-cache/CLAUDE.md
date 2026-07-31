# CLAUDE.md — spring-cloud-common-cache 多级缓存

> 本文档面向 AI 编码助手，用于在 `spring-cloud-common-cache/` 目录下工作时提供模块约束、功能计划与开发规范。
> 工作前**必须**先读取父目录的 [`spring-cloud-common/CLAUDE.md`](../CLAUDE.md) 与 [`spring-cloud-alibaba/CLAUDE.md`](../../CLAUDE.md) 了解全局规范。

---

## 1. 模块定位

`spring-cloud-common-cache` 提供 Caffeine + Redis 多级缓存，通过注解化使用，热点数据 L1（本地）+ L2（远程）双层缓存。

**核心设计原则**：
1. **注解化**：`@LayeredCache` 注解声明缓存，业务方零配置可用
2. **L1 短 L2 长**：L1（Caffeine）5s 防穿透，L2（Redis）30m 防雪崩
3. **防穿透**：布隆过滤器 + 空值缓存，**禁止**直接打 DB

| 维度 | 值 |
|------|-----|
| 顶级包 | `com.xytang.common.cache` |
| 父 POM | `com.xytang:spring-cloud-alibaba:1.0-SNAPSHOT` |
| 当前 artifactId | `spring-cloud-common-cache` |
| packaging | `jar` |
| 是否有代码 | ❌ 空壳（仅 pom.xml，**本 CLAUDE.md 为规划书**） |

---

## 2. 技术栈选型

| 技术 | 版本 | 用途 |
|------|------|------|
| Caffeine | 3.2.0 | L1 本地缓存 |
| Spring Data Redis（内部模块） | Spring Boot 管理 | L2 远程缓存 |
| Spring AOP | Spring Boot 管理 | 注解切面 |
| Lombok | 父 POM 全局声明 | 注解简化 |

---

## 3. 功能计划

### 3.1 `@LayeredCache` 注解

- **职责**：声明多级缓存
- **属性**：
  | 属性 | 默认值 | 说明 |
  |------|--------|------|
  | `key` | 必填 | 缓存 Key（支持 SpEL，如 `'user:' + #id`） |
  | `l1Ttl` | `5s` | L1 本地缓存 TTL |
  | `l2Ttl` | `30m` | L2 远程缓存 TTL |
  | `randomTtl` | `true` | 是否加 ±10% 随机数（防雪崩） |
- **实现技术**：注解 + AOP

### 3.2 LayeredCacheAspect AOP 切面

- **职责**：Around 拦截 `@LayeredCache` 方法
- **流程**：
  1. 拼 Key（SpEL）
  2. 查 L1（Caffeine），命中直接返回
  3. L1 未命中，查 L2（Redis），命中写回 L1 并返回
  4. L2 未命中，调方法，结果写入 L1 和 L2
- **实现技术**：`@Around` + Caffeine + RedisTemplate

### 3.3 MultiLevelCacheManager 管理器

- **职责**：统一管理 L1/L2 缓存
- **方法**：`get`/`put`/`evict`/`clear`
- **实现技术**：组合 `CaffeineCache` + `RedisCache`

### 3.4 CaffeineCache / RedisCache 实现

- **CaffeineCache**：L1，基于 `Caffeine.newBuilder().expireAfterWrite()`
- **RedisCache**：L2，基于 `RedisTemplate.opsForValue()`

### 3.5 CachePenetrationPolicy 防穿透

- **职责**：缓存穿透防护
- **策略**：
  - 空值缓存：DB 未命中也缓存 null（TTL 5min，防止穿透）
  - 布隆过滤器：高频查询的固定 ID 用 BloomFilter 预过滤
- **实现技术**：`BloomFilter.create(Funnels...)` + null cache

---

## 4. POM 依赖模板（计划）

```xml
<dependencies>
    <!-- 内部依赖 -->
    <dependency>
        <groupId>com.xytang</groupId>
        <artifactId>spring-cloud-common-core</artifactId>
    </dependency>

    <!-- 内部依赖：Redis（L2） -->
    <dependency>
        <groupId>com.xytang</groupId>
        <artifactId>spring-cloud-common-redis</artifactId>
    </dependency>

    <!-- Caffeine（L1） -->
    <dependency>
        <groupId>com.github.ben-manes.caffeine</groupId>
        <artifactId>caffeine</artifactId>
    </dependency>

    <!-- AOP -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-aop</artifactId>
    </dependency>
</dependencies>
```

---

## 5. 配置项（计划）

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `xytang.cache.l1.spec` | `expireAfterWrite=5s,maximumSize=10000` | Caffeine 规格 |
| `xytang.cache.l2.ttl` | `30m` | L2 默认 TTL |
| `xytang.cache.l2.random-ttl-ratio` | `0.1` | 随机 TTL 比例（防雪崩） |
| `xytang.cache.null-ttl` | `5m` | 空值缓存 TTL |
| `xytang.cache.bloom.enabled` | `false` | 是否启用布隆过滤器 |

> 子模块内**无** `application.yml`，所有配置在 Nacos 中。

---

## 6. 与其他模块的关系

| 关系 | 模块 |
|------|------|
| 依赖 | `spring-cloud-common-core`、`spring-cloud-common-redis` |
| 被依赖 | `spring-cloud-auth`、`spring-cloud-system`、`spring-cloud-workflow` 等需要缓存的服务 |

---

## 7. 红线

1. ❌ 缓存 TTL 不加 ±10% 随机数（导致雪崩，DB 瞬间被打）
2. ❌ 热点数据不用 `@LayeredCache`（导致 Redis 单点瓶颈）
3. ❌ 缓存穿透不打空值缓存或布隆过滤器（导致 DB 被穿透）
4. ❌ 缓存对象不实现 `Serializable`（导致 Redis 反序列化失败）
5. ❌ 缓存 Key 不加业务前缀（导致跨业务冲突）
6. ❌ 缓存大 Value（>10KB）不压缩（占用 Redis 内存）
7. ❌ AutoConfiguration Bean 不加 `@ConditionalOnMissingBean`

---

## 8. 实施状态

⚠️ 本模块当前为**空壳**，仅 `pom.xml`，**无任何 .java 代码**。

| 任务 | 状态 |
|------|------|
| pom.xml | ✅ 存在 |
| `@LayeredCache` 注解 | ❌ 未实现 |
| LayeredCacheAspect | ❌ 未实现 |
| MultiLevelCacheManager | ❌ 未实现 |
| CaffeineCache | ❌ 未实现 |
| RedisCache | ❌ 未实现 |
| CachePenetrationPolicy | ❌ 未实现 |
| CacheAutoConfiguration | ❌ 未实现 |
| AutoConfiguration.imports | ❌ 未实现 |
| 单元测试 | ❌ 未实现 |

> 落地实现时请对照本规划，并在完成后更新本 CLAUDE.md 第 2-7 节为实际代码状态。
