-- ============================================================
-- Nacos 专用库与账号初始化（对齐 NACOS-配置与改造指南.md §4.1.1）
-- 密码占位符 __NACOS_DB_PASSWORD__ 由部署时替换为 .env 中 NACOS_DB_PASSWORD
-- ============================================================

CREATE DATABASE IF NOT EXISTS nacos DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'nacos'@'%' IDENTIFIED BY '__NACOS_DB_PASSWORD__';
GRANT ALL PRIVILEGES ON nacos.* TO 'nacos'@'%';
FLUSH PRIVILEGES;