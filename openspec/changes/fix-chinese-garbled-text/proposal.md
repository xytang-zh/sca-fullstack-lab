# 修复中文乱码（UTF-8 → Latin-1 双重编码）

## Why

数据库和页面展示的中文内容出现乱码，如 `æœ¬æ–‡ä»‹ç» Spring Cloud Alibaba å¾®æœåŠ¡æž¶æž„ã€‚`（"本文介绍 Spring Cloud Alibaba 微服务架构。"）。这是典型的 UTF-8 字节被按 Latin-1/ISO-8859-1 解码存储产生的 mojibake（双重编码），导致所有依赖中文内容的页面、检索、展示功能受损。

## What Changes

- **数据库连接编码显式化**：将所有 JDBC URL 的 `characterEncoding=utf8` 升级为 `utf8mb4`（MySQL 8 中 `utf8` 仅是 `utf8mb3` 别名，不覆盖 4 字节字符），并核对 Nacos `spring-cloud-shared.yaml` 中的共享数据源配置（该文件不在仓库内，需人工核对）。
- **建表 SQL 显式声明字符集**：为 `docker/compose/init/sql/` 与 `db/migration/` 下所有 CREATE TABLE 补 `DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci`，不再依赖服务器默认值；文章/正文列（`t_article.content_md`、`portal_content.body` 等 MEDIUMTEXT）必须显式声明。
- **存量乱码数据修复**：提供修复 SQL 脚本（先备份，再对双重编码数据执行字节级还原，如 `CONVERT(CAST(col AS BINARY) USING utf8mb4)`），并配套校验 SQL 用于修复前后对比。
- **提供诊断检查清单**：输出一组用于核验连接字符集、库/表/列字符集的检查 SQL 与操作说明（含 Nacos 配置核对项）。

## Capabilities

### New Capabilities

- `data-encoding`: 全链路（MySQL 连接 → 建表 DDL → 存量数据）UTF-8 编码规范与乱码修复能力，保证中文内容写入、存储、展示正确。

### Modified Capabilities

（无现有 spec 被修改）

## Non-goals

- 不改造前端展示层（已确认 `<meta charset="UTF-8">` 与 HTTP 编码正确，乱码源于数据本身）。
- 不修改 Java 业务代码（已排查无双重编码转换逻辑）。
- 不处理 Nacos 中仓库外文件的自动改定（需人工核对后同步）。

## Impact

- **Affected modules（后端）**：
  - `spring-cloud-alibaba/spring-cloud-auth/src/main/resources/application.yml`
  - `spring-cloud-alibaba/spring-cloud-system/src/main/resources/application.yml`
  - `spring-cloud-alibaba/spring-cloud-article/src/main/resources/application.yml`
  - `spring-cloud-alibaba/spring-cloud-common/spring-cloud-common-database/`（若存在共享数据源模块）
  - Nacos 共享配置 `spring-cloud-shared.yaml`（仓库外，需核对）
- **Affected modules（基础设施）**：
  - `docker/compose/docker-compose.infra.yml`（MySQL 容器参数，已正确，需确认）
  - `docker/compose/init/sql/sca-system-init.sql`
  - `db/migration/V1.0.0__*.sql`（system / article 等模块）
- **数据**：存量乱码数据修复为可逆操作（先备份），涉及文章、系统字典等中文内容表。
