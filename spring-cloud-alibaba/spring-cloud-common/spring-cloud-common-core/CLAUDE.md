# CLAUDE.md — spring-cloud-common-core 核心工具

> 本文档面向 AI 编码助手，用于在 `spring-cloud-common-core/` 目录下工作时提供模块约束、功能清单与开发规范。
> 工作前**必须**先读取父目录的 [`spring-cloud-common/CLAUDE.md`](../CLAUDE.md) 与 [`spring-cloud-alibaba/CLAUDE.md`](../../CLAUDE.md) 了解全局规范。

---

## 1. 模块定位

`spring-cloud-common-core` 是整个项目的**核心工具层**，提供纯 POJO + 工具类，**无 Spring 依赖**，被所有其他 common 子模块和业务服务依赖。

**核心设计原则**：
1. **纯 POJO**：禁止引入 Spring、MyBatis、Servlet 等任何运行时框架依赖
2. **API 与实现分离**：本模块只定义接口、DTO、异常、常量、工具，**不写实现**
3. **稳定优先**：本模块被所有服务依赖，**禁止**频繁变更，**禁止**破坏性重构

| 维度 | 值 |
|------|-----|
| 顶级包 | `com.xytang.common.core` |
| 父 POM | `com.xytang:spring-cloud-alibaba:1.0-SNAPSHOT` |
| 当前 artifactId | `spring-cloud-common-core` |
| packaging | `jar` |
| 是否有代码 | ✅ 完整 |

---

## 2. 目录结构

```
spring-cloud-common-core/
├── pom.xml
└── src/
    ├── main/
    │   └── java/com/xytang/common/core/
    │       ├── response/             统一响应与分页
    │       ├── constant/             常量与 Key 前缀
    │       ├── event/                事件基类
    │       ├── exception/           业务异常体系
    │       └── util/                 工具类
    └── test/
        └── java/com/xytang/common/core/
            └── response/            单元测试
```

---

## 3. 包结构

| 包 | 职责 |
|----|------|
| `com.xytang.common.core.response` | 统一响应 `R<T>`、错误码 `ErrorCode`/`BizCode`、分页 `PageResult`/`PageQuery` |
| `com.xytang.common.core.constant` | 缓存 Key `CacheKeyConstants`、通用常量 `CommonConstants`、HTTP 头 `HeaderConstants` |
| `com.xytang.common.core.event` | 事件基类 `BaseEvent`（所有 MQ 事件的父类） |
| `com.xytang.common.core.exception` | 业务异常体系（`BusinessException` 等 12 个异常类） |
| `com.xytang.common.core.util` | 工具类（`PasswordPolicyValidator` 等） |

---

## 4. POM 依赖模板

