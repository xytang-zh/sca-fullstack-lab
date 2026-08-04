## Context

当前 article 与 comment 是两个独立 Spring Boot 3.5 微服务，均通过 Nacos（`127.0.0.1:8848`）做 Spring Cloud 注册发现，服务间无任何 RPC 能力。CLAUDE.md 已规划 Dubbo 端口（article 20893 / comment 20894）与 RPC 契约，但**未落地**：父 POM 无 Dubbo 依赖（经核实 SCA BOM 2025.0.0.0 仅管理 Nacos/Sentinel/Seata，**不管理 Dubbo 版本**）、无 RPC 接口定义、application.yml 无 Dubbo 配置。本设计打通 Dubbo 通讯环境并用双向调用验证。动机见 `proposal.md - Why`，行为契约见 `specs/dubbo-rpc/spec.md`。

用户已确认三个决策：**Triple 协议**、**新建 `spring-cloud-common-dubbo` 模块**、**双向调用验证**（comment→article 校验存在 + article→comment 聚合评论数）。

## Goals / Non-Goals

**Goals:**
- 在父 POM 统一管理 Dubbo 版本，article/comment 两个服务引入 Dubbo 依赖并完成配置
- 新建 `spring-cloud-common-dubbo` 公共模块承载 RPC 契约，被调方/调用方共享
- 打通双向调用链路并可通过观察日志/Nacos 控制台验证连通
- 兼容现有 Spring Cloud Nacos discovery 与 HTTP 网关链路，不破坏现有功能

**Non-Goals:**
- 不接入 Dubbo 服务治理（限流/熔断/灰度/可观测性/链路透传）
- 不接入 search/file/system 等其他服务的 Dubbo
- 不改变前端评论数据获取方式（详情页评论仍由前端直接调 comment 服务）
- 不做多注册中心 / 多协议 / TLS 等高级配置

## Decisions

### 1. Dubbo 版本 3.3.6 + 父 POM 导入 `dubbo-bom`

`spring-cloud-alibaba-dependencies` BOM 2025.0.0.0 不管理 Dubbo 版本，故在父 POM `<properties>` 声明 `<dubbo.version>3.3.6</dubbo.version>`，并在 `<dependencyManagement>` 导入 `org.apache.dubbo:dubbo-bom:3.3.6`，子模块只声明 `dubbo-spring-boot-starter` 不写版本（符合"版本统一父 POM 声明"红线）。

- 备选：各依赖显式写版本 → 违反集中管理红线，否决。
- 备选：用 `spring-cloud-starter-alibaba-dubbo`（SCA 集成 starter）→ 本机仓库无该 artifact，且新版 SCA 已不再推荐，否决。

### 2. 新增 `spring-cloud-common-dubbo` 模块承载 RPC 契约

RPC 接口（纯接口 + 可序列化 DTO）不依赖 Dubbo 运行时 API，仅 `implements Serializable`，保证模块轻量、无 Dubbo 类路径。在 `spring-cloud-common/pom.xml` `<modules>` 注册，article/comment 服务 POM 依赖该模块。

- 备选：放入 `spring-cloud-common-core` 的 `rpc` 包（CLAUDE.md 原规划）→ common-core 是"纯 POJO 核心工具"定位，混入跨服务契约会使其依赖膨胀，且用户已确认新建独立模块，否决。

### 3. 协议选择 Triple（`tri`）

Dubbo 3.x 官方推荐协议，HTTP/2 传输、gRPC 兼容、序列化更优，`dubbo-spring-boot-starter` 已内置 `dubbo-rpc-triple`，无需额外依赖。端口按 CLAUDE.md 规划：article 20893、comment 20894。

- 备选：传统 `dubbo://`（TCP + Hessian2）→ 兼容旧版但非新项目首选，用户已确认 Triple，否决。

### 4. 注册中心复用 Nacos

`dubbo.registry.address=nacos://${NACOS_ADDR:127.0.0.1:8848}`，与 Spring Cloud discovery 共用同一 Nacos 实例，不新增基础设施容器。Dubbo 与 Spring Cloud 各自注册实例（服务类型不同），互不干扰。

- 备选：ZooKeeper → 需新增容器，且与现有 Nacos 体系不统一，否决。

### 5. Consumer 注入用 `@DubboReference` 字段注入

经核实 Dubbo 3.3.6 的 `@DubboReference` 注解 `@Target` 仅支持 `FIELD`/`METHOD`/`ANNOTATION_TYPE`，**不支持 `PARAMETER`**（构造器参数注入不可行）。故采用字段注入 `@DubboReference`（Dubbo 官方惯例，非 Spring `@Autowired`，不违反项目"禁止 @Autowired 字段注入"红线）。`@RequiredArgsConstructor` 仍用于 Spring 依赖（Mapper 等）。

- 备选：构造器参数 `@DubboReference` → 该版本注解不支持 PARAMETER，编译期无法识别，否决。
- 备选：setter 注入 `@DubboReference`（METHOD 目标支持）→ 可行但非 Dubbo 惯例，且引入非 final 可变字段，否决。

