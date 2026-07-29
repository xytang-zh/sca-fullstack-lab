# 数据模型：Platform MVP Foundation

**功能分支**：`001-platform-mvp`
**创建日期**：2026-07-30
**关联规格**：[spec.md](./spec.md) | [plan.md](./plan.md) | [research.md](./research.md)

> 本文为 `/speckit-plan` Phase 1 输出。所有表名遵循 `sys_{模块}_{实体}` 命名规范，字段用 `snake_case`；时间字段统一 `DATETIME`，默认 `CURRENT_TIMESTAMP`。

---

## 1. 实体清单

| # | 实体 | 表名 | 分表策略 | 备注 |
|---|------|------|----------|------|
| 1 | 用户 | `sys_user` | 不分表 | 五态生命周期 |
| 2 | 角色 | `sys_role` | 不分表 | 五级数据范围 |
| 3 | 菜单 | `sys_menu` | 不分表 | 树形结构 |
| 4 | 部门 | `sys_dept` | 不分表 | 树形结构 |
| 5 | 字典 | `sys_dict` | 不分表 | 类型+编码唯一 |
| 6 | 参数 | `sys_param` | 不分表 | 键唯一 |
| 7 | 通知 | `sys_notice` | 不分表 | 公告 |
| 8 | 公开内容 | `portal_content` | 不分表 | 四态发布工作流 |
| 9 | 操作日志 | `sys_operation_log_YYYYMM` | 按月分表（ShardingSphere 精确分片） | 保留 1 年 |
| 10 | 登录日志 | `sys_login_log_YYYYMM` | 按月分表（ShardingSphere 精确分片） | 保留 1 年 |

### 关联表

| # | 表名 | 关联关系 |
|---|------|----------|
| 11 | `sys_user_role` | 用户-角色多对多 |
| 12 | `sys_role_menu` | 角色-菜单多对多（含按钮权限点） |
| 13 | `sys_role_dept` | 角色-部门多对多（数据范围=5 自定义时使用） |
| 14 | `sys_oauth2_client` | OAuth2 客户端注册表 |

---

## 2. 实体定义

### 2.1 sys_user（用户表）

```sql
CREATE TABLE sys_user (
    id              BIGINT          NOT NULL COMMENT '主键 ID（雪花 ID）',
    username        VARCHAR(64)     NOT NULL COMMENT '登录账号',
    password        VARCHAR(128)    NOT NULL COMMENT '密码（BCrypt 加密存储，禁止明文）',
    nickname        VARCHAR(64)     NOT NULL DEFAULT '' COMMENT '昵称',
    email           VARCHAR(128)    NULL COMMENT '邮箱',
    phone           VARCHAR(32)     NULL COMMENT '手机号',
    avatar          VARCHAR(255)    NULL COMMENT '头像 URL',
    dept_id         BIGINT          NULL COMMENT '所属部门 ID',
    status          TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：1=待激活 2=正常 3=禁用 4=锁定 5=已删除(软删除)',
    fail_count      INT             NOT NULL DEFAULT 0 COMMENT '连续登录失败次数（5 次后锁定 15 分钟）',
    lock_until      DATETIME        NULL COMMENT '锁定截止时间（NULL 表示未锁定）',
    last_login_time DATETIME        NULL COMMENT '最后登录时间',
    last_login_ip   VARCHAR(64)     NULL COMMENT '最后登录 IP',
    creator         BIGINT          NULL COMMENT '创建人 ID',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater         BIGINT          NULL COMMENT '更新人 ID',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    version         INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    deleted         TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0=未删 1=已删',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    KEY idx_dept_id (dept_id),
    KEY idx_status (status),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
```

**状态机**（FR-026）：

```
       创建账号
[1 待激活] ──── 首次登录/管理员审核 ───> [2 正常]
                                         │
                                         ├──管理员禁用──> [3 禁用] ──管理员启用──> [2 正常]
                                         │
                                         ├──连续5次登录失败──> [4 锁定] ──15分钟后/管理员解锁──> [2 正常]
                                         │
                                         └──软删除──> [5 已删除]
```

