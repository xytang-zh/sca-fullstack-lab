# 系统管理契约：spring-cloud-system

**服务**：`spring-cloud-system`（端口 8082）
**前缀**：`/api/system/`（由 Gateway `StripPrefix=2` 剥离）
**关联规格**：spec FR-009 ~ FR-012、FR-025、FR-026、FR-027、FR-028

> 本契约覆盖用户、角色、菜单、部门、字典、参数、通知七类资源的 CRUD 与状态迁移接口。所有接口遵循 [common-patterns.md](./common-patterns.md)。

---

## 1. 用户管理 `/api/system/users`

### 1.1 列表（分页 + 数据权限）

#### GET /api/system/users

**鉴权**：`@SaCheckPermission("system:user:list")` + `@DataScope(deptAlias="d", userAlias="u")`

**Query 参数** `UserPageQuery`：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `pageNum` | int | 否 | 默认 1 |
| `pageSize` | int | 否 | 默认 10，最大 100 |
| `username` | string | 否 | 模糊查询 |
| `phone` | string | 否 | 模糊查询 |
| `status` | int | 否 | 1=待激活 2=正常 3=禁用 4=锁定 5=已删除 |
| `deptId` | long | 否 | 按部门筛选（含子部门） |
| `orderBy` | string | 否 | 默认 `create_time desc` |

**响应** `PageVO<UserListVO>`：

```json
{
  "code": 200, "msg": "success", "timestamp": "...",
  "data": {
    "list": [
      {
        "id": 10001,
        "username": "admin",
        "nickname": "超级管理员",
        "email": "a****@example.com",
        "phone": "138****8888",
        "deptId": 1, "deptName": "总部",
        "status": 2, "statusText": "正常",
        "createTime": "2026-07-01T10:00:00+08:00"
      }
    ],
    "total": 123, "pageNum": 1, "pageSize": 10, "pages": 13
  }
}
```

---

### 1.2 详情

#### GET /api/system/users/{id}

**鉴权**：`@SaCheckPermission("system:user:query")`

**响应** `UserVO`：含 `roles` 数组与 `perms` 数组（脱敏）。

---

### 1.3 创建

#### POST /api/system/users

**鉴权**：`@SaCheckPermission("system:user:create")`

**请求体** `UserCreateDTO`：

```json
{
  "username": "zhangsan",
  "password": "Init@1234",
  "nickname": "张三",
  "email": "zhangsan@example.com",
  "phone": "13800138000",
  "deptId": 100,
  "roleIds": [1, 2],
  "status": 1
}
```

**校验**：
- `username`：3-64 字符，字母/数字/下划线/中划线，全局唯一
- `password`：8-32 字符，字母+数字组合
- `email`：标准邮箱格式
- `phone`：11 位中国大陆手机号或 `+CC-xxxxxxxx`
- `deptId`：必须存在
- `roleIds`：必须非空数组

**业务规则**：
- 密码 BCrypt 加密存储。
- 初始状态 1=待激活（FR-026），首次登录强制改密。
- 创建后写入操作日志（FR-019）。
- 使用 `X-Idempotency-Key` 实现幂等（10.5 节）。

---

### 1.4 全量更新

#### PUT /api/system/users/{id}

**鉴权**：`@SaCheckPermission("system:user:update")`

**请求体** `UserUpdateDTO`：与 `UserCreateDTO` 类似，但不含 `password`。

**业务规则**：
- 乐观锁校验：请求需带 `version` 字段，冲突时返回 409（FR-027）。
- 不能修改 `username`（唯一标识）。
- 角色变更对已登录用户在下一次请求后立即生效（FR-011）。

---

### 1.5 部分更新（修改密码 / 状态）

#### PATCH /api/system/users/{id}/password

**请求体**：

```json
{ "newPassword": "NewPass@1234", "version": 1 }
```

#### PATCH /api/system/users/{id}/status

**请求体**：

```json
{ "status": 3, "version": 1 }
```

**状态迁移**（FR-026）：
- 1→2（激活）：管理员审核通过或用户首次登录
- 2→3（禁用）：管理员手动
- 2→4（锁定）：系统连续登录失败 5 次
- 4→2（解锁）：15 分钟后自动或管理员手动
- 1/2/3/4→5（软删除）：仅超级管理员

