# 17 · 自定义 Starter 需求（spring-cloud-starters）

> 2 个自定义 Starter：SSO Client + Monitor Agent。业务服务引入即用，零配置自动装配。

---

## 1. 聚合模块定位

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
| 1 | `spring-cloud-starter-sso-client` | SSO Client 自动装配 | 所有业务微服务（除 auth） |
| 2 | `spring-cloud-starter-monitor-agent` | 监控指标采集与上报 | 所有需要监控的微服务 |

---

## 3. spring-cloud-starter-sso-client

### 3.1 定位

为业务微服务提供 SSO Client 能力，自动完成：
1. 拦截未登录请求 → 跳转 `auth` 中心
2. 接收 `auth` 颁发的授权码 → 用 code 换 token
3. 从 `X-Login-Id` 头还原登录态（与 Gateway 协作）
4. 全端注销回调

### 3.2 模块结构

```
spring-cloud-starter-sso-client/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/xytang/starter/ssoclient/
    │   │   ├── config/
    │   │   │   ├── SsoClientAutoConfiguration.java
    │   │   │   ├── SsoClientWebMvcAutoConfiguration.java
    │   │   │   └── SsoClientProperties.java
    │   │   ├── filter/
    │   │   │   └── SaTokenContextFilter.java
    │   │   ├── processor/
    │   │   │   └── SaSsoClientProcessor.java
    │   │   ├── interceptor/
    │   │   │   └── SsoLoginInterceptor.java
    │   │   └── constant/
    │   │       └── SsoClientConstants.java
    │   └── resources/
    │       └── META-INF/
    │           ├── spring/
    │           │   └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
    │           └── additional-spring-configuration-metadata.json
    └── test/
        └── java/com/xytang/starter/ssoclient/
            └── SsoClientAutoConfigurationTest.java
```

### 3.3 功能清单

| 功能 | 描述 | 实现要点 |
|------|------|----------|
| 自动装配 | 业务方加依赖即可用 | `@AutoConfiguration` |
| 配置开关 | 可关闭 | `spring-cloud.sso-client.enabled` |
| X-Login-Id 还原 | 从 Header 取登录态 | `SaTokenContextFilter` |
| 未登录跳转 | 拦截 → 跳 auth | `SsoLoginInterceptor` |
| Code 换 Token | SSO 回调处理 | `SaSsoClientProcessor` |
| 单端注销 | 仅当前 Client 下线 | 调用 auth `/sso/logoutByAlone` |
| 全端注销回调 | auth 推送注销消息 | HTTP 接口 |

### 3.4 配置项

```yaml
spring-cloud:
  sso-client:
    enabled: true                              # 默认启用
    server-url: http://auth.example.com/sso    # SSO Server 地址
    client-name: spring-cloud-system           # Client 标识
    sign-key: ${SSO_SIGN_KEY:change-me}        # 签名密钥（与 Server 端一致）
    timeout: 60                                # 通信超时（秒）
    login-path: /sso/login                     # 本地登录回调路径
    callback-path: /sso/callback               # code 回调路径
    logout-path: /sso/logout                   # 单端注销路径
    allow-urls:                                # 允许的回调 URL 白名单
      - http://localhost:5173/**
      - https://*.example.com/**
```

### 3.5 使用方式

业务服务 POM 加依赖：

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

### 3.6 关键代码

#### 3.6.1 AutoConfiguration

```java
@AutoConfiguration
@ConditionalOnProperty(
    prefix = "spring-cloud.sso-client",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
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

    @Bean
    @ConditionalOnMissingBean
    public SsoLoginInterceptor ssoLoginInterceptor(SsoClientProperties props) {
        return new SsoLoginInterceptor(props);
    }
}
```

#### 3.6.2 SaTokenContextFilter

```java
@Component
@RequiredArgsConstructor
public class SaTokenContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws ServletException, IOException {
        String loginId = req.getHeader("X-Login-Id");
        if (StrUtil.isNotBlank(loginId)) {
            SaTokenContext.setLoginId(loginId);
            LoginUserContext.set(loginId);
        }
        try { chain.doFilter(req, resp); }
        finally { LoginUserContext.clear(); }
    }
}
```

#### 3.6.3 SsoLoginInterceptor

