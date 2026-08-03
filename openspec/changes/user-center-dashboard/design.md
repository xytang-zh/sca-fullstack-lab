## Context

当前状态（见 proposal.md - Why）：
- 前端两个独立应用：`apps/admin`（系统管理/登录/仪表盘，仅 `system/*` 视图已实现）与 `apps/portal`（公开博客：Home/ArticleDetail/Login/Register/Profile）
- 后端已实现：`spring-cloud-auth`（登录/注册/验证码/修改密码 `PATCH /me/password` 已存在）、`spring-cloud-article`（文章分页/详情/发布/点赞/收藏，Caffeine 缓存）、`spring-cloud-system`（RBAC 骨架）
- 后端未实现：`spring-cloud-comment`（仅 CLAUDE.md，无代码）、专栏、关注关系、用户 bio
- 前端 API 层已规范：`@sca/api` 统辖 `/api/article/*`、`/api/comments/*`、`/api/auth/*`、`/api/system/*`

约束：`R<T>` 响应、雪花 ID 序列化为 String（前端必须 string）、`Authorization`/`X-Login-Id` 透传、RESTful 动词子资源、阿里规范 checkstyle 强制。

## Goals / Non-Goals

**Goals:**
- 将 admin 与 portal 合并为单一前端应用，公开浏览与登录后 dashboard 共存
- 登录后统一进入 `/dashboard`，按角色渲染菜单/页面/接口
- 后端补齐用户中心接口（我的文章/草稿/专栏/收藏/点赞/回答/关注订阅）与数据模型
- 提供种子数据便于测试
- 公开博客首页改造为知乎风格顶栏、移除大卡片

**Non-Goals:**
- 不实现 WebSocket 实时消息（消息/私信为静态占位）
- 不实现搜索/全文检索落地（仅顶栏入口 + 占位结果页）
- 不迁移 admin 中未实现的 monitor/message/file/log/job 模块（视图删除或占位）
- 不引入新第三方依赖（复用现有技术栈）

## Decisions

### D1 单应用合并：admin 并入 portal

删除 `apps/admin`，保留 `@sca/portal` 作为唯一前端应用（dev 端口 5174），将 admin 已有的系统管理视图（`system/*`、`layouts/default`、`Dashboard`、`Login`）迁入 portal 的 admin 角色路由下。

```
apps/
├── admin/  ✗ 删除，视图迁入 portal
└── portal/ ✅ 唯一应用（@sca/portal，端口 5174）
    └── src/
        ├── views/
        │   ├── public/        Home / ArticleDetail / Login / Register
        │   ├── user/          Profile / Password / MyArticles / Drafts /
        │   │                  Columns / Favorites / Likes / Answers / Follows / Write
        │   └── admin/         Stats / ArticleAudit / CommentAudit / UserList / SystemMan-(迁入)
        ├── layouts/
        │   ├── PublicLayout.vue     知乎风格顶栏 + 内容
        │   └── DashboardLayout.vue  左侧菜单 + 顶栏 + 内容
        └── router/  index.ts（static）+ permission.ts（动态路由按角色）
```

理由：用户明确要求合并，避免双应用部署维护；portal 是公开入口，用户中心基于其改造最自然。替代方案（保留 admin 为基座 / 新建独立应用）被否：前者需重构公开页，后者工作量更大。

### D2 前端路由与布局

```
/                公开博客首页（PublicLayout，知乎顶栏）
/articles/:id    文章详情（PublicLayout）
/login /register 登录/注册（BlankLayout）
/dashboard       仪表盘（DashboardLayout，需登录，未登录重定向 /login?redirect=）
  ├── profile       个人主页
  ├── password      修改密码
  ├── articles      我的文章
  ├── drafts        草稿
  ├── columns       专栏
  ├── favorites     收藏
  ├── likes         点赞
  ├── answers       回答（我的评论）
  ├── follows       关注订阅（Tab：我关注的人/关注我的人/我订阅的专栏）
  ├── write         撰写文章
  │  └── admin 角色额外：
  ├── stats         统计
  ├── audit/articles 文章审核
  ├── audit/comments 评论审核
  └── users         用户管理
```

- 静态路由（`router/index.ts`）声明公开页与 dashboard 骨架；dashboard 子路由由 `/api/system/menus/routes`（或前端按角色配置）动态注入
- 角色判定：`userStore.roles`（来自 `/api/auth/me` 返回的 roles）
- 未登录访问 `/dashboard/**` → 重定向 `/login?redirect=...`；登录成功后回跳或进 `/dashboard`

