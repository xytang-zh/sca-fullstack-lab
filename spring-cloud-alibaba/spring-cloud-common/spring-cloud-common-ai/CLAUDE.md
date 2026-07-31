# CLAUDE.md — spring-cloud-common-ai Spring AI 集成

> 本文档面向 AI 编码助手，用于在 `spring-cloud-common-ai/` 目录下工作时提供模块约束、功能计划与开发规范。
> 工作前**必须**先读取父目录的 [`spring-cloud-common/CLAUDE.md`](../CLAUDE.md) 与 [`spring-cloud-alibaba/CLAUDE.md`](../../CLAUDE.md) 了解全局规范。

---

## 1. 模块定位

`spring-cloud-common-ai` 基于 Spring AI 提供 ChatClient 配置、Advisor 链、VectorStore 抽象，被 `spring-cloud-ai` 业务服务依赖，是**AI 能力的基础设施层**。

**核心设计原则**：
1. **API Key 不硬编码**：所有密钥从环境变量/Nacos 注入
2. **Token 用量必记**：每次调用记录 prompt/completion/total tokens，便于成本分析
3. **prompt 防注入**：用户输入必须经过模板包装，**禁止**原样拼接到 system prompt

| 维度 | 值 |
|------|-----|
| 顶级包 | `com.xytang.common.ai` |
| 父 POM | `com.xytang:spring-cloud-alibaba:1.0-SNAPSHOT` |
| 当前 artifactId | `spring-cloud-common-ai` |
| packaging | `jar` |
| 是否有代码 | ❌ 空壳（仅 pom.xml，**本 CLAUDE.md 为规划书**） |

---

## 2. 技术栈选型

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring AI | 1.1.0 | ChatClient/Advisor/VectorStore 抽象 |
| Spring AI OpenAI | 1.1.0 | OpenAI 兼容协议（含国产大模型兼容） |
| Spring AI pgvector | 1.1.0 | 向量存储 |
| PostgreSQL + pgvector | 16 + 0.8 | 向量数据库 |
| Lombok | 父 POM 全局声明 | 注解简化 |

> ⚠️ 父 POM 当前**未声明** Spring AI 版本，落地时需要在 `spring-cloud-alibaba/pom.xml` 的 `<properties>` 中加入 `<spring-ai.version>1.1.0</spring-ai.version>` 并在 `<dependencyManagement>` 中引入 `spring-ai-bom`。

---

## 3. 功能计划

### 3.1 ChatClientConfig 默认 ChatClient

- **职责**：注册 `ChatClient.Builder`，设置默认 system prompt
- **默认模型**：从 Nacos 配置 `xytang.ai.default-model` 读取
- **实现技术**：`@Configuration` + `@Bean` + `ChatClient.builder()`

### 3.2 VectorStoreConfig pgvector 配置

- **职责**：注册 `VectorStore` Bean，用 pgvector
- **向量维度**：1536（OpenAI text-embedding-3-small），**禁止**修改（要重建索引）
- **索引类型**：HNSW（高斯近邻，性能与精度平衡）
- **表名**：`ai_vector_store`
- **实现技术**：`PgVectorStore` + `JdbcTemplate`

### 3.3 MemoryConfig 会话记忆

- **职责**：注册 `ChatMemoryAdvisor`，多轮对话上下文
- **存储**：MongoDB（持久化）+ Redis（短期缓存）
- **实现技术**：实现 `ChatMemoryAdvisor`

### 3.4 QuestionAnswerAdvisor RAG 增强

- **职责**：检索向量库 → 拼 prompt → 调 LLM
- **流程**：用户提问 → embedding → 检索 topK → 拼接 context → LLM 回答
- **实现技术**：继承 Spring AI `QuestionAnswerAdvisor`

### 3.5 LoggingAdvisor Token 用量记录

- **职责**：拦截 LLM 调用，记录 prompt_tokens/completion_tokens/total_tokens 到 MongoDB
- **实现技术**：实现 `Advisor` 接口

### 3.6 EmbeddingService 向量化

- **职责**：调用 embedding 模型，把文本转向量
- **批量优化**：批量 embedding 减少调用次数
- **实现技术**：`EmbeddingClient`

### 3.7 RagService 检索 + 拼 prompt

