## 1. 后端：数据模型与迁移脚本

- [x] 1.1 在 `spring-cloud-services/spring-cloud-article/src/main/resources/db/migration/` 新增 `V1.1.0__add_column_tables.sql`：`t_column`（user_id/name/description/cover_image/status 等）、`t_column_subscribe`（user_id/column_id 唯一键），`t_article` 增加 `column_id BIGINT NULL` 列（验收：`mvn clean compile -pl spring-cloud-article -am` 通过）
- [x] 1.2 在 `spring-cloud-services/spring-cloud-system/src/main/resources/db/migration/` 新增 `V1.1.0__add_follow_tables.sql`：`t_follow`（follower_id/followee_id 唯一键），`sys_user` 增加 `bio VARCHAR(512) NULL` 列
- [x] 1.3 在 `spring-cloud-services/spring-cloud-comment/src/main/resources/db/migration/` 新增 `V1.0.0__init_comment_tables.sql`：`t_comment`（article_id/user_id/parent_id/reply_to_id/content/status/ip/likes 等）

## 2. 后端：article 服务用户中心接口

- [x] 2.1 新增专栏实体 `Column.java`、Mapper、`ColumnService`/`ColumnServiceImpl`、`ColumnVO`/`ColumnCreateDTO`；实现 `GET /api/article/columns`（公开，?userId= 过滤）、`GET /api/article/columns/my`（登录）、`POST /api/article/columns`（创建）、`PUT /api/article/columns/{id}`（作者）、`DELETE /api/article/columns/{id}`（作者）
- [x] 2.2 实现 `POST /api/article/columns/{id}/subscribe`（订阅/取消订阅，幂等）与 `GET /api/article/columns/my/subscriptions`（我订阅的专栏）
- [x] 2.3 在 `ArticleController` 新增 `GET /api/article/articles/my`（我的已发布文章）、`GET /api/article/articles/my/drafts`（我的草稿）、`GET /api/article/articles/my/likes`（我点赞的文章）、`GET /api/article/articles/my/favorites`（我收藏的文章），均以 `StpUtil.getLoginIdAsLong()` 隔离、分页返回
- [x] 2.4 `ArticleCreateDTO` 增加 `columnId` 字段，`create`/`update` 逻辑写入 `column_id`；`ArticleServiceImpl` 增加 `update` 方法（当前缺失）

## 3. 后端：comment 服务实现

- [x] 3.1 创建 `spring-cloud-comment` 服务骨架：`pom.xml`（父 POM 引用，不覆盖版本）、`SpringCloudCommentApplication`、`application.yml`/`bootstrap.yml`（端口 8094）、SaToken 配置
- [x] 3.2 新增 `Comment.java` 实体、Mapper、`CommentService`/`CommentServiceImpl`；实现 `GET /api/comments/articles/{articleId}`（仅已审核，按 parent_id 分组）、`POST /api/comments`（发表，保存 IP/UA，状态默认 PENDING）、`POST /api/comments/{id}/reply`（二级回复）
- [x] 3.3 实现 `POST /api/comments/{id}/like`（评论点赞/取消，幂等）与 `GET /api/comments/my`（我的评论，含所属文章标题，分页）
- [x] 3.4 实现 `GET /api/comments/pending`（待审核列表，`@SaCheckRole("ADMIN")`）与 `POST /api/comments/{id}/audit`（通过/驳回）
- [x] 3.5 `spring-cloud-services/pom.xml` 的 `<modules>` 确认包含 `spring-cloud-comment`（若已声明则跳过）

## 4. 后端：system 服务关注关系

- [x] 4.1 新增 `Follow.java` 实体、Mapper、`FollowService`/`FollowServiceImpl`；实现 `POST /api/system/users/{id}/follow`（关注/取消关注，幂等，禁止关注自己）
- [x] 4.2 实现 `GET /api/system/users/{id}/followers`（粉丝列表）、`GET /api/system/users/{id}/following`（关注列表）；`GET /api/system/users/me/mine` 返回当前用户资料（含 bio、关注数、粉丝数）
- [x] 4.3 `User` 实体增加 `bio` 字段，`UserMapper` 支持按 id 查询（现有 `selectById` 即可），`UserVO` 增加 `bio`/`followCount`/`followerCount`

## 5. 后端：种子数据

- [x] 5.1 新增 `scripts/seed.sql`（或 `docker/mysql/init/`）：插入 admin（ADMIN）、alice（AUTHOR）、bob/user（USER）、carol（USER）等用户（密码 Argon2id 哈希统一 `Admin@123`）、8-12 篇已发布文章 + 1 草稿 + 1 待审核、每作者 1-2 个专栏、评论与二级回复、交叉点赞/收藏、关注与专栏订阅关系
- [x] 5.2 在 README 或 `scripts/README.md` 补充种子数据执行说明（`mysql -u root -p sca < scripts/seed.sql`）

## 6. 前端：合并 admin 与 portal 为单应用

