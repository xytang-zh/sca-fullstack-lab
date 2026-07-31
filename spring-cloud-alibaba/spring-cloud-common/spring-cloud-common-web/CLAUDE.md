# CLAUDE.md — spring-cloud-common-web Web 通用层

> 本文档面向 AI 编码助手，用于在 `spring-cloud-common-web/` 目录下工作时提供模块约束、功能清单与开发规范。
> 工作前**必须**先读取父目录的 [`spring-cloud-common/CLAUDE.md`](../CLAUDE.md) 与 [`spring-cloud-alibaba/CLAUDE.md`](../../CLAUDE.md) 了解全局规范。

---

## 1. 模块定位

`spring-cloud-common-web` 是 Spring MVC 全局配置层，提供**所有 Web 服务必依赖**的能力：全局异常处理、统一响应包装、CORS、Trace-Id 透传、密码编码器、XSS 过滤（计划）等。

**核心设计原则**：
1. **开箱即用**：业务方加依赖即用，无需 `@Import`
2. **不写 Controller**：本模块只暴露基础设施 Bean，**禁止**包含 Controller
3. **不覆盖业务**：所有 Bean 都用 `@ConditionalOnMissingBean`，业务方可覆盖

| 维度 | 值 |
|------|-----|
| 顶级包 | `com.xytang.common.web` |
| 父 POM | `com.xytang:spring-cloud-alibaba:1.0-SNAPSHOT` |
| 当前 artifactId | `spring-cloud-common-web` |
| packaging | `jar` |
| 是否有代码 | ✅ 完整 |

---

## 2. 目录结构

```
spring-cloud-common-web/
├── pom.xml
└── src/
    ├── main/
    │   └── java/com/xytang/common/web/
    │       ├── config/             配置类
    │       ├── handler/            全局异常处理器
    │       ├── advice/             ResponseBodyAdvice
    │       └── filter/             过滤器
    └── test/
        └── java/com/xytang/common/web/
            ├── config/             配置测试
            └── handler/            异常处理测试
```

---

## 3. 包结构

| 包 | 职责 |
|----|------|
| `com.xytang.common.web.config` | 配置类：密码编码器、Web MVC、Jackson、CORS（计划） |
| `com.xytang.common.web.handler` | 全局异常处理器：`GlobalExceptionHandler` |
| `com.xytang.common.web.advice` | ResponseBodyAdvice：`RResponseAdvice` 自动包装 `R<T>` |
| `com.xytang.common.web.filter` | 过滤器：`TraceIdFilter` |

---

## 4. POM 依赖模板

```xml
<dependencies>
    <!-- 内部依赖 -->
    <dependency>
        <groupId>com.xytang</groupId>
        <artifactId>spring-cloud-common-core</artifactId>
    </dependency>

    <!-- Spring Boot Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- 参数校验 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- 密码哈希：Argon2id -->
    <dependency>
        <groupId>org.bouncycastle</groupId>
        <artifactId>bcprov-jdk18on</artifactId>
    </dependency>

    <!-- XSS 过滤：HTML 解析 -->
    <dependency>
        <groupId>org.jsoup</groupId>
        <artifactId>jsoup</artifactId>
    </dependency>
</dependencies>
```

---

## 5. 技术栈选型

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot Web | 3.5.0 | Spring MVC 基座 |
| Hibernate Validator | Spring Boot 管理 | 参数校验 |
| Bouncy Castle | 1.78.1 | Argon2id 密码哈希（替代 BCrypt） |
| Jsoup | 1.17.2 | XSS 过滤（HTML 清洗） |
| Lombok | 父 POM 全局声明 | 注解简化 |

> ⚠️ **密码哈希方案**：项目使用 **Argon2id**（Bouncy Castle 实现），**不是** BCrypt。Argon2id 是 OWASP 推荐的现代密码哈希算法，抗 GPU/ASIC 攻击。

---

## 6. 功能清单

### 6.1 全局异常处理 `GlobalExceptionHandler`

- **职责**：捕获所有 Controller 抛出的异常，统一转为 `R<Void>` 响应
- **异常映射**：
  | 异常类型 | HTTP 状态 | 业务 code |
  |---------|----------|----------|
  | `BusinessException`（含子类） | 200 | 异常自带 code |
  | `MethodArgumentNotValidException` | 400 | 40010 |
  | `ConstraintViolationException` | 400 | 40010 |
  | `HttpMessageNotReadableException` | 400 | 40010 |
  | `NoHandlerFoundException` | 404 | 40404 |
  | `HttpRequestMethodNotSupportedException` | 405 | 40010 |
  | `Throwable`（兜底） | 500 | 50000 |
