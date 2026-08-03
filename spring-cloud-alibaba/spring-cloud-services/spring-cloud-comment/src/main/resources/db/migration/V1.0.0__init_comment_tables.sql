-- ============================================================
-- spring-cloud-comment 博客评论域初始化脚本
-- 手动执行（与 article/system 服务相同，未启用 Flyway 自动迁移）
-- 注意：所有 SQL 必须可跨 MySQL/KingbaseES/DM8 运行
-- ============================================================

-- ===== 1. t_comment 评论表 =====
CREATE TABLE IF NOT EXISTS t_comment (
    id          BIGINT        NOT NULL                          COMMENT '主键 ID（雪花 ID）',
    article_id  BIGINT        NOT NULL                          COMMENT '所属文章 ID（t_article.id）',
    article_title VARCHAR(128) NOT NULL DEFAULT ''               COMMENT '所属文章标题（冗余，便于我的评论展示）',
    user_id     BIGINT        NOT NULL                          COMMENT '评论者 ID（sys_user.id）',
    nickname    VARCHAR(64)   NOT NULL DEFAULT ''               COMMENT '评论者昵称（冗余）',
    avatar      VARCHAR(255)  NULL                              COMMENT '评论者头像（冗余）',
    parent_id   BIGINT        NOT NULL DEFAULT 0                COMMENT '父评论 ID（0=一级评论）',
    reply_to_id BIGINT        NOT NULL DEFAULT 0                COMMENT '被回复评论 ID（0=无）',
    reply_to_nickname VARCHAR(64) NOT NULL DEFAULT ''           COMMENT '被回复评论者昵称（冗余）',
    content     VARCHAR(2000) NOT NULL                          COMMENT '评论内容（过滤后纯文本）',
    status      SMALLINT      NOT NULL DEFAULT 1                COMMENT '状态：1=待审核 2=已审核 3=已驳回 4=已删除',
    ip          VARCHAR(64)   NULL                              COMMENT '评论者 IP',
    user_agent  VARCHAR(255)  NULL                              COMMENT '评论者 UA',
    likes       BIGINT        NOT NULL DEFAULT 0                COMMENT '点赞数',
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    version     INT           NOT NULL DEFAULT 0                COMMENT '乐观锁版本号',
    deleted     SMALLINT      NOT NULL DEFAULT 0                COMMENT '逻辑删除：0=未删 1=已删',
    PRIMARY KEY (id)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE INDEX idx_comment_article_status ON t_comment (article_id, status);
CREATE INDEX idx_comment_user ON t_comment (user_id);
CREATE INDEX idx_comment_parent ON t_comment (parent_id);

-- ===== 2. t_comment_like 评论点赞表（user_id + comment_id 唯一，保证幂等）=====
CREATE TABLE IF NOT EXISTS t_comment_like (
    id          BIGINT      NOT NULL                          COMMENT '主键 ID（雪花 ID）',
    comment_id  BIGINT      NOT NULL                          COMMENT '评论 ID',
    user_id     BIGINT      NOT NULL                          COMMENT '点赞用户 ID',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_comment_like (comment_id, user_id)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE INDEX idx_comment_like_user ON t_comment_like (user_id);