**校验规则**：
- `username`：3-64 字符，仅允许字母/数字/下划线/中划线，全局唯一。
- `password`：BCrypt 加密存储，禁止明文。最小长度 8，需含字母+数字。
- `email`：标准 email 格式（Hibernate Validator `@Email`）。
- `phone`：11 位中国大陆手机号或国际格式 `+CC-xxxxxxxx`。
- **禁止删除最后一个活跃超级管理员**（FR-012）。

---

### 2.2 sys_role（角色表）

```sql
CREATE TABLE sys_role (
    id              BIGINT          NOT NULL COMMENT '主键 ID',
    role_code       VARCHAR(64)     NOT NULL COMMENT '角色码（用于权限点标识，全小写下划线分隔）',
    role_name       VARCHAR(64)     NOT NULL COMMENT '角色名称',
    data_scope      TINYINT         NOT NULL DEFAULT 1 COMMENT '数据范围：1=全部 2=本部门及以下 3=仅本部门 4=仅本人 5=自定义',
    sort            INT             NOT NULL DEFAULT 0 COMMENT '排序',
    status          TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：1=正常 2=禁用',
    remark          VARCHAR(255)    NULL COMMENT '备注',
    creator         BIGINT          NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         BIGINT          NULL,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version         INT             NOT NULL DEFAULT 0,
    deleted         TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (role_code),
    KEY idx_data_scope (data_scope)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';
```

**数据范围语义**（FR-025）：
- `1 全部`：不拼接 SQL 条件。
- `2 本部门及以下`：`dept_id IN (SELECT id FROM sys_dept WHERE id=? OR FIND_IN_SET(?, ancestors))`。
- `3 仅本部门`：`dept_id = ?`。
- `4 仅本人`：`creator = ?`。
- `5 自定义`：通过 `sys_role_dept` 关联表查询。

---

### 2.3 sys_menu（菜单表，树形）

```sql
CREATE TABLE sys_menu (
    id              BIGINT          NOT NULL COMMENT '主键 ID',
    parent_id       BIGINT          NOT NULL DEFAULT 0 COMMENT '父菜单 ID（0=根）',
    menu_name       VARCHAR(64)     NOT NULL COMMENT '菜单名称',
    menu_type       TINYINT         NOT NULL COMMENT '类型：1=目录 2=菜单 3=按钮',
    path            VARCHAR(255)    NULL COMMENT '前端路由路径（type=2 时必填）',
    component       VARCHAR(255)    NULL COMMENT '前端组件路径',
    perms           VARCHAR(128)    NULL COMMENT '权限标识（如 system:user:list）',
    icon            VARCHAR(64)     NULL COMMENT '图标',
    sort            INT             NOT NULL DEFAULT 0,
    visible         TINYINT         NOT NULL DEFAULT 1 COMMENT '是否可见：1=是 0=否',
    status          TINYINT         NOT NULL DEFAULT 1,
    creator         BIGINT          NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         BIGINT          NULL,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version         INT             NOT NULL DEFAULT 0,
    deleted         TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_parent_id (parent_id),
    KEY idx_perms (perms)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单表';
```

**树形约束**：
- 通过 `parent_id` 自关联形成树；删除节点时若有子节点，必须先迁移子节点或级联软删除。
- `perms` 全局唯一（按钮权限点）；`menu_type=3` 时必填。
- **关键资源**：菜单树整体编辑需走分布式锁（FR-028），锁 Key `lock:menu:tree`。

---

### 2.4 sys_dept（部门表，树形）