```java
@Component
@RequiredArgsConstructor
public class SsoLoginInterceptor implements HandlerInterceptor {

    private final SsoClientProperties props;

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) {
        Object loginId = StpUtil.getLoginIdByToken(req.getHeader("X-Token"));
        if (loginId == null) {
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

## 4. spring-cloud-starter-monitor-agent

### 4.1 定位

为业务微服务提供监控指标采集能力，自动完成：
1. 用 OSHI 采集本机 CPU、内存、磁盘、JVM 指标
2. 定时（默认 5 秒）上报到 `spring-cloud-monitor` 服务
3. 暴露 `/actuator/prometheus` 给 Prometheus 拉

### 4.2 模块结构

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
    │   │   │   ├── MetricsCollector.java
    │   │   │   ├── OshiMetricsCollector.java
    │   │   │   ├── JvmMetricsCollector.java
    │   │   │   └── BusinessMetricsCollector.java
    │   │   ├── reporter/
    │   │   │   ├── MetricsReporter.java
    │   │   │   ├── HttpMetricsReporter.java
    │   │   │   └── NoopMetricsReporter.java
    │   │   ├── scheduler/
    │   │   │   └── MetricsReportScheduler.java
    │   │   ├── model/
    │   │   │   ├── MetricsSnapshot.java
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

### 4.3 功能清单

| 功能 | 描述 | 实现要点 |
|------|------|----------|
| OSHI 采集 | CPU、内存、磁盘、网络 | 单例 SystemInfo |
| JVM 采集 | 堆/非堆/线程/GC | JMX |
| 业务自定义采集 | 业务方实现 BusinessMetricsCollector | 接口注入 |
| 定时上报 | 默认 5 秒 | ScheduledExecutorService |
| 失败降级 | 上报失败不阻塞业务 | NoopMetricsReporter |
| 配置开关 | 可关闭 | `spring-cloud.monitor-agent.enabled` |
| Prometheus 整合 | 暴露 `/actuator/prometheus` | micrometer |

### 4.4 配置项

```yaml
spring-cloud:
  monitor-agent:
    enabled: true                              # 默认启用
    monitor-url: http://spring-cloud-monitor/api/agent/report
    service-name: ${spring.application.name}
    instance: ${HOSTNAME:unknown}:${server.port}
    env: ${spring.profiles.active:dev}
    interval: 5                                # 采集间隔（秒）
    timeout: 3                                 # 上报超时（秒）
    enabled-metrics:
      - system
      - jvm
      - business
    business-metrics:
      enabled: false
```

### 4.5 使用方式

业务服务 POM 加依赖：

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

启动服务，自动采集系统指标并上报。

### 4.6 关键代码

#### 4.6.1 AutoConfiguration

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

#### 4.6.2 OshiMetricsCollector

```java
@RequiredArgsConstructor
@Slf4j
public class OshiMetricsCollector implements MetricsCollector {

    private final SystemInfo systemInfo = new SystemInfo();
    private final Hardware hardware = systemInfo.getHardware();

    @Override
    public String getName() { return "system"; }

    @Override
    public Map<String, Object> collect() {
        Map<String, Object> metrics = new HashMap<>();
        CentralProcessor cpu = hardware.getProcessor();
        GlobalMemory memory = hardware.getMemory();
        long total = memory.getTotal();
        long available = memory.getAvailable();
        metrics.put("cpu_usage", cpu.getSystemCpuLoad() * 100);
        metrics.put("cpu_load", cpu.getSystemLoadAverage(3)[0]);
        metrics.put("mem_total", total);
        metrics.put("mem_used", total - available);
        metrics.put("mem_usage", (double)(total - available) / total * 100);
        return metrics;
    }
}
```

#### 4.6.3 MetricsReportScheduler

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
    public void stop() { scheduler.shutdown(); }

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

## 5. Starter 设计原则（强制约束）

### 5.1 命名规范

- artifactId：`spring-cloud-starter-{能力名}`
- 顶级包：`com.xytang.starter.{能力名}`

### 5.2 自动装配规范

- 用 `@AutoConfiguration` 注解（Spring Boot 2.7+ 风格）
- 必须有 `@ConditionalOnProperty(matchIfMissing = true)` 让业务方可关闭
- 必须用 `@ConditionalOnClass` 防止依赖缺失
- 必须用 `@ConditionalOnMissingBean` 让业务方可覆盖
- **禁止**用旧的 `META-INF/spring.factories`（已废弃）

### 5.3 配置项规范

- 前缀统一：`spring-cloud.{能力名}.*`
- 必须有 `enabled` 字段，默认 `true`
- 必须有默认值（开发环境友好）
- **禁止**硬编码 URL

### 5.4 依赖规范

- **禁止**传递业务依赖（如 mybatis-plus、spring-cloud-starter-gateway）
- `spring-boot-configuration-processor` 必须标记 `<optional>true</optional>`
- 通过父 POM 管理版本

### 5.5 异常处理规范

- Starter 内部异常**必须**打印日志，**禁止**抛到业务方
- 必须有降级方案（如 monitor 上报失败 → NoopMetricsReporter）
- 定时任务必须 `try-catch` 包裹，避免因异常导致调度停止

### 5.6 测试规范

- 必须有 `AutoConfigurationTest`，用 `ApplicationContextRunner` 验证
- 必须测试"启用/关闭"两种状态
- 必须测试"业务方覆盖默认 Bean"的场景

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

## 6. 验收标准

### 6.1 SSO Client Starter

- ✅ 业务服务加依赖后自动具备 SSO Client 能力
- ✅ 未登录请求跳转到 auth 中心
- ✅ Code 换 Token 流程正常
- ✅ 从 X-Login-Id 还原登录态
- ✅ `enabled=false` 时关闭
- ✅ 业务方可覆盖默认 Bean

### 6.2 Monitor Agent Starter

- ✅ 业务服务加依赖后自动采集指标
- ✅ OSHI 采集 CPU/内存/磁盘/JVM
- ✅ 定时 5 秒上报到 monitor 服务
- ✅ 上报失败不阻塞业务
- ✅ `enabled=false` 时关闭
- ✅ Prometheus `/actuator/prometheus` 正常

---

下一步：[18 · 实施路线图与里程碑](./18-实施路线图与里程碑.md)
