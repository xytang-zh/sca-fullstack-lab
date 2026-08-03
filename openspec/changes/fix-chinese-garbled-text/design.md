# 设计：中文乱码修复

## Context

动机见 proposal.md。当前状态（调查结论）：

- 仓库内 3 处 JDBC URL 均为 `characterEncoding=utf8`（MySQL 8 中映射 `utf8mb3`，不覆盖 4 字节字符）；但数据源配置按项目约定放 Nacos `spring-cloud-shared.yaml`（仓库外），其实际值待核对。
- 建表 SQL（`sca-system-init.sql`、两处 `db/migration/V1.0.0__*.sql`）无显式 CHARACTER SET，依赖服务器默认；MySQL 8.4.3 容器参数已为 utf8mb4。
- 仓库源文件均为有效 UTF-8，Java 代码无双重编码转换；乱码 `æœ¬æ–‡...` 只可能来自外部通道（mysql CLI / 客户端工具以 latin1 连接）写入。
- 乱码本质：UTF-8 字节序列被按 Latin-1 解释后存入 utf8mb4 列（每个字节 → 一个 latin1 字符），即**双重编码**。

全链路编码流向：

```
[UTF-8 文本] --客户端 charset=latin1 连接--> [MySQL: latin1 字符 = 原始 UTF-8 字节] --列字符集转换--> [utf8mb4 列: 双重编码乱码]
```

修复思路即逆操作：

```
[双重编码乱码] --CAST AS BINARY（latin1 字符还原为原始字节）--> [原始 UTF-8 字节] --CONVERT USING utf8mb4--> [正确中文]
```

## Goals / Non-Goals

**Goals:**

- 消除存量双重编码乱码数据，并确保新增中文数据不再乱码。
- 连接层、DDL 层、数据层三层 utf8mb4 显式化，全部可独立验证。

**Non-Goals:**

- 不改前端展示层、不改 Java 业务代码（已排查无问题）。
- 不自动改写仓库外 Nacos 配置（提供核对清单人工处理）。

## Decisions

### D1. 连接层：JDBC 参数显式 utf8mb4（已执行回退方案）

实测结论（2026-08-03）：**MySQL Connector/J 9.2.0 不接受 `characterEncoding=utf8mb4`**（启动即报 `Unsupported character encoding 'utf8mb4'`，该参数只认 Java 字符集名）。已按预案回退到 `connectionCollation` 方案，最终配置：

```
characterEncoding=UTF-8&connectionCollation=utf8mb4_unicode_ci
```

- `characterEncoding=UTF-8`：Java 标准名，Connector/J 支持，映射服务器 utf8mb4 字符集
- `connectionCollation=utf8mb4_unicode_ci`：显式指定 utf8mb4 collation，连接级 utf8mb4 唯一确定
- 备选 A（`characterEncoding=utf8` + `connectionCollation`）：同样可行，但 `UTF-8` 语义更清晰
- 备选 B：仅依赖服务器端默认 utf8mb4，URL 不加参数 —— 与"显式化"目标冲突，且 Nacos 配置存在覆盖风险
- 验证结果：重启后通过新连接写入中文文章，查库、接口、页面三处一致，`character_set_connection` 经真实读写确认有效

### D2. DDL 层：建表脚本显式声明字符集

所有 CREATE TABLE 增加 `DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci`；`t_article`、`portal_content` 的 MEDIUMTEXT 列所在表一并声明。

- 备选：批量 ALTER TABLE 现有表 —— 仅当核验发现现有表字符集非 utf8mb4 时执行（旧 mysql-data 卷场景），修复脚本中附该 SQL，默认不执行。
- 理由：DDL 与连接层双保险，杜绝依赖默认值；`utf8mb4_unicode_ci` 兼容项目既有 compose 参数。

### D3. 数据层：字节级还原修复脚本

对双重编码行执行：

```sql
UPDATE t_article
SET content_md = CONVERT(CAST(content_md AS BINARY) USING utf8mb4)
WHERE content_md LIKE '%æ%' OR content_md LIKE '%å%' OR content_md LIKE '%ä%';
```

要点：

- **备份先行**：`CREATE TABLE t_article_bak_20260803 AS SELECT * FROM t_article;`（及同库其他中文表）。
- **仅命中乱码行**：用 latin1 字符特征（`æ`/`å`/`ä` 等）过滤，避免触碰正常数据；执行前先 SELECT 统计影响行数。
- **单次往返无损**：字节 → latin1 → utf8mb4 可逆；修复后字符串长度缩短（每汉字 3→1 字符），无溢出风险。
- **执行通道**：必须用 utf8mb4 连接执行（`mysql --default-character-set=utf8mb4`），否则修复语句本身会被再次转码。
- 若某列字符集实为 latin1（反转场景），脚本不适用 —— 校验阶段用 `SELECT HEX(col)` 前置判定，另附反转修复 SQL（`CONVERT(col USING utf8mb4)`）备用。

### D4. 验证清单

输出 `docs/` 或 `scripts/` 下检查清单，包含：

1. `SHOW VARIABLES LIKE 'character_set_%'`（连接、服务器、库、表、列五级）
2. `SHOW CREATE TABLE t_article`（表默认字符集）
3. 乱码行统计 SQL（修复前基数、修复后为 0）
4. Nacos 配置核对项（`spring-cloud-shared.yaml` 的 datasource.url 是否含 utf8mb4）
5. 端到端回归：接口新建中文文章 → 查库 → 页面展示，三者一致

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| 修复 SQL 误伤正常中文行 | 仅对 latin1 特征字符行执行；先 SELECT 统计；先备份表 |
| 字符集判断错误（列实为 latin1） | 修复前 `SELECT HEX(col)` 前置判定，附反转修复 SQL 备用 |
| Connector/J 不支持 `characterEncoding=utf8mb4` | 改后验证 `character_set_connection`，失败回退 `connectionCollation` 方案 |
| Nacos 共享配置覆盖仓库配置 | 以 Nacos 实际值覆盖仓库值后再验证；核对项列入检查清单 |
| 多表多列手工修复遗漏 | 检查清单列出全库需核验的中文表（文章、字典、用户等），按表逐个执行 |

## Migration Plan

1. **核对**：Nacos 共享配置 + 库/表/列实际字符集（检查清单步骤 1-2），确定乱码范围。
2. **备份**：对含乱码的表执行备份 SQL。
3. **修复**：按 D3 脚本逐个表执行，每表执行后 SELECT 抽查。
4. **验证**：乱码行归零 + `SHOW VARIABLES` 确认连接字符集 + 写读一致性回归。
5. **固化**：更新 3 处 application.yml（D1）与建表 SQL（D2），提交 PR。
6. **回滚**：修复 SQL 出错时从备份表恢复（`INSERT ... SELECT * FROM t_article_bak_...`）。

## Open Questions

- 各服务是否共用单一数据源（Nacos 中仅一个 datasource.url 还是按库拆分）——不影响本方案，执行时按实际核对。
- 中文内容表完整清单（除 `t_article`、`portal_content` 外，system 字典等表）——实施时用检查清单逐库确认。
