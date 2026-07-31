# CLAUDE.md — spring-cloud-common-swagger OpenAPI 文档

> 本文档面向 AI 编码助手，用于在 `spring-cloud-common-swagger/` 目录下工作时提供模块约束、功能计划与开发规范。
> 工作前**必须**先读取父目录的 [`spring-cloud-common/CLAUDE.md`](../CLAUDE.md) 与 [`spring-cloud-alibaba/CLAUDE.md`](../../CLAUDE.md) 了解全局规范。

---

## 1. 模块定位

`spring-cloud-common-swagger` 基于 springdoc-openapi + Knife4j 提供 OpenAPI 3 文档生成与聚合能力。

**核心设计原则**：
1. **注解驱动**：Controller 必须用 `@Tag`/`@Operation`，DTO/VO 字段必须用 `@Schema`
2. **生产关闭**：生产环境必须 `knife4j.production=true`，避免接口暴露
3. **聚合在网关**：Knife4j 在网关聚合所有服务文档，单服务不暴露 `/v3/api-docs`

| 维度 | 值 |
|------|-----|
| 顶级包 | `com.xytang.common.swagger` |
| 父 POM | `com.xytang:spring-cloud-alibaba:1.0-SNAPSHOT` |
| 当前 artifactId | `spring-cloud-common-swagger` |
| packaging | `jar` |
| 是否有代码 | ❌ 空壳（仅 pom.xml，**本 CLAUDE.md 为规划书**） |

---

## 2. 技术栈选型

| 技术 | 版本 | 用途 |
|------|------|------|
| springdoc-openapi | 2.6.0 | OpenAPI 3 文档生成 |
| Knife4j | 4.5.0 | 增强 UI + 聚合 |
| Lombok | 父 POM 全局声明 | 注解简化 |

---

## 3. 功能计划

### 3.1 SwaggerAutoConfiguration 自动装配

- **职责**：注册 OpenAPI 相关 Bean，加载配置
- **扫描包**：`com.xytang.*.controller`
- **实现技术**：`@Configuration` + `@EnableConfigurationProperties`

### 3.2 OpenApiConfig 全局定义

- **职责**：定义全局 OpenAPI 元数据（title、version、description、securityScheme）
- **安全方案**：`Bearer` JWT，所有接口默认需要 Token（除白名单）
- **实现技术**：`@Bean OpenAPI`

### 3.3 Knife4jConfig 增强文档

- **职责**：启用 Knife4j 增强（在线调试、文档聚合）
- **生产环境**：`knife4j.production=true` 关闭
- **实现技术**：`@Configuration` + `@Bean Knife4jOpenApiCustomizer`

### 3.4 SwaggerResourceHandler 聚合

- **职责**：聚合各服务的 `/v3/api-docs` 到网关
- **路由**：在网关配置 `spring-cloud-{service}/v3/api-docs`
- **实现技术**：`@RestController` + `OpenApiResource`

### 3.5 SwaggerProperties 可配置属性

- **职责**：暴露 `xytang.swagger.*` 配置项
- **字段**：`enabled`、`title`、`version`、`description`、`scan-base-package`
- **实现技术**：`@ConfigurationProperties`

---

## 4. POM 依赖模板（计划）

```xml
<dependencies>
    <!-- 内部依赖 -->
    <dependency>
        <groupId>com.xytang</groupId>
        <artifactId>spring-cloud-common-core</artifactId>
    </dependency>

    <!-- springdoc-openapi（Web MVC 服务用） -->
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    </dependency>

    <!-- Knife4j -->
    <dependency>
        <groupId>com.github.xiaoymin</groupId>
        <artifactId>knife4j-openapi3-jakarta-spring-boot-starter</artifactId>
    </dependency>
</dependencies>
```

> ⚠️ 网关用 `springdoc-openapi-starter-webflux-ui`（响应式），见 `spring-cloud-gateway/CLAUDE.md`。

---

## 5. 配置项（计划）

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `springdoc.api-docs.enabled` | `true` | 是否启用 API 文档 |
| `springdoc.api-docs.path` | `/v3/api-docs` | 文档路径 |
| `springdoc.swagger-ui.enabled` | `true` | 是否启用 Swagger UI |
| `springdoc.swagger-ui.path` | `/swagger-ui.html` | UI 路径 |
| `knife4j.enable` | `true` | 是否启用 Knife4j |
| `knife4j.production` | `false` | 生产模式（关闭增强） |
| `xytang.swagger.title` | `SCA 一体化平台 API` | 文档标题 |
| `xytang.swagger.version` | `1.0-SNAPSHOT` | 文档版本 |

> 子模块内**无** `application.yml`，所有配置在 Nacos 中。

---

## 6. 与其他模块的关系

| 关系 | 模块 |
|------|------|
| 依赖 | `spring-cloud-common-core` |
| 被依赖 | 所有业务服务（除网关）、`spring-cloud-auth` |
| 网关 | `spring-cloud-gateway` 用 `springdoc-openapi-starter-webflux-ui`（响应式） |

---

## 7. 红线

1. ❌ Controller 不用 `@Tag`/`@Operation` 注解（导致文档缺失）
2. ❌ DTO/VO 字段不用 `@Schema` 注解（导致字段说明缺失）
3. ❌ 生产环境不关闭文档（`knife4j.production` 必须 `true`）
4. ❌ 在文档示例中暴露真实密码/Token
5. ❌ 在白名单中暴露 `/v3/api-docs` 路径（必须鉴权）
6. ❌ AutoConfiguration Bean 不加 `@ConditionalOnMissingBean`

---

## 8. 实施状态

⚠️ 本模块当前为**空壳**，仅 `pom.xml`，**无任何 .java 代码**。

| 任务 | 状态 |
|------|------|
| pom.xml | ✅ 存在 |
| SwaggerAutoConfiguration | ❌ 未实现 |
| OpenApiConfig | ❌ 未实现 |
| Knife4jConfig | ❌ 未实现 |
| SwaggerResourceHandler | ❌ 未实现 |
| SwaggerProperties | ❌ 未实现 |
| AutoConfiguration.imports | ❌ 未实现 |
| 单元测试 | ❌ 未实现 |

> 落地实现时请对照本规划，并在完成后更新本 CLAUDE.md 第 2-7 节为实际代码状态。
