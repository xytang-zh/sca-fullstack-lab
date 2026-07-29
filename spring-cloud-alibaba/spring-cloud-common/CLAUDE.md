# CLAUDE.md — spring-cloud-common 公共模块聚合

> 本文档面向 AI 编码助手，用于在 `spring-cloud-common/` 目录下（或任意子模块下）工作时提供模块约束、技术栈版本、对外暴露能力与开发规范。
> 工作前**必须**先读取父目录的 [`spring-cloud-alibaba/CLAUDE.md`](../CLAUDE.md) 了解全局规范。

---

## 1. 模块定位

`spring-cloud-common` 是整个项目的 **公共能力下沉层**，由 17 个子模块组成，每个子模块聚焦一个垂直能力（Redis、MyBatis、MQ、Sa-Token、Netty 等）。

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
| 子模块数量 | 17 |

---

## 2. 子模块结构

```
spring-cloud-common/
├── pom.xml                              packaging=pom，声明 17 个子模块
├── spring-cloud-common-core/             核心工具：异常/响应/常量/枚举/Dubbo 接口
├── spring-cloud-common-web/              Web 通用：全局异常/拦截器/AOP/参数解析
├── spring-cloud-common-redis/            Redis 工具：序列化/分布式缓存/限速
├── spring-cloud-common-redisson/         Redisson：分布式锁/限流/延迟队列（注解化）
├── spring-cloud-common-mybatis/          MyBatis-Plus：分页/多租户/数据权限/雪花 ID
├── spring-cloud-common-datasource/       dynamic-datasource：多数据源切换
├── spring-cloud-common-mq/               RabbitMQ：事件基类/幂等消费
├── spring-cloud-common-mongo/            MongoDB：配置/通用 DAO
├── spring-cloud-common-es/               ElasticSearch：配置/索引管理
├── spring-cloud-common-ai/               Spring AI：ChatClient/Advisor/VectorStore
├── spring-cloud-common-satoken/          Sa-Token：登录上下文/SSO Client 基类
├── spring-cloud-common-security/         网关鉴权：过滤器/路由元数据
├── spring-cloud-common-log/              日志：@OperationLog 注解/AOP 切面
├── spring-cloud-common-swagger/          OpenAPI：springdoc 聚合/Knife4j
├── spring-cloud-common-cache/            多级缓存：Caffeine + Redis
├── spring-cloud-common-netty/            Netty：WebSocket Server/心跳/路由
└── spring-cloud-common-test/             测试：基类/Testcontainers/H2
```

---

## 3. 各子模块详细说明

### 3.1 spring-cloud-common-core（核心，无 Spring 依赖）

**职责**：纯 POJO + 工具类，被所有其他模块依赖。

**包结构**：
```
com.xytang.common.core
├── api/
│   ├── R.java                    统一响应 R<T>
│   ├── PageVO.java               分页响应
│   └── ResultCode.java           错误码枚举
├── exception/
│   ├── BusinessException.java     业务异常基类
│   ├── AuthException.java
│   ├── BusinessExceptionEnum.java
│   └── SystemException.java
├── constant/
│   ├── CommonConstants.java
│   ├── RedisKeyConstants.java    Redis Key 前缀
│   └── MqConstants.java          Exchange/Queue 名
├── enums/
│   ├── ResultEnum.java
│   ├── YesNoEnum.java
│   └── DeviceTypeEnum.java
├── util/
│   ├── SnowflakeIdUtils.java
│   ├── JsonUtils.java            Jackson 静态封装
│   ├── DateUtils.java
│   ├── EncryptUtils.java         AES/BCrypt
│   └── IpUtils.java
├── dto/                          跨服务 DTO（如 UserDTO、DeptDTO）
└── rpc/                          Dubbo 接口定义
    ├── UserRpcService.java
    ├── DeptRpcService.java
    └── FileRpcService.java
```

**对外暴露**：
- `R<T>`：`{code, msg, data, timestamp}`
- `PageVO<T>`：`{list, total, pageNum, pageSize, pages}`
- `BusinessException`、`ResultCode`

**禁止**：
- ❌ 在 core 中加 Spring 依赖（保持纯 POJO）
- ❌ 在 core 中加 MyBatis/JPA 注解
- ❌ 在 core 中加 Servlet API

---

### 3.2 spring-cloud-common-web（Web 通用）

**职责**：Spring MVC 全局配置、异常处理、参数解析、AOP。