```sql
CREATE TABLE sys_dept (
    id              BIGINT          NOT NULL,
    parent_id       BIGINT          NOT NULL DEFAULT 0,
    ancestors       VARCHAR(512)    NOT NULL DEFAULT '' COMMENT '祖级列表（如 0,100,101），用于加速"本部门及以下"查询',
    dept_name       VARCHAR(64)     NOT NULL,
    dept_code       VARCHAR(64)    NOT NULL,
    leader          VARCHAR(64)     NULL COMMENT '负责人',
    phone           VARCHAR(32)    NULL,
    sort            INT             NOT NULL DEFAULT 0,
    status          TINYINT         NOT NULL DEFAULT 1,
    creator         BIGINT          NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         BIGINT          NULL,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version         INT             NOT NULL DEFAULT 0,
    deleted         TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_dept_code (dept_code),
    KEY idx_parent_id (parent_id),
    KEY idx_ancestors (ancestors)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';
```

**`ancestors` 字段维护规则**：插入/更新部门时，由 Service 层根据 `parent_id` 递归计算并维护 `ancestors`，禁止由前端直接传入。

---

### 2.5 sys_dict（字典表）

```sql
CREATE TABLE sys_dict (
    id              BIGINT          NOT NULL,
    dict_type       VARCHAR(64)     NOT NULL COMMENT '字典类型（如 sys_user_sex）',
    dict_label      VARCHAR(64)     NOT NULL COMMENT '字典标签（如"男"）',
    dict_value      VARCHAR(64)     NOT NULL COMMENT '字典键值（如"1"）',
    sort            INT             NOT NULL DEFAULT 0,
    status          TINYINT         NOT NULL DEFAULT 1,
    remark          VARCHAR(255)    NULL,
    creator         BIGINT          NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         BIGINT          NULL,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version         INT             NOT NULL DEFAULT 0,
    deleted         TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_type_value (dict_type, dict_value),
    KEY idx_dict_type (dict_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典表';
```

**缓存**：字典读取高频低变，使用 Caffeine + Redis 多级缓存，TTL 1 小时 + ±10% 随机；变更时主动清除缓存。

---

### 2.6 sys_param（参数表）

```sql
CREATE TABLE sys_param (
    id              BIGINT          NOT NULL,
    param_key       VARCHAR(128)    NOT NULL COMMENT '参数键',
    param_value     VARCHAR(512)    NOT NULL COMMENT '参数值',
    param_type      TINYINT         NOT NULL DEFAULT 1 COMMENT '值类型：1=string 2=number 3=boolean 4=json',
    remark          VARCHAR(255)    NULL,
    creator         BIGINT          NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         BIGINT          NULL,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version         INT             NOT NULL DEFAULT 0,
    deleted         TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_param_key (param_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统参数表';
```

**热更新**：参数变更通过 Nacos 配置中心 + `@RefreshScope` 实现热刷新；同时通过 RabbitMQ 事件 `sys.param.changed` 通知所有实例清缓存。

---

### 2.7 sys_notice（通知公告表）

```sql
CREATE TABLE sys_notice (
    id              BIGINT          NOT NULL,
    notice_title    VARCHAR(128)    NOT NULL,
    notice_type     TINYINT         NOT NULL COMMENT '1=通知 2=公告',
    notice_content  TEXT           NOT NULL COMMENT '富文本内容（HTML，入库前 XSS 清洗）',
    status          TINYINT         NOT NULL DEFAULT 1 COMMENT '1=草稿 2=已发布 3=已撤回',
    publish_time    DATETIME        NULL COMMENT '发布时间',
    creator         BIGINT          NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         BIGINT          NULL,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version         INT             NOT NULL DEFAULT 0,
    deleted         TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_status (status),
    KEY idx_publish_time (publish_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知公告表';
```

**XSS 防护**：`notice_content` 入库前通过 `jsoup` 清洗（白名单标签 + 属性），禁止 `<script>` / `onload` / `onerror` 等。

---

### 2.8 portal_content（公开内容表）