- **实现技术**：`@RestControllerAdvice` + `@ExceptionHandler`

### 6.2 统一响应包装 `RResponseAdvice`

- **职责**：Controller 返回非 `R<T>` 类型时，自动包装为 `R<T>`
- **触发条件**：Controller 返回类型不是 `R`/`ResponseEntity`/`void`/`String`（视图名）
- **实现技术**：`ResponseBodyAdvice` + `@RestControllerAdvice`
- **关闭方式**：方法或类加 `@ResponseWrap(ignore = true)` 注解（计划，未实现）

### 6.3 Trace-Id 透传 `TraceIdFilter`

- **职责**：从 `X-Trace-Id` 头读取网关生成的 traceId，写入 MDC；缺失则生成
- **下游使用**：所有日志自动带上 `X-Trace-Id`（通过 logback 的 `%X{traceId}`）
- **实现技术**：`OncePerRequestFilter` + MDC

### 6.4 密码编码器 `PasswordEncoderConfig`

- **职责**：注册 `PasswordEncoder` Bean，使用 Argon2id 算法
- **参数**：内存 64MB、迭代 3 次、并行度 1（OWASP 推荐参数）
- **实现技术**：`BCrypt` 已废弃，改用 `org.springframework.security.crypto.argon2.Argon2PasswordEncoder`（底层 Bouncy Castle）

### 6.5 计划功能（TODO）

| 功能 | 状态 | 说明 |
|------|------|------|
| `@LoginUser` 注解 + `LoginUserArgumentResolver` | 未实现 | 从 Sa-Token 上下文注入当前用户 ID |
| `@ResponseWrap` 注解（关闭自动包装） | 未实现 | 标注在方法/类上跳过 `RResponseAdvice` |
| `@RepeatSubmit` 防重提交 | 未实现 | Redis token 机制，TTL 5s |
| `XssFilter` | 未实现 | Jsoup 清洗请求体，富文本字段可配置豁免 |
| `JacksonConfig` Long→String | 未实现 | 雪花 ID 序列化为 String 避免前端精度丢失 |
| `CorsConfig` | 未实现 | 全局 CORS 白名单 |
| `PageArgumentResolver` | 未实现 | `PageQuery` 自动解析 |
| `RateLimitInterceptor` | 未实现 | 接口级限流，基于 Redisson |
| `LogInterceptor` | 未实现 | 请求日志 |
| `AutoConfiguration.imports` | 未实现 | 当前未声明自动装配入口 |

---

## 7. 关键类清单

| 类名 | 包 | 职责 |
|------|----|------|
| `PasswordEncoderConfig` | `config` | Argon2id 密码编码器配置 |
| `GlobalExceptionHandler` | `handler` | 全局异常 → R<Void> 转换 |
| `RResponseAdvice` | `advice` | 自动包装 R<T> |
| `TraceIdFilter` | `filter` | Trace-Id 透传 |

---

## 8. 配置项

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `xytang.web.trace-id.header` | `X-Trace-Id` | Trace-Id 头名 |
| `xytang.web.response-wrap.enabled` | `true` | 是否开启自动包装 R<T> |

> 子模块内**无** `application.yml`，所有配置在 Nacos `spring-cloud-shared.yaml` 中。

---

## 9. 与其他模块的关系

| 关系 | 模块 |
|------|------|
| 依赖 | `spring-cloud-common-core` |
| 被依赖 | `spring-cloud-auth`、所有业务服务（除网关）、`spring-cloud-starter-sso-client` |

---

## 10. 红线

1. ❌ 业务服务再写 `@RestControllerAdvice`（会和 `GlobalExceptionHandler` 冲突）
2. ❌ 业务服务自定义 `PasswordEncoder` Bean（必须用本模块的 Argon2id）
3. ❌ 业务服务自定义 `JacksonConfig`（会覆盖 Long→String，导致前端精度丢失）
3. ❌ 在本模块写 Controller（只暴露基础设施 Bean）
4. ❌ Controller 不返回 `R<T>` 也不被 `RResponseAdvice` 包装
5. ❌ 用 BCrypt 存密码（必须 Argon2id）
6. ❌ AutoConfiguration Bean 不加 `@ConditionalOnMissingBean`（导致业务方无法覆盖）
7. ❌ 在 `GlobalExceptionHandler` 中吞掉异常（必须转 `R<Void>` 或重新抛出）
8. ❌ 在 `TraceIdFilter` 中阻塞 IO（必须用 `Mono.fromCallable` 包装阻塞调用）
