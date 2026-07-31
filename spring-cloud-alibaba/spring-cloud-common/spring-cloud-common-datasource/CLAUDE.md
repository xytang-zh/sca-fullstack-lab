# CLAUDE.md — spring-cloud-common-datasource 多数据源

> 本文档面向 AI 编码助手，用于在 `spring-cloud-common-datasource/` 目录下工作时提供模块约束、功能计划与开发规范。
> 工作前**必须**先读取父目录的 [`spring-cloud-common/CLAUDE.md`](../CLAUDE.md) 与 [`spring-cloud-alibaba/CLAUDE.md`](../../CLAUDE.md) 了解全局规范。

---

## 1. 模块定位

`spring-cloud-common-datasource` 基于 dynamic-datasource 封装，提供多数据源切换能力，支持 **MySQL 主库 + PostgreSQL pgvector + 人大金仓 KingbaseES + 达梦 DM** 等多种数据库适配。

**核心设计原则**：
1. **国产化适配**：支持人大金仓、达梦等国产数据库
2. **注解切换**：基于 `@DS` 注解声明式切换数据源
3. **与 ShardingSphere 集成**：敏感字段加密走 ShardingSphere

| 维度 | 值 |
|------|-----|
| 顶级包 | `com.xytang.common.datasource` |
| 父 POM | `com.xytang:spring-cloud-alibaba:1.0-SNAPSHOT` |
| 当前 artifactId | `spring-cloud-common-datasource` |
| packaging | `jar` |
| 是否有代码 | ❌ 空壳（仅 pom.xml，**本 CLAUDE.md 为规划书**） |

---

## 2. 技术栈选型

| 技术 | 版本 | 用途 |
|------|------|------|
| dynamic-datasource | 4.3.1 | 多数据源切换 |
| Apache ShardingSphere | 5.5.2 | 分库分表 + 敏感字段加密 |
| Druid | Spring Boot 管理 | 数据库连接池 |
| Lombok | 父 POM 全局声明 | 注解简化 |

---

## 3. 功能计划

### 3.1 多数据源配置

- **计划数据源**：
  | 数据源名 | 数据库类型 | 用途 |
  |---------|----------|------|
  | `master` | MySQL 8.4 | 业务主库 |
  | `pgvector` | PostgreSQL 16 + pgvector 0.8 | 向量库（AI 服务） |
  | `kingbase` | KingbaseES V8 R6 | 国产化适配 |
  | `dm` | DM8 | 国产化适配 |

### 3.2 `@DS` 注解切换

- **职责**：标注在 Service 或 Mapper 方法上，声明使用的数据源
- **示例**：`@DS("pgvector")` → 切换到 PostgreSQL 向量库
- **实现技术**：重导出 dynamic-datasource 的 `@DS` 注解 + AOP 切面

### 3.3 国产库适配

- **驱动适配**：KingbaseES 用 `com.kingbase8.Driver`，DM 用 `dm.jdbc.driver.DmDriver`
- **方言适配**：MyBatis-Plus `DbType` 枚举扩展
- **SQL 兼容**：用标准 SQL（`COALESCE`、`||`、`CURRENT_TIMESTAMP`），**禁止** MySQL 私有函数

### 3.4 敏感字段加密（ShardingSphere）

- **职责**：手机号、身份证号等敏感字段入库前加密，读出后解密
- **实现技术**：ShardingSphere `encrypt` 规则 + AES 算法
- **计划范围**：`sys_user.phone`、`sys_user.id_card`、`sys_user.email`

---

## 4. POM 依赖模板（计划）

```xml
<dependencies>
    <!-- 内部依赖 -->
    <dependency>
        <groupId>com.xytang</groupId>
        <artifactId>spring-cloud-common-core</artifactId>
    </dependency>

    <!-- dynamic-datasource -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>dynamic-datasource-spring-boot3-starter</artifactId>
    </dependency>

    <!-- ShardingSphere（敏感字段加密） -->
    <dependency>
        <groupId>org.apache.shardingsphere</groupId>
        <artifactId>shardingsphere-jdbc</artifactId>
    </dependency>
</dependencies>
```

---

## 5. 配置项（计划）

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `spring.datasource.dynamic.primary` | `master` | 默认数据源 |
| `spring.datasource.dynamic.strict` | `false` | 是否严格模式（未匹配时抛异常） |
| `spring.datasource.dynamic.datasource.master.url` | （Nacos 注入） | 主库 JDBC URL |
| `spring.datasource.dynamic.datasource.pgvector.url` | （Nacos 注入） | 向量库 JDBC URL |
| `spring.datasource.dynamic.datasource.kingbase.url` | （Nacos 注入） | 人大金仓 JDBC URL |
| `spring.datasource.dynamic.datasource.dm.url` | （Nacos 注入） | 达梦 JDBC URL |

> 子模块内**无** `application.yml`，所有配置在 Nacos 中。

---

## 6. 与其他模块的关系

| 关系 | 模块 |
|------|------|
| 依赖 | `spring-cloud-common-core` |
| 被依赖 | `spring-cloud-system`、`spring-cloud-ai`（向量库）、`spring-cloud-report`、`spring-cloud-portal`（国产库） |
| 不依赖 | `spring-cloud-common-mybatis`（避免循环依赖，由业务服务同时引入） |

---

## 7. 红线

1. ❌ 跨数据源事务用 `@Transactional`（必须用 `@DSTransactional`）
2. ❌ 用 MySQL 私有函数（`IFNULL`、`CONCAT`、`DATE_FORMAT`）（必须用标准 SQL `COALESCE`、`||`、`TO_CHAR`）
3. ❌ 用自增主键（国产库不支持 `AUTO_INCREMENT`，必须用雪花 ID）
4. ❌ 在 Service 直接 `new DataSource(...)`（必须用 `@DS` 注解）
5. ❌ 国产库切换不验证方言（导致 SQL 语法错误）
6. ❌ 敏感字段明文存储（必须用 ShardingSphere 加密）
7. ❌ AutoConfiguration Bean 不加 `@ConditionalOnMissingBean`

---

## 8. 实施状态

⚠️ 本模块当前为**空壳**，仅 `pom.xml`，**无任何 .java 代码**。

| 任务 | 状态 |
|------|------|
| pom.xml | ✅ 存在 |
| DataSourceConfig 配置类 | ❌ 未实现 |
| `@DS` 注解重导出 | ❌ 未实现 |
| 国产库驱动适配 | ❌ 未实现 |
| ShardingSphere 加密规则 | ❌ 未实现 |
| AutoConfiguration.imports | ❌ 未实现 |
| 单元测试 | ❌ 未实现 |

> 落地实现时请对照本规划，并在完成后更新本 CLAUDE.md 第 2-7 节为实际代码状态。
