# 03 · GitHub 热门参考项目调研

> 本文汇总与你列出的技术栈相关的、在 GitHub 上 stars 较多且具有代表性的开源项目，
> 分析它们的 **功能模块**、**架构亮点**、**可借鉴之处**，作为我们项目需求设计的"事实依据"。
>
> ⚠️ 说明：受当前网络环境限制，本节信息基于 context7 MCP 官方文档与对开源社区的长期认知整理；
> 部分项目的最新 stars 数可能滞后，建议你访问原仓库二次核实。

---

## 3.1 调研对象清单

| # | 项目 | 主仓库 | 主要借鉴点 |
|---|------|--------|------------|
| 1 | **Pig** | `pig-mesh/pig` | Spring Cloud Alibaba 微服务脚手架、OAuth2、代码生成 |
| 2 | **RuoYi-Cloud** | `yangmingji/ruoyi-cloud`（Gitee 主仓） | 系统管理、监控、代码生成、定时任务 |
| 3 | **mall**（mall-cloud） | `macrozheng/mall` | 电商业务、RabbitMQ、ElasticSearch 综合应用 |
| 4 | **Sa-Token** | `dromara/Sa-Token` | SSO 三模式、踢人下线、微服务网关鉴权 |
| 5 | **Warm-Flow** | `dromara/warm-flow` | 7 表工作流模型、流程设计器 |
| 6 | **Spring AI** | `spring-projects/spring-ai` | ChatClient、Advisor、VectorStore RAG |
| 7 | **spring-ai-alibaba** | `alibaba/spring-ai-alibaba` | 通义千问、DashScope 集成 |
| 8 | **dynamic-datasource** | `baomidou/dynamic-datasource` | 多数据源切换 |
| 9 | **MyBatis-Plus** | `baomidou/mybatis-plus` | ORM、分页、多租户、代码生成 |
| 10 | **Redisson** | `redisson/redisson` | 分布式锁、限流、延迟队列 |
| 11 | **vben-admin** | `vbenjs/vue-vben-admin` | Vue3 + TS + Vite 后台模板 |
| 12 | **velocity-admin** / **pure-admin** | `pure-admin/vue-pure-admin` | Vue3 + TS + UnoCSS 后台模板 |
| 13 | **Naive UI Admin** | `jinenng/nb-admin`（及社区） | Naive UI + Vite 后台 |
| 14 | **Tduck-Form** / **AJ-Report** | 社区项目 | 表单设计、低代码报表（可作为参考） |
| 15 | **SpringBlade** | `chillzhuang/SpringBlade` | 微服务脚手架、Sword 设计 |
| 16 | **EFBladeX** / **Pig Cloud** | 同上 | 多租户 SaaS 架构 |

---

## 3.2 详细调研

### 3.2.1 Pig（`pig-mesh/pig`）

- **定位**：Spring Cloud 微服务脚手架，国内使用最广的微服务模板之一
- **技术栈**：Spring Boot + Spring Cloud + Spring Cloud Alibaba + OAuth2 + MyBatis-Plus + Vue
- **架构亮点**：
  - 基于 `spring-authorization-server` 实现 OAuth2
  - 基于 `spring-cloud-gateway` 做统一鉴权
  - `pig-auth` 认证服务 + `pig-upms` 用户权限服务 + `pig-common` 公共模块
- **借鉴点**：
  - **模块分层**：common / auth / business / gateway
  - **代码生成器**：基于 MyBatis-Plus Generator 二次封装
  - **数据权限**：基于 SQL 拦截器实现行级数据权限
- **本项目借鉴方式**：
  - 模块结构参考 Pig 的 `common` 拆分
  - 但认证改用 **Sa-Token**（更轻、SSO 文档更全）

### 3.2.2 RuoYi-Cloud

- **定位**：老牌后台管理系统，Cloud 版做微服务化
- **技术栈**：Spring Cloud + Spring Cloud Alibaba + MyBatis + Vue
- **架构亮点**：
  - **系统管理模块**：用户、角色、菜单、岗位、部门、字典、参数、通知
  - **系统监控模块**：在线用户、定时任务、数据监控、服务监控
  - **代码生成模块**：可视化生成 CRUD
  - **RuoYi-Cloud-Plus**：社区魔改版，升级到 Spring Boot 3 + MyBatis-Plus + Sa-Token + Redisson
