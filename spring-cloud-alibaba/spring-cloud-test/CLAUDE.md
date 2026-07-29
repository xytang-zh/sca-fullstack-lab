# CLAUDE.md — spring-cloud-test 集成测试模块

> 本文档面向 AI 编码助手，用于在 `spring-cloud-test/` 目录下工作时提供模块约束、技术栈版本、测试策略与开发规范。
> 工作前**必须**先读取父目录的 [`spring-cloud-alibaba/CLAUDE.md`](../CLAUDE.md) 了解全局规范。

---

## 1. 模块定位

`spring-cloud-test` 是项目的 **端到端（E2E）集成测试模块**，承担：

1. **跨服务集成测试**：验证微服务之间的 Dubbo RPC、RabbitMQ 事件、Sa-Token 透传是否正常
2. **核心链路回归测试**：每次提交前跑核心链路（登录 → CRUD → 审批 → 推送）
3. **SSO 全流程测试**：从 Client 跳转 → Server 登录 → 颁发 code → 换 token → 业务调用
4. **踢人下线测试**：模拟同账号两台设备登录，验证踢下线逻辑
5. **数据库迁移验证**：Flyway 脚本在多库（MySQL/PG/KingbaseES/DM）上能正确执行
6. **并发与限流测试**：高并发下 Sentinel 限流是否生效、Redisson 分布式锁是否正确

| 维度 | 值 |
|------|-----|
| 父 POM | `com.xytang:spring-cloud-alibaba:1.0-SNAPSHOT` |
| 当前 artifactId | `spring-cloud-test` |
| packaging | `jar` |
| 顶级包 | `com.xytang.test` |
| 测试类型 | 集成测试（@SpringBootTest + Testcontainers） |

> ⚠️ **本模块只用于测试，禁止打 JAR 部署到生产**。CI 流水线在 `mvn verify` 阶段执行本模块的所有测试。

---

## 2. 模块结构

```
spring-cloud-test/
├── pom.xml
└── src/
    ├── main/                                  （本模块无 main 代码，仅 test）
    │   └── resources/
    │       └── logback-test.xml               测试日志配置
    └── test/
        ├── java/com/xytang/test/
        │   ├── base/
        │   │   ├── BaseE2ETest.java              E2E 测试基类
        │   │   ├── BaseIntegrationTest.java       集成测试基类
        │   │   └── BaseContainerTest.java          Testcontainers 基类
        │   ├── container/
        │   │   ├── ComposeContainers.java          Docker Compose 容器集合
        │   │   ├── MysqlTestContainer.java         单 MySQL 容器
        │   │   ├── RedisTestContainer.java
        │   │   ├── RabbitMqTestContainer.java
        │   │   ├── MongoTestContainer.java
        │   │   ├── ElasticSearchTestContainer.java
        │   │   └── TdengineTestContainer.java
        │   ├── scenario/
        │   │   ├── SsoLoginScenarioTest.java       SSO 完整流程
        │   │   ├── KickoutScenarioTest.java        踢人下线
        │   │   ├── OAuth2AuthorizeScenarioTest.java OAuth2 授权
        │   │   ├── UserCrudScenarioTest.java        用户 CRUD 全链路
        │   │   ├── WorkflowApprovalScenarioTest.java 工作流审批
        │   │   ├── RagChatScenarioTest.java          RAG 问答
        │   │   └── MonitorPushScenarioTest.java      监控推送
        │   ├── rpc/
        │   │   ├── DubboRpcTest.java               Dubbo 跨服务调用
        │   │   └── FeignFallbackTest.java          Feign 降级
        │   ├── event/
        │   │   ├── UserLoginEventTest.java         MQ 事件
        │   │   ├── TaskTodoEventTest.java
        │   │   └── OperationLogEventTest.java
        │   ├── concurrency/
        │   │   ├── DistributedLockTest.java       Redisson 分布式锁
        │   │   ├── RateLimitTest.java              Sentinel 限流
        │   │   └── CachePenetrationTest.java       缓存穿透
        │   ├── migration/
        │   │   ├── FlywayMysqlTest.java            Flyway 在 MySQL
        │   │   ├── FlywayKingbaseTest.java         Flyway 在 KingbaseES
        │   │   └── FlywayDmTest.java               Flyway 在 DM
        │   └── util/
        │       ├── TestDataBuilder.java            测试数据构造器
        │       ├── TokenHelper.java                 测试用 Token 生成
        │       └── RestClientHelper.java            RestTemplate 封装
        └── resources/
            ├── docker-compose.test.yml              测试环境容器
            ├── sql/
            │   ├── init-test-data.sql              测试种子数据
            │   └── cleanup-test-data.sql            清理脚本
            ├── workflow/
            │   └── leave-flow.json                 测试用流程定义
            └── application-test.yml                测试配置
```

