## Why

当前平台拆分为 `admin`（管理后台）与 `portal`（公开博客）两个独立 Vue 应用：普通用户登录后只能进入简陋的个人主页，无法管理自己的博客内容；管理员与普通用户需分别部署维护两个应用，成本高。同时公开博客首页顶栏简陋、缺少导航与搜索入口，首页大卡片占用首屏空间。需要统一为单应用 + 权限化 dashboard，让不同角色登录后进入同一 dashboard，并按角色展示菜单、页面与接口。

## What Changes

- **前后端合并为单应用**：删除 `apps/admin`，将其中已有的视图/路由/API 并入 `apps/portal`（保留 `@sca/portal` 作为唯一应用，dev 端口 5174）；网关与后端服务不变，`/api/system/*` 等管理接口继续经网关访问
- **权限化 dashboard**：登录成功后统一进入 `/dashboard` 用户中心，按角色渲染菜单与页面（普通用户：个人主页、修改密码、文章、草稿、专栏、收藏、点赞、回答、关注订阅；管理员额外：统计、文章审核、评论审核、用户管理、系统管理）
- **用户内容中心**：后端新增用户中心接口（我的文章/草稿/专栏/收藏/点赞/回答/关注订阅，均按登录用户隔离），前端新增对应页面
- **顶栏改造**：公开博客列表页顶栏改为仿知乎导航（文字 logo、关注、最新、热门、专栏、搜索框、加号、消息、私信、用户头像下拉），移除首页上方大卡片
- **修改密码**：登录用户可修改自己的密码
- **模拟数据**：提供种子数据（多角色用户、博客文章、评论、点赞、收藏、专栏、关注关系），便于测试

## Capabilities

### New Capabilities

- `dashboard`: 单应用架构下登录后的权限化 dashboard 用户中心，未登录访问公开页面，登录后按角色渲染菜单/页面/接口
- `user-center`: 用户内容中心域，覆盖个人主页、修改密码、我的文章、草稿、专栏、收藏、点赞、回答、关注订阅

### Modified Capabilities

- `user-auth`: 登录成功跳转由"个人主页"改为"dashboard 用户中心"；新增"修改密码"需求
- `public-browse`: 顶栏改为知乎风格导航（含关注/最新/热门/专栏/搜索/消息/私信/头像下拉），移除首页大卡片
- `blog-domain`: 新增专栏（用户自定义分类）、草稿/收藏/点赞/评论列表查询、关注订阅（关注与被关注、订阅专栏）

## Impact

- 后端模块：`spring-cloud-system`（用户信息、关注关系）、`spring-cloud-article`（专栏、草稿、收藏、点赞列表）、`spring-cloud-comment`（我的评论列表）、`spring-cloud-auth`（修改密码）、`spring-cloud-services`（种子数据 SQL）
- 前端：删除 `apps/admin`，扩展 `apps/portal`（路由/布局/视图/API/类型）、`packages/api`、`packages/types`、`packages/utils`
- 依赖：无新增第三方依赖，复用现有技术栈（Naive UI / Pinia / Vue Router / UnoCSS）
- 契约：`R<T>` 统一响应、雪花 ID String、`Authorization`/`X-Login-Id` 透传不变；新增角色权限约束（USER/AUTHOR/ADMIN）

## Non-goals

- ❌ 不做第三方 OAuth2 / 短信验证码登录（保留现有账号密码 + 文字验证码登录）
- ❌ 不做点赞/评论/消息的实时推送（WebSocket），消息与私信先用静态占位
- ❌ 不迁移 admin 中尚未实现的 monitor/message/file/log/job 虚拟模块（以占位页或删除处理）
- ❌ 不做移动端专属 App，仅保证响应式布局
- ❌ 不实现全文搜索与 RSS 订阅（已有 spec，本期不落地）