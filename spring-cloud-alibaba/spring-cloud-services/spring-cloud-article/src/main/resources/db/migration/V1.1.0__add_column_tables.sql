-- ============================================================
-- spring-cloud-article 用户中心增量脚本
-- 手动执行（与 V1.0.0 相同，未启用 Flyway 自动迁移）
-- 注意：所有 SQL 必须可跨 MySQL/KingbaseES/DM8 运行
-- ============================================================

-- ===== 1. t_column 专栏表（用户自定义博客分类）=====
CREATE TABLE IF NOT EXISTS t_column (
    id          BIGINT       NOT NULL                          COMMENT '主键 ID（雪花 ID）',
    user_id     BIGINT       NOT NULL                          COMMENT '作者 ID（sys_user.id）',
    name        VARCHAR(64)  NOT NULL                          COMMENT '专栏名称',
    description VARCHAR(512) NULL                              COMMENT '专栏简介',
    cover_image VARCHAR(255) NULL                              COMMENT '封面图 URL',
    status      SMALLINT     NOT NULL DEFAULT 1                COMMENT '状态：1=正常 0=隐藏',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    version     INT          NOT NULL DEFAULT 0                COMMENT '乐观锁版本号',
    deleted     SMALLINT     NOT NULL DEFAULT 0                COMMENT '逻辑删除：0=未删 1=已删',
    PRIMARY KEY (id)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE INDEX idx_column_user ON t_column (user_id);

-- ===== 2. t_column_subscribe 专栏订阅表（user_id + column_id 唯一，保证幂等）=====
CREATE TABLE IF NOT EXISTS t_column_subscribe (
    id          BIGINT       NOT NULL                          COMMENT '主键 ID（雪花 ID）',
    user_id     BIGINT       NOT NULL                          COMMENT '订阅用户 ID',
    column_id   BIGINT       NOT NULL                          COMMENT '订阅专栏 ID',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '订阅时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sub_user_column (user_id, column_id)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE INDEX idx_sub_column ON t_column_subscribe (column_id);

-- ===== 3. t_article 增加专栏关联列 =====
ALTER TABLE t_article ADD COLUMN column_id BIGINT NULL COMMENT '所属专栏 ID（可空）';
CREATE INDEX idx_article_column ON t_article (column_id);