```sql
CREATE TABLE portal_content (
    id              BIGINT          NOT NULL,
    title           VARCHAR(255)    NOT NULL COMMENT '标题',
    slug            VARCHAR(255)    NOT NULL COMMENT 'URL 友好标识（如 my-first-blog）',
    content_type    TINYINT         NOT NULL COMMENT '1=博客 2=新闻 3=产品介绍',
    body            MEDIUMTEXT      NOT NULL COMMENT '正文（Markdown + HTML，XSS 清洗）',
    summary         VARCHAR(512)    NULL COMMENT '摘要（用于列表与 SEO meta description）',
    cover_image     VARCHAR(255)    NULL COMMENT '封面图 URL',
    category        VARCHAR(64)     NULL COMMENT '分类',
    tags            VARCHAR(255)    NULL COMMENT '标签（逗号分隔）',
    seo_title       VARCHAR(255)    NULL,
    seo_description VARCHAR(512)    NULL,
    seo_keywords    VARCHAR(255)    NULL,
    og_image        VARCHAR(255)    NULL COMMENT 'Open Graph 图片',
    status          TINYINT         NOT NULL DEFAULT 1 COMMENT '1=草稿 2=待审核 3=已发布 4=已下架',
    author_id       BIGINT          NOT NULL COMMENT '作者（用户 ID）',
    reviewer_id     BIGINT          NULL COMMENT '审核人（用户 ID）',
    publish_time    DATETIME        NULL COMMENT '发布时间',
    creator         BIGINT          NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         BIGINT          NULL,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version         INT             NOT NULL DEFAULT 0,
    deleted         TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_slug (slug),
    KEY idx_content_type_status (content_type, status),
    KEY idx_publish_time (publish_time),
    KEY idx_author_id (author_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公开内容表';
```

**状态机**（FR-029）：

```
[1 草稿] ──提交──> [2 待审核] ──审批通过──> [3 已发布] ──下架──> [4 已下架]
                       │                       │
                       └──审批驳回──> [1 草稿]  │
                                                │
                                                └──重新发布──> [1 草稿]
```

**SSG 触发**：仅"待审核→已发布"与"已发布→已下架"状态变更发送 RabbitMQ 事件 `portal.content.published` / `portal.content.unpublished`，触发 portal 前端 SSG 重新构建。

---

### 2.9 sys_operation_log（操作日志表，按月分表）

```sql
-- 模板表 sys_operation_log_YYYYMM
CREATE TABLE sys_operation_log_202607 (
    id              BIGINT          NOT NULL,
    user_id         BIGINT          NULL COMMENT '操作人 ID',
    username        VARCHAR(64)     NULL COMMENT '操作人账号（冗余字段，避免关联查询）',
    module          VARCHAR(64)     NOT NULL COMMENT '模块（如 system/user）',
    operation       VARCHAR(64)     NOT NULL COMMENT '操作类型（如 INSERT/UPDATE/DELETE/KICKOUT）',
    method          VARCHAR(255)    NOT NULL COMMENT '方法（如 UserController.create）',
    request_url     VARCHAR(255)    NULL,
    request_method  VARCHAR(10)     NULL COMMENT 'HTTP 方法',
    request_params  TEXT            NULL COMMENT '关键入参（敏感字段脱敏）',
    response_result TEXT            NULL COMMENT '响应结果摘要',
    ip              VARCHAR(64)     NULL,
    location        VARCHAR(128)    NULL COMMENT '地理位置（IP 反查）',
    cost_ms         INT             NULL COMMENT '耗时（毫秒）',
    status          TINYINT         NOT NULL COMMENT '1=成功 0=失败',
    error_msg       TEXT            NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_user_id_create_time (user_id, create_time),
    KEY idx_module_create_time (module, create_time),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表（按月分表）';
```

**ShardingSphere 分片规则**：
- 分片键：`create_time`（DATETIME）。
- 分片算法：`PRECISE_ALGORITHM=MONTH_PRECISE`，路由到 `sys_operation_log_YYYYMM`。
- 预创建：每月 25 日由 XXL-JOB 任务预创建下月分表（覆盖 12 个月滚动窗口）。
- 归档：1 年前的分表由 XXL-JOB 月度任务归档至冷存表 `sys_operation_log_cold`。

---

### 2.10 sys_login_log（登录日志表，按月分表）

