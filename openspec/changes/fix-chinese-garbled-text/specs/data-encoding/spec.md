# data-encoding 数据编码规范

## Purpose

定义 MySQL 全链路 UTF-8（utf8mb4）编码契约：数据库连接、建表 DDL、存量乱码数据修复三层的规范与验收标准，保证中文内容从写入、存储到展示全程不乱码。

## ADDED Requirements

### Requirement: 数据库连接必须使用 utf8mb4 字符集

所有微服务与 MySQL 建立 JDBC 连接时，SHALL 显式声明 `characterEncoding=utf8mb4`（MySQL 8 中 `utf8` 仅为 `utf8mb3` 别名，不足以覆盖 4 字节字符），且 MUST NOT 依赖服务器端默认字符集。

#### Scenario: 服务连接 MySQL 使用 utf8mb4

- **WHEN** 任一微服务（auth / system / article 等）通过 JDBC 连接 MySQL
- **THEN** 连接 URL 携带 `characterEncoding=utf8mb4` 参数，且 Nacos 共享配置（`spring-cloud-shared.yaml`）中的 `datasource.url` 与仓库内 application.yml 保持一致

#### Scenario: 连接字符集可被核验

- **WHEN** 使用已配置的应用连接执行 `SHOW VARIABLES LIKE 'character_set_connection'`
- **THEN** 返回值为 `utf8mb4`

### Requirement: 建表 DDL 必须显式声明 utf8mb4

所有新建表的 CREATE TABLE 语句 SHALL 显式携带 `DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci`（或兼容 COLLATE），MUST NOT 隐式依赖服务器全局默认值；存储中文的文本列（VARCHAR / TEXT / MEDIUMTEXT）必须位于 utf8mb4 表内。

#### Scenario: 初始化建表脚本显式声明字符集

- **WHEN** 执行 `docker/compose/init/sql/` 或 `db/migration/` 下的建表脚本
- **THEN** 每张表均显式声明 `CHARACTER SET utf8mb4`，`SHOW CREATE TABLE` 中 `DEFAULT CHARSET=utf8mb4`

### Requirement: 存量乱码数据可修复并验证

系统 SHALL 提供针对双重编码（UTF-8 字节被按 Latin-1 存储）乱码数据的修复 SQL 脚本，修复 MUST 先备份源表，且修复结果可独立校验。

#### Scenario: 修复乱码文章内容

- **WHEN** 对 `t_article.content_md` 中形如 `æœ¬æ–‡...` 的乱码数据执行修复脚本
- **THEN** 修复后内容恢复为正确中文（"本文介绍..."），且修复前已生成备份表或备份文件

#### Scenario: 修复前后校验可对比

- **WHEN** 执行校验 SQL 统计乱码行数与修复后正确行数
- **THEN** 乱码行数为 0，修复后数据与源 UTF-8 原文一致（`CONVERT(CAST(col AS BINARY) USING utf8mb4)` 往返无损）

### Requirement: 中文内容写读一致性

应用通过正式接口写入的中文内容，从数据库读取并经前端页面展示后，MUST 与写入原文完全一致，不出现 mojibake 或替换字符。

#### Scenario: 新建文章中文内容完整往返

- **WHEN** 通过文章创建接口写入含中文标题与正文的 `t_article` 记录
- **THEN** 数据库存储值、接口返回值与前端展示值均与输入原文一致，无乱码

#### Scenario: 乱码不再新增

- **WHEN** 修复完成后继续通过正式接口写入新的中文数据
- **THEN** 新写入数据始终为正确 UTF-8，不再产生 `æœ¬` 类乱码模式
