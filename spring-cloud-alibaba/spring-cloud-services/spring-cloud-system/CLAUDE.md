# CLAUDE.md — spring-cloud-system 系统管理服务

> 本文档面向 AI 编码助手，用于在 `spring-cloud-system/` 目录下工作时提供模块约束、技术栈版本、服务职责与开发规范。
> 工作前**必须**先读取父目录 [`spring-cloud-services/CLAUDE.md`](../CLAUDE.md)、后端聚合 [`spring-cloud-alibaba/CLAUDE.md`](../../CLAUDE.md) 与仓库根 [`sca-fullstack-lab/CLAUDE.md`](../../../CLAUDE.md) 了解全局规范。

---

## 1. 模块定位

系统管理服务，按个人博客需求文档已**裁剪为 RBAC 核心**：用户/角色/菜单/权限，为博客 USER/AUTHOR/ADMIN 三角色提供统一的数据支撑，是所有服务的"地基"。其他服务通过 Dubbo 调用它的接口获取用户/角色/权限信息。

| 维度 | 值 |
|------|-----|
| 父 POM | `com.xytang:spring-cloud-alibaba:1.0-SNAPSHOT` |
| 顶级包 | `com.xytang.system` |
| 端口 | HTTP 8082 / Dubbo 20882 / XXL-JOB 10000 |

> ⚠️ 部门/岗位/字典/参数/通知等企业级模块**已移出职责边界**，不再作为本服务核心功能（代码裁剪由后续变更承接）。

---

## 2. 核心功能（RBAC 核心）

| 模块 | 功能点 |
|------|--------|
| 用户模块 | 用户 CRUD、重置密码、启用禁用、导入导出 Excel、分配角色（USER/AUTHOR/ADMIN） |
| 角色模块 | 角色 CRUD、菜单分配、权限标识 |
| 菜单模块 | 菜单树 CRUD、按钮权限标识、路由元数据、缓存策略 |
| 权限模块 | 权限标识管理、角色-权限关联、数据权限（MyBatis-Plus 拦截器 + `@DataScope` 注解） |

### 2.1 博客三角色

| 角色 | 博客域权限 |
|------|-----------|
| USER | 阅读、评论、点赞、收藏 |
| AUTHOR | USER + 直接发布文章 |
| ADMIN | 全部 + 文章审核、评论审核、用户管理、统计 |

---

## 3. 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.5.0（父 POM） | 服务基座 |
| MyBatis-Plus | 3.5.9（父 POM） | 分页、多租户、数据权限拦截器 |
| dynamic-datasource | 4.3.1（父 POM） | 多数据源（主库 MySQL，备 KingbaseES/DM） |
| Caffeine + Redis | 3.2.0 / 4.0.0（父 POM） | 用户/角色/菜单多级缓存 |
| Dubbo | 3.3+（Spring Cloud Alibaba 管理） | 暴露 `UserRpcService`、`RoleRpcService` 等 |
| Sa-Token | 1.44.0（父 POM） | 权限数据源（StpInterface 实现） |
| EasyExcel | 计划（父 POM 未声明） | 用户导入导出 |

---

## 4. 关键接口（RESTful）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/system/users` | 用户分页 |
| GET | `/api/system/users/{id}` | 用户详情 |
| POST | `/api/system/users` | 新增用户 |
| PUT | `/api/system/users/{id}` | 修改用户 |
| PATCH | `/api/system/users/{id}/password` | 重置密码 |
| PATCH | `/api/system/users/{id}/status` | 启用/禁用 |
| PATCH | `/api/system/users/{id}/role` | 分配角色 |
| GET | `/api/system/roles` | 角色分页 |
| GET | `/api/system/menus/tree` | 菜单树 |
| GET | `/api/system/menus/routes` | 前端动态路由 |

Dubbo 接口（RPC 契约）：

| 接口 | 方法 | 说明 |
|------|------|------|
| `UserRpcService` | `getById` / `getRoles` | 用户信息与角色（供 article/comment 等调用） |
| `RoleRpcService` | `listPermissionsByUserId` | 权限标识列表（Sa-Token StpInterface 数据源） |

---

## 5. 数据模型

```
sys_user, sys_role, sys_menu, sys_user_role, sys_role_menu
```

> 数据权限表（sys_role_dept 等）随部门模块移出职责边界。

---

## 6. 服务间通信

| 方向 | 方式 | 说明 |
|------|------|------|
| 所有服务 → system | Dubbo | 用户/角色/权限查询 |
| auth → system | Dubbo | 注册创建用户 |

---

## 7. 开发规范（本服务特有）

- 用户/角色/菜单数据变更后**必须**失效对应缓存（Key：`spring-cloud:system:user:{id}` 等）
- 用户密码**必须** Argon2id 哈希（Bouncy Castle 1.78.1），**禁止** BCrypt/MD5
- `@DataScope` 数据权限注解**必须**配合 MyBatis-Plus 拦截器使用
- RESTful 规范遵循 `spring-cloud-services/CLAUDE.md` §6

---

## 8. 红线（违反即拒绝）

1. ❌ 子服务 POM 覆盖父 POM 依赖版本
2. ❌ `@Autowired` 字段注入（必须 `@RequiredArgsConstructor`）
3. ❌ `throw new RuntimeException` / `catch (Exception e)`
4. ❌ `System.out.println` / `e.printStackTrace()`
5. ❌ 用 BCrypt/MD5 存密码（必须 Argon2id）
6. ❌ 在日志/响应中泄露密码、Token
7. ❌ 接口未加 `@SaCheckPermission`/`@SaCheckRole`
8. ❌ 业务配置硬编码（必须放 Nacos）
