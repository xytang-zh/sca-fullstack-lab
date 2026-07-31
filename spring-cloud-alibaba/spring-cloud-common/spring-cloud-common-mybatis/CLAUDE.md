# CLAUDE.md — spring-cloud-common-mybatis MyBatis-Plus 增强

> 本文档面向 AI 编码助手，用于在 `spring-cloud-common-mybatis/` 目录下工作时提供模块约束、功能清单与开发规范。
> 工作前**必须**先读取父目录的 [`spring-cloud-common/CLAUDE.md`](../CLAUDE.md) 与 [`spring-cloud-alibaba/CLAUDE.md`](../../CLAUDE.md) 了解全局规范。

---

## 1. 模块定位

`spring-cloud-common-mybatis` 提供 MyBatis-Plus 拦截器链配置、数据权限、自动填充、雪花 ID 等增强能力，**所有依赖关系库的服务**都会引入此模块。

**核心设计原则**：
1. **拦截器链统一**：分页、多租户（可选）、数据权限通过 `MybatisPlusInterceptor` 统一注册
2. **数据权限基于注解**：`@DataScope` + `RbacContext` 实现声明式数据权限
3. **雪花 ID 强制**：所有主键用雪花 ID，**禁止**自增（避免暴露业务量）

| 维度 | 值 |
|------|-----|
| 顶级包 | `com.xytang.common.mybatis` |
| 父 POM | `com.xytang:spring-cloud-alibaba:1.0-SNAPSHOT` |
| 当前 artifactId | `spring-cloud-common-mybatis` |
| packaging | `jar` |
| 是否有代码 | ✅ 完整 |

---

## 2. 目录结构

```
spring-cloud-common-mybatis/
├── pom.xml
└── src/
    └── main/
        └── java/com/xytang/common/mybatis/
            ├── config/
            │   └── MybatisPlusConfig.java
            ├── annotation/
            │   └── DataScope.java
            ├── interceptor/
            │   └── DataPermissionInnerInterceptor.java
            ├── handler/
            │   └── MetaObjectHandlerImpl.java
            └── rbac/
                └── RbacContext.java
```

---

## 3. 包结构

| 包 | 职责 |
|----|------|
| `com.xytang.common.mybatis.config` | MyBatis-Plus 拦截器链配置 |
| `com.xytang.common.mybatis.annotation` | 注解：`@DataScope` |
| `com.xytang.common.mybatis.interceptor` | 内部拦截器：`DataPermissionInnerInterceptor` |
| `com.xytang.common.mybatis.handler` | MetaObjectHandler：自动填充字段 |
| `com.xytang.common.mybatis.rbac` | RBAC 上下文：`RbacContext`（数据权限运行时上下文） |

> 计划新增包：`base`（`BaseEntity`、`BaseController`）、`enums`（`DataType`）。当前未实现。

---

## 4. POM 依赖模板

```xml
<dependencies>
    <!-- 内部依赖 -->
    <dependency>
        <groupId>com.xytang</groupId>
        <artifactId>spring-cloud-common-core</artifactId>
    </dependency>

    <!-- MyBatis-Plus -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
    </dependency>

    <!-- 数据库驱动（按需引入，由业务方决定） -->
    <!--
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>
    -->
</dependencies>
```

---

## 5. 技术栈选型

| 技术 | 版本 | 用途 |
|------|------|------|
| MyBatis-Plus | 3.5.9 | ORM 增强（分页、自动填充、雪花 ID） |
| MyBatis | MyBatis-Plus 管理 | SQL 映射 |
| Spring Boot | 3.5.0 | 基座 |
| Lombok | 父 POM 全局声明 | 注解简化 |

---

## 6. 功能清单

### 6.1 拦截器链配置 `MybatisPlusConfig`（已实现）

- **职责**：注册 `MybatisPlusInterceptor`，添加内部拦截器
- **拦截器顺序**：
  1. `PaginationInnerInterceptor` — 分页
  2. `DataPermissionInnerInterceptor` — 数据权限
- **数据库类型**：`DbType.MYSQL`（默认，可在 Nacos 覆盖）
- **实现技术**：`@Configuration` + `@Bean`

### 6.2 `@DataScope` 注解（已实现）

- **职责**：声明式数据权限，标注在 Service 或 Mapper 方法上
- **属性**：
  | 属性 | 默认值 | 说明 |
  |------|--------|------|
  | `tableAlias` | `""` | 表别名 |
  | `deptField` | `dept_id` | 部门字段名 |
  | `userField` | `create_by` | 用户字段名 |
  | `dataScope` | `DATA_SCOPE_ALL` | 数据范围：全部/部门/部门及下级/本人 |
- **实现技术**：注解 + AOP（通过 `DataPermissionInnerInterceptor` 拦截 SQL）

