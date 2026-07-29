# CLAUDE.md — spring-cloud-starters 自定义 Starter 聚合

> 本文档面向 AI 编码助手，用于在 `spring-cloud-starters/` 目录下（或任意子 Starter 下）工作时提供模块约束、技术栈版本、Starter 设计原则与开发规范。
> 工作前**必须**先读取父目录的 [`spring-cloud-alibaba/CLAUDE.md`](../CLAUDE.md) 了解全局规范。

---

## 1. 聚合模块定位

`spring-cloud-starters` 是项目自定义 Starter 的 **Maven 聚合 POM**，目前包含 2 个 Starter：

1. `spring-cloud-starter-sso-client` — SSO Client 自动装配，所有需要接入 SSO 的微服务引入此依赖即可零配置启用 SSO Client 能力
2. `spring-cloud-starter-monitor-agent` — 监控 Agent，所有需要上报监控指标的微服务引入此依赖即可自动采集 OSHI 指标并上报到 `spring-cloud-monitor`

| 维度 | 值 |
|------|-----|
| 父 POM | `com.xytang:spring-cloud-alibaba:1.0-SNAPSHOT` |
| 当前 artifactId | `spring-cloud-starters` |
| packaging | `pom` |
| 子 Starter 数量 | 2 |
| 顶级包 | `com.xytang.starter.{能力名}` |

---

## 2. 子 Starter 清单

| # | Starter | 用途 | 使用者 |
|---|---------|------|--------|
| 1 | `spring-cloud-starter-sso-client` | SSO Client 端自动装配（拦截未登录 → 跳 auth） | 所有业务微服务（除 auth） |
| 2 | `spring-cloud-starter-monitor-agent` | 监控指标采集与上报 | 所有需要监控的微服务 |

> 后续可扩展：`spring-cloud-starter-workflow`（Warm-Flow 自动装配）、`spring-cloud-starter-sharding`（分表自动装配）。

---

## 3. Starter 设计原则（强制约束）

### 3.1 命名规范

- **第三方 Starter**：`{名}-spring-boot-starter`（如 `xxx-spring-boot-starter`，官方推荐）
- **本项目 Starter**：`spring-cloud-starter-{能力名}`（如 `spring-cloud-starter-sso-client`）

> 命名原因：本项目是 Spring Cloud 体系，使用 `spring-cloud-starter-` 前缀更符合规范。**禁止**用 `spring-boot-starter-{name}`（这是 Spring Boot 官方保留前缀）。

### 3.2 设计原则

1. **零配置开箱即用**：业务方加依赖即可用，不需要任何 `@Import` 或 `@EnableXxx`
2. **可选退出**：所有 AutoConfiguration **必须**有 `@ConditionalOnXxx`，业务方可通过配置项关闭
3. **依赖最小化**：Starter **禁止**传递业务依赖（如 `mybatis-plus`），只包含能力本身需要的依赖
4. **配置项前缀统一**：`spring-cloud.{能力名}.*`，如 `spring-cloud.sso-client.*`、`spring-cloud.monitor-agent.*`
5. **不暴露实现类**：所有实现类**必须**在 `internal` 或 `impl` 子包，对外只暴露注解和接口
6. **不写 Controller**：Starter **禁止**包含 Controller（如需暴露 HTTP 端点，必须用 `@RestController` + `@ConditionalOnProperty(name="xxx.enabled", havingValue="true")`）

### 3.3 自动装配规范

#### 3.3.1 Spring Boot 2.7+ 风格（推荐）

放在 `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`：

```
com.xytang.starter.ssoclient.config.SsoClientAutoConfiguration
com.xytang.starter.ssoclient.config.SsoClientWebMvcAutoConfiguration
```

> **禁止**用旧的 `META-INF/spring.factories`（已废弃）。

#### 3.3.2 AutoConfiguration 类规范

