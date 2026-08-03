# CLAUDE.md — spring-cloud-starter-monitor-agent 监控 Agent

> 本文档面向 AI 编码助手，用于在 `spring-cloud-starter-monitor-agent/` 目录下工作时提供模块约束与开发规范。
> 工作前**必须**先读取父目录 [`spring-cloud-starters/CLAUDE.md`](../CLAUDE.md)、后端聚合 [`spring-cloud-alibaba/CLAUDE.md`](../../CLAUDE.md) 与仓库根 [`sca-fullstack-lab/CLAUDE.md`](../../../CLAUDE.md)。

---

## 1. 模块定位

自定义 Starter（保留），为业务微服务提供监控指标采集能力，自动完成：

1. 用 OSHI 采集本机 CPU、内存、磁盘、JVM 指标
2. 定时（默认 5 秒）上报到 `spring-cloud-monitor` 服务
3. 暴露 `/actuator/prometheus` 给 Prometheus 拉取

| 维度 | 值 |
|------|-----|
| 顶级包 | `com.xytang.starter.monitoragent` |
| 配置前缀 | `spring-cloud.monitor-agent.*` |

---

## 2. 核心功能

| 组件 | 职责 |
|------|------|
| `MonitorAgentAutoConfiguration` | 主装配，注册采集器/上报器/调度器 Bean |
| `MonitorAgentProperties` | `@ConfigurationProperties(prefix = "spring-cloud.monitor-agent")` |
| `MetricsCollector` | 采集器接口（`getName()`、`collect()`） |
| `OshiMetricsCollector` | OSHI 系统指标采集（CPU/内存/磁盘） |
| `JvmMetricsCollector` | JVM 指标采集（堆/非堆/GC/线程） |
| `MetricsReporter` | 上报器接口 |
| `HttpMetricsReporter` | HTTP 上报到 `spring-cloud-monitor` |
| `NoopMetricsReporter` | 关闭时的空实现（降级方案） |
| `MetricsReportScheduler` | 定时调度（`ScheduledExecutorService`） |

---

## 3. 配置项

```yaml
spring-cloud:
  monitor-agent:
    enabled: true                              # 默认启用
    monitor-url: http://spring-cloud-monitor/api/agent/report   # 上报地址
    service-name: ${spring.application.name}   # 服务名
    instance: ${HOSTNAME:unknown}:${server.port}   # 实例标识
    env: ${spring.profiles.active:dev}         # 环境
    interval: 5                                # 采集间隔（秒）
    timeout: 3                                 # 上报超时（秒）
```

---

## 4. 开发规范（Starter 特有）

1. **必须**用 `@AutoConfiguration`（Spring Boot 2.7+ 风格），**禁止** `META-INF/spring.factories`
2. **必须**加 `@ConditionalOnProperty(... matchIfMissing = true)` 让业务方可关闭
3. **必须**有 `enabled` 开关与全部默认值
4. 调度**必须**用 `ScheduledExecutorService`（守护线程），**禁止** `Timer`/`@Scheduled`
5. 调度任务**必须** try-catch 包裹，异常不中断调度
6. **必须** `@PreDestroy` 关闭调度器
7. 线程**必须**命名（如 `monitor-agent-scheduler`）
8. 上报失败**必须**降级（NoopMetricsReporter），**禁止**抛到业务方
9. **禁止**传递业务依赖，`spring-boot-configuration-processor` 必须 `<optional>`

---

## 5. 红线（违反即拒绝）

1. ❌ 用 `@Configuration` 而非 `@AutoConfiguration`
2. ❌ 用 `META-INF/spring.factories`
3. ❌ AutoConfiguration 不加 `@ConditionalOnXxx`
4. ❌ 无 `enabled` 开关 / 无默认值
5. ❌ 用 `Timer` 或 `@Scheduled` 做调度
6. ❌ Starter 内部异常抛给业务方
7. ❌ 在 Starter 中包含业务依赖
8. ❌ 用 `@Autowired` 字段注入
