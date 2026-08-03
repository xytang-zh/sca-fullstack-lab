# CLAUDE.md — spring-cloud-monitor 服务器监控服务

> 本文档面向 AI 编码助手，用于在 `spring-cloud-monitor/` 目录下工作时提供模块约束、技术栈版本、服务职责与开发规范。
> 工作前**必须**先读取父目录 [`spring-cloud-services/CLAUDE.md`](../CLAUDE.md)、后端聚合 [`spring-cloud-alibaba/CLAUDE.md`](../../CLAUDE.md) 与仓库根 [`sca-fullstack-lab/CLAUDE.md`](../../../CLAUDE.md) 了解全局规范。

---

## 1. 模块定位

服务器监控服务，经用户确认**保留**（管理员可随时监控服务器）。采集服务器/JVM 指标，存时序库，通过 WebSocket 推送到前端大盘，并提供告警能力。

| 维度 | 值 |
|------|-----|
| 父 POM | `com.xytang:spring-cloud-alibaba:1.0-SNAPSHOT` |
| 顶级包 | `com.xytang.monitor` |
| 端口 | HTTP 8083 / Dubbo 20883 / WebSocket 9090 / XXL-JOB 10001 |

---

## 2. 核心功能

| 模块 | 功能 |
|------|------|
| 采集模块 | OSHI 采集本机指标；接收 monitor-agent 上报 |
| 存储模块 | TDengine 超级表 `aurora_metrics` |
| 推送模块 | Netty WebSocket Server（端口 9090），按 `userId → Channel` 推送实时指标 |
| 历史查询 | 按 service_name + 时间范围查询 |
| 告警模块 | 阈值规则（Nacos 配置）→ 命中发 MQ（`alert.trigger`）→ message 服务推送 |
| Prometheus 整合 | 暴露 `/actuator/prometheus` 供 Prometheus 拉取 |

---

## 3. 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| OSHI | 6.6（父 POM 未声明，落地时补充） | 系统信息采集 |
| TDengine | 3.3.2.0 + taos-jdbcdriver（父 POM 未声明） | 时序存储 |
| Netty | 4.1.x（Spring Boot 管理） | WebSocket Server（9090） |
| Spring Boot Actuator | 3.5.0（父 POM） | micrometer-registry-prometheus 指标暴露 |
| RabbitMQ | Spring Boot AMQP（父 POM） | 告警事件 `alert.trigger` |
| Redis | 7.4+ | 告警状态去抖（`monitor:alert:{service}:{metric}`） |

---

## 4. 关键接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/monitor/realtime` | 当前所有服务最新指标 |
| GET | `/api/monitor/history` | 按 service_name + 时间范围查历史 |
| GET | `/api/monitor/services` | 列出所有被监控服务 |
| GET | `/api/monitor/alerts` | 当前活跃告警 |
| WS | `/ws/monitor/{userId}` | WebSocket 订阅实时指标 |

---

## 5. 数据模型

- TDengine 超级表 `aurora_metrics`
- MySQL `biz_alert_rule` 告警规则表
- Redis `monitor:alert:{service}:{metric}` 告警状态去抖

---

## 6. 服务间通信

| 方向 | 方式 | 说明 |
|------|------|------|
| monitor → message | RabbitMQ（`alert.trigger`） | 告警触发推送 |
| monitor → system | Dubbo | `UserRpcService.getRoles` 用户角色查询 |

---

## 7. 开发规范（本服务特有）

- WebSocket 连接**必须**在组件卸载/断开时清理 Channel
- 告警**必须**去抖（Redis），避免风暴
- 采集指标**必须**带上 service_name 与 instance 标签
- RESTful 规范遵循 `spring-cloud-services/CLAUDE.md` §6

---

## 8. 红线（违反即拒绝）

1. ❌ 子服务 POM 覆盖父 POM 依赖版本
2. ❌ `@Autowired` 字段注入
3. ❌ `throw new RuntimeException` / `catch (Exception e)`
4. ❌ `System.out.println` / `e.printStackTrace()`
5. ❌ 告警不做去抖（Redis）导致告警风暴
6. ❌ WebSocket 推送不校验登录态
7. ❌ 业务配置硬编码（必须放 Nacos）