---

## 3. 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| JUnit 5 | 5.10+ | 测试框架 |
| Spring Boot Test | 3.5.0 | `@SpringBootTest`、`@WebMvcTest`、`MockMvc` |
| Testcontainers | 1.20+ | Docker 容器化测试（MySQL/Redis/MQ/ES/Mongo/TDengine） |
| WireMock | 3.x | HTTP Mock（模拟第三方 OAuth2 服务） |
| Awaitility | 4.x | 异步断言（MQ 事件、WebSocket 推送） |
| RestAssured | 5.x | API 集成测试 DSL |
| Mockito | 5.x | Mock 框架 |
| AssertJ | 3.25+ | 流式断言 |
| H2 | 2.x | 内存数据库（快速单测） |
| ArchUnit | 1.3+ | 架构守护（包依赖、分层规范） |
| JaCoCo | 0.8+ | 覆盖率统计 |

---

## 4. POM 依赖

```xml
<dependencies>
    <!-- Spring Boot Test -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- Testcontainers -->
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>testcontainers</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>mysql</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>postgresql</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>redis</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>com.redis</groupId>
        <artifactId>testcontainers-redis</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>rabbitmq</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>elasticsearch</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>mongodb</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- 其他工具 -->
    <dependency>
        <groupId>org.awaitility</groupId>
        <artifactId>awaitility</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>io.rest-assured</groupId>
        <artifactId>rest-assured</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>com.tngtech.archunit</groupId>
        <artifactId>archunit-junit5</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.wiremock</groupId>
        <artifactId>wiremock-standalone</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- 内部测试基类 -->
    <dependency>
        <groupId>com.xytang</groupId>
        <artifactId>spring-cloud-common-test</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- 引入所有业务服务依赖（用于 E2E 测试启动完整上下文） -->
    <dependency>
        <groupId>com.xytang</groupId>
        <artifactId>spring-cloud-auth</artifactId>
        <version>${project.version}</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>com.xytang</groupId>
        <artifactId>spring-cloud-system</artifactId>
        <version>${project.version}</version>
        <scope>test</scope>
    </dependency>
    <!-- 其他业务服务按需引入 -->
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <configuration>
                <argLine>@{argLine} -Xmx2g</argLine>
                <forkCount>1</forkCount>
                <reuseForks>false</reuseForks>
            </configuration>
        </plugin>
        <plugin>
            <groupId>org.jacoco</groupId>
            <artifactId>jacoco-maven-plugin</artifactId>
            <executions>
                <execution>
                    <goals><goal>report</goal></goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

---

## 5. 测试分层策略

### 5.1 测试金字塔

```
        /\
       /  \         E2E 测试（少量，慢，本模块）
      /----\
     /      \       集成测试（中量，中速，本模块 + 各服务内）
    /--------\
   /          \     单元测试（大量，快，各业务服务内）
  /____________\