```java
@AutoConfiguration
@ConditionalOnProperty(
    prefix = "spring-cloud.sso-client",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true          // 默认启用
)
@EnableConfigurationProperties(SsoClientProperties.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({StpUtil.class, SaSsoClientProcessor.class})
public class SsoClientAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SaSsoClientProcessor saSsoClientProcessor(SsoClientProperties props) {
        return new SaSsoClientProcessor(props);
    }

    @Bean
    @ConditionalOnMissingBean
    public SaTokenContextFilter saTokenContextFilter() {
        return new SaTokenContextFilter();
    }
}
```

要点：
- `@AutoConfiguration` 而非 `@Configuration`（保证加载顺序）
- `@ConditionalOnMissingBean` 让业务方可覆盖
- `@ConditionalOnClass` 防止依赖缺失导致启动失败
- `@ConditionalOnWebApplication(type = SERVLET)` 限定应用类型

---

## 4. spring-cloud-starter-sso-client 详解

### 4.1 定位

为业务微服务提供 SSO Client 能力，自动完成：
1. 拦截未登录请求 → 跳转 `auth` 中心
2. 接收 `auth` 颁发的授权码 → 用 code 换 token
3. 从 `X-Login-Id` 头还原登录态（与 Gateway 协作）
4. 全端注销回调

### 4.2 模块结构

```
spring-cloud-starter-sso-client/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/xytang/starter/ssoclient/
    │   │   ├── config/
    │   │   │   ├── SsoClientAutoConfiguration.java         主装配
    │   │   │   ├── SsoClientWebMvcAutoConfiguration.java   MVC 配置
    │   │   │   └── SsoClientProperties.java               配置项
    │   │   ├── filter/
    │   │   │   └── SaTokenContextFilter.java                从 X-Login-Id 还原登录态
    │   │   ├── processor/
    │   │   │   └── SaSsoClientProcessor.java                SSO Client 主处理器
    │   │   ├── interceptor/
    │   │   │   └── SsoLoginInterceptor.java                  未登录拦截 → 跳 auth
    │   │   └── constant/
    │   │       └── SsoClientConstants.java
    │   └── resources/
    │       └── META-INF/
    │           ├── spring/
    │           │   └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
    │           └── additional-spring-configuration-metadata.json    IDE 提示
    └── test/
        └── java/com/xytang/starter/ssoclient/
            └── SsoClientAutoConfigurationTest.java
```

### 4.3 配置项

```yaml
spring-cloud:
  sso-client:
    enabled: true                              # 默认启用，可关闭
    server-url: http://auth.example.com/sso    # SSO Server 地址
    client-name: spring-cloud-system           # Client 标识（用于校验 allowUrls）
    sign-key: ${SSO_SIGN_KEY:change-me}        # 签名密钥（与 Server 端一致）
    timeout: 60                                # 通信超时（秒）
    login-path: /sso/login                     # 本地登录回调路径
    callback-path: /sso/callback               # code 回调路径
    logout-path: /sso/logout                   # 单端注销路径
    allow-urls:                                # 允许的回调 URL 白名单
      - http://localhost:5173/**
      - https://*.example.com/**
```

### 4.4 POM 依赖

```xml
<dependencies>
    <!-- 内部依赖 -->
    <dependency>
        <groupId>com.xytang</groupId>
        <artifactId>spring-cloud-common-core</artifactId>
    </dependency>
    <dependency>
        <groupId>com.xytang</groupId>
        <artifactId>spring-cloud-common-satoken</artifactId>
    </dependency>
    <dependency>
        <groupId>com.xytang</groupId>
        <artifactId>spring-cloud-common-web</artifactId>
    </dependency>

    <!-- Sa-Token SSO Client -->
    <dependency>
        <groupId>cn.dev33</groupId>
        <artifactId>sa-token-spring-boot3-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>cn.dev33</groupId>
        <artifactId>sa-token-sso</artifactId>
    </dependency>
    <dependency>
        <groupId>cn.dev33</groupId>
        <artifactId>sa-token-redis-jackson</artifactId>
    </dependency>

    <!-- 自动装配必需 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-autoconfigure</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-configuration-processor</artifactId>
        <optional>true</optional>           <!-- 不传递给业务方 -->
    </dependency>
</dependencies>
```

