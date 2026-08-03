-- ============================================================
-- spring-cloud-article 博客内容域初始化脚本
-- 手动执行（与 system 服务 V1.0.0 相同，未启用 Flyway 自动迁移）
-- 注意：所有 SQL 必须可跨 MySQL/KingbaseES/DM8 运行
-- ============================================================

-- ===== 1. t_article 文章表 =====
CREATE TABLE IF NOT EXISTS t_article (
    id           BIGINT          NOT NULL                          COMMENT '主键 ID（雪花 ID）',
    title        VARCHAR(128)    NOT NULL                          COMMENT '标题',
    summary      VARCHAR(512)    NULL                              COMMENT '摘要',
    content_md   MEDIUMTEXT      NOT NULL                          COMMENT 'Markdown 正文',
    status       SMALLINT        NOT NULL DEFAULT 1                COMMENT '1=草稿 2=待审核 3=已发布 4=已驳回',
    author_id    BIGINT          NOT NULL                          COMMENT '作者 ID（sys_user.id）',
    slug         VARCHAR(255)    NULL                              COMMENT 'URL 友好标识',
    cover_image  VARCHAR(255)    NULL                              COMMENT '封面图 URL',
    views        BIGINT          NOT NULL DEFAULT 0                COMMENT '阅读量',
    likes        BIGINT          NOT NULL DEFAULT 0                COMMENT '点赞数',
    favorites    BIGINT          NOT NULL DEFAULT 0                COMMENT '收藏数',
    comments     BIGINT          NOT NULL DEFAULT 0                COMMENT '评论数',
    publish_time DATETIME        NULL                              COMMENT '发布时间',
    create_time  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    version      INT             NOT NULL DEFAULT 0                COMMENT '乐观锁版本号',
    deleted      SMALLINT        NOT NULL DEFAULT 0                COMMENT '逻辑删除：0=未删 1=已删',
    PRIMARY KEY (id),
    UNIQUE KEY uk_article_slug (slug)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE INDEX idx_article_status_publish ON t_article (status, publish_time);
CREATE INDEX idx_article_author ON t_article (author_id);

-- ===== 2. t_like_record 点赞记录表 =====
CREATE TABLE IF NOT EXISTS t_like_record (
    id          BIGINT      NOT NULL,
    article_id  BIGINT      NOT NULL,
    user_id     BIGINT      NOT NULL,
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_like_article_user (article_id, user_id)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE INDEX idx_like_record_user ON t_like_record (user_id);

-- ===== 3. t_favorite 收藏记录表 =====
CREATE TABLE IF NOT EXISTS t_favorite (
    id          BIGINT      NOT NULL,
    article_id  BIGINT      NOT NULL,
    user_id     BIGINT      NOT NULL,
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_favorite_article_user (article_id, user_id)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE INDEX idx_favorite_user ON t_favorite (user_id);