```

### 5.2 测试类型与职责划分

| 测试类型 | 所在位置 | 速度 | 职责 |
|----------|----------|------|------|
| 单元测试 | 各业务服务的 `src/test/.../service/` | <100ms | 验证 Service 业务逻辑 |
| Web 层测试 | 各业务服务的 `src/test/.../controller/` | <1s | 验证 Controller 参数解析、响应格式 |
| 集成测试 | `spring-cloud-test/scenario/` | 1-10s | 验证多服务协作（Dubbo、MQ） |
| 容器测试 | `spring-cloud-test/migration/` | 10-60s | 验证数据库迁移、Flyway 多库 |
| E2E 测试 | `spring-cloud-test/scenario/` | >30s | 验证完整业务链路 |
| 架构守护 | `spring-cloud-test/` | <1s | 验证包依赖、分层规范 |

### 5.3 测试命名约定

- 测试类：`{业务}{场景}Test.java`，如 `UserCrudScenarioTest`、`SsoLoginScenarioTest`
- 测试方法：`should_{期望结果}_when_{条件}`，如 `should_returnOnlineUsers_when_adminLogin`
- 测试方法**必须**用 `@DisplayName` 中文描述：
  ```java
  @Test
  @DisplayName("管理员登录后可以查看在线用户列表")
  void should_returnOnlineUsers_when_adminLogin() { ... }
  ```

---

## 6. 测试基类

### 6.1 BaseContainerTest（容器基类）

```java
@Testcontainers
public abstract class BaseContainerTest {

