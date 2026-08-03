# CLAUDE.md — spring-cloud-message 消息中心服务

> 本文档面向 AI 编码助手，用于在 `spring-cloud-message/` 目录下工作时提供模块约束、技术栈版本、服务职责与开发规范。
> 工作前**必须**先读取父目录 [`spring-cloud-services/CLAUDE.md`](../CLAUDE.md)、后端聚合 [`spring-cloud-alibaba/CLAUDE.md`](../../CLAUDE.md) 与仓库根 [`sca-fullstack-lab/CLAUDE.md`](../../../CLAUDE.md) 了解全局规范。

---

## 1. 模块定位

统一的消息出口，经用户确认**保留**（站内信/评论通知等）。职责：MQ 消费 → WebSocket 推送 → 站内信/邮件/短信。博客系统中承担评论通知、待办提醒等站内信推送。

| 维度 | 值 |
|------|-----|
| 父 POM | `com.xytang:spring-cloud-alibaba:1.0-SNAPSHOT` |
| 顶级包 | `com.xytang.message` |
| 端口 | HTTP 8086 / Dubbo 20886 / WebSocket 9091 / XXL-JOB 10004 |

---

## 2. 核心功能

| 模块 | 功能 |
|------|------|
| MQ 消费模块 | 监听 `user.register`、`alert.trigger`、`comment.created`、`user.kickout` 等事件 |
| WebSocket 模块 | Netty Server（端口 9091），`userId → Channel` 路由实时推送 |
| 站内信模块 | 通知 CRUD、已读未读、批量删除、未读数 |
| 邮件模块 | Thymeleaf 模板 + spring-boot-starter-mail |
| 短信模块 | 阿里云/腾讯云 SDK，限流 |
| 客服模块 | 访客排队、客服分配、消息路由 |

### 2.1 博客场景（评论通知）

- comment 服务发 `comment.created` 事件 → message 消费 → 生成站内信 → WebSocket 推送被回复用户

---

## 3. 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot AMQP | 3.5.0（父 POM） | RabbitMQ 消费 |
| Netty | 4.1.x（Spring Boot 管理） | WebSocket Server（9091） |
| spring-boot-starter-mail | 3.5.0（父 POM） | 邮件发送 |
| Aliyun/Tencent SMS SDK | 父 POM 未声明，落地时补充 | 短信发送 |
| Thymeleaf | 3.5.0（父 POM） | 邮件模板 |
| Redisson | 4.0.0（父 POM） | 客服分配分布式锁 |

---

## 4. 关键接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/messages` | 站内信分页 |
| PATCH | `/api/messages/{id}/read` | 标记已读 |
| POST | `/api/messages/read-all` | 全部已读 |
| DELETE | `/api/messages/{id}` | 删除 |
| GET | `/api/messages/unread-count` | 未读数 |
| WS | `/ws/messages/{userId}` | WebSocket 订阅消息推送 |
| POST | `/api/messages/customer-service/connect` | 访客接入客服 |

---

## 5. 服务间通信

| 方向 | 方式 | 说明 |
|------|------|------|
| auth → message | RabbitMQ（`user.register`） | 注册欢迎信 |
| comment → message | RabbitMQ（`comment.created`） | 评论通知 ★博客 |
| monitor → message | RabbitMQ（`alert.trigger`） | 告警推送 |

---

## 6. 开发规范（本服务特有）

- 站内信**必须**有已读/未读状态，未读数走 Redis 计数
- WebSocket 连接**必须**在组件卸载/断开时清理 Channel
- 消费事件**必须**用 `AbstractEventListener<T>`（自动幂等）
- 客服分配**必须**用 Redisson 分布式锁

---

## 7. 红线（违反即拒绝）

1. ❌ 子服务 POM 覆盖父 POM 依赖版本
2. ❌ `@Autowired` 字段注入
3. ❌ `throw new RuntimeException` / `catch (Exception e)`
4. ❌ `System.out.println` / `e.printStackTrace()`
5. ❌ 消息消费不幂等（重复推送）
6. ❌ WebSocket 推送不校验登录态
7. ❌ 短信发送无限流
8. ❌ 业务配置硬编码（必须放 Nacos）