**包结构**：
```
com.xytang.common.web
├── config/
│   ├── WebMvcConfig.java          注册拦截器/参数解析器
│   ├── JacksonConfig.java         Long → String（前端精度问题）
│   └── CorsConfig.java            CORS 全局
├── handler/
│   ├── GlobalExceptionHandler.java 统一异常 → R<Void>
│   ├── PageArgumentResolver.java   PageQuery 自动解析
│   └── LoginUserArgumentResolver.java @LoginUser 注解解析
├── advice/
│   ├── ResponseBodyAdvice.java   自动包装 R<T>
│   └── LogTraceAdvice.java       生成 X-Trace-Id
├── interceptor/
│   ├── LogInterceptor.java       请求日志
│   └── RateLimitInterceptor.java 接口级限流
├── annotation/
│   ├── LoginUser.java
│   ├── ResponseWrap.java
│   └── RepeatSubmit.java          防重提交
└── filter/
    └── XssFilter.java             XSS 过滤
```

**关键能力**：
- 全局异常处理：把 `BusinessException`、`MethodArgumentNotValidException`、`ConstraintViolationException` 统一转为 `R<Void>`
- 自动包装：Controller 返回非 `R` 类型时自动包装（可关闭）
- 雪花 ID Long → String：避免前端 JS 精度丢失

---

### 3.3 spring-cloud-common-redis（Redis 工具）

**职责**：RedisTemplate 配置、序列化、缓存工具。

**包结构**：
```
com.xytang.common.redis
├── config/
│   ├── RedisConfig.java           RedisTemplate + StringRedisTemplate
│   └── RedissonConfig.java        （如果用 Redisson）
├── util/
│   ├── RedisUtils.java            静态封装常用操作
│   ├── RateLimitUtils.java
│   └── CacheBatchUtils.java
├── serializer/
│   ├── JsonRedisSerializer.java   Jackson 序列化
│   └── ProtobufRedisSerializer.java
└── lock/
    └── LockCallback.java          函数式锁回调
```

**关键配置**：
- 序列化：Key 用 String，Value 用 Jackson（带 Java 8 时间模块）
- 默认 RedisTemplate**必须**用 `GenericJackson2JsonRedisSerializer`，避免反序列化失败

---

### 3.4 spring-cloud-common-redisson（分布式锁/限流）

**职责**：把 Redisson 的能力封装成注解。

**包结构**：
```
com.xytang.common.redisson
├── annotation/
│   ├── DistributedLock.java       @DistributedLock(name="xxx", waitTime=3, leaseTime=10)
│   ├── RateLimit.java             @RateLimit(name="xxx", rate=10, interval=1)
│   └── DelayedQueue.java          @DelayedQueue
├── aspect/
│   ├── DistributedLockAspect.java
│   ├── RateLimitAspect.java
│   └── DelayedQueueAspect.java
├── handler/
│   └── LockFailureHandler.java    获取锁失败处理
└── config/
    └── RedissonAutoConfiguration.java
```

**使用示例**：
```java
@DistributedLock(name = "'order:' + #orderId", waitTime = 3, leaseTime = 10)
public Order processOrder(Long orderId) { ... }

@RateLimit(name = "userLogin", rate = 5, interval = 1)  // 每秒 5 次
public R<LoginVO> login(LoginDTO dto) { ... }
```

---

### 3.5 spring-cloud-common-mybatis（MyBatis-Plus 增强）

**职责**：MyBatis-Plus 拦截器配置、雪花 ID、多租户、数据权限。

**包结构**：
```
com.xytang.common.mybatis
├── config/
│   ├── MybatisPlusConfig.java     拦截器链
│   └── MapperScannerConfig.java
├── interceptor/
│   ├── DataPermissionInterceptor.java  数据权限
│   └── TenantLineInterceptor.java      多租户（可选）
├── handler/
│   ├── DataPermissionHandler.java       @DataScope 注解处理器
│   ├── MetaObjectHandler.java           自动填充 createTime/updateTime
│   └── IdentifierGenerator.java          雪花 ID
├── annotation/
│   ├── DataScope.java                  数据权限注解
│   └── Tenant.java
└── base/
    ├── BaseEntity.java                  id, createTime, updateTime, createBy, updateBy, delFlag
    └── BaseController.java              基础 CRUD 控制器
```