- [x] 6.1 删除 `apps/admin`，将已实现的 `system/*` 视图、`layouts/default`、`Dashboard`、`Login` 迁入 `apps/portal/src/views/admin/` 等路径
- [x] 6.2 调整 `vue-web-ui/package.json` 脚本（移除 `dev:admin`/`build:admin`，保留 `dev:portal`）、`pnpm-workspace.yaml` 确认仅 admin 移除后无依赖残留
- [x] 6.3 重构 `apps/portal/src/router`：新增 `PublicLayout`（知乎顶栏）、`DashboardLayout`（左侧菜单 + 顶栏）、`BlankLayout`（登录/注册）；`/dashboard` 及子路由懒加载，`meta.requiresAuth` + `meta.roles` 守卫
- [x] 6.4 新增 `apps/portal/src/store/permission.ts`（角色菜单配置与过滤），`userStore` 增加 `roles` 字段与 `fetchUserInfo` 解析 `/api/auth/me` 返回的 roles

## 7. 前端：知乎风格顶栏与首页改造

- [x] 7.1 新增 `apps/portal/src/components/NavBar.vue`：从左至右文字 logo、关注、最新（默认激活）、热门、专栏、搜索框、加号、消息、私信、头像下拉（我的主页/设置/退出）；未登录显示"登录"按钮
- [x] 7.2 改造 `apps/portal/src/views/Home.vue`：移除 Hero 大卡片，首屏直接为工具条 + 文章列表；顶栏"最新/热门"切换排序
- [x] 7.3 实现顶栏"关注"标签（已登录展示关注用户文章，未登录引导登录）、"专栏"标签（专栏列表）、搜索框（跳转 `/search?q=` 占位结果页）、加号（→ `/dashboard/write`，未登录引导登录）
- [x] 7.4 头像下拉"我的主页"进入 `/dashboard/profile`、"设置"进入 `/dashboard/password`、"退出"调用 `logout` 后回首页

## 8. 前端：用户中心页面

- [x] 8.1 实现 `/dashboard/profile`（个人主页：头像/昵称/bio/关注数/粉丝数/文章数/专栏数 + 最近文章）
- [x] 8.2 实现 `/dashboard/password`（修改密码表单，调 `PATCH /api/auth/me/password`，成功后重新登录）
- [x] 8.3 实现 `/dashboard/articles`（我的文章列表，分页）与 `/dashboard/drafts`（我的草稿列表，可删除）与 `/dashboard/write`（撰写文章表单，含标题/摘要/正文/专栏选择，草稿与发布两个动作）
- [x] 8.4 实现 `/dashboard/columns`（我的专栏：列表 + 创建/编辑/删除弹窗）
- [x] 8.5 实现 `/dashboard/favorites`（我的收藏）与 `/dashboard/likes`（我的点赞），点击进入文章详情
- [x] 8.6 实现 `/dashboard/answers`（我的回答/评论列表，点击跳转原文）
- [x] 8.7 实现 `/dashboard/follows`（关注订阅页，Tab 切换：我关注的人 / 关注我的人 / 我订阅的专栏）

## 9. 前端：管理员菜单页面

- [x] 9.1 实现 `/dashboard/stats`（统计：文章数/评论数/用户数/点赞数卡片 + 简单趋势图）
- [x] 9.2 实现 `/dashboard/audit/articles`（文章审核列表：待审核 → 通过/驳回）与 `/dashboard/audit/comments`（评论审核列表：待审核 → 通过/驳回）
- [x] 9.3 按角色过滤菜单：`permission.ts` 中 ADMIN 额外注册 stats/audit/users 菜单，USER/AUTHOR 仅用户中心菜单

## 10. 前端：API 与类型扩展

- [x] 10.1 `packages/api/src/services/article.ts` 新增列专栏/我的文章/草稿/点赞/收藏 API；`packages/api/src/services/comment.ts` 新增我的评论 API；`packages/api/src/services/user.ts` 新增关注/粉丝/资料 API；`packages/api/src/services/auth.ts` 确认 `me` 返回 roles
- [x] 10.2 `packages/types/src/blog.ts` 新增 `ColumnVO`、`ColumnCreateDTO`、`FollowVO`、`ProfileVO` 等类型；`packages/types/src/auth.ts` 的 `UserInfoVO` 增加 `roles`；全部 ID 用 `string`

## 11. 联调与验证

- [x] 11.1 后端编译与 checkstyle：`mvn clean install -DskipTests`（根目录）通过，无 checkstyle 违规
- [x] 11.2 前端类型与 lint：在 `vue-web-ui` 执行 `pnpm typecheck` 与 `pnpm lint` 通过
- [x] 11.3 启动 gateway + auth + system + article + comment，执行种子数据后：USER 登录进入 dashboard 仅见用户中心菜单、ADMIN 登录额外见管理菜单；未登录访问 `/dashboard` 重定向登录；越权调管理接口返回 403
- [x] 11.4 浏览器验证：未登录首页顶部为大卡片移除后的知乎顶栏 + 文章列表；登录后各用户中心页面（文章/草稿/专栏/收藏/点赞/回答/关注订阅）数据正确