### 6. RPC 失败降级：精确捕获 `RpcException`

article 聚合评论数失败时降级为 0 不阻塞详情返回（符合 spec"降级"场景）。捕获类型用 `org.apache.dubbo.rpc.RpcException`（Dubbo 调用异常，精确捕获，符合"禁止 catch Exception"红线）。

- 备选：Dubbo `mock` 降级机制 → 需额外写 mock 类且调试复杂，否决。

### 7. RPC 参数/返回值用 `Long` 原始类型

雪花 ID 在 RPC 内部保持 `Long`（RPC 序列化不丢精度）；仅 HTTP 出参经 `spring-cloud-common-web` 的 Jackson 序列化为 String 供前端接收。RPC 契约层不引入 String 转换。

## 架构与调用流程

```
┌────────────────────────┐   tri://   ┌────────────────────────┐
│ spring-cloud-article   │  Dubbo 3.3 │ spring-cloud-comment   │
│  Provider              │◄──────────►│  Provider              │
│  ArticleRpcServiceImpl │            │  CommentRpcServiceImpl │
│  port 20893            │            │  port 20894            │
└──────────┬─────────────┘            └──────────┬─────────────┘
           │             Nacos 注册中心          │
           └───────────────┬────────────────────┘
                           │ 127.0.0.1:8848
                           ▼
                    ┌──────────────┐
                    │    Nacos     │
                    └──────────────┘
```

调用链 1（comment → article）：
```
发表评论 POST /api/articles/{id}/comments
  └─ CommentServiceImpl.create
       └─ @DubboReference ArticleRpcService.existsById(id) ──► article 服务查库
            └─ true → 受理；false → BusinessException(文章不存在)
```

调用链 2（article → comment）：
```
请求详情 GET /api/articles/{id}
  └─ ArticleServiceImpl.detail
       └─ @DubboReference CommentRpcService.countByArticleId(id) ──► comment 服务查库
            └─ 成功 → 写入 ArticleDetailVO.comments；失败(RpcException) → 降级 0
```

模块结构（新增/改动）：

```
spring-cloud-common/
  └── spring-cloud-common-dubbo/                      ★新增
        ├── pom.xml
        └── src/main/java/com/xytang/common/dubbo/
              ├── ArticleRpcService.java              existsById(Long): boolean
              └── CommentRpcService.java             countByArticleId(Long): long

spring-cloud-article/
  ├── pom.xml                                         + dubbo starter / common-dubbo
  └── src/main/resources/application.yml              + dubbo 配置
spring-cloud-comment/
  ├── pom.xml                                         + dubbo starter / common-dubbo
  └── src/main/resources/application.yml              + dubbo 配置
```

每个服务 Dubbo 配置模板（端口按服务替换）：

```yaml
dubbo:
  application:
    name: ${spring.application.name}
  registry:
    address: nacos://${NACOS_ADDR:127.0.0.1:8848}
  protocol:
    name: tri
    port: 20893          # article；comment 为 20894
  consumer:
    timeout: 3000
    check: false         # 启动不因被调方未起而失败
```

## Risks / Trade-offs

- [Risk] Dubbo 与 Spring Cloud 各自向 Nacos 注册实例，服务列表出现两类条目，运维可能困惑 → 在 Nacos 控制台以 `providers/consumers` 分组区分，并在 CLAUDE.md §10 补充说明；两者仅共享注册中心，不共享服务实例。
- [Risk] Triple 协议与 Dubbo 3.3.6 的兼容性 / 依赖冲突（如 Netty 版本）→ 版本为本地已验证可用的 3.3.6，落地时先 `mvn -pl spring-cloud-article -am clean install` 验证编译，再启动双服务验证调用。
- [Risk] Nacos 未启动时 Dubbo 注册失败导致服务启动卡顿/失败 → `registry` 配置 `check: false` + `consumer.check: false`；本地开发先启动 Nacos 容器。
- [Trade-off] comment 发表路径增加一次 RPC 往返延迟 → 当前可接受（验证环境为主）；后续可加本地缓存（文章存在性短 TTL）或拆为事件校验，留待后续变更。
- [Trade-off] 评论数聚合失败时降级 0，可能短暂显示不准确 → 与"详情不因评论数失败而报错"权衡，选择可用性优先。

## Migration Plan

- 无数据库变更、无前端变更。
- 部署顺序：① `mvn clean install -pl spring-cloud-common/spring-cloud-common-dubbo -am`（先产出公共模块）；② 启动 article、comment 两个服务；③ 验证 Nacos 注册与双向调用。
- 回滚：从父 POM 与两个服务 POM 移除 Dubbo 依赖与配置，删除 `spring-cloud-common-dubbo` 模块即可恢复原状。

## Open Questions

无（Dubbo 上下文透传、服务治理等已明确列入 Non-Goals，后续独立变更处理）。