```sql
-- 模板表 sys_login_log_YYYYMM
CREATE TABLE sys_login_log_202607 (
    id              BIGINT          NOT NULL,
    username        VARCHAR(64)     NOT NULL COMMENT '登录账号',
    user_id         BIGINT          NULL COMMENT '用户 ID（登录失败可能为空）',
    login_time      DATETIME        NOT NULL,
    ip              VARCHAR(64)     NULL,
    location        VARCHAR(128)    NULL,
    browser         VARCHAR(64)     NULL,
    os              VARCHAR(64)     NULL,
    device          VARCHAR(64)     NULL COMMENT '设备类型',
    login_type      TINYINT         NOT NULL COMMENT '1=登录 2=登出 3=踢人下线',
    result          TINYINT         NOT NULL COMMENT '1=成功 0=失败',
    fail_reason     VARCHAR(255)    NULL COMMENT '失败原因（如"密码错误"/"账号锁定"）',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_username_create_time (username, create_time),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志表（按月分表）';
```

---

### 2.11 sys_user_role（用户-角色关联表）

```sql
CREATE TABLE sys_user_role (
    user_id         BIGINT          NOT NULL,
    role_id         BIGINT          NOT NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id),
    KEY idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-角色关联表';
```

---

### 2.12 sys_role_menu（角色-菜单关联表）

```sql
CREATE TABLE sys_role_menu (
    role_id         BIGINT          NOT NULL,
    menu_id         BIGINT          NOT NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id, menu_id),
    KEY idx_menu_id (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色-菜单关联表';
```

---

### 2.13 sys_role_dept（角色-部门关联表，数据范围=5 自定义时使用）

```sql
CREATE TABLE sys_role_dept (
    role_id         BIGINT          NOT NULL,
    dept_id         BIGINT          NOT NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id, dept_id),
    KEY idx_dept_id (dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色-部门关联表（数据范围=自定义）';
```

---

### 2.14 sys_oauth2_client（OAuth2 客户端注册表）

```sql
CREATE TABLE sys_oauth2_client (
    id              BIGINT          NOT NULL,
    client_id       VARCHAR(64)     NOT NULL COMMENT '客户端 ID',
    client_secret   VARCHAR(128)    NOT NULL COMMENT '客户端密钥（BCrypt 加密）',
    client_name     VARCHAR(128)    NOT NULL,
    redirect_uris   VARCHAR(1024)   NOT NULL COMMENT '回调地址（逗号分隔）',
    grant_types     VARCHAR(128)    NOT NULL DEFAULT 'authorization_code' COMMENT '授权模式（仅 authorization_code）',
    scopes          VARCHAR(255)    NULL COMMENT '作用域',
    access_token_validity INT       NOT NULL DEFAULT 7200 COMMENT '访问令牌有效期（秒）',
    refresh_token_validity INT      NOT NULL DEFAULT 86400 COMMENT '刷新令牌有效期（秒）',
    status          TINYINT         NOT NULL DEFAULT 1,
    creator         BIGINT          NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         BIGINT          NULL,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version         INT             NOT NULL DEFAULT 0,
    deleted         TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_client_id (client_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OAuth2 客户端注册表';
```

---

## 3. DTO/VO 分层规范

### 3.1 分层原则

- **Entity**：与数据库表 1:1 映射，仅用于 Mapper 层；禁止直接序列化为接口响应。
- **DTO（Data Transfer Object）**：入参对象，含 `@Validated` 校验注解。
- **VO（View Object）**：出参对象，敏感字段脱敏后呈现给前端。

### 3.2 命名规范

| 类型 | 命名模式 | 示例 |
|------|----------|------|
| 创建 DTO | `{Entity}CreateDTO` | `UserCreateDTO` |
| 更新 DTO | `{Entity}UpdateDTO` | `UserUpdateDTO` |
| 查询 DTO（分页） | `{Entity}PageQuery` | `UserPageQuery` |
| 详情 VO | `{Entity}VO` | `UserVO` |
| 列表 VO | `{Entity}ListVO` | `UserListVO` |

### 3.3 敏感字段脱敏规则