- **借鉴点**：
  - **系统管理表结构设计**（sys_user、sys_role、sys_menu、sys_dict 等）
  - **定时任务调度**（基于 Quartz 或 xxl-job）
  - **服务器监控**：基于 OSHI 库采集 CPU/内存/磁盘
- **本项目借鉴方式**：
  - sys_* 表结构直接参考
  - monitor-service 借鉴 RuoYi 的 OSHI 采集 + 推送方案，但存储换为 TDengine

### 3.2.3 Sa-Token（`dromara/Sa-Token`）

- **官方文档**：[sa-token.cc](https://sa-token.cc)
- **关键能力**：
  - 登录认证、权限/角色认证
  - **踢人下线**（基于设备ID）
  - **单点登录（SSO）三模式**
  - **OAuth2.0**（实现授权码、密码、客户端凭证等模式）
  - **微服务网关鉴权**（Gateway 统一拦截 + 子服务 Context 透传）
- **借鉴点**：
  - SSO 模式二（Server 授权 + Client 校验）是本项目要实现的
  - 踢人下线 = 同设备重复登录踢前者；密码修改/封禁 → 全端下线
- **本项目借鉴方式**：
  - auth-center 实现 SSO Server
  - 各业务微服务作为 SSO Client
  - Gateway 配置 `SaTokenContext` 透传登录态

### 3.2.4 Warm-Flow（`dromara/warm-flow`）

- **官方文档**：[warm-flow.cn](https://warm-flow.cn)
- **关键能力**：
  - 7 张表核心模型
  - 支持 SpringBoot 3 + MyBatis-Plus starter
  - 流程设计器（前端组件）
  - 多租户、软删除
  - 节点类型：Start、Middle、End、Exclusive、Parallel
  - 任务操作：审批、驳回、转办、委托、加签、减签、会签
- **借鉴点**：
  - 请假 / 报销 / 合同审批 / 采购申请 等典型流程
  - 流程实例与业务表通过 `business_id` 关联
- **本项目借鉴方式**：
  - workflow-service 集成 Warm-Flow，实现 4 个典型流程

### 3.2.5 Spring AI + spring-ai-alibaba

- **官方文档**：[docs.spring.io/spring-ai](https://docs.spring.io/spring-ai/reference/)
- **关键能力**：
  - `ChatClient`：流式 / 同步调用大模型
  - `Advisor`：RAG 切入点（QuestionAnswerAdvisor、VectorStoreChatMemoryAdvisor）
  - `VectorStore`：抽象多种向量库（pgvector、milvus、redis、chroma、pinecone）
  - `EmbeddingModel`：支持 OpenAI、Ollama、通义、智谱等
  - `spring-ai-alibaba`：阿里扩展，集成通义千问、DashScope
- **借鉴点**：
  - 知识库管理（文档上传 → 分块 → 向量化 → 检索 → 生成）
  - 对话历史存储（MongoDB）
  - 流式响应（SSE）
- **本项目借鉴方式**：
  - ai-assistant-service 实现 RAG 问答
  - 文档存储：MongoDB（原文件） + pgvector（向量）
  - 前端用 SSE 接收流式回答

### 3.2.6 dynamic-datasource + MyBatis-Plus（`baomidou`）

- **关键能力**：
  - `@DS` 注解切换数据源
  - `@DSTransactional` 跨库事务
  - 支持 Druid、HikariCP 连接池
  - 支持多租户、读写分离、分库分表（配合 sharding-jdbc）
- **借鉴点**：
  - 多数据源场景：MySQL（业务） + PG（向量） + 国产库（信创）
- **本项目借鉴方式**：
  - common-datasource 模块封装 dynamic-datasource
  - 配置 4 类数据源：mysql / pg / kingbase / dm

### 3.2.7 Redisson

- **关键能力**：
  - 分布式锁（可重入、公平、读写、RedLock）
  - 限流器（令牌桶、漏桶）
  - 延迟队列
  - 分布式集合（Map、Set、Queue）
- **借鉴点**：
  - 秒杀场景：Redisson + Redis + RabbitMQ
  - 订单延迟关闭：RDelayedQueue
  - 接口限流：RRateLimiter 注解化
- **本项目借鉴方式**：
  - common-lock 模块封装 `@DistributedLock` 注解
  - common-ratelimiter 模块封装 `@RateLimit` 注解

### 3.2.8 Vue3 前端模板

#### vben-admin（`vbenjs/vue-vben-admin`）

- **定位**：Vue3 + TS + Vite 后台管理系统，Stars 20k+
- **技术栈**：Vue3 + TS + Vite + Ant Design Vue + Tailwind + Pince（Pinia）
- **架构亮点**：
  - Monorepo（pnpm workspace）
  - 应用 / 包 / 演示分离
  - 主题色、暗黑模式、国际化
- **借鉴点**：目录结构、权限路由、菜单动态生成

#### pure-admin / vue-pure-admin

- **定位**：Vue3 + TS + Vite + Element Plus + UnoCSS
- **亮点**：UnoCSS 实战示例、暗黑模式、国际化、多标签页

#### Naive Admin / nb-admin

- **定位**：Vue3 + Naive UI + Vite + TS
- **亮点**：Naive UI 实战、组件封装

**本项目借鉴方式**：
- 采用 vben-admin 的 Monorepo 结构（pnpm workspace）
- UI 用 Naive UI（与用户提到的 "Native UI" 笔误修正）
- 样式用 UnoCSS（学习 pure-admin 的写法）
- 状态用 Pinia，路由用 Vue Router 4

### 3.2.9 其他参考

- **mall**（`macrozheng/mall`）：RabbitMQ、ES、MongoDB 综合电商实战
- **SpringBlade**（`chillzhuang/SpringBlade`）：多租户 SaaS 架构
- **Tduck-Form**：表单设计器（可作为 workflow-service 的表单引擎参考）
- **AJ-Report**：低代码报表（可作为监控大盘参考）

---

## 3.3 调研结论：可复用功能模块清单

| 模块 | 来源参考 | 是否纳入本项目 |
|------|----------|----------------|
| 用户、角色、菜单、字典、参数 | RuoYi / Pig | ✅ |
| 部门、岗位 | RuoYi | ✅ |
| 在线用户、踢人下线 | Sa-Token | ✅ |
| 操作日志、登录日志 | RuoYi / Pig | ✅ |
| 定时任务（Quartz / xxl-job） | RuoYi | ✅（用 Quartz 简单版） |
| 服务器监控（CPU/内存/JVM） | RuoYi / OSHI | ✅（存储改 TDengine） |
| 工作流引擎 | Warm-Flow | ✅ |
| AI 知识库问答 | Spring AI + RAG | ✅ |
| 文件管理（MinIO） | mall / RuoYi | ✅ |
| 站内信、消息推送 | Sa-Token 示例 + Netty | ✅ |
| 全文检索 | mall（ES） | ✅ |
| 公开门户（SEO） | Hexo / Vitepress 思路 | ✅（用 Vite SSG） |
| 代码生成器 | MyBatis-Plus Generator | ✅（二次封装） |
| 多租户 | SpringBlade / Warm-Flow 自带 | ⚪ 预留接口，不深入 |
| 国际化 i18n | pure-admin | ✅（中英文） |
| 暗黑模式 | pure-admin / vben | ✅ |
| 数据权限（行级） | Pig / MyBatis-Plus | ✅（基于 SQL 拦截） |

---

## 3.4 项目核心差异点（与参考项目的区别）

| 维度 | 参考项目 | 本项目差异 |
|------|----------|------------|
| 认证 | Pig 用 OAuth2 | 改用 Sa-Token，文档对新手更友好 |
| 工作流 | RuoYi 集成 Activiti | 改用 Warm-Flow，国产、轻量、表少 |
| AI | 多数项目无 | 引入 Spring AI + RAG + pgvector |
| 监控存储 | RuoYi 用内存 + 直接查询 | 改用 TDengine 时序库 + WebSocket 推送 |
| 多数据库 | 多数只 MySQL | 引入 MySQL + PG + Mongo + ES + TDengine + 国产库 |
| 国产化 | 几乎没有 | 显式适配人大金仓、达梦 |
| 前端 UI | Element Plus / Ant Design Vue | 用 Naive UI + UnoCSS |

---

下一步：[04 · 服务架构设计](./04-服务架构设计.md)
