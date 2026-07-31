# CLAUDE.md — spring-cloud-common-es ElasticSearch 配置

> 本文档面向 AI 编码助手，用于在 `spring-cloud-common-es/` 目录下工作时提供模块约束、功能计划与开发规范。
> 工作前**必须**先读取父目录的 [`spring-cloud-common/CLAUDE.md`](../CLAUDE.md) 与 [`spring-cloud-alibaba/CLAUDE.md`](../../CLAUDE.md) 了解全局规范。

---

## 1. 模块定位

`spring-cloud-common-es` 提供 ElasticSearch 8 客户端配置、索引管理与批量操作，用于**全文检索**场景（文章、知识库、日志）。

**核心设计原则**：
1. **索引双写过渡**：重建索引时新旧双写，避免查询空窗
2. **批量操作**：所有写入用 Bulk，**禁止**单条 `index`
3. **Mapping 强类型**：所有字段用 `@Field` 显式声明类型

| 维度 | 值 |
|------|-----|
| 顶级包 | `com.xytang.common.es` |
| 父 POM | `com.xytang:spring-cloud-alibaba:1.0-SNAPSHOT` |
| 当前 artifactId | `spring-cloud-common-es` |
| packaging | `jar` |
| 是否有代码 | ❌ 空壳（仅 pom.xml，**本 CLAUDE.md 为规划书**） |

---

## 2. 技术栈选型

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Data Elasticsearch | Spring Boot 3.5.0 管理 | ES 8 客户端 |
| ES Java Client | 8.15+ | 原生客户端（备用） |
| Lombok | 父 POM 全局声明 | 注解简化 |

---

## 3. 功能计划

### 3.1 EsConfig 配置类

- **职责**：注册 `RestClient`、`ElasticsearchClient`、`ElasticsearchOperations`
- **认证**：Basic Auth（用户名/密码从 Nacos 注入）
- **实现技术**：`@Configuration` + `@Bean`

### 3.2 EsIndexAutoConfiguration 索引自动创建

- **职责**：扫描 `@EsDocument` 注解的类，启动时自动创建索引
- **失败策略**：索引已存在跳过，创建失败抛异常终止启动
- **实现技术**：`ApplicationRunner` + `IndicesClient`

### 3.3 `@EsDocument` / `@EsField` 注解

- **`@EsDocument`**：标注索引名、分片数、副本数
- **`@EsField`**：字段类型、分词器、是否索引
- **实现技术**：注解 + 类扫描

### 3.4 BaseEsRepository 通用查询

- **职责**：通用 CRUD、分页、批量
- **方法**：`save`/`findById`/`search`/`page`/`bulkIndex`
- **实现技术**：泛型接口 + `ElasticsearchOperations`

### 3.5 IndexManager 索引管理

- **职责**：创建/删除/重建索引
- **重建流程**：创建新索引 v2 → 双写 → 全量同步 → 切换别名 → 删除旧索引 v1
- **实现技术**：`IndicesClient` + alias

### 3.6 BulkOperator 批量操作

- **职责**：批量写入/更新/删除
- **批量大小**：默认 1000 条/批，可配置
- **实现技术**：`BulkRequest` + `BulkIngester`

---

## 4. POM 依赖模板（计划）

```xml
<dependencies>
    <!-- 内部依赖 -->
    <dependency>
        <groupId>com.xytang</groupId>
        <artifactId>spring-cloud-common-core</artifactId>
    </dependency>

    <!-- Spring Data Elasticsearch -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
    </dependency>
</dependencies>
```

---

## 5. 配置项（计划）

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `spring.elasticsearch.uris` | `http://localhost:9200` | ES URL |
| `spring.elasticsearch.username` | （Nacos 注入） | Basic Auth 用户名 |
| `spring.elasticsearch.password` | （Nacos 注入） | Basic Auth 密码 |
| `xytang.es.index-prefix` | `sca_` | 索引名前缀 |
| `xytang.es.bulk-size` | `1000` | 批量操作大小 |

> 子模块内**无** `application.yml`，所有配置在 Nacos 中。

---

## 6. 与其他模块的关系

| 关系 | 模块 |
|------|------|
| 依赖 | `spring-cloud-common-core` |
| 被依赖 | `spring-cloud-search`、`spring-cloud-log`（ES 检索） |

---

## 7. 红线

1. ❌ 文档不用 `@Document` + `@Field`（导致字段类型推断错误）
2. ❌ 用单条 `index` 写入（必须用 Bulk）
3. ❌ 索引重建时直接删旧索引（必须双写过渡，避免查询空窗）
4. ❌ 字段类型不显式声明（依赖动态推断导致 mapping 漂移）
5. ❌ 中文不分词（必须用 `ik_max_word` 分词器）
6. ❌ 大结果集不分页（导致 ES OOM）
7. ❌ AutoConfiguration Bean 不加 `@ConditionalOnMissingBean`

---

## 8. 实施状态

⚠️ 本模块当前为**空壳**，仅 `pom.xml`，**无任何 .java 代码**。

| 任务 | 状态 |
|------|------|
| pom.xml | ✅ 存在 |
| EsConfig 配置类 | ❌ 未实现 |
| EsIndexAutoConfiguration | ❌ 未实现 |
| `@EsDocument`/`@EsField` 注解 | ❌ 未实现 |
| BaseEsRepository | ❌ 未实现 |
| IndexManager | ❌ 未实现 |
| BulkOperator | ❌ 未实现 |
| AutoConfiguration.imports | ❌ 未实现 |
| 单元测试 | ❌ 未实现 |

> 落地实现时请对照本规划，并在完成后更新本 CLAUDE.md 第 2-7 节为实际代码状态。
