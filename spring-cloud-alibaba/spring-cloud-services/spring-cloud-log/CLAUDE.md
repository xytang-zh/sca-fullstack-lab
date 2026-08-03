# CLAUDE.md — spring-cloud-log 日志服务

> 本文档面向 AI 编码助手，用于在 `spring-cloud-log/` 目录下工作时提供模块约束、技术栈版本、服务职责与开发规范。
> 工作前**必须**先读取父目录 [`spring-cloud-services/CLAUDE.md`](../CLAUDE.md)、后端聚合 [`spring-cloud-alibaba/CLAUDE.md`](../../CLAUDE.md) 与仓库根 [`sca-fullstack-lab/CLAUDE.md`](../../../CLAUDE.md) 了解全局规范。

---

## 1. 模块定位

日志服务，经用户确认**保留**（操作/登录日志审计）。统一记录操作日志、登录日志、审计日志，存 MongoDB（按月分表）。

| 维度 | 值 |
|------|-----|
| 父 POM | `com.xytang:spring-cloud-alibaba:1.0-SNAPSHOT` |
| 顶级包 | `com.xytang.log` |
| 端口 | HTTP 8089 / Dubbo 20889 / XXL-JOB 10007 |

---

## 2. 核心功能

| 模块 | 功能 |
|------|------|
| 操作日志模块 | `@OperationLog` 注解 + AOP，自动记录接口调用 |
| 登录日志模块 | 监听 auth 服务的 `user.login` 事件 |
| 审计日志模块 | 监听敏感操作（删除、权限变更） |
| 查询模块 | 按用户、时间、类型查询 |

---

## 3. 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring AOP | 3.5.0（父 POM） | `@OperationLog` 切面 |
| MongoDB | 7.0+（基础设施） | 日志存储 |
| RabbitMQ | Spring Boot AMQP（父 POM） | 异步写入（`log.operation` 事件） |
| Apache ShardingSphere | 5.5.2（父 POM） | 按月分表（可选） |

---

## 4. 关键接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/logs/operations` | 操作日志分页 |
| GET | `/api/logs/logins` | 登录日志分页 |
| GET | `/api/logs/audits` | 审计日志分页 |
| POST | `/api/logs/operations` | 内部接口：接收日志事件 |

---

## 5. 数据模型（MongoDB）

```
biz_operation_log_YYYYMM  操作日志（按月分表）
biz_login_log_YYYYMM      登录日志（按月分表）
biz_audit_log             审计日志
```

---

## 6. 开发规范（本服务特有）

- 日志写入**必须**异步（MQ），**禁止**阻塞业务主流程
- **禁止**记录密码、Token、身份证号等敏感字段（自动脱敏）
- `@OperationLog` 注解**必须**支持 SpEL 表达式（`key = "'user:' + #id"`）
- 日志保留期 30 天（job 服务 `logCleanupJob` 清理）

---

## 7. 红线（违反即拒绝）

1. ❌ 子服务 POM 覆盖父 POM 依赖版本
2. ❌ `@Autowired` 字段注入
3. ❌ `throw new RuntimeException` / `catch (Exception e)`
4. ❌ `System.out.println` / `e.printStackTrace()`
5. ❌ 日志中泄露密码/Token/身份证号
6. ❌ 日志写入阻塞业务主流程（必须异步）
7. ❌ 业务配置硬编码（必须放 Nacos）