**MybatisPlusConfig**：
```java
@Bean
public MybatisPlusInterceptor mybatisPlusInterceptor() {
    MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
    interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
    interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(tenantHandler()));
    interceptor.addInnerInterceptor(new DataPermissionInterceptor(dataPermissionHandler()));
    return interceptor;
}
```

---

### 3.6 spring-cloud-common-datasource（多数据源）

**职责**：封装 dynamic-datasource，提供国产库适配。

**包结构**：
```
com.xytang.common.datasource
├── config/
│   ├── DataSourceConfig.java       4 个数据源：master/pgvector/kingbase/dm
│   └── DynamicDataSourceAutoConfiguration.java
├── enums/
│   └── DataSourceEnum.java          MASTER/PGVECTOR/KINGBASE/DM
├── handler/
│   └── DynamicDataSourceHandler.java   基于 ThreadLocal 动态切换
├── annotation/
│   └── DS.java                      重导出 dynamic-datasource 的注解
└── constant/
    └── DataSourceConstant.java
```

---

### 3.7 spring-cloud-common-mq（RabbitMQ 事件总线）

**职责**：统一事件基类、幂等消费、死信队列。

**包结构**：
```
com.xytang.common.mq
├── config/
│   ├── RabbitMqConfig.java        Exchange/Queue 声明（可配置化）
│   └── RabbitMqRetryConfig.java    重试/死信
├── base/
│   ├── AbstractEventListener.java   <T extends BaseEvent>  幂等消费
│   └── BaseEvent.java                所有事件的基类（eventId, timestamp, ...）
├── annotation/
│   └── EventListener.java           自定义注解，配合 AbstractEventListener
├── producer/
│   └── EventPublisher.java          统一发送入口
├── interceptor/
    └── MqTraceInterceptor.java      链路追踪
└── constant/
    └── MqExchange.java               Exchange 常量
```

**关键能力**：
- 所有事件**必须**继承 `BaseEvent`，包含 `eventId`、`eventType`、`source`、`timestamp`
- `AbstractEventListener` 自动用 Redis 做幂等（eventId 去重，TTL 24h）
- 消费失败自动重试 3 次，超过进死信队列 `dead.queue`

---

### 3.8 spring-cloud-common-mongo（MongoDB 配置）

**职责**：MongoDB 配置、通用 DAO、分页。

**包结构**：
```
com.xytang.common.mongo
├── config/
│   └── MongoConfig.java           MongoTemplate + 索引初始化
├── base/
│   ├── BaseMongoRepository.java
│   └── BaseEntity.java            @Document 索引自动生成
├── util/
│   ├── MongoPageUtil.java
│   └── MongoIndexUtil.java
└── converter/
    └── JsonReaderConverter.java
```

---

### 3.9 spring-cloud-common-es（ElasticSearch 配置）

**职责**：ES 8 客户端配置、索引管理。

**包结构**：
```
com.xytang.common.es
├── config/
│   ├── EsConfig.java               RestClient + ElasticsearchClient
│   └── EsIndexAutoConfiguration.java  索引自动创建
├── annotation/
│   ├── EsDocument.java             @Document(indexName="article")
    └── EsField.java
├── base/
│   └── BaseEsRepository.java
└── manager/
    ├── IndexManager.java           创建/删除/重建索引
    └── BulkOperator.java           批量写入
```

---

### 3.10 spring-cloud-common-ai（Spring AI）

**职责**：ChatClient 配置、Advisor 链、VectorStore 抽象。

**包结构**：
```
com.xytang.common.ai
├── config/
│   ├── ChatClientConfig.java       默认 ChatClient.Builder
│   ├── VectorStoreConfig.java      pgvector 默认配置
│   └── MemoryConfig.java           ChatMemoryAdvisor
├── advisor/
│   ├── QuestionAnswerAdvisor.java   RAG 增强
│   ├── VectorStoreChatMemoryAdvisor.java
│   └── LoggingAdvisor.java          日志记录
├── model/
│   ├── ChatMessage.java            role/content/tokens/metadata
│   └── KnowledgeBase.java
├── service/
│   ├── EmbeddingService.java       向量化
│   └── RagService.java              检索 + 拼接上下文
└── constant/
    └── AiConstants.java
```

---

### 3.11 spring-cloud-common-satoken（Sa-Token 集成）

**职责**：Sa-Token 上下文透传、SSO Client 基类。