### D3 权限模型

- 后端：管理接口加 `@SaCheckRole("ADMIN")`，用户中心接口 `@SaCheckLogin` + 服务内按 `X-Login-Id`（`StpUtil.getLoginIdAsLong()`）隔离数据
- 前端：`meta.roles` + 路由守卫控制菜单与页面；按钮级权限用 `usePermission` 判断（如审核按钮仅 ADMIN 可见）
- 菜单数据源：一期用前端静态菜单配置（按角色过滤），后端 `/api/system/menus/routes` 已有契约，后续可无缝切换

### D4 后端用户中心接口设计

**spring-cloud-article（8093）**：

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| GET | `/api/article/articles/my` | 我的已发布文章（分页） | 登录 |
| GET | `/api/article/articles/my/drafts` | 我的草稿（分页） | 登录 |
| GET | `/api/article/articles/my/likes` | 我点赞的文章（分页） | 登录 |
| GET | `/api/article/articles/my/favorites` | 我收藏的文章（分页） | 登录 |
| GET | `/api/article/columns` | 专栏列表（公开，?userId= 过滤） | 匿名 |
| GET | `/api/article/columns/my` | 我的专栏 | 登录 |
| GET | `/api/article/columns/my/subscriptions` | 我订阅的专栏 | 登录 |
| POST | `/api/article/columns` | 创建专栏 | 登录 |
| PUT | `/api/article/columns/{id}` | 编辑专栏（作者） | 登录 |
| DELETE | `/api/article/columns/{id}` | 删除专栏（作者） | 登录 |
| POST | `/api/article/columns/{id}/subscribe` | 订阅/取消订阅（幂等） | 登录 |

- 文章 `ArticleCreateDTO` 增加 `columnId` 字段；`t_article` 增加 `column_id` 列
- 我的文章/草稿/点赞/收藏列表均以 `StpUtil.getLoginIdAsLong()` 为条件，忽略请求中的用户 ID 参数（内容隔离）

**spring-cloud-comment（8094，本期从零实现）**：

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| GET | `/api/comments/articles/{articleId}` | 文章评论列表（已审核） | 匿名 |
| POST | `/api/comments` | 发表评论 | 登录 |
| POST | `/api/comments/{id}/reply` | 二级回复 | 登录 |
| POST | `/api/comments/{id}/like` | 评论点赞（幂等） | 登录 |
| GET | `/api/comments/my` | 我的评论（回答，含所属文章，分页） | 登录 |
| GET | `/api/comments/pending` | 待审核评论（管理员） | ADMIN |
| POST | `/api/comments/{id}/audit` | 审核（通过/驳回） | ADMIN |

**spring-cloud-system（8082）**：

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| POST | `/api/system/users/{id}/follow` | 关注/取消关注（幂等） | 登录 |
| GET | `/api/system/users/{id}/followers` | 关注此用户的人（粉丝） | 登录 |
| GET | `/api/system/users/{id}/following` | 我关注的人（按登录用户） | 登录 |
| GET | `/api/system/users/me/mine` | 我的完整资料（含 bio/关注/粉丝数） | 登录 |

- `sys_user` 增加 `bio` 字段（个人简介）；关注数/粉丝数实时查 `t_follow` 计数
- 用户资料读取统一走 system 服务，article 展示作者信息时经 Dubbo 调 system（或网关聚合）；一期文章列表作者信息由前端拼装 `authorId` 后调用 `/api/system/users/{id}` 获取

**spring-cloud-auth（8081）**：修改密码接口已存在（`PATCH /me/password`），本期仅前端补页面；`/api/auth/me` 返回 `roles` 供前端角色判定。

### D5 数据模型（新增表，沿用现有 SQL 规范）