```xml
<dependencies>
    <!-- 工具集（不指定版本，由父 POM 管理） -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
    </dependency>

    <!-- 仅测试时引入 Spring（保持 main 代码纯 POJO） -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

> ⚠️ **禁止**在 main 代码中引入 Spring/MyBatis/Servlet 依赖。

---

## 5. 技术栈选型

| 技术 | 版本 | 用途 |
|------|------|------|
| Hutool | 5.8.27 | 通用工具（StrUtil、DateUtil、BeanUtil） |
| Jackson | Spring Boot 管理 | JSON 序列化（仅测试用） |
| JUnit 5 | Spring Boot 管理 | 单元测试 |
| Lombok | 父 POM 全局声明 | 注解简化（`@Data`、`@Builder`） |

---

## 6. 功能清单

### 6.1 统一响应 `R<T>`

- **职责**：所有 HTTP 接口的统一返回包装类
- **字段**：`code`（业务状态码）、`message`（友好文案）、`data`（数据）、`timestamp`（毫秒时间戳）、`traceId`（链路追踪 ID）
- **静态工厂**：`R.ok()`、`R.ok(data)`、`R.ok(message, data)`、`R.fail(ErrorCode)`、`R.fail(ErrorCode, message)`、`R.fail(code, message)`
- **实现技术**：纯 POJO + Lombok `@Data` + 静态工厂方法

### 6.2 分页 `PageResult` 与 `PageQuery`

- **`PageResult<T>`**：分页响应，字段 `records`、`total`、`page`、`size`、`pages`、`hasPrevious`、`hasNext`
- **`PageQuery`**：分页入参基类，字段 `page`（从 1 开始）、`size`（默认 10，最大 100）、`orderBy`
- **实现技术**：纯 POJO + Lombok

### 6.3 错误码 `ErrorCode` 与 `BizCode`

- **`ErrorCode`**：错误码接口，`getCode()/getUserMessage()/getDevMessage()/getHttpStatus()`，支持自定义实现
- **`BizCode`**：枚举实现错误码，5 位数字 = 区段(1) + 模块(1) + 序号(3)；区段 1 参数 / 2 用户权限 / 3 业务 / 4 第三方 / 5 系统；成功码固定 200
- **实现技术**：Java 枚举 + `formatDevMessage(Object...)` 占位符填充

### 6.5 常量

- **`CacheKeyConstants`**：Redis 缓存 Key 前缀（如 `spring-cloud:{service}:{biz}:{id}`）
- **`CommonConstants`**：通用常量（分隔符、默认值）
- **`HeaderConstants`**：HTTP 头名（`X-Login-Id`、`X-Trace-Id`、`X-Gray-Version`、`Authorization`）

### 6.6 事件基类 `BaseEvent`

- **职责**：所有 MQ 事件的父类
- **字段**：`eventId`（UUID）、`eventType`、`source`、`timestamp`、`traceId`
- **实现技术**：抽象基类 + Lombok `@Builder`

### 6.7 业务异常体系

| 异常类 | 用途 | 触发场景 |
|--------|------|---------|
| `BusinessException` | 业务异常基类 | 所有业务异常的父类 |
| `BizException` | 通用业务异常 | 业务逻辑不满足 |
| `AuthException` | 认证异常 | 未登录、Token 无效 |
| `PermissionException` | 权限异常 | 无权限访问 |
| `SystemException` | 系统异常 | 系统级故障 |
| `AccountLockedException` | 账户锁定 | 登录失败次数过多 |
| `SsoTicketInvalidException` | SSO 票据无效 | SSO 授权码/Token 过期 |
| `ContentStatusTransitionException` | 内容状态转换非法 | 工作流/内容状态机非法跳转 |
| `LastSuperAdminException` | 最后一个超级管理员 | 禁止删除最后一个超管 |
| `OptimisticLockException` | 乐观锁失败 | 版本号冲突 |
| `DataScopeDeniedException` | 数据权限拒绝 | 跨部门访问数据 |
| `UserNotFoundException` | 用户不存在 | 登录/查询用户不存在 |

### 6.8 工具类

- **`PasswordPolicyValidator`**：密码策略校验（长度、复杂度、黑名单）
- **实现技术**：静态工具类 + 正则 + 内置黑名单

---

## 7. 关键类清单

| 类名 | 包 | 职责 |
|------|----|------|
| `R<T>` | `response` | 统一响应包装 |
| `ErrorCode` | `response` | 错误码接口 |
| `BizCode` | `response` | 业务码枚举（实现 `ErrorCode`） |
| `PageResult<T>` | `response` | 分页响应 |
| `PageQuery` | `response` | 分页入参基类 |
| `CacheKeyConstants` | `constant` | 缓存 Key 前缀 |
| `CommonConstants` | `constant` | 通用常量 |
| `HeaderConstants` | `constant` | HTTP 头名 |
| `BaseEvent` | `event` | 事件基类 |
| `BusinessException` | `exception` | 业务异常基类 |
| `AuthException` | `exception` | 认证异常 |
| `PermissionException` | `exception` | 权限异常 |
| `SystemException` | `exception` | 系统异常 |
| `AccountLockedException` | `exception` | 账户锁定 |
| `SsoTicketInvalidException` | `exception` | SSO 票据无效 |
| `ContentStatusTransitionException` | `exception` | 状态转换非法 |
| `LastSuperAdminException` | `exception` | 最后超管 |
| `OptimisticLockException` | `exception` | 乐观锁 |
| `DataScopeDeniedException` | `exception` | 数据权限拒绝 |
| `UserNotFoundException` | `exception` | 用户不存在 |
| `BizException` | `exception` | 通用业务异常 |
| `PasswordPolicyValidator` | `util` | 密码策略 |

---

## 8. 配置项

本模块**无配置项**（纯 POJO，不读 yml）。所有常量在类内静态声明。

---

## 9. 与其他模块的关系

| 关系 | 模块 |
|------|------|
| 被依赖 | 所有 common 子模块、所有业务服务、所有 Starter |
| 依赖 | 无（仅 Hutool + Lombok + 测试时 Spring Boot Test） |

---

## 10. 红线

1. ❌ 在 main 代码中引入 Spring/MyBatis/Servlet/JPA 依赖（必须保持纯 POJO）
2. ❌ 在 main 代码中引入 `@Component`/`@Service`/`@Configuration` 等 Spring 注解
3. ❌ 在 main 代码中写 `@Document`/`@TableName`/`@Table` 等持久化注解
4. ❌ 在 `R<T>` 之外暴露其他响应格式
5. ❌ 业务异常不继承 `BusinessException`（必须走统一异常体系）
6. ❌ 用 `throw new RuntimeException(...)`（必须用具体异常类）
7. ❌ 频繁变更 `BizCode` 枚举值（影响所有服务）
8. ❌ 在 `constant` 包硬编码业务路径（如 `/api/system/users`）
9. ❌ 在 `util` 工具类中加 Spring Bean（必须静态方法）
