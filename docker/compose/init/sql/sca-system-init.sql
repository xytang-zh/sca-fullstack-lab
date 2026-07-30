-- ============================================================
-- Platform MVP Foundation 初始化脚本
-- 对齐 specs/001-platform-mvp/data-model.md §2 实体定义
-- 注意：所有 SQL 必须可跨 MySQL/KingbaseES/DM8 运行，避免 MySQL 私有语法
-- ============================================================

-- ===== 1. sys_user 用户表 =====
CREATE TABLE IF NOT EXISTS sys_user (
    id              BIGINT          NOT NULL                          COMMENT '主键 ID（雪花 ID）',
    username        VARCHAR(64)     NOT NULL                          COMMENT '登录账号',
    password        VARCHAR(255)    NOT NULL                          COMMENT '密码（Argon2id 哈希存储，禁止明文）',
    nickname        VARCHAR(64)    NOT NULL DEFAULT ''               COMMENT '昵称',
    email           VARCHAR(128)    NULL                              COMMENT '邮箱',
    phone           VARCHAR(32)     NULL                              COMMENT '手机号',
    avatar          VARCHAR(255)    NULL                              COMMENT '头像 URL',
    dept_id         BIGINT          NULL                              COMMENT '所属部门 ID',
    status          SMALLINT        NOT NULL DEFAULT 1                COMMENT '状态：1=待激活 2=正常 3=禁用 4=锁定 5=已删除',
    fail_count      INT            NOT NULL DEFAULT 0                 COMMENT '连续登录失败次数',
    lock_until      DATETIME        NULL                              COMMENT '锁定截止时间',
    last_login_time DATETIME        NULL                              COMMENT '最后登录时间',
    last_login_ip   VARCHAR(64)     NULL                              COMMENT '最后登录 IP',
    creator         BIGINT          NULL                              COMMENT '创建人 ID',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater         BIGINT          NULL                              COMMENT '更新人 ID',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    version         INT            NOT NULL DEFAULT 0                 COMMENT '乐观锁版本号',
    deleted         SMALLINT        NOT NULL DEFAULT 0                COMMENT '逻辑删除：0=未删 1=已删',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
);
CREATE INDEX idx_sys_user_dept_id ON sys_user (dept_id);
CREATE INDEX idx_sys_user_status ON sys_user (status);
CREATE INDEX idx_sys_user_create_time ON sys_user (create_time);

-- ===== 2. sys_role 角色表 =====
CREATE TABLE IF NOT EXISTS sys_role (
    id              BIGINT          NOT NULL,
    role_code       VARCHAR(64)     NOT NULL                          COMMENT '角色码（全小写下划线分隔）',
    role_name       VARCHAR(64)     NOT NULL                          COMMENT '角色名称',
    data_scope      SMALLINT        NOT NULL DEFAULT 1                COMMENT '数据范围：1=全部 2=本部门及以下 3=仅本部门 4=仅本人 5=自定义',
    sort            INT            NOT NULL DEFAULT 0,
    status          SMALLINT        NOT NULL DEFAULT 1                COMMENT '1=正常 2=禁用',
    remark          VARCHAR(255)    NULL,
    creator         BIGINT          NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         BIGINT          NULL,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INT            NOT NULL DEFAULT 0,
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (role_code)
);
CREATE INDEX idx_sys_role_data_scope ON sys_role (data_scope);

-- ===== 3. sys_menu 菜单表（树形） =====
CREATE TABLE IF NOT EXISTS sys_menu (
    id              BIGINT          NOT NULL,
    parent_id       BIGINT          NOT NULL DEFAULT 0                COMMENT '父菜单 ID（0=根）',
    menu_name       VARCHAR(64)     NOT NULL                          COMMENT '菜单名称',
    menu_type       SMALLINT        NOT NULL                          COMMENT '类型：1=目录 2=菜单 3=按钮',
    path            VARCHAR(255)    NULL                              COMMENT '前端路由路径',
    component       VARCHAR(255)    NULL                              COMMENT '前端组件路径',
    perms           VARCHAR(128)    NULL                              COMMENT '权限标识',
    icon            VARCHAR(64)     NULL,
    sort            INT            NOT NULL DEFAULT 0,
    visible         SMALLINT        NOT NULL DEFAULT 1                COMMENT '1=可见 0=隐藏',
    status          SMALLINT        NOT NULL DEFAULT 1,
    creator         BIGINT          NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         BIGINT          NULL,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INT            NOT NULL DEFAULT 0,
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);
CREATE INDEX idx_sys_menu_parent_id ON sys_menu (parent_id);
CREATE INDEX idx_sys_menu_perms ON sys_menu (perms);

