# CLAUDE.md — spring-cloud-job 定时任务服务

> 本文档面向 AI 编码助手，用于在 `spring-cloud-job/` 目录下工作时提供模块约束、技术栈版本、服务职责与开发规范。
> 工作前**必须**先读取父目录 [`spring-cloud-services/CLAUDE.md`](../CLAUDE.md)、后端聚合 [`spring-cloud-alibaba/CLAUDE.md`](../../CLAUDE.md) 与仓库根 [`sca-fullstack-lab/CLAUDE.md`](../../../CLAUDE.md) 了解全局规范。

---

## 1. 模块定位

XXL-JOB 执行器，所有定时任务集中管理。按个人博客需求文档职责**微调**：承担 ES 全量同步补偿、统计聚合、RSS 定时生成。

| 维度 | 值 |
|------|-----|
| 父 POM | `com.xytang:spring-cloud-alibaba:1.0-SNAPSHOT` |
| 顶级包 | `com.xytang.job` |
| 端口 | HTTP 8091 / Dubbo 20891 / XXL-JOB 10009 |
| 对外 HTTP | 无（任务由 XXL-JOB Admin 调度触发） |

---

## 2. 核心任务清单

| 任务 | 触发时机 | 说明 |
|------|----------|------|
| `articleSyncToEsJob` | 每天 02:00 | ES 全量索引重建（经 Dubbo 调 search `rebuildAllIndex`）★博客 |
| `syncFailedRetryJob` | 每 30 秒 | 扫描 `t_sync_failed_log`（PENDING）重试 ES 同步（最多 3 次）★博客 |
| `metricsAggregateJob` | 每分钟 | 监控指标聚合 |
| `rssGenerateJob` | 每 10 分钟 | RSS 缓存预热（调 search 服务）★博客 |
| `statisticsAggregateJob` | 每天 03:00 | 博客数据统计聚合 ★博客 |
| `workflowRemindJob` | — | **已移除**（workflow 服务已删除） |
| `aiMessageArchiveJob` | — | **已移除**（ai 服务已删除） |
| `logCleanupJob` | 每天凌晨 | 清理 30 天前日志 |
| `createNextMonthTablesJob` | 每月 25 号 | 创建下月分表 |

---

## 3. 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| XXL-JOB Core | 3.5.0（**父 POM 未声明**，落地时补充） | 分布式调度执行器 |
| Spring Boot | 3.5.0（父 POM） | 服务基座 |
| Dubbo | 3.3+（Spring Cloud Alibaba 管理） | 调用其他服务执行任务 |

---

## 4. 服务间通信

| 方向 | 方式 | 说明 |
|------|------|------|
| job → search | Dubbo | ES 全量重建 / RSS 缓存预热 |
| job → article | Dubbo | 拉取已发布文章列表（全量重建数据源）、补偿重试查询 |
| job → monitor | Dubbo | 指标聚合 |

---

## 5. 开发规范（本服务特有）

- 每个任务**必须**有独立 Job Handler，命名 `{Biz}Job`，与 XXL-JOB Admin 注册名一致
- 任务**必须**幂等（重复执行无副作用）
- 任务日志**必须**输出执行结果摘要（成功/失败/耗时）
- 重试任务**必须**遵守 `t_sync_failed_log` 的 `max_retry` 上限，避免死循环

---

## 6. 红线（违反即拒绝）

1. ❌ 子服务 POM 覆盖父 POM 依赖版本
2. ❌ `@Autowired` 字段注入
3. ❌ `throw new RuntimeException` / `catch (Exception e)`
4. ❌ `System.out.println` / `e.printStackTrace()`
5. ❌ 任务不幂等（重复执行产生脏数据）
6. ❌ 重试任务无限重试（必须遵守 max_retry）
7. ❌ 业务配置硬编码（必须放 Nacos / XXL-JOB Admin）
