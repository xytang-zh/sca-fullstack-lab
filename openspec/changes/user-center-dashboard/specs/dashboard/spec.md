## Purpose

定义单应用架构下登录后的权限化 dashboard 用户中心：所有角色登录成功后进入统一 dashboard 页面，按角色渲染菜单、页面与接口权限，未登录访问受保护页面时重定向登录。

## ADDED Requirements

### Requirement: 登录后统一跳转 dashboard

系统 SHALL 在用户登录成功后统一跳转到 `/dashboard` 用户中心页面，无论用户角色（USER/AUTHOR/ADMIN）如何；未登录用户访问 `/dashboard` 或任何受保护页面 SHALL 重定向到登录页，登录成功后返回原目标页面。

#### Scenario: 普通用户登录进入 dashboard

- **WHEN** 刚注册的普通用户（USER 角色）通过账号密码登录成功
- **THEN** 系统跳转到 `/dashboard` 页面，展示用户中心布局与菜单

#### Scenario: 管理员登录进入 dashboard

- **WHEN** 管理员（ADMIN 角色）登录成功
- **THEN** 系统跳转到 `/dashboard` 页面，同时展示用户中心菜单与管理菜单

#### Scenario: 未登录访问 dashboard

- **WHEN** 未登录用户直接访问 `/dashboard` URL
- **THEN** 系统重定向到登录页，登录成功后返回 `/dashboard`

### Requirement: 角色化菜单渲染

系统 SHALL 根据当前登录用户角色渲染不同菜单项：USER/AUTHOR 角色 SHALL 看到用户中心菜单（个人主页、修改密码、文章、草稿、专栏、收藏、点赞、回答、关注订阅）；ADMIN 角色 SHALL 额外看到管理菜单（统计、文章审核、评论审核、用户管理、系统管理）；无权限的菜单项 SHALL 不展示。

#### Scenario: 普通用户菜单

- **WHEN** 普通用户登录后展开 dashboard 左侧菜单
- **THEN** 仅展示用户中心菜单项，不展示任何管理菜单项

#### Scenario: 管理员菜单

- **WHEN** 管理员登录后展开 dashboard 左侧菜单
- **THEN** 同时展示用户中心菜单项与管理菜单项

### Requirement: 接口与按钮权限控制

系统 SHALL 对 dashboard 涉及的接口与按钮进行权限控制：用户 SHALL 仅能访问自己角色允许的接口与操作，越权访问 SHALL 返回业务码 403（无权限），前端 SHALL 隐藏无权限的按钮并提示。

#### Scenario: 越权访问管理接口

- **WHEN** 普通用户直接调用管理员专属接口（如文章审核）
- **THEN** 后端返回业务码 403，前端提示无权限，不产生任何数据变更

#### Scenario: 无权限按钮隐藏

- **WHEN** 普通用户打开包含审核按钮的页面
- **THEN** 审核按钮不显示或置灰，点击无响应

### Requirement: 单应用统一入口

系统 SHALL 将 admin 与 portal 合并为单一前端应用：公开浏览页面（博客列表、文章详情、登录、注册）与登录后的 dashboard 用户中心共存于同一应用，由同一路由体系管理，不再需要分别部署两个应用。

#### Scenario: 未登录访问博客首页

- **WHEN** 未登录用户在浏览器打开应用根路径
- **THEN** 展示公开博客列表页（知乎风格顶栏），无需登录

#### Scenario: 已登录访问公开页

- **WHEN** 已登录用户访问应用根路径或博客列表页
- **THEN** 公开页面正常展示，顶栏展示用户头像与下拉菜单，头像菜单可进入 dashboard 用户中心