---

### 1.6 业务动作（动词子资源）

#### POST /api/system/users/{id}/reset-password

**鉴权**：`@SaCheckPermission("system:user:reset")`

**响应**：返回临时重置密码（明文，仅一次显示）：

```json
{ "code": 200, "msg": "success", "data": { "password": "TempX9a2K" } }
```

#### POST /api/system/users/{id}/unlock

**鉴权**：`@SaCheckPermission("system:user:unlock")`

**业务规则**：将状态从 4→2，清空 `fail_count` 与 `lock_until`。

---

### 1.7 删除（软删除）

#### DELETE /api/system/users/{id}

**鉴权**：`@SaCheckPermission("system:user:delete")`

**业务规则**：
- 软删除（`deleted=1`）。
- 禁止删除最后一个活跃超级管理员（FR-012）。
- 禁止删除自己。
- 同时清空 `sys_user_role` 关联。

---

## 2. 角色管理 `/api/system/roles`

### 2.1 列表 / 2.2 详情 / 2.3 创建 / 2.4 更新 / 2.5 删除

接口模式与用户一致，省略。

#### POST /api/system/roles/{id}/allocate-menus

**鉴权**：`@SaCheckPermission("system:role:allocate")` + `@DistributedLock(lock:role:{id})`

**请求体**：

```json
{ "menuIds": [1, 2, 3, 10, 11] }
```

**业务规则**：批量分配菜单与按钮权限，使用分布式锁避免并发覆盖（FR-028）。

#### POST /api/system/roles/{id}/allocate-data-scope

**请求体**：

```json
{ "dataScope": 5, "deptIds": [100, 101, 102] }
```

**业务规则**：`dataScope=5` 自定义时需提供 `deptIds`，写入 `sys_role_dept` 关联表。

---

## 3. 菜单管理 `/api/system/menus`

### 3.1 列表（树形）

#### GET /api/system/menus/tree

**鉴权**：`@SaCheckPermission("system:menu:list")`

**响应** `MenuTreeVO`（树形）：

```json
{
  "code": 200, "msg": "success", "timestamp": "...",
  "data": [
    {
      "id": 1, "parentId": 0, "menuName": "系统管理", "menuType": 1,
      "children": [
        { "id": 10, "parentId": 1, "menuName": "用户管理", "menuType": 2, "perms": "system:user:list",
          "children": [
            { "id": 100, "parentId": 10, "menuName": "新增", "menuType": 3, "perms": "system:user:create" }
          ]
        }
      ]
    }
  ]
}
```

### 3.2 创建 / 3.3 更新 / 3.4 删除

接口模式同用户。**更新整个菜单树时使用分布式锁 `lock:menu:tree`**（FR-028）。

---

## 4. 部门管理 `/api/system/depts`

接口模式同菜单（树形）。`ancestors` 字段由 Service 自动维护，禁止前端直接传入。

---

## 5. 字典管理 `/api/system/dicts`

### GET /api/system/dicts/types

返回所有字典类型列表。

### GET /api/system/dicts/type/{dictType}

**响应** `DictVO[]`：按类型查所有字典项（高频读取，走多级缓存 Caffeine+Redis）。

### POST /api/system/dicts

创建字典项，同时发送 `sys.dict.changed` 事件清缓存。

---

## 6. 参数管理 `/api/system/params`

### GET /api/system/params/{paramKey}

按 key 查参数（走 Redis 缓存，TTL 1 小时 + ±10% 随机）。

### PUT /api/system/params/{id}

更新参数，同时发送 `sys.param.changed` 事件，通过 Nacos `@RefreshScope` 热刷新。

---

## 7. 通知管理 `/api/system/notices`

标准 CRUD，`notice_content` 入库前 jsoup 清洗 XSS。

### POST /api/system/notices/{id}/publish

```json
{ "version": 1 }
```

**业务规则**：状态 1=草稿→2=已发布，记录 `publish_time`。

### POST /api/system/notices/{id}/revoke

**业务规则**：状态 2=已发布→3=已撤回。
