# 企业级一体化智能管理平台 · 项目总览

> 一份基于主流开源技术栈从 0 到 1 落地的学习型项目，按"学习路径"由浅入深组织。
> 本文档面向 **软件开发新手**，但采用 **高级架构师** 的视角来设计，便于你在学习过程中建立完整的工程思维。

---

## 一、项目定位

| 维度 | 说明 |
|------|------|
| **项目类型** | 前后端分离 + 微服务架构 + AI 增强的企业级一体化管理平台 |
| **核心目标** | 通过"一体化管理平台"作为统一入口，实现 **单点登录（SSO）** + **多业务系统跳转免登** + **会话统一治理** |
| **学习目标** | 把 SpringCloud Alibaba / Spring AI / Warm-Flow / 多数据库 / Netty 等主流技术栈串成一个真实可用的项目，覆盖架构师必备技能 |
| **业务目标** | 系统管理、服务器监控、工作流审批、AI 知识问答、消息推送、全文检索等典型企业能力 |

---

## 二、核心用户故事

1. **管理员** 在浏览器打开 `https://portal.example.com`，跳转到统一登录中心，输入账号密码登录。
2. 登录成功后回到"一体化管理平台"首页，看到服务器监控大盘、待办审批、AI 助手入口、各业务系统跳转卡片。
3. 点击「工作流系统」卡片 → **不需要再次登录** → 直接进入工作流审批页面。
4. 当管理员在另一个浏览器再次登录同一账号 → 系统检测到「同设备类型重复登录」→ **把前一浏览器踢下线**，前端弹窗"登录已失效，请重新登录"并自动跳回一体化登录中心。
5. 工作流系统 Token 过期 → 前端拦截 401 → 自动跳回一体化登录中心做续期或重新登录。

---

## 三、技术栈总览（详细选型见 `docs/02-技术栈选型.md`）

| 层次 | 主要技术 |
|------|----------|
| **后端框架** | SpringBoot 3.5 / SpringCloud 2025.0 / SpringCloud Alibaba 2025.0 |
| **微服务治理** | Nacos（注册/配置）、Sentinel（限流熔断）、Seata（分布式事务）、Dubbo 3（RPC） |
| **认证授权** | Sa-Token 1.44（SSO 单点登录 / 踢人下线 / OAuth2） |
| **AI 能力** | Spring AI 1.1（ChatClient / Advisor / VectorStore） + RAG + pgvector |
| **工作流** | Warm-Flow 1.8.8（dromara 出品，7 张表核心模型） |
| **ORM** | MyBatis-Plus 3.5（含代码生成、分页、多租户） |
| **多数据源** | dynamic-datasource-spring-boot-starter（baomidou） |
| **分库分表** | Apache ShardingSphere 5.5（按月分表 + 加密） |
| **任务调度** | XXL-JOB 3.5.0（独立 Admin + 微服务执行器） |
| **数据库** | MySQL 8（业务主库） + PostgreSQL 16（pgvector 向量库） + MongoDB 7（日志/对话） + TDengine 3（时序监控） + ElasticSearch 8（全文检索） + 人大金仓 KingbaseES V8 + 达梦 DM8（国产化适配） |
| **缓存** | Redis 7 + Redisson 4（分布式锁） + Caffeine（本地缓存，多级缓存） |
| **消息** | RabbitMQ 3.13（事件总线 / 异步任务） |
| **实时通信** | Netty 4 + WebSocket（监控推送 / 站内信 / 在线客服） |
| **API 文档** | springdoc-openapi（OpenAPI 3 规范） |
| **低代码报表** | JimuReport 2.3.4（在线报表设计器 + 大屏 + 导出） |
| **监控告警** | Prometheus 2.55 + Grafana 11（系统指标） + TDengine（业务监控） |
| **开发方法论** | GitHub Spec-Kit（规格驱动开发 SDD：spec→plan→tasks→implement） |
| **前端** | Vue 3.5 + TypeScript 5.5 + Vite 5 + Naive UI + UnoCSS + Pinia + Vue Router |
| **运维** | Docker / Docker Compose / Nginx / GitHub Actions CI/CD |

---

## 四、文档导航（按学习顺序阅读）

| # | 文档 | 内容摘要 |
|---|------|----------|
| 01 | [项目概述](./docs/01-项目概述.md) | 项目背景、目标、整体架构图、模块关系 |
| 02 | [技术栈选型](./docs/02-技术栈选型.md) | 每项技术的版本号、用途、为什么选、替换方案 |
| 03 | [GitHub 调研](./docs/03-GitHub调研.md) | 参考的热门开源项目清单与功能借鉴点 |
| 04 | [服务架构设计](./docs/04-服务架构设计.md) | 13 个微服务拆分、各服务职责、模块功能清单 |
| 05 | [单点登录与会话管理](./docs/05-单点登录与会话管理.md) | SSO 三种模式、踢人下线、Token 续期、跨域跳转 |
| 06 | [多数据库与多数据源](./docs/06-多数据库与多数据源.md) | 多数据库场景、动态切换、国产化适配、ShardingSphere 分表 |
| 07 | [部署与 DevOps](./docs/07-部署与DevOps.md) | Docker、Nginx、CI/CD、Prometheus、Grafana、XXL-JOB-Admin、JimuReport 容器 |
| 08 | [学习路径与实施步骤](./docs/08-学习路径与实施步骤.md) | 新手分阶段学习路线 + 13 周实施计划（含第 0 周 Spec-Kit SDD） |
| 09 | [项目需求文档](./docs/09-项目需求文档.md) | **13 个服务逐个详述**：需求/模块/功能/技术栈/接口/数据模型 |
| 10 | [IDEA 与工程结构](./docs/10-IDEA与工程结构.md) | **目录树 + IDEA 一次打开同时运行前后端的配置** |

---

## 五、一句话总结

> 这个项目的本质是 **"用一整套主流技术栈串起一个真实业务系统"**。
> 不追求生产级稳定性，但追求 **覆盖度** 与 **真实场景**——让你学完后能在简历上自信写："独立完成基于 SpringCloud Alibaba + Spring AI + Warm-Flow 的微服务架构设计与落地"。

下一步：打开 [`docs/01-项目概述.md`](./docs/01-项目概述.md) 开始吧。
