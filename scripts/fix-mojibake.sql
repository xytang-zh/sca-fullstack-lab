-- =====================================================================
-- 修复中文乱码（UTF-8 → Latin-1 双重编码）
-- 变更：fix-chinese-garbled-text（openspec/changes/fix-chinese-garbled-text）
-- 日期：2026-08-03
--
-- 必须使用 utf8mb4 连接执行，否则修复语句本身会被再次转码：
--   mysql -uroot -p --default-character-set=utf8mb4 sca_system < fix-mojibake.sql
--
-- 原理：双重编码数据 = 原始 UTF-8 字节按 latin1 解码后存入 utf8mb4 列。
--   修复 = CONVERT(col USING latin1)（utf8mb4 字符映射回 latin1 字符，字节=原始 UTF-8）
--        → CAST(... AS BINARY)（标记为字节串，阻止后续字符级转换）
--        → CONVERT(... USING utf8mb4)（字节按 utf8mb4 解码为正确中文）
-- 备份表：t_article_bak_20260803 / sys_dict_bak_20260803 / sys_role_bak_20260803
-- 回滚：INSERT INTO <原表> SELECT * FROM <备份表>;
-- =====================================================================

USE sca_system;

-- ---------------------------------------------------------------------
-- 0. 修复前基线：预期 t_article=3 行、sys_dict=2 行、sys_role=3 行
-- ---------------------------------------------------------------------
SELECT 't_article' AS tbl, COUNT(*) AS mojibake_rows
FROM t_article
WHERE CONCAT_WS(' ', title, summary, content_md) REGEXP '[æåä]';

SELECT 'sys_dict' AS tbl, COUNT(*) AS mojibake_rows
FROM sys_dict
WHERE CONCAT_WS(' ', dict_type, dict_label, dict_value) REGEXP '[æåä]';

SELECT 'sys_role' AS tbl, COUNT(*) AS mojibake_rows
FROM sys_role
WHERE CONCAT_WS(' ', role_code, role_name, remark) REGEXP '[æåä]';

-- ---------------------------------------------------------------------
-- 1. 修复 t_article（仅命中乱码特征行，逐列还原字节序列）
-- ---------------------------------------------------------------------
UPDATE t_article
SET title = CONVERT(CAST(CONVERT(title USING latin1) AS BINARY) USING utf8mb4),
    summary = CONVERT(CAST(CONVERT(summary USING latin1) AS BINARY) USING utf8mb4),
    content_md = CONVERT(CAST(CONVERT(content_md USING latin1) AS BINARY) USING utf8mb4)
WHERE CONCAT_WS(' ', title, summary, content_md) REGEXP '[æåä]';

-- ---------------------------------------------------------------------
-- 2. 修复 sys_dict.dict_label
-- ---------------------------------------------------------------------
UPDATE sys_dict
SET dict_label = CONVERT(CAST(CONVERT(dict_label USING latin1) AS BINARY) USING utf8mb4)
WHERE CONCAT_WS(' ', dict_type, dict_label, dict_value) REGEXP '[æåä]';

-- ---------------------------------------------------------------------
-- 3. 修复 sys_role.role_name
-- ---------------------------------------------------------------------
UPDATE sys_role
SET role_name = CONVERT(CAST(CONVERT(role_name USING latin1) AS BINARY) USING utf8mb4)
WHERE CONCAT_WS(' ', role_code, role_name, remark) REGEXP '[æåä]';

-- ---------------------------------------------------------------------
-- 4. 修复后校验：乱码行数应为 0
-- ---------------------------------------------------------------------
SELECT 't_article' AS tbl, COUNT(*) AS mojibake_rows
FROM t_article
WHERE CONCAT_WS(' ', title, summary, content_md) REGEXP '[æåä]';

SELECT 'sys_dict' AS tbl, COUNT(*) AS mojibake_rows
FROM sys_dict
WHERE CONCAT_WS(' ', dict_type, dict_label, dict_value) REGEXP '[æåä]';

SELECT 'sys_role' AS tbl, COUNT(*) AS mojibake_rows
FROM sys_role
WHERE CONCAT_WS(' ', role_code, role_name, remark) REGEXP '[æåä]';
