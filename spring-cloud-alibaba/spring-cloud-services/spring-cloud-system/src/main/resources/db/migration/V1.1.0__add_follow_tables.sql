-- ============================================================
-- spring-cloud-system 用户中心增量脚本
-- 手动执行（与 V1.0.0 相同，未启用 Flyway 自动迁移）
-- 注意：所有 SQL 必须可跨 MySQL/KingbaseES/DM8 运行
-- ============================================================

-- ===== 1. t_follow 关注表（follower_id + followee_id 唯一，保证幂等）=====
CREATE TABLE IF NOT EXISTS t_follow (
    id          BIGINT       NOT NULL                          COMMENT '主键 ID（雪花 ID）',
    follower_id BIGINT       NOT NULL                          COMMENT '关注者 ID（主动关注方）',
    followee_id BIGINT       NOT NULL                          COMMENT '被关注者 ID',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_follow (follower_id, followee_id)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE INDEX idx_follow_followee ON t_follow (followee_id);

-- ===== 2. sys_user 增加个人简介列 =====
ALTER TABLE sys_user ADD COLUMN bio VARCHAR(512) NULL COMMENT '个人简介';