    @Container
    protected static final MySQLContainer<?> MYSQL =
        new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("aurora_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    protected static final GenericContainer<?> REDIS =
        new GenericContainer<>("redis:7.4-alpine")
            .withExposedPorts(6379);

    @Container
    protected static final RabbitMQContainer RABBIT =
        new RabbitMQContainer("rabbitmq:3.13-management");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.dynamic.datasource.master.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.dynamic.datasource.master.username", MYSQL::getUsername);
        registry.add("spring.datasource.dynamic.datasource.master.password", MYSQL::getPassword);
        registry.add("spring.redis.host", REDIS::getHost);
        registry.add("spring.redis.port", REDIS::getFirstMappedPort);
        registry.add("spring.rabbitmq.host", RABBIT::getHost);
        registry.add("spring.rabbitmq.port", RABBIT::getAmqpPort);
    }
}
```

### 6.2 BaseIntegrationTest（集成测试基类）

```java
@SpringBootTest(
    classes = {AuroraSystemApplication.class, AuroraAuthApplication.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(BaseContainerTest.class)
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected ObjectMapper objectMapper;

    protected HttpHeaders authHeaders(Long loginId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Login-Id", String.valueOf(loginId));
        return headers;
    }
}
```

### 6.3 BaseE2ETest（E2E 基类）

```java
@SpringBootTest(classes = {AllServicesConfiguration.class})
@ActiveProfiles("e2e")
@TestPropertySource(properties = {
    "spring.cloud.nacos.discovery.enabled=false",
    "spring.cloud.nacos.config.enabled=false"
})
public abstract class BaseE2ETest {

    @LocalServerPort
    protected int port;

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected RabbitTemplate rabbitTemplate;

    protected String baseUrl() {
        return "http://localhost:" + port;
    }

    protected void waitForEvent(String exchange, String routingKey, Duration timeout) {
        await().atMost(timeout).untilAsserted(() -> {
            // 验证消息已消费
        });
    }
}
```

---

## 7. 关键场景测试用例

### 7.1 SsoLoginScenarioTest（SSO 完整流程）

```java
@DisplayName("SSO 单点登录完整流程")
class SsoLoginScenarioTest extends BaseE2ETest {

    @Test
    @DisplayName("首次登录：Client A 跳转 Server → 登录 → 回跳 → 鉴权")
    void should_fullFlow_when_firstLogin() {
        // 1. 访问 Client A 受保护资源
        ResponseEntity<String> resp1 = restTemplate.getForEntity(
            baseUrl() + "/api/system/users/1", String.class);
        assertThat(resp1.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // 2. 跳转 SSO Server 登录
        ResponseEntity<String> resp2 = restTemplate.postForEntity(
            baseUrl() + "/sso/auth?redirect=client-a",
            new LoginDTO("admin", "admin123", "captcha-token", "device-001"),
            String.class);
        assertThat(resp2.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        String location = resp2.getHeaders().getFirst("Location");
        assertThat(location).contains("/sso/callback?code=");

        // 3. 用 code 换 token
        String code = extractCode(location);
        ResponseEntity<R> resp3 = restTemplate.postForEntity(
            baseUrl() + "/sso/code2session",
            new Code2SessionDTO(code, "client-a"),
            R.class);
        assertThat(resp3.getBody().getCode()).isEqualTo(200);
    }

    @Test
    @DisplayName("二次跳转：已登录 Server 时访问 Client B 不需要再次输密码")
    void should_skipPassword_when_secondClient() { ... }
}
```

### 7.2 KickoutScenarioTest（踢人下线）

```java
@DisplayName("踢人下线场景")
class KickoutScenarioTest extends BaseE2ETest {

    @Test
    @DisplayName("同账号第二台设备登录 → 第一台设备下线")
    void should_kickoutFirstDevice_when_sameAccountLoginOnSecondDevice() {
        // 1. 设备 A 登录
        LoginVO loginA = authService.login("admin", "pwd", "PC", "device-A");
        String tokenA = loginA.getAccessToken();

        // 2. 用 tokenA 访问资源（成功）
        ResponseEntity<R> resp1 = restTemplate.exchange(
            "/api/system/users/1", GET,
            withToken(tokenA), R.class);
        assertThat(resp1.getStatusCode()).isEqualTo(OK);

        // 3. 设备 B 登录（同账号同设备类型）
        LoginVO loginB = authService.login("admin", "pwd", "PC", "device-B");

        // 4. 用 tokenA 访问资源（应失败 - 被踢下线）
        ResponseEntity<R> resp2 = restTemplate.exchange(
            "/api/system/users/1", GET,
            withToken(tokenA), R.class);
        assertThat(resp2.getStatusCode()).isEqualTo(UNAUTHORIZED);
        assertThat(resp2.getBody().getCode()).isEqualTo(40102);  // 被踢下线
    }

    @Test
    @DisplayName("管理员主动踢下线 → 用户立即失效")
    void should_userInvalid_when_adminKickout() { ... }
}
```

### 7.3 WorkflowApprovalScenarioTest（工作流审批）

```java
@DisplayName("工作流审批完整链路")
class WorkflowApprovalScenarioTest extends BaseE2ETest {

    @Test
    @DisplayName("请假流程：申请人发起 → 直属领导审批 → HR 审批 → 归档")
    void should_leaveFlow_when_normalApprove() {
        // 1. 申请人发起请假
        Long instanceId = workflowService.startLeave(LeaveDTO.builder()
            .applicantId(10001L).days(3).type("SICK").reason("感冒").build());

        // 2. 直属领导审批通过
        Long taskId = workflowService.getTodoList(10002L).get(0).getId();
        workflowService.approve(taskId, "同意");

        // 3. 验证：消息推送 MQ 已发送（用 Awaitility 等待）
        await().atMost(5, SECONDS).untilAsserted(() -> {
            verify(messageListener, times(1)).onMessage(any(TaskTodoEvent.class));
        });

        // 4. HR 审批
        Long nextTaskId = workflowService.getTodoList(10003L).get(0).getId();
        workflowService.approve(nextTaskId, "归档");

        // 5. 验证：流程实例已完成
        assertThat(workflowService.getInstance(instanceId).getStatus())
            .isEqualTo(FlowStatusEnum.COMPLETED);
    }
}
```

### 7.4 DistributedLockTest（分布式锁）

```java
@DisplayName("Redisson 分布式锁")
class DistributedLockTest extends BaseIntegrationTest {

    @Test
    @DisplayName("并发 100 个请求抢同一把锁 → 只有一个进入临界区")
    void should_onlyOneEnter_when_100Concurrent() throws Exception {
        int threadCount = 100;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                start.await();
                try {
                    testService.lockAndIncrement(successCount);
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        done.await();
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(1);
    }
}
```

### 7.5 ArchUnitTest（架构守护）

```java
@AnalyzeClasses(packages = "com.xytang")
class ArchUnitTest {

    @ArchTest
    static final ArchRule common_should_not_depend_on_business =
        noClasses().that().resideInAPackage("..common..")
            .should().dependOnClassesThat()
            .resideInAPackage("..system..")
            .orShould().dependOnClassesThat()
            .resideInAPackage("..auth..");

    @ArchTest
    static final ArchRule controllers_should_only_be_in_business_services =
        classes().that().areAnnotatedWith(RestController.class)
            .should().resideInAPackage("..controller..");

    @ArchTest
    static final ArchRule no_cycles_between_packages =
        slices().matching("..(*)..").should().beFreeOfCycles();
}
```

---

## 8. 必须遵守的开发规范

### 8.1 测试设计规范

1. **必须**遵循 AAA 模式：Arrange（准备）→ Act（执行）→ Assert（断言）
2. **必须**用 `@DisplayName` 中文描述测试用例
3. **必须**用 AssertJ 流式断言，**禁止**用 JUnit 原生 `assertEquals`
4. **必须**有明确的 Given-When-Then 注释（或代码块）
5. **禁止**测试方法之间相互依赖（每个测试必须独立）
6. **禁止**测试方法依赖执行顺序
7. **必须**在 `@BeforeEach` 中清理测试数据
8. **必须**用 `@Sql` 或 `@SqlGroup` 注解加载测试数据

### 8.2 容器使用规范

1. **必须**用 Testcontainers 而非 H2 测试集成逻辑（H2 与 MySQL SQL 方言差异大）
2. 容器**必须**用 `@Container` 静态字段，确保整个测试类共享一个容器（启动慢）
3. **必须**用 `@DynamicPropertySource` 注入容器连接信息
4. **禁止**在 `@BeforeEach` 中启容器（太慢）
5. 容器版本**必须**与生产环境一致：MySQL 8.4、Redis 7.4、RabbitMQ 3.13

### 8.3 异步测试规范

1. **必须**用 `Awaitility` 等待异步事件，**禁止**用 `Thread.sleep`
2. 等待超时**必须** ≥ 5s（避免 CI 环境 Flaky）
3. **必须**有明确断言（不能光等待不断言）
4. MQ 测试**必须**验证幂等性（重复消费不出错）

```java
// ✅ 正确
await().atMost(5, SECONDS).untilAsserted(() ->
    assertThat(repository.count()).isEqualTo(1));

// ❌ 错误
Thread.sleep(2000);
assertThat(repository.count()).isEqualTo(1);
```

### 8.4 Mock 规范

1. **必须**用 `@MockBean` 替换外部依赖（如发短信、第三方 OAuth2）
2. **禁止** Mock 自己的服务（用真实组件测集成链路）
3. **必须**用 `verify(mock, times(n))` 验证调用次数
4. **必须**用 `ArgumentCaptor` 捕获参数验证细节

### 8.5 测试数据规范

1. **必须**用 `@Sql` 或 `@SqlGroup` 加载测试数据，**禁止**在代码里手写 `INSERT INTO`
2. 测试数据**必须**有明确含义，**禁止**用 `user1`、`test123`
3. **必须**在 `@AfterEach` 中清理测试数据（或用 `@Sql(executionPhase = AFTER_TEST_METHOD)`）
4. 测试数据库**必须**与开发数据库隔离

### 8.6 CI 集成规范

1. CI 流水线**必须**在 `mvn verify` 阶段执行本模块所有测试
2. 失败的测试**必须**立即修复，**禁止**用 `@Disabled` 跳过
3. Flaky 测试**必须**重试 3 次稳定后才能合并
4. 测试覆盖率**必须** ≥ 70%（用 JaCoCo 统计）

### 8.7 性能与资源规范

1. 单个测试方法**必须**在 30s 内完成
2. 单个测试类**必须**在 60s 内完成
3. 整个 spring-cloud-test 模块**必须**在 10 分钟内完成
4. **必须**用 `@TestInstance(Lifecycle.PER_CLASS)` 减少 JVM 启动次数
5. **必须**用 `@AutoConfigureMockMvc` 而非 `@SpringBootTest(webEnvironment=RANDOM_PORT)` 加 RestTemplate（更快）

---

## 9. application-test.yml

```yaml
spring:
  profiles:
    active: test
  main:
    lazy-initialization: true   # 测试用懒加载，加速启动
  datasource:
    dynamic:
      datasource:
        master:
          url: jdbc:tc:mysql:8.4:///aurora_test
          driver-class-name: org.testcontainers.jdbc.ContainerDatabaseDriver
  redis:
    host: ${REDIS_HOST:127.0.0.1}
    port: ${REDIS_PORT:6379}
  rabbitmq:
    host: ${RABBITMQ_HOST:127.0.0.1}
    port: ${RABBITMQ_PORT:5672}
  cloud:
    nacos:
      discovery:
        enabled: false          # 测试用 standalone
      config:
        enabled: false

sa-token:
  timeout: 300
  is-concurrent: false
  is-kickout: true
  jwt-secret-key: test-secret-key-for-test-only

logging:
  level:
    root: WARN
    com.xytang: DEBUG
```

---

## 10. 必须实现的测试清单

| # | 类 | 测试方法 | 验证点 |
|---|----|----------|--------|
| 1 | SsoLoginScenarioTest | 首次登录全流程 | Client → Server → callback → code2session |
| 2 | SsoLoginScenarioTest | 二次跳转免密码 | Server 会话存在时不要求重新登录 |
| 3 | KickoutScenarioTest | 同账号并发踢下线 | is-concurrent=false 生效 |
| 4 | KickoutScenarioTest | 管理员主动踢下线 | StpUtil.kickout 生效 |
| 5 | OAuth2AuthorizeScenarioTest | 授权码模式全流程 | authorize → code → token → userinfo |
| 6 | UserCrudScenarioTest | 用户 CRUD 全链路 | 增删改查 + 缓存一致性 |
| 7 | UserCrudScenarioTest | 用户导入导出 Excel | EasyExcel 正常工作 |
| 8 | WorkflowApprovalScenarioTest | 请假流程 | 发起 → 审批 → 归档 |
| 9 | WorkflowApprovalScenarioTest | 驳回 + 转办 | reject + transfer |
| 10 | RagChatScenarioTest | 上传文档 → 向量化 → 提问 | RAG 完整链路 |
| 11 | MonitorPushScenarioTest | WebSocket 推送 | Netty 实时推送 |
| 12 | DistributedLockTest | 并发 100 抢锁 | 只有一个进入 |
| 13 | RateLimitTest | 限流阈值 | 超过阈值返回 429 |
| 14 | CachePenetrationTest | 缓存穿透 | 布隆过滤器生效 |
| 15 | FlywayMysqlTest | Flyway 在 MySQL | 脚本执行不报错 |
| 16 | FlywayKingbaseTest | Flyway 在 KingbaseES | 国产库兼容 |
| 17 | DubboRpcTest | Dubbo 跨服务调用 | Provider 正常返回 |
| 18 | UserLoginEventTest | MQ 用户登录事件 | 异步消费 + 幂等 |
| 19 | ArchUnitTest | common 不依赖业务 | 架构守护 |
| 20 | ArchUnitTest | Controller 只在 controller 包 | 架构守护 |

---

## 11. 测试运行命令

```bash
# 运行本模块所有测试（CI 用）
mvn clean verify -pl spring-cloud-test -am

# 运行单个测试类
mvn test -pl spring-cloud-test -Dtest=SsoLoginScenarioTest

# 运行单个测试方法
mvn test -pl spring-cloud-test -Dtest=SsoLoginScenarioTest#should_fullFlow_when_firstLogin

# 跳过测试（紧急发布时）
mvn clean install -DskipTests

# 生成覆盖率报告
mvn clean verify -pl spring-cloud-test jacoco:report
# 报告位置：target/site/jacoco/index.html
```

---

## 12. 红线（违反即拒绝）

1. ❌ 用 `Thread.sleep` 等待异步（必须用 Awaitility）
2. ❌ 用 H2 测试集成逻辑（必须用 Testcontainers MySQL）
3. ❌ 测试方法之间相互依赖
4. ❌ 用 `@Disabled` 跳过失败的测试（必须修复）
5. ❌ 用 JUnit 原生 `assertEquals`（必须用 AssertJ）
6. ❌ 测试方法没有 `@DisplayName` 中文描述
7. ❌ 在 `@BeforeEach` 中启容器（太慢）
8. ❌ Mock 自己的服务（应该用真实组件）
9. ❌ 在测试中硬编码密码 / Token（必须用 `application-test.yml`）
10. ❌ 测试覆盖率 < 70%
11. ❌ 单个测试超过 30s
12. ❌ 测试用例命名不清晰（必须用 `should_X_when_Y` 命名）
13. ❌ 没有 `@AfterEach` 清理测试数据
14. ❌ 把本模块打 JAR 部署到生产（本模块只用于测试）