-- ===== 4. sys_dept 部门表（树形） =====
CREATE TABLE IF NOT EXISTS sys_dept (
    id              BIGINT          NOT NULL,
    parent_id       BIGINT          NOT NULL DEFAULT 0,
    ancestors       VARCHAR(512)    NOT NULL DEFAULT ''               COMMENT '祖级列表（如 0,100,101）',
    dept_name       VARCHAR(64)     NOT NULL,
    dept_code       VARCHAR(64)     NOT NULL,
    leader          VARCHAR(64)     NULL,
    phone           VARCHAR(32)     NULL,
    sort            INT            NOT NULL DEFAULT 0,
    status          SMALLINT        NOT NULL DEFAULT 1,
    creator         BIGINT          NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         BIGINT          NULL,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INT            NOT NULL DEFAULT 0,
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_dept_code (dept_code)
);
CREATE INDEX idx_sys_dept_parent_id ON sys_dept (parent_id);
CREATE INDEX idx_sys_dept_ancestors ON sys_dept (ancestors);

-- ===== 5. sys_dict 字典表 =====
CREATE TABLE IF NOT EXISTS sys_dict (
    id              BIGINT          NOT NULL,
    dict_type       VARCHAR(64)     NOT NULL                          COMMENT '字典类型',
    dict_label      VARCHAR(64)     NOT NULL                          COMMENT '字典标签',
    dict_value      VARCHAR(64)     NOT NULL                          COMMENT '字典键值',
    sort            INT            NOT NULL DEFAULT 0,
    status          SMALLINT        NOT NULL DEFAULT 1,
    remark          VARCHAR(255)    NULL,
    creator         BIGINT          NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         BIGINT          NULL,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INT            NOT NULL DEFAULT 0,
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_dict_type_value (dict_type, dict_value)
);
CREATE INDEX idx_sys_dict_type ON sys_dict (dict_type);

-- ===== 6. sys_param 参数表 =====
CREATE TABLE IF NOT EXISTS sys_param (
    id              BIGINT          NOT NULL,
    param_key       VARCHAR(128)    NOT NULL                          COMMENT '参数键',
    param_value     VARCHAR(512)    NOT NULL                          COMMENT '参数值',
    param_type      SMALLINT        NOT NULL DEFAULT 1                COMMENT '1=string 2=number 3=boolean 4=json',
    remark          VARCHAR(255)    NULL,
    creator         BIGINT          NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         BIGINT          NULL,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INT            NOT NULL DEFAULT 0,
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_param_key (param_key)
);

-- ===== 7. sys_notice 通知公告表 =====
CREATE TABLE IF NOT EXISTS sys_notice (
    id              BIGINT          NOT NULL,
    notice_title    VARCHAR(128)    NOT NULL,
    notice_type     SMALLINT        NOT NULL                          COMMENT '1=通知 2=公告',
    notice_content  TEXT            NOT NULL                          COMMENT '富文本（HTML，入库前 XSS 清洗）',
    status          SMALLINT        NOT NULL DEFAULT 1                COMMENT '1=草稿 2=已发布 3=已撤回',
    publish_time    DATETIME        NULL,
    creator         BIGINT          NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         BIGINT          NULL,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INT            NOT NULL DEFAULT 0,
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);
CREATE INDEX idx_sys_notice_status ON sys_notice (status);
CREATE INDEX idx_sys_notice_publish_time ON sys_notice (publish_time);

-- ===== 8. portal_content 公开内容表 =====
CREATE TABLE IF NOT EXISTS portal_content (
    id              BIGINT          NOT NULL,
    title           VARCHAR(255)    NOT NULL,
    slug            VARCHAR(255)    NOT NULL                          COMMENT 'URL 友好标识',
    content_type    SMALLINT        NOT NULL                          COMMENT '1=博客 2=新闻 3=产品介绍',
    body            MEDIUMTEXT      NOT NULL                         COMMENT '正文（Markdown + HTML，XSS 清洗）',
    summary         VARCHAR(512)    NULL,
    cover_image     VARCHAR(255)    NULL,
    category        VARCHAR(64)     NULL,
    tags            VARCHAR(255)    NULL,
    seo_title       VARCHAR(255)    NULL,
    seo_description VARCHAR(512)    NULL,
    seo_keywords    VARCHAR(255)    NULL,
    og_image        VARCHAR(255)    NULL,
    status          SMALLINT        NOT NULL DEFAULT 1                COMMENT '1=草稿 2=待审核 3=已发布 4=已下架',
    author_id       BIGINT          NOT NULL,
    reviewer_id     BIGINT          NULL,
    publish_time    DATETIME        NULL,
    creator         BIGINT          NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         BIGINT          NULL,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INT            NOT NULL DEFAULT 0,
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_portal_slug (slug)
);
CREATE INDEX idx_portal_content_type_status ON portal_content (content_type, status);
CREATE INDEX idx_portal_content_publish_time ON portal_content (publish_time);
CREATE INDEX idx_portal_content_author_id ON portal_content (author_id);