**包结构**：
```
com.xytang.common.satoken
├── config/
│   ├── SaTokenAutoConfiguration.java
│   └── SaSsoClientAutoConfiguration.java
├── filter/
│   └── SaTokenContextFilter.java    从 X-Login-Id 头还原登录态
├── processor/
│   └── SaSsoClientProcessor.java     SSO Client 端处理
├── annotation/
│   └── SaCheckPermission.java        重导出 sa-token 注解
└── context/
    └── LoginUserContext.java          ThreadLocal 当前用户
```

**关键能力**：
- 网关鉴权后，下游服务通过 `SaTokenContextFilter` 从 `X-Login-Id` 头还原登录态
- 子服务直接 `StpUtil.getLoginIdAsLong()` 拿到当前用户 ID，无需再次鉴权

---

### 3.12 spring-cloud-common-security（网关鉴权）

**职责**：网关层统一鉴权过滤器。

**包结构**：
```
com.xytang.common.security
├── filter/
│   ├── SaTokenGatewayFilterFactory.java   网关自定义过滤器
│   └── AuthGatewayFilter.java
├── handler/
│   ├── AuthSuccessHandler.java
│   └── AuthFailureHandler.java             401/403/429 响应
├── config/
│   ├── SecurityAutoConfiguration.java
│   ├── IgnorePathsConfig.java              白名单配置
│   └── CorsConfig.java
└── context/
    └── SatokenContext.java
```

**关键能力**：
- 自定义 `SaTokenGatewayFilterFactory`，校验 Token → 透传 `X-Login-Id`
- 白名单路径：`/api/auth/**`、`/api/system/public/**`、`/actuator/health`

---

### 3.13 spring-cloud-common-log（日志切面）

**职责**：`@OperationLog` 注解 + AOP 切面，异步发送到 MQ。

**包结构**：
```
com.xytang.common.log
├── annotation/
│   └── OperationLog.java          @OperationLog(title="用户管理", businessType=BusinessType.INSERT)
├── aspect/
│   └── OperationLogAspect.java    Around 切面，捕获入参/出参/异常
├── enums/
│   └── BusinessType.java           INSERT/UPDATE/DELETE/EXPORT/IMPORT/OTHER
├── model/
│   └── OperationLogEvent.java      发到 MQ 的事件
└── producer/
    └── LogEventPublisher.java
```

**使用示例**：
```java
@OperationLog(title = "用户管理", businessType = BusinessType.INSERT)
@PostMapping("/users")
public R<UserVO> create(@RequestBody @Validated UserCreateDTO dto) { ... }
```

---

### 3.14 spring-cloud-common-swagger（OpenAPI 文档）

**职责**：springdoc-openapi 配置、Knife4j 聚合。

**包结构**：
```
com.xytang.common.swagger
├── config/
│   ├── SwaggerAutoConfiguration.java
│   ├── OpenApiConfig.java          全局 OpenAPI 定义
│   └── Knife4jConfig.java
├── handler/
│   └── SwaggerResourceHandler.java   聚合各服务文档
└── properties/
    └── SwaggerProperties.java
```

**关键配置**：
- 默认扫描 `com.xytang.*.controller` 包
- Knife4j 增强文档：`http://localhost:8080/doc.html`（Gateway 聚合）
- 生产环境**必须**关闭：`springdoc.api-docs.enabled=false`、`knife4j.production=true`

---

### 3.15 spring-cloud-common-cache（多级缓存）

**职责**：Caffeine + Redis 多级缓存，注解化使用。

**包结构**：
```
com.xytang.common.cache
├── annotation/
│   └── LayeredCache.java           @LayeredCache(key="'user:'+#id", l1Ttl="5s", l2Ttl="5m")
├── aspect/
│   └── LayeredCacheAspect.java
├── manager/
│   ├── MultiLevelCacheManager.java
│   ├── CaffeineCache.java          L1 本地
│   └── RedisCache.java             L2 远程
├── policy/
│   └── CachePenetrationPolicy.java  布隆过滤器 + 空值缓存
└── config/
    └── CacheAutoConfiguration.java
```

**使用示例**：
```java
@LayeredCache(key = "'user:' + #id", l1Ttl = "5s", l2Ttl = "5m")
public User getById(Long id) { return userMapper.selectById(id); }
```

---

### 3.16 spring-cloud-common-netty（WebSocket）

**职责**：Netty Server + WebSocket 协议封装。