### 4.5 使用方式

业务服务只需在 POM 中加依赖：

```xml
<dependency>
    <groupId>com.xytang</groupId>
    <artifactId>spring-cloud-starter-sso-client</artifactId>
</dependency>
```

无需任何 `@Import` 或 `@EnableXxx`，自动启用 SSO Client 能力。

### 4.6 关键代码

#### 4.6.1 SaTokenContextFilter（从 X-Login-Id 还原登录态）

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class SaTokenContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws ServletException, IOException {
        // 1. Gateway 已经鉴权，下游从 X-Login-Id 头读取
        String loginId = req.getHeader("X-Login-Id");
        if (StrUtil.isNotBlank(loginId)) {
            // 2. 还原 Sa-Token 上下文
            SaTokenContext.setLoginId(loginId);
            // 3. 也放一份到 ThreadLocal，方便业务层 @LoginUser 取
            LoginUserContext.set(loginId);
        }
        try {
            chain.doFilter(req, resp);
        } finally {
            LoginUserContext.clear();
        }
    }
}
```

#### 4.6.2 SsoLoginInterceptor（未登录跳转）

```java
@Component
@RequiredArgsConstructor
public class SsoLoginInterceptor implements HandlerInterceptor {

    private final SsoClientProperties props;

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) {
        Object loginId = StpUtil.getLoginIdByToken(req.getHeader("X-Token"));
        if (loginId == null) {
            // 未登录 → 跳转 SSO Server
            String redirect = req.getRequestURL().toString();
            String ssoUrl = props.getServerUrl() + "/auth?redirect=" + UrlUtil.encode(redirect);
            resp.sendRedirect(ssoUrl);
            return false;
        }
        return true;
    }
}
```

---

## 5. spring-cloud-starter-monitor-agent 详解

### 5.1 定位

为业务微服务提供监控指标采集能力，自动完成：
1. 用 OSHI 采集本机 CPU、内存、磁盘、JVM 指标
2. 定时（默认 5 秒）上报到 `spring-cloud-monitor` 服务
3. 暴露 `/actuator/prometheus` 给 Prometheus 拉

### 5.2 模块结构

```
spring-cloud-starter-monitor-agent/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/xytang/starter/monitoragent/
    │   │   ├── config/
    │   │   │   ├── MonitorAgentAutoConfiguration.java
    │   │   │   └── MonitorAgentProperties.java
    │   │   ├── collector/
    │   │   │   ├── MetricsCollector.java              指标采集接口
    │   │   │   ├── OshiMetricsCollector.java          OSHI 系统指标
    │   │   │   ├── JvmMetricsCollector.java            JVM 指标
    │   │   │   └── BusinessMetricsCollector.java       业务自定义指标
    │   │   ├── reporter/
    │   │   │   ├── MetricsReporter.java                上报接口
    │   │   │   ├── HttpMetricsReporter.java            HTTP 上报到 monitor
    │   │   │   └── NoopMetricsReporter.java            关闭时用
    │   │   ├── scheduler/
    │   │   │   └── MetricsReportScheduler.java          定时上报
    │   │   ├── model/
    │   │   │   ├── MetricsSnapshot.java                指标快照
    │   │   │   └── ServiceInstance.java
    │   │   └── constant/
    │   │       └── MonitorConstants.java
    │   └── resources/
    │       └── META-INF/
    │           ├── spring/
    │           │   └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
    │           └── additional-spring-configuration-metadata.json
    └── test/
        └── java/com/xytang/starter/monitoragent/
            ├── MonitorAgentAutoConfigurationTest.java
            └── OshiMetricsCollectorTest.java