-- ===== 9. sys_user_role 用户-角色关联表 =====
CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id         BIGINT          NOT NULL,
    role_id         BIGINT          NOT NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id)
);
CREATE INDEX idx_sys_user_role_role_id ON sys_user_role (role_id);

-- ===== 10. sys_role_menu 角色-菜单关联表 =====
CREATE TABLE IF NOT EXISTS sys_role_menu (
    role_id         BIGINT          NOT NULL,
    menu_id         BIGINT          NOT NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id, menu_id)
);
CREATE INDEX idx_sys_role_menu_menu_id ON sys_role_menu (menu_id);

-- ===== 11. sys_role_dept 角色-部门关联表（数据范围=5 自定义） =====
CREATE TABLE IF NOT EXISTS sys_role_dept (
    role_id         BIGINT          NOT NULL,
    dept_id         BIGINT          NOT NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id, dept_id)
);
CREATE INDEX idx_sys_role_dept_dept_id ON sys_role_dept (dept_id);

-- ===== 12. sys_oauth2_client OAuth2 客户端注册表 =====
CREATE TABLE IF NOT EXISTS sys_oauth2_client (
    id              BIGINT          NOT NULL,
    client_id       VARCHAR(64)     NOT NULL,
    client_secret   VARCHAR(128)    NOT NULL                          COMMENT 'BCrypt 加密',
    client_name     VARCHAR(128)    NOT NULL,
    redirect_uris   VARCHAR(1024)   NOT NULL                          COMMENT '回调地址（逗号分隔）',
    grant_types     VARCHAR(128)    NOT NULL DEFAULT 'authorization_code',
    scopes          VARCHAR(255)    NULL,
    access_token_validity INT       NOT NULL DEFAULT 7200,
    refresh_token_validity INT      NOT NULL DEFAULT 86400,
    status          SMALLINT        NOT NULL DEFAULT 1,
    creator         BIGINT          NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         BIGINT          NULL,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INT            NOT NULL DEFAULT 0,
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_oauth2_client_id (client_id)
);

-- ===== 13. 初始超级管理员数据 =====
-- 密码 admin123 已用 Argon2id 哈希（memory=16384KB / iterations=3 / parallelism=2 / key=32B / salt=16B）
-- 参数与 Nacos spring-cloud-shared.yaml security.password.argon2.* 对齐；明文 admin123，生产环境首次登录强制改密
INSERT INTO sys_user (id, username, password, nickname, email, status, create_time, update_time, version, deleted)
VALUES (1, 'admin', '$argon2id$v=19$m=16384,t=3,p=2$+27WTLFAqxSTRl5oyRAIjw$FDm+vGxZbK72A/m7fGobGmU6Kgg6RsyuHLHJnwyfXzc', '超级管理员', 'admin@example.com', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0)
ON DUPLICATE KEY UPDATE update_time = CURRENT_TIMESTAMP;

INSERT INTO sys_role (id, role_code, role_name, data_scope, sort, status, create_time, update_time, version, deleted)
VALUES (1, 'super_admin', '超级管理员', 1, 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0)
ON DUPLICATE KEY UPDATE update_time = CURRENT_TIMESTAMP;

INSERT INTO sys_user_role (user_id, role_id, create_time) VALUES (1, 1, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE create_time = CURRENT_TIMESTAMP;

-- ===== 14. 初始字典数据 =====
INSERT INTO sys_dict (id, dict_type, dict_label, dict_value, sort, status, create_time, update_time)
VALUES (1001, 'sys_user_sex', '男', '1', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE update_time = CURRENT_TIMESTAMP;
INSERT INTO sys_dict (id, dict_type, dict_label, dict_value, sort, status, create_time, update_time)
VALUES (1002, 'sys_user_sex', '女', '2', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE update_time = CURRENT_TIMESTAMP;
INSERT INTO sys_dict (id, dict_type, dict_label, dict_value, sort, status, create_time, update_time)
VALUES (1003, 'sys_notice_type', '通知', '1', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE update_time = CURRENT_TIMESTAMP;
INSERT INTO sys_dict (id, dict_type, dict_label, dict_value, sort, status, create_time, update_time)
VALUES (1004, 'sys_notice_type', '公告', '2', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE update_time = CURRENT_TIMESTAMP;
