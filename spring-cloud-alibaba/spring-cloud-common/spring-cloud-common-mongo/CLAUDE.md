# CLAUDE.md — spring-cloud-common-mongo MongoDB 配置

> 本文档面向 AI 编码助手，用于在 `spring-cloud-common-mongo/` 目录下工作时提供模块约束、功能计划与开发规范。
> 工作前**必须**先读取父目录的 [`spring-cloud-common/CLAUDE.md`](../CLAUDE.md) 与 [`spring-cloud-alibaba/CLAUDE.md`](../../CLAUDE.md) 了解全局规范。

---

## 1. 模块定位

`spring-cloud-common-mongo` 提供 MongoDB 客户端配置、通用 DAO、分页与索引管理，用于存储**非结构化/半结构化数据**（如 AI 对话历史、操作日志）。

**核心设计原则**：
1. **文档模型**：所有文档必须用 `@Document` 注解，**禁止**裸 POJO
2. **索引优先**：高频查询字段必须建索引（自动初始化）
3. **与 MySQL 分工**：业务主数据在 MySQL，MongoDB 仅存文档型数据

| 维度 | 值 |
|------|-----|
| 顶级包 | `com.xytang.common.mongo` |
| 父 POM | `com.xytang:spring-cloud-alibaba:1.0-SNAPSHOT` |
| 当前 artifactId | `spring-cloud-common-mongo` |
| packaging | `jar` |
| 是否有代码 | ❌ 空壳（仅 pom.xml，**本 CLAUDE.md 为规划书**） |

---

## 2. 技术栈选型

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Data MongoDB | Spring Boot 3.5.0 管理 | MongoDB 客户端 |
| Jackson | Spring Boot 管理 | JSON 序列化 |
| Lombok | 父 POM 全局声明 | 注解简化 |

---

## 3. 功能计划

### 3.1 MongoConfig 配置类

- **职责**：注册 `MongoTemplate`、`MongoClient`，初始化索引
- **索引初始化**：扫描 `@Document` 注解的类，自动建索引
- **实现技术**：`@Configuration` + `@Bean` + `MongoTemplate.indexOps()`

### 3.2 BaseMongoRepository 通用 DAO

- **职责**：通用 CRUD、分页、批量操作
- **方法**：`save`/`findById`/`findAll`/`deleteById`/`page`/`batchInsert`
- **实现技术**：泛型基类 + `MongoTemplate`

### 3.3 BaseEntity（MongoDB 文档基类）

- **字段**：`id`（ObjectId）、`createTime`、`updateTime`、`createBy`、`updateBy`、`delFlag`
- **注解**：`@Document`、`@Indexed`
- **实现技术**：抽象基类 + Lombok

### 3.4 MongoPageUtil 分页工具

- **职责**：基于 `Pageable` 的分页查询
- **方法**：`page(Query, pageNum, pageSize)` 返回 `PageVO<T>`
- **实现技术**：`MongoTemplate.count()` + `Query.with(Pageable)`

### 3.5 MongoIndexUtil 索引管理

- **职责**：创建/删除/重建索引
- **方法**：`createIndex`/`dropIndex`/`reindex`
- **实现技术**：`MongoTemplate.indexOps()`

### 3.6 JsonReaderConverter JSON 字段转换

- **职责**：MongoDB 文档中的 JSON 字段与 Java 对象互转
- **实现技术**：实现 `org.springframework.core.convert.converter.Converter`

---

## 4. POM 依赖模板（计划）

```xml
<dependencies>
    <!-- 内部依赖 -->
    <dependency>
        <groupId>com.xytang</groupId>
        <artifactId>spring-cloud-common-core</artifactId>
    </dependency>

    <!-- Spring Data MongoDB -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-mongodb</artifactId>
    </dependency>
</dependencies>
```

---

## 5. 配置项（计划）

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `spring.data.mongodb.uri` | `mongodb://localhost:27017/sca` | MongoDB 连接 URI |
| `spring.data.mongodb.database` | `sca` | 默认数据库 |
| `xytang.mongo.auto-index` | `true` | 是否自动建索引 |

> 子模块内**无** `application.yml`，所有配置在 Nacos 中。

---

## 6. 与其他模块的关系

| 关系 | 模块 |
|------|------|
| 依赖 | `spring-cloud-common-core` |
| 被依赖 | `spring-cloud-ai`（对话历史）、`spring-cloud-log`（操作日志） |

---

## 7. 红线

1. ❌ 文档类不用 `@Document` 注解（导致 Spring Data MongoDB 不识别）
2. ❌ 高频查询字段不建索引（导致全表扫描）
3. ❌ 用裸 POJO 存 MongoDB（必须继承 `BaseEntity`）
4. ❌ 在 MongoDB 存业务主数据（业务主数据在 MySQL，MongoDB 仅存文档型）
5. ❌ 不分页查询大集合（导致 OOM）
6. ❌ AutoConfiguration Bean 不加 `@ConditionalOnMissingBean`

---

## 8. 实施状态

⚠️ 本模块当前为**空壳**，仅 `pom.xml`，**无任何 .java 代码**。

| 任务 | 状态 |
|------|------|
| pom.xml | ✅ 存在 |
| MongoConfig 配置类 | ❌ 未实现 |
| BaseMongoRepository | ❌ 未实现 |
| BaseEntity | ❌ 未实现 |
| MongoPageUtil | ❌ 未实现 |
| MongoIndexUtil | ❌ 未实现 |
| JsonReaderConverter | ❌ 未实现 |
| AutoConfiguration.imports | ❌ 未实现 |
| 单元测试 | ❌ 未实现 |

> 落地实现时请对照本规划，并在完成后更新本 CLAUDE.md 第 2-7 节为实际代码状态。