```sql
-- 专栏表（article 服务）
t_column (
  id BIGINT PK, user_id BIGINT NOT NULL, name VARCHAR(64) NOT NULL,
  description VARCHAR(512), cover_image VARCHAR(255),
  status SMALLINT DEFAULT 1, create_time, update_time, version INT DEFAULT 0, deleted SMALLINT DEFAULT 0
)
-- 订阅专栏表（article 服务）
t_column_subscribe (
  id BIGINT PK, user_id BIGINT NOT NULL, column_id BIGINT NOT NULL, create_time,
  UNIQUE uk_sub_user_column (user_id, column_id)
)
-- 关注表（system 服务）
t_follow (
  id BIGINT PK, follower_id BIGINT NOT NULL, followee_id BIGINT NOT NULL,
  create_time, UNIQUE uk_follow (follower_id, followee_id)
)
-- 评论表（comment 服务）
t_comment (
  id BIGINT PK, article_id BIGINT NOT NULL, user_id BIGINT NOT NULL,
  parent_id BIGINT DEFAULT 0, reply_to_id BIGINT DEFAULT 0,
  content VARCHAR(2000) NOT NULL, status SMALLINT DEFAULT 1,  -- 1待审 2已审 3驳回 4删除
  ip VARCHAR(64), user_agent VARCHAR(255), likes BIGINT DEFAULT 0,
  create_time, update_time, version INT DEFAULT 0, deleted SMALLINT DEFAULT 0
)
```

- `t_article` 增列：`column_id BIGINT NULL`（关联专栏）
- `sys_user` 增列：`bio VARCHAR(512) NULL`
- 新增表 SQL 放各服务 `src/main/resources/db/migration/` 下（沿用 V1.0.0 手动执行模式），并同步 `scripts/` 种子脚本

### D6 模拟数据

- 新增 `scripts/seed.sql`（或 `docker/mysql/init/`），插入：
  - 用户：admin（ADMIN）、alice（AUTHOR）、bob/user（USER）、carol（USER）等，密码统一 `Admin@123`（Argon2id 哈希复用注册脚本生成的串）
  - 文章：8-12 篇已发布（含 1-2 篇被点赞/收藏较多的高热度文）、1 篇草稿、1 篇待审核
  - 专栏：每作者 1-2 个，文章关联专栏
  - 评论：多篇正文下 3-6 条一级评论 + 二级回复
  - 互动：交叉点赞/收藏记录
  - 关注：用户间关注关系、专栏订阅关系
- 保证 `R<T>` 分页查询在无基础设施时也能 README 一键初始化

### D7 知乎风格顶栏（PublicLayout）

```
┌──────────────────────────────────────────────────────────────┐
│ Sca博客  关注  最新·  热门  专栏   [🔍 搜索]   ➕   🔔   ✉️   [头像] │
└──────────────────────────────────────────────────────────────┘
```
- 从左至右：文字 logo、关注、最新（默认页，激活态）、热门、专栏、搜索框、加号（→ `/dashboard/write`，未登录引导登录）、消息、私信、头像下拉（我的主页/设置/退出）
- 未登录：右侧显示"登录"按钮；已登录：头像 + 下拉（复用现有 `userStore`）
- 首页移除 Hero 大卡片，直接渲染工具条 + 文章列表
- "关注"标签页：已登录用户展示其关注用户的文章（调用 `/api/article/articles?feed=following`），未登录引导登录

## Risks / Trade-offs

- [删除 admin 应用丢失其未实现模块] → 设计明确仅迁移已实现的 system 视图，其余模块不做；若后续需要可重建
- [评论服务从零实现，工作量大] → 本期只做最小闭环（列表/发表/回复/点赞/我的评论/审核），不做敏感词过滤的完整词库，仅过滤占位
- [单应用 bundle 变大] → 路由懒加载 + Vite manualChunks 分包，保持首屏（公开页）只加载必要 chunk
- [作者信息跨服务获取（article 需 system 用户资料）] → 一期前端聚合调用，避免 Dubbo 依赖；后续可改用 Dubbo 或网关聚合
- [菜单权限前后端双份维护] → 一期前端静态菜单 + 后端接口鉴权兜底，业务码 403 由前端拦截器统一提示

## Migration Plan

1. 后端先落地：表结构（article/comment/system 迁移脚本）→ 专栏/关注/我的列表接口 → 评论服务实现 → 种子数据
2. 前端改造：合并应用（删除 admin，迁入视图）→ 路由与布局 → 用户中心页面 → 知乎顶栏 → 权限菜单
3. 联调：种子数据 + 登录验证各角色菜单与接口
4. 回滚：git 分支粒度，前端保留 admin 分支备份；后端变更均为增量表与接口，可独立回滚

## Open Questions

- 是否需要"消息/私信"的真实功能（本期占位）？可能影响后续 message 服务规划
- 文章"关注"Feed 是否需要后端实现 `?feed=following`，还是前端先按关注用户文章 ID 过滤？