# CLAUDE.md — spring-cloud-common-dubbo 跨服务 RPC 契约

> 本文档面向 AI 编码助手，用于在 `spring-cloud-common-dubbo/` 目录下工作时提供模块约束与开发规范。
> 工作前**必须**先读取父目录 [`spring-cloud-common/CLAUDE.md`](../CLAUDE.md) 与 [`spring-cloud-alibaba/CLAUDE.md`](../../CLAUDE.md) 了解全局规范。

---

## 1. 模块定位

跨服务 Dubbo RPC 接口契约层：**只定义接口与其出入参 DTO**，供被调方（Provider）与调用方（Consumer）共享，**不引入 Dubbo 运行时依赖**，保持轻量。

| 维度 | 值 |
|------|-----|
| 顶级包 | `com.xytang.common.dubbo` |
| 父 POM | `com.xytang:spring-cloud-alibaba:1.0-SNAPSHOT` |
| 当前 artifactId | `spring-cloud-common-dubbo` |
| packaging | `jar` |
| 是否有代码 | ✅ 完整 |

---

## 2. 目录结构

```
spring-cloud-common-dubbo/
├── pom.xml
└── src/main/java/com/xytang/common/dubbo/
    ├── ArticleRpcService.java    existsById(Long): boolean
    └── CommentRpcService.java    countByArticleId(Long): long
```

---

## 3. 已定义 RPC 契约

| 接口 | 方法 | 说明 |
|------|------|------|
| `ArticleRpcService` | `existsById(Long articleId)` | 校验文章是否存在（未软删除），供 comment 等服务调用 |
| `CommentRpcService` | `countByArticleId(Long articleId)` | 统计某文章已审核评论数，供 article 等服务调用 |

---

## 4. 开发规范

1. **只放接口与 DTO**：禁止写任何实现类（实现类放各业务服务的 `rpc` 包）
2. **不引入 Dubbo 运行时依赖**：接口是纯 Java 接口 + 可序列化 DTO（`implements Serializable`），保持模块轻量
3. **雪花 ID 用 `Long`**：RPC 内部保持 `Long` 原始类型，不转 String（仅 HTTP 出参才序列化为 String）
4. **返回值类型**：基本类型或可序列化 DTO，禁止返回 `R<T>` 等 HTTP 响应包装
5. **新增契约**：跨服务接口必须定义在本模块，禁止在单个服务内私定义

---

## 5. 与其他模块的关系

| 关系 | 模块 |
|------|------|
| 被依赖 | `spring-cloud-article`、`spring-cloud-comment`（及其他需要 RPC 的业务服务） |
| 依赖 | `spring-cloud-common-core`（提供异常/响应等基础类型） |

---

## 6. 红线

1. ❌ 在本模块写 Provider/Consumer 实现类（必须放业务服务 `rpc` 包）
2. ❌ 引入 Dubbo 运行时依赖（`dubbo-spring-boot-starter` 等）
3. ❌ 在接口中返回 `R<T>` 等 HTTP 响应包装
4. ❌ 在单个业务服务内私定义跨服务接口（必须放本模块）