```

### 5.3 配置项

```yaml
spring-cloud:
  monitor-agent:
    enabled: true                              # 默认启用
    monitor-url: http://spring-cloud-monitor/api/agent/report   # 上报地址
    service-name: ${spring.application.name}  # 服务名（默认取应用名）
    instance: ${HOSTNAME:unknown}:${server.port}   # 实例标识
    env: ${spring.profiles.active:dev}        # 环境
    interval: 5                                # 采集间隔（秒）
    timeout: 3                                 # 上报超时（秒）
    enabled-metrics:                          # 启用的采集器
      - system                                # 系统指标（CPU、内存、磁盘）
      - jvm                                   # JVM 指标
      - business                              # 业务自定义指标
    business-metrics:                         # 业务自定义指标（占位，业务方实现 BusinessMetricsCollector）
      enabled: false
```

### 5.4 POM 依赖

```xml
<dependencies>
    <!-- 内部依赖 -->
    <dependency>
        <groupId>com.xytang</groupId>
        <artifactId>spring-cloud-common-core</artifactId>
    </dependency>

    <!-- OSHI 系统信息采集 -->
    <dependency>
        <groupId>com.github.oshi</groupId>
        <artifactId>oshi-core</artifactId>
        <version>6.6.0</version>
    </dependency>

    <!-- HTTP 客户端（上报用） -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- 自动装配必需 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-autoconfigure</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-configuration-processor</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

### 5.5 关键代码

#### 5.5.1 MonitorAgentAutoConfiguration

```java
@AutoConfiguration
@ConditionalOnProperty(
    prefix = "spring-cloud.monitor-agent",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
@EnableConfigurationProperties(MonitorAgentProperties.class)
@ConditionalOnClass({MetricsCollector.class, SystemInfo.class})
public class MonitorAgentAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public OshiMetricsCollector oshiMetricsCollector() {
        return new OshiMetricsCollector();
    }

    @Bean
    @ConditionalOnMissingBean
    public JvmMetricsCollector jvmMetricsCollector() {
        return new JvmMetricsCollector();
    }

    @Bean
    @ConditionalOnMissingBean(MetricsReporter.class)
    public MetricsReporter metricsReporter(MonitorAgentProperties props,
                                            RestTemplateBuilder builder) {
        if (StrUtil.isBlank(props.getMonitorUrl())) {
            return new NoopMetricsReporter();
        }
        return new HttpMetricsReporter(props, builder.build());
    }

    @Bean
    @ConditionalOnMissingBean
    public MetricsReportScheduler metricsReportScheduler(
            List<MetricsCollector> collectors,
            MetricsReporter reporter,
            MonitorAgentProperties props) {
        return new MetricsReportScheduler(collectors, reporter, props);
    }
}
```

#### 5.5.2 OshiMetricsCollector

```java
@RequiredArgsConstructor
@Slf4j
public class OshiMetricsCollector implements MetricsCollector {

    private final SystemInfo systemInfo = new SystemInfo();
    private final Hardware hardware = systemInfo.getHardware();

    @Override
    public String getName() {
        return "system";
    }

    @Override
    public Map<String, Object> collect() {
        Map<String, Object> metrics = new HashMap<>();
        // CPU
        CentralProcessor cpu = hardware.getProcessor();
        metrics.put("cpu_usage", cpu.getSystemCpuLoad() * 100);
        metrics.put("cpu_load", cpu.getSystemLoadAverage(3)[0]);
        // 内存
        GlobalMemory memory = hardware.getMemory();
        long total = memory.getTotal();
        long available = memory.getAvailable();
        metrics.put("mem_total", total);
        metrics.put("mem_used", total - available);
        metrics.put("mem_usage", (double)(total - available) / total * 100);
        // 磁盘
        // ...
        return metrics;
    }
}
```

#### 5.5.3 MetricsReportScheduler