| 字段 | 脱敏格式 | 示例 |
|------|----------|------|
| 手机号 | `中间4位*` | `138****8888` |
| 邮箱 | `@前只保留前1位+*` | `x****@example.com` |
| 身份证号 | `前6后4，中间*` | `110101********1234` |
| 密码 / Token | 完全不返回 | N/A |

---

## 4. 数据访问层规范

### 4.1 Mapper 接口

- 所有 Mapper 继承 `BaseMapper<T>`（MyBatis-Plus）。
- 自定义查询使用 `@DataPermission` 注解标记需要数据权限拦截的方法。
- 禁止在 Service 拼接 SQL 字符串；复杂查询用 XML Mapper 或 `LambdaQueryWrapper`。

### 4.2 拦截器链

注册顺序（重要，影响 SQL 改写顺序）：
1. `DataPermissionInnerInterceptor`（自研，数据权限）
2. `PaginationInnerInterceptor(DbType.MYSQL)`（分页）
3. `OptimisticLockerInnerInterceptor`（乐观锁）

### 4.3 主键策略

- 全部使用雪花 ID（`IdType.ASSIGN_ID`）。
- 禁止使用数据库自增（避免分库分表时主键冲突）。

---

## 5. 异常体系

```
RuntimeException
└── BusinessException（自定义基类，含 code + msg）
    ├── AuthException（认证类）
    │   ├── LoginFailedException
    │   ├── AccountLockedException
    │   └── SsoTicketInvalidException
    ├── PermissionException（权限类）
    │   ├── RoleNotFoundException
    │   └── DataScopeDeniedException
    ├── BizException（业务类）
    │   ├── UserNotFoundException
    │   ├── LastSuperAdminException
    │   ├── OptimisticLockException
    │   └── ContentStatusTransitionException
    └── SystemException（系统类）
        ├── DbException
        └── MqException
```

- 所有异常由 `spring-cloud-common-core` 的 `GlobalExceptionHandler` 统一捕获，转换为 `R<T>` 响应。
- 业务异常 HTTP 状态码 = 200 + `R.code` 业务码；HTTP 5xx 由 Spring 默认处理。
- 乐观锁冲突返回 HTTP 409 + `R.code=40901`。

---

## 6. 缓存 Key 规范

| 用途 | Key 格式 | TTL |
|------|----------|-----|
| 用户会话 | `sa:token:login:token:{tokenValue}` | 30 分钟（可配） |
| 用户权限点 | `spring-cloud:auth:user:perms:{userId}` | 30 分钟 + ±10% 随机 |
| 用户角色 | `spring-cloud:auth:user:roles:{userId}` | 30 分钟 + ±10% 随机 |
| 字典缓存 | `spring-cloud:system:dict:{dictType}` | 1 小时 + ±10% 随机 |
| 参数缓存 | `spring-cloud:system:param:{paramKey}` | 1 小时 + ±10% 随机 |
| 分布式锁 | `lock:{resource_type}:{resource_id}` | 30 秒（默认 TTL） |
| SSO Ticket | `sa:sso:ticket:{ticket}` | 60 秒（一次性） |

---

## 7. 关键约束

- **逻辑删除**：所有业务表加 `deleted TINYINT`，`deleted=1` 即软删除；列表查询默认排除。
- **乐观锁**：所有可编辑业务表加 `version INT NOT NULL DEFAULT 0`，与 `@Version` 注解配合。
- **审计字段**：所有业务表加 `creator / create_time / updater / update_time`，由 `MetaObjectHandler` 自动填充。
- **字符集**：统一 `utf8mb4`，避免 emoji 等 4 字节字符问题。
- **存储引擎**：统一 `InnoDB`，支持事务与外键（虽不显式定义外键约束，由 Service 层保证一致性）。
- **国产化适配**：建表语句避免 MySQL 特有语法（如 `AUTO_INCREMENT`、`ON UPDATE CURRENT_TIMESTAMP`），通过 ShardingSphere 适配层在 KingbaseES/DM8 上自动改写。