### 6.3 数据权限拦截器 `DataPermissionInnerInterceptor`（已实现）

- **职责**：拦截 SQL，根据 `@DataScope` 注解和 `RbacContext` 注入权限条件
- **工作流程**：
  1. 解析方法上的 `@DataScope` 注解
  2. 从 `RbacContext` 获取当前用户角色和数据范围
  3. 重写 SQL，注入 `WHERE dept_id IN (...)` 或 `WHERE create_by = ?`
- **实现技术**：继承 MyBatis-Plus 的 `DataPermissionInterceptor`，重写 SQL 解析

### 6.4 自动填充 `MetaObjectHandlerImpl`（已实现）

- **职责**：自动填充 `createTime`/`updateTime`/`createBy`/`updateBy` 字段
- **触发时机**：`insert` 时填充 `createTime`/`updateTime`/`createBy`/`updateBy`；`update` 时填充 `updateTime`/`updateBy`
- **实现技术**：实现 `MetaObjectHandler` 接口

### 6.5 RBAC 上下文 `RbacContext`（已实现）

- **职责**：ThreadLocal 存储当前用户的角色、部门、数据权限范围
- **字段**：`userId`、`deptId`、`roleIds`、`dataScope`
- **生命周期**：请求开始时由 Filter/Interceptor 写入，请求结束时清理
- **实现技术**：`ThreadLocal` + 静态方法

### 6.6 计划功能（TODO）

| 功能 | 状态 | 说明 |
|------|------|------|
| `BaseEntity` | 未实现 | id/createTime/updateTime/createBy/updateBy/delFlag 基类 |
| `BaseController` | 未实现 | 基础 CRUD 控制器 |
| `IdentifierGenerator` 雪花 ID | 未实现 | 自定义雪花算法（默认用 MyBatis-Plus 内置） |
| `TenantLineInnerInterceptor` 多租户 | 未实现 | 多租户拦截器（可选） |
| `AutoConfiguration.imports` | 未实现 | 当前未声明自动装配入口 |

---

## 7. 关键类清单

| 类名 | 包 | 职责 |
|------|----|------|
| `MybatisPlusConfig` | `config` | 拦截器链配置 |
| `DataScope` | `annotation` | `@DataScope` 注解定义 |
| `DataPermissionInnerInterceptor` | `interceptor` | 数据权限 SQL 拦截器 |
| `MetaObjectHandlerImpl` | `handler` | 自动填充字段 |
| `RbacContext` | `rbac` | RBAC 上下文（ThreadLocal） |

---

## 8. 配置项

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `mybatis-plus.global-config.db-config.id-type` | `ASSIGN_ID` | 主键策略：雪花 ID |
| `mybatis-plus.global-config.db-config.logic-delete-field` | `delFlag` | 逻辑删除字段 |
| `mybatis-plus.global-config.db-config.logic-delete-value` | `1` | 已删除值 |
| `mybatis-plus.global-config.db-config.logic-not-delete-value` | `0` | 未删除值 |
| `mybatis-plus.configuration.map-underscore-to-camel-case` | `true` | 下划线转驼峰 |
| `mybatis-plus.mapper-locations` | `classpath*:mapper/**/*.xml` | XML 位置 |

> 子模块内**无** `application.yml`，所有配置在 Nacos `spring-cloud-{service}.yaml` 中。

---

## 9. 与其他模块的关系

| 关系 | 模块 |
|------|------|
| 依赖 | `spring-cloud-common-core` |
| 被依赖 | `spring-cloud-auth`、`spring-cloud-system`、`spring-cloud-log` 等需要持久化的服务 |
| 不依赖 | `spring-cloud-common-datasource`（datasource 仅做多数据源切换，本模块只用默认数据源） |

---

## 10. 红线

1. ❌ 用自增主键（必须雪花 ID，避免暴露业务量）
2. ❌ 在 Service 直接拼接 SQL（必须用 MyBatis-Plus 的 `LambdaQueryWrapper` 或 XML）
3. ❌ 跨数据源事务用 `@Transactional`（必须用 `@DSTransactional`，dynamic-datasource 提供）
4. ❌ 在 `MetaObjectHandlerImpl` 中读取 `HttpServletRequest`（必须从 `RbacContext` 取）
5. ❌ 数据权限不通过 `@DataScope` 注解，而在业务代码里手动写 SQL（导致权限绕过）
6. ❌ 主键 ID 用 `Integer`（必须 `Long`，避免雪花 ID 超出范围）
7. ❌ `@DataScope` 注解加在 Controller 上（必须加在 Service 或 Mapper 方法上）
8. ❌ AutoConfiguration Bean 不加 `@ConditionalOnMissingBean`
