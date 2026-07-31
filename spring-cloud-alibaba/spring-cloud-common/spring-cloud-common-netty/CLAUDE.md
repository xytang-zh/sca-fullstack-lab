# CLAUDE.md — spring-cloud-common-netty WebSocket

> 本文档面向 AI 编码助手，用于在 `spring-cloud-common-netty/` 目录下工作时提供模块约束、功能计划与开发规范。
> 工作前**必须**先读取父目录的 [`spring-cloud-common/CLAUDE.md`](../CLAUDE.md) 与 [`spring-cloud-alibaba/CLAUDE.md`](../../CLAUDE.md) 了解全局规范。

---

## 1. 模块定位

`spring-cloud-common-netty` 基于 Netty 4.1 提供 WebSocket Server 封装，用于**实时推送**场景（监控大盘、消息中心）。

**核心设计原则**：
1. **握手即鉴权**：握手时从 query 参数取 Token 校验，**禁止**接受未鉴权连接
2. **多端在线**：一个 `userId` 可同时多端在线（`userId → List<Channel>`）
3. **心跳保活**：30s 推 ping，60s 无响应关闭连接

| 维度 | 值 |
|------|-----|
| 顶级包 | `com.xytang.common.netty` |
| 父 POM | `com.xytang:spring-cloud-alibaba:1.0-SNAPSHOT` |
| 当前 artifactId | `spring-cloud-common-netty` |
| packaging | `jar` |
| 是否有代码 | ❌ 空壳（仅 pom.xml，**本 CLAUDE.md 为规划书**） |

---

## 2. 技术栈选型

| 技术 | 版本 | 用途 |
|------|------|------|
| Netty | 4.1.x | WebSocket Server |
| Spring Boot | 3.5.0 | 基座 |
| Lombok | 父 POM 全局声明 | 注解简化 |

---

## 3. 功能计划

### 3.1 NettyServer 启动入口

- **职责**：`CommandLineRunner` 启动 Netty Server
- **端口**：默认 9090，可配置
- **BossGroup/WorkerGroup**：NIO EventLoopGroup
- **实现技术**：`ServerBootstrap` + `NioServerSocketChannel`

### 3.2 WebSocketServerInitializer ChannelInitializer

- **职责**：初始化 Channel Pipeline
- **Pipeline 顺序**：
  1. `HttpServerCodec`（HTTP 编解码）
  2. `HttpObjectAggregator`（HTTP 聚合）
  3. `ChunkedWriteHandler`（大数据流）
  4. `AuthHandshakeHandler`（握手鉴权）
  5. `WebSocketServerProtocolHandler`（WebSocket 协议）
  6. `HeartbeatHandler`（心跳）
  7. `WebSocketFrameHandler`（业务处理）
- **实现技术**：继承 `ChannelInitializer<SocketChannel>`

### 3.3 WebSocketFrameHandler 文本帧处理

- **职责**：处理 WebSocket TextFrame，按 `MessageType` 分发
- **协议**：JSON 消息 `{type, to, content, timestamp}`
- **实现技术**：`SimpleChannelInboundHandler<TextWebSocketFrame>`

### 3.4 HeartbeatHandler 心跳/断线检测

- **职责**：30s 推 ping，60s 无响应关闭连接
- **实现技术**：`IdleStateHandler` + `ChannelInboundHandlerAdapter.userEventTriggered`

### 3.5 AuthHandshakeHandler 握手鉴权

- **职责**：握手时从 query 参数取 Token 校验
- **校验**：调 Sa-Token `StpUtil.getLoginIdByToken`
- **绑定**：校验通过，绑定 `userId` 到 `Channel.attr(...)`
- **实现技术**：`ChannelInboundHandlerAdapter` + FullHttpRequest query 解析

### 3.6 ChannelRouter 路由表

- **职责**：维护 `userId → List<Channel>` 路由表
- **数据结构**：`ConcurrentHashMap<Long, CopyOnWriteArrayList<Channel>>`
- **方法**：`add`/`remove`/`get`/`sendToUser`/`broadcast`
- **实现技术**：`ConcurrentHashMap` + `CopyOnWriteArrayList`

### 3.7 Message / MessageType 协议

- **MessageType**：`METRIC`/`PUSH`/`CHAT`/`NOTICE`/`PING`/`PONG`
- **Message**：`{type, to, content, timestamp}`
- **实现技术**：POJO + 枚举

---

## 4. POM 依赖模板（计划）

```xml
<dependencies>
    <!-- 内部依赖 -->
    <dependency>
        <groupId>com.xytang</groupId>
        <artifactId>spring-cloud-common-core</artifactId>
    </dependency>

    <!-- 内部依赖：Sa-Token（握手鉴权） -->
    <dependency>
        <groupId>com.xytang</groupId>
        <artifactId>spring-cloud-common-satoken</artifactId>
    </dependency>

    <!-- Netty -->
    <dependency>
        <groupId>io.netty</groupId>
        <artifactId>netty-all</artifactId>
    </dependency>
</dependencies>
```

---

## 5. 配置项（计划）

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `xytang.netty.port` | `9090` | WebSocket 端口 |
| `xytang.netty.boss-threads` | `1` | Boss 线程数 |
| `xytang.netty.worker-threads` | `CPU 核数 * 2` | Worker 线程数 |
| `xytang.netty.heartbeat.interval` | `30s` | 心跳间隔 |
| `xytang.netty.heartbeat.timeout` | `60s` | 心跳超时 |
| `xytang.netty.max-frame-size` | `65536` | 最大帧大小 |

> 子模块内**无** `application.yml`，所有配置在 Nacos 中。

---

## 6. 与其他模块的关系

| 关系 | 模块 |
|------|------|
| 依赖 | `spring-cloud-common-core`、`spring-cloud-common-satoken`（握手鉴权） |
| 被依赖 | `spring-cloud-monitor`（监控推送，端口 9090）、`spring-cloud-message`（消息推送，端口 9091） |

---

## 7. 红线

1. ❌ 握手时不鉴权（必须从 query 取 Token 校验，**禁止**接受未鉴权连接）
2. ❌ 不实现心跳（导致僵尸连接占用资源）
3. ❌ 组件卸载时不关闭 Channel（导致连接泄漏）
4. ❌ 用 `synchronized` 跨 Channel 同步（必须用 `ConcurrentHashMap` + `CopyOnWriteArrayList`）
5. ❌ Channel 直接持有业务对象（导致内存泄漏，必须用 `Channel.attr(AttributeKey)`）
6. ❌ 在 Netty EventLoop 中阻塞 IO（导致整个 EventLoop 阻塞）
7. ❌ AutoConfiguration Bean 不加 `@ConditionalOnMissingBean`

---

## 8. 实施状态

⚠️ 本模块当前为**空壳**，仅 `pom.xml`，**无任何 .java 代码**。

| 任务 | 状态 |
|------|------|
| pom.xml | ✅ 存在 |
| NettyServer | ❌ 未实现 |
| WebSocketServerInitializer | ❌ 未实现 |
| WebSocketFrameHandler | ❌ 未实现 |
| HeartbeatHandler | ❌ 未实现 |
| AuthHandshakeHandler | ❌ 未实现 |
| ChannelRouter | ❌ 未实现 |
| Message/MessageType | ❌ 未实现 |
| NettyAutoConfiguration | ❌ 未实现 |
| AutoConfiguration.imports | ❌ 未实现 |
| 单元测试 | ❌ 未实现 |

> 落地实现时请对照本规划，并在完成后更新本 CLAUDE.md 第 2-7 节为实际代码状态。