**包结构**：
```
com.xytang.common.netty
├── server/
│   ├── NettyServer.java            启动入口（CommandLineRunner）
│   └── WebSocketServerInitializer.java  ChannelInitializer
├── handler/
│   ├── WebSocketFrameHandler.java   文本帧处理
│   ├── HeartbeatHandler.java        心跳 / 断线检测
│   └── AuthHandshakeHandler.java    握手时鉴权
├── router/
│   └── ChannelRouter.java           userId → Channel 路由表（ConcurrentHashMap）
├── protocol/
│   ├── Message.java                 {type, to, content, timestamp}
│   └── MessageType.java             METRIC/PUSH/CHAT/NOTICE
└── config/
    └── NettyAutoConfiguration.java
```

**关键能力**：
- `ChannelRouter` 维护 `userId → List<Channel>`，支持一个用户多端在线
- 握手时从 query 参数取 Token，校验后绑定到 Channel.attr(...)
- 心跳：30s 推一次 `{"type":"ping"}`，60s 无响应关闭连接

---

### 3.17 spring-cloud-common-test（测试基类）

**职责**：测试基类、Testcontainers 集成。

**包结构**：
```
com.xytang.common.test
├── base/
│   ├── BaseServiceTest.java          @SpringBootTest 基类
│   ├── BaseMvcTest.java              @WebMvcTest 基类（MockMvc）
│   └── BaseDataTest.java              @DataJpaTest / @MybatisPlusTest
├── container/
│   ├── MysqlContainer.java           Testcontainers MySQL 8
│   ├── RedisContainer.java
│   ├── RabbitMqContainer.java
│   ├── ElasticSearchContainer.java
│   └── MongoContainer.java
├── annotation/
│   ├── IntegrationTest.java          组合 @SpringBootTest + @Testcontainers
│   └── MockUser.java                 模拟登录用户
└── util/
    └── TestDataBuilder.java           测试数据构造器
```

---

## 4. 技术栈版本矩阵

| 子模块 | 关键依赖 | 版本 |
|--------|----------|------|
| core | Hutool | 5.8.27 |
| core | Jackson | Spring Boot 3.5 管理 |
| web | spring-boot-starter-web | 3.5.0 |
| web | spring-boot-starter-validation | 3.5.0 |
| redis | spring-boot-starter-data-redis | 3.5.0 |
| redisson | redisson-spring-boot-starter | 4.0.0 |
| mybatis | mybatis-plus-spring-boot3-starter | 3.5.9 |
| datasource | dynamic-datasource-spring-boot3-starter | 4.3.1 |
| mq | spring-boot-starter-amqp | 3.5.0 |
| mongo | spring-boot-starter-data-mongodb | 3.5.0 |
| es | spring-boot-starter-data-elasticsearch | 3.5.0 |
| ai | spring-ai-openai-spring-boot-starter | 1.1.0 |
| ai | spring-ai-pgvector-store-spring-boot-starter | 1.1.0 |
| satoken | sa-token-spring-boot3-starter | 1.44.0 |
| security | spring-cloud-starter-gateway | 2025.0.0 |
| log | spring-boot-starter-aop | 3.5.0 |
| swagger | springdoc-openapi-starter-webmvc-ui | 2.6+ |
| cache | caffeine | 3.2+ |
| netty | netty-all | 4.1.x |
| test | testcontainers | 1.20+ |
| test | h2 | 2.x |

> 所有版本**必须**在父 POM 的 `<properties>` 声明，common 子模块**禁止**覆盖。

---

## 5. 公共能力 → 业务模块的依赖映射

| 业务模块 | 依赖的 common 子模块 |
|----------|---------------------|
| spring-cloud-gateway | security、swagger、core、web（部分） |
| spring-cloud-auth | core、web、redis、redisson、satoken、mq、cache、log、swagger |
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

> 这样业务方加依赖即可用，无需 @Import。

### 6.3 注解化原则

- 公共能力**尽量**用注解暴露（如 `@DistributedLock`、`@RateLimit`、`@OperationLog`）
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

### 6.7 日志规范

- **必须**用 `@Slf4j`（Lombok），**禁止**手动 `LoggerFactory.getLogger(...)`
- **禁止**打印密码、Token、身份证号
- 关键操作**必须**用 `@OperationLog` 注解
- 慢操作（>500ms）**必须**打 WARN 日志

### 6.8 测试规范

- 每个注解**必须**有对应的切面测试
- 每个 AutoConfiguration**必须**有 `@EnabledAutoConfiguration` 测试
- 公共工具类**必须**有 100% 覆盖率测试

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