```java
@RequiredArgsConstructor
@Slf4j
public class MetricsReportScheduler {

    private final List<MetricsCollector> collectors;
    private final MetricsReporter reporter;
    private final MonitorAgentProperties props;

    private final ScheduledExecutorService scheduler =
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "monitor-agent-scheduler");
            t.setDaemon(true);
            return t;
        });

    @PostConstruct
    public void start() {
        scheduler.scheduleAtFixedRate(this::report, 5, props.getInterval(), TimeUnit.SECONDS);
    }

    @PreDestroy
    public void stop() {
        scheduler.shutdown();
    }

    private void report() {
        try {
            Map<String, Object> snapshot = new HashMap<>();
            for (MetricsCollector collector : collectors) {
                snapshot.putAll(collector.collect());
            }
            reporter.report(MetricsSnapshot.builder()
                .serviceName(props.getServiceName())
                .instance(props.getInstance())
                .env(props.getEnv())
                .metrics(snapshot)
                .timestamp(System.currentTimeMillis())
                .build());
        } catch (Exception e) {
            log.error("[monitor-agent] 上报失败: {}", e.getMessage());
        }
    }
}
```

---

## 6. 必须遵守的开发规范

### 6.1 设计规范

1. **必须**用 `@AutoConfiguration` 注解（Spring Boot 2.7+ 风格）
2. **必须**用 `@ConditionalOnProperty(... matchIfMissing = true)` 让业务方可关闭
3. **必须**用 `@ConditionalOnClass(...)` 防止依赖缺失导致启动失败
4. **必须**用 `@ConditionalOnMissingBean` 让业务方可覆盖默认实现
5. **必须**用 `@EnableConfigurationProperties` 加载配置项
6. **必须**有 `spring-cloud.{能力名}.enabled=true|false` 开关
7. **禁止**用 `META-INF/spring.factories`（已废弃）

### 6.2 配置项规范

1. **必须**用 `@ConfigurationProperties(prefix = "spring-cloud.{能力名}")`
2. 所有字段**必须**有默认值
3. **必须**有 `enabled` 字段，默认 `true`
4. **必须**有 `server-url` 或类似字段，默认指向本地或常见地址
5. **禁止**用硬编码 URL（必须可配置）
6. **必须**生成 `additional-spring-configuration-metadata.json`，让 IDE 有提示

### 6.3 命名规范

| 类型 | 后缀 | 示例 |
|------|------|------|
| AutoConfiguration | `AutoConfiguration` | `SsoClientAutoConfiguration` |
| Properties | `Properties` | `SsoClientProperties` |
| Collector | `Collector` | `OshiMetricsCollector` |
| Reporter | `Reporter` | `HttpMetricsReporter` |
| Filter | `Filter` | `SaTokenContextFilter` |
| Interceptor | `Interceptor` | `SsoLoginInterceptor` |
| Scheduler | `Scheduler` | `MetricsReportScheduler` |

### 6.4 包结构规范

```
com.xytang.starter.{能力名}
├── config/           AutoConfiguration + Properties
├── filter/           过滤器
├── interceptor/      拦截器
├── collector/        采集器（monitor-agent 专用）
├── reporter/         上报器
├── scheduler/        定时器
├── processor/        处理器
├── model/            数据模型
├── constant/         常量
└── internal/         内部实现（不对外暴露）
```

### 6.5 依赖规范

1. **禁止**传递业务依赖（如 `mybatis-plus`、`spring-cloud-starter-gateway`）
2. **必须**把 `spring-boot-configuration-processor` 标记为 `<optional>true</optional>`
3. **必须**用 `scope=provided` 或 `optional=true` 处理可选依赖
4. **必须**通过父 POM 管理版本

### 6.6 异常处理规范

1. Starter 内部异常**必须**打印日志，**禁止**抛到业务方
2. **必须**有降级方案（如 monitor 上报失败 → NoopMetricsReporter）
3. **必须**有 `try-catch` 包裹定时任务，避免因异常导致调度停止

### 6.7 测试规范

- **必须**有 `AutoConfigurationTest`，用 `ApplicationContextRunner` 验证自动装配
- **必须**测试"启用/关闭"两种状态
- **必须**测试"业务方覆盖默认 Bean"的场景

#### 示例测试

