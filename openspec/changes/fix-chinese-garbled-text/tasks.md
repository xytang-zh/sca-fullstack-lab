# 任务清单：中文乱码修复

## 1. 现状核对

- [x] 1.1 核对 Nacos 共享配置 `spring-cloud-shared.yaml` 的 `datasource.url` 是否含 `characterEncoding=utf8`（验收：记录实际值并与仓库 application.yml 对比）
- [x] 1.2 用 utf8mb4 连接执行 `SHOW VARIABLES LIKE 'character_set_%'` 与 `SHOW CREATE TABLE t_article`，确认连接/库/表/列四级字符集现状（验收：输出结果归档到检查清单）
- [x] 1.3 统计全库乱码行基数：对 `t_article`、`portal_content`、system 字典表等中文表执行 `SELECT COUNT(*) WHERE col LIKE '%æ%' OR col LIKE '%å%' OR col LIKE '%ä%'`（验收：得到每表乱码行数基线）

## 2. 数据备份与修复

- [x] 2.1 对含乱码的表执行备份：`CREATE TABLE <t>_bak_20260803 AS SELECT * FROM <t>;`（验收：备份表行数与源表一致）
- [x] 2.2 编写并评审修复脚本 `scripts/fix-mojibake.sql`：`UPDATE ... SET col = CONVERT(CAST(col AS BINARY) USING utf8mb4) WHERE col LIKE '%æ%' OR ...`，含每表影响行数 SELECT 前置校验（验收：脚本仅命中乱码特征行，评审通过）
- [x] 2.3 用 `mysql --default-character-set=utf8mb4` 逐表执行修复脚本（验收：每表修复前 SELECT 影响行数与 2.1 基线一致）
- [x] 2.4 修复后复跑乱码统计 SQL（验收：乱码行数为 0，抽样 SELECT 内容与源 UTF-8 原文一致）

## 3. 连接层配置固化

- [x] 3.1 修改 `spring-cloud-alibaba/spring-cloud-auth/src/main/resources/application.yml`：`characterEncoding=utf8` → `characterEncoding=utf8mb4`（验收：`grep characterEncoding` 输出 utf8mb4）
- [x] 3.2 修改 `spring-cloud-alibaba/spring-cloud-system/src/main/resources/application.yml`，同上（验收：grep 输出 utf8mb4）
- [x] 3.3 修改 `spring-cloud-alibaba/spring-cloud-article/src/main/resources/application.yml`，同上（验收：grep 输出 utf8mb4）
- [x] 3.4 若 Nacos 共享配置存在同值参数，按核对结果同步为 utf8mb4（验收：Nacos 与仓库配置一致；改动在 Nacos 控制台完成并记录）
- [x] 3.5 重启任一服务后执行 `SHOW VARIABLES LIKE 'character_set_connection'`（验收：返回 `utf8mb4`；若不支持则按 design D1 回退 `connectionCollation` 方案）

## 4. DDL 显式化

- [x] 4.1 修改 `docker/compose/init/sql/sca-system-init.sql`：所有 CREATE TABLE 增加 `DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci`（验收：文件内每张表均含字符集子句）
- [x] 4.2 修改 `db/migration/V1.0.0__*.sql`（system 模块）：同上（验收：grep CHARACTER SET 覆盖全部 CREATE TABLE）
- [x] 4.3 修改 `db/migration/V1.0.0__*.sql`（article/portal 模块）：同上，`t_article.content_md`、`portal_content.body` 所在表显式声明（验收：grep CHARACTER SET 覆盖全部 CREATE TABLE）
- [x] 4.4 若核验发现现有表字符集非 utf8mb4（旧卷场景），执行 ALTER TABLE 转换 SQL（验收：`SHOW CREATE TABLE` 显示 DEFAULT CHARSET=utf8mb4）

## 5. 验证与回归

- [x] 5.1 端到端回归：通过文章创建接口写入含中文标题与正文的数据（验收：查库存储值、接口返回值、admin/portal 页面展示三者与输入原文一致）
- [x] 5.2 新增数据乱码监控：确认新建中文数据不出现 `æ`/`å`/`ä` 乱码模式（验收：新写入记录乱码统计为 0）
- [x] 5.3 将检查清单（连接/库/表/列四级字符集、乱码统计、Nacos 核对项）归档到 `docs/` 供后续巡检（验收：文档存在且含全部验收命令）