- **职责**：RAG 全流程：检索 → 拼 prompt → 调 LLM → 返回
- **实现技术**：组合 `VectorStore` + `ChatClient`

### 3.8 ChatMessage 消息模型

- **字段**：`role`、`content`、`tokens`、`metadata`、`timestamp`
- **实现技术**：POJO + Lombok

### 3.9 SSE 流式响应

- **协议**：`text/event-stream`，每条 `data: {chunk}`，结束 `data: [DONE]`
- **背压**：用 `Flux<ServerSentEvent>` 背压
- **实现技术**：Spring AI `ChatClient.stream()` + `Flux`

---

## 4. POM 依赖模板（计划）

```xml
<dependencies>
    <!-- 内部依赖 -->
    <dependency>
        <groupId>com.xytang</groupId>
        <artifactId>spring-cloud-common-core</artifactId>
    </dependency>

    <!-- Spring AI BOM（在父 POM 声明） -->
    <!--
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-bom</artifactId>
        <type>pom</type>
        <scope>import</scope>
    </dependency>
    -->

    <!-- Spring AI OpenAI -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
    </dependency>

    <!-- Spring AI pgvector -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-pgvector-store-spring-boot-starter</artifactId>
    </dependency>
</dependencies>
```

---

## 5. 配置项（计划）

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `spring.ai.openai.api-key` | （Nacos 注入） | OpenAI 兼容 API Key |
| `spring.ai.openai.base-url` | （Nacos 注入） | API Base URL（支持国产大模型） |
| `spring.ai.openai.chat.options.model` | `gpt-4o-mini` | 默认对话模型 |
| `spring.ai.openai.chat.options.temperature` | `0.7` | 温度 |
| `spring.ai.openai.embedding.options.model` | `text-embedding-3-small` | 默认 embedding 模型 |
| `spring.ai.vectorstore.pgvector.dimensions` | `1536` | 向量维度（**禁止**修改） |
| `spring.ai.vectorstore.pgvector.distance-type` | `COSINE_DISTANCE` | 距离算法 |
| `xytang.ai.rag.top-k` | `5` | RAG 检索 topK |
| `xytang.ai.token-recording.enabled` | `true` | 是否记录 token 用量 |

> 子模块内**无** `application.yml`，所有配置在 Nacos 中。

---

## 6. 与其他模块的关系

| 关系 | 模块 |
|------|------|
| 依赖 | `spring-cloud-common-core` |
| 被依赖 | `spring-cloud-ai` 业务服务 |
| 集成 | `spring-cloud-common-mongo`（对话历史）、`spring-cloud-common-cache`（模型配置缓存） |

---

## 7. 红线

1. ❌ 硬编码 API Key（必须从环境变量/Nacos 注入）
2. ❌ 用户输入原样拼到 system prompt（prompt 注入风险，必须用模板包装）
3. ❌ 修改 pgvector 维度（必须重建索引，影响所有历史数据）
4. ❌ 不记录 token 用量（无法成本分析）
5. ❌ 不限制 token 上限（导致滥用）
6. ❌ SSE 流式响应不背压（导致客户端 OOM）
7. ❌ AutoConfiguration Bean 不加 `@ConditionalOnMissingBean`

---

## 8. 实施状态

⚠️ 本模块当前为**空壳**，仅 `pom.xml`，**无任何 .java 代码**。

| 任务 | 状态 |
|------|------|
| pom.xml | ✅ 存在 |
| 父 POM 引入 Spring AI BOM | ❌ 未实现（需要在 `spring-cloud-alibaba/pom.xml` 加 `spring-ai.version` 和 BOM） |
| ChatClientConfig | ❌ 未实现 |
| VectorStoreConfig | ❌ 未实现 |
| MemoryConfig | ❌ 未实现 |
| QuestionAnswerAdvisor | ❌ 未实现 |
| LoggingAdvisor | ❌ 未实现 |
| EmbeddingService | ❌ 未实现 |
| RagService | ❌ 未实现 |
| ChatMessage 模型 | ❌ 未实现 |
| SSE 流式响应 | ❌ 未实现 |
| AutoConfiguration.imports | ❌ 未实现 |
| 单元测试 | ❌ 未实现 |

> 落地实现时请对照本规划，并在完成后更新本 CLAUDE.md 第 2-7 节为实际代码状态。