```java
class SsoClientAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

    @Test
    void should_auto_configure_when_enabled() {
        contextRunner
            .withConfiguration(AutoConfigurations.of(SsoClientAutoConfiguration.class))
            .withPropertyValues("spring-cloud.sso-client.enabled=true")
            .run(context -> assertThat(context).hasSingleBean(SaSsoClientProcessor.class));
    }

    @Test
    void should_skip_when_disabled() {
        contextRunner
            .withConfiguration(AutoConfigurations.of(SsoClientAutoConfiguration.class))
            .withPropertyValues("spring-cloud.sso-client.enabled=false")
            .run(context -> assertThat(context).doesNotHaveBean(SaSsoClientProcessor.class));
    }
}
```

---

## 7. 业务方使用示例

### 7.1 引入 SSO Client

业务服务（如 `spring-cloud-system`）在 POM 加：

```xml
<dependency>
    <groupId>com.xytang</groupId>
    <artifactId>spring-cloud-starter-sso-client</artifactId>
</dependency>
```

Nacos 配置 `spring-cloud-system.yaml`：

```yaml
spring-cloud:
  sso-client:
    server-url: http://auth.example.com/sso
    client-name: spring-cloud-system
    sign-key: ${SSO_SIGN_KEY}
    allow-urls:
      - http://localhost:5173/**
```

启动服务，自动具备 SSO Client 能力。

### 7.2 引入 Monitor Agent

业务服务（如 `spring-cloud-system`）在 POM 加：

```xml
<dependency>
    <groupId>com.xytang</groupId>
    <artifactId>spring-cloud-starter-monitor-agent</artifactId>
</dependency>
```

Nacos 配置：

```yaml
spring-cloud:
  monitor-agent:
    enabled: true
    monitor-url: http://spring-cloud-monitor/api/agent/report
    interval: 5
```

启动服务，自动开始采集系统指标并上报。

---

## 8. 配置元数据（IDE 提示）

### 8.1 自动生成

在 POM 中引入 `spring-boot-configuration-processor`，会自动扫描 `@ConfigurationProperties` 类，生成 `additional-spring-configuration-metadata.json`。

### 8.2 手动补充

对一些复杂配置（如枚举、默认值说明），手动编辑 `META-INF/additional-spring-configuration-metadata.json`：

```json
{
  "properties": [
    {
      "name": "spring-cloud.sso-client.enabled",
      "type": "java.lang.Boolean",
      "description": "是否启用 SSO Client，默认 true。",
      "defaultValue": true
    },
    {
      "name": "spring-cloud.sso-client.sign-key",
      "type": "java.lang.String",
      "description": "SSO 通信签名密钥，必须与 Server 端一致。",
      "deprecation": {
        "level": "error",
        "reason": "生产环境必须从环境变量注入，禁止硬编码。"
      }
    }
  ]
}
```

---

## 9. 红线（违反即拒绝）

1. ❌ 用 `@Configuration` 而非 `@AutoConfiguration`（顺序问题）
2. ❌ 用 `META-INF/spring.factories` 而非 `AutoConfiguration.imports`（已废弃）
3. ❌ AutoConfiguration 不加 `@ConditionalOnXxx`（导致 Bean 冲突）
4. ❌ 没有 `enabled` 开关（业务方无法关闭）
5. ❌ 没有默认值（业务方必须显式配置才能用）
6. ❌ 在 Starter 中包含 Controller
7. ❌ 在 Starter 中包含业务依赖（如 mybatis-plus）
8. ❌ 用 `@Autowired` 字段注入（必须用构造器注入）
9. ❌ 没有用 `@ConditionalOnMissingBean` 让业务方可覆盖
10. ❌ 把 `spring-boot-configuration-processor` 传递给业务方（必须 optional）
11. ❌ 在 Starter 中硬编码 URL（必须可配置）
12. ❌ Starter 内部异常抛给业务方（必须 catch + 降级）
13. ❌ 没有写 AutoConfigurationTest（必须验证启用/关闭/覆盖三种场景）
