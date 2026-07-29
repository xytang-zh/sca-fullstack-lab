# 快速验证手册：Platform MVP Foundation

**功能分支**：`001-platform-mvp`
**创建日期**：2026-07-30
**关联规格**：[spec.md](./spec.md) | [plan.md](./plan.md) | [research.md](./research.md) | [data-model.md](./data-model.md) | [contracts/](./contracts/)

> 本文为端到端可运行的验证手册，用于证明 MVP 功能可用。实现细节见 `tasks.md`（由 `/speckit-tasks` 产出）。

---

## 1. 前置条件

### 1.1 开发环境

| 软件 | 最低版本 | 用途 |
|------|----------|------|
| OpenJDK | 21 LTS | 后端运行时 |
| Maven | 3.9+ | 后端构建 |
| Node.js | 20 LTS | 前端构建 |
| pnpm | 9+ | 前端包管理 |
| Git | 2.40+ | 版本控制 |

### 1.2 中间件（用 Docker Compose 启动）

```yaml
# docker-compose.yml（位于仓库根）
services:
  mysql:
    image: mysql:8.4
    ports: ["3306:3306"]
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: sca_mvp
    volumes: ["./data/mysql:/var/lib/mysql"]

  redis:
    image: redis:7.4
    ports: ["6379:6379"]

  rabbitmq:
    image: rabbitmq:3.13-management
    ports: ["5672:5672", "15672:15672"]

  nacos:
    image: nacos/nacos-server:v2.4
    ports: ["8848:8848", "9848:9848"]
    environment:
      MODE: standalone
```

启动：
```bash
docker compose up -d
```

### 1.3 初始化数据

```bash
# 导入建表 SQL（含 sys_user/sys_role/sys_menu/sys_dept/sys_dict/sys_param/sys_notice/portal_content + 12 张日志分表）
mysql -h127.0.0.1 -uroot -proot sca_mvp < spring-cloud-alibaba/spring-cloud-test/sql/init.sql

# 初始化超级管理员账号（默认 admin / Admin@1234，首次登录强制改密）
mysql -h127.0.0.1 -uroot -proot sca_mvp < spring-cloud-alibaba/spring-cloud-test/sql/seed-admin.sql
```

### 1.4 Nacos 配置

导入 `spring-cloud-shared.yaml`（共享配置）与各服务独立配置：

```yaml
# spring-cloud-shared.yaml（节选）
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/sca_mvp?useUnicode=true&characterEncoding=utf8mb4
    username: root
    password: root
  data:
    redis:
      host: localhost
      port: 6379
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest

sa-token:
  timeout: 1800
  sso:
    mode: 3
    server-url: http://localhost:8080/api/auth/sso
```

---

## 2. 后端启动

### 2.1 构建父工程

```bash
cd spring-cloud-alibaba
mvn clean install -DskipTests
```

预期输出：`BUILD SUCCESS`，所有 33 个 Maven 模块构建通过。

### 2.2 启动顺序

按依赖顺序启动 5 个 MVP 必要服务（窗口分离，或用 `mvn spring-boot:run` 后台）：

| 序 | 服务 | 端口 | 启动命令 |
|----|------|------|----------|
| 1 | Nacos | 8848 | `docker compose up -d nacos` |
| 2 | spring-cloud-gateway | 8080 | `cd spring-cloud-gateway && mvn spring-boot:run` |
| 3 | spring-cloud-auth | 8081 | `cd spring-cloud-auth && mvn spring-boot:run` |
| 4 | spring-cloud-system | 8082 | `cd spring-cloud-system && mvn spring-boot:run` |
| 5 | spring-cloud-log | 8089 | `cd spring-cloud-log && mvn spring-boot:run` |
| 6 | spring-cloud-portal | 8090 | `cd spring-cloud-portal && mvn spring-boot:run` |

### 2.3 健康检查

```bash
curl http://localhost:8080/actuator/health
```

预期响应：`{"status":"UP", ...}`，所有下游服务状态为 UP。

---

## 3. 前端启动

### 3.1 安装依赖

```bash
cd vue-web-ui
pnpm install
```

### 3.2 启动 admin

```bash
cd apps/admin
pnpm dev
```

访问 `http://localhost:5173`。

### 3.3 启动 portal

```bash
cd apps/portal
pnpm dev      # 开发模式
pnpm build    # SSG 预渲染生产构建
```

访问 `http://localhost:5174`。

---

## 4. 端到端验证场景

### 场景 1：超级管理员登录与改密（对齐 spec SC-001）

**步骤**：
1. 访问 `http://localhost:5173/login`。
2. 输入 `admin` / `Admin@1234` / 验证码 `a1b2`（从 `/api/auth/captcha` 获取）。
3. 点击"登录"。

**预期结果**：
- 跳转到工作台首页 `/dashboard`，看到统计卡片与待办列表（≤ 30 秒，SC-001）。
- 首次登录强制改密弹窗，输入 `NewPass@1234` 后改密成功，跳回登录页。

### 场景 2：创建用户与分配角色（对齐 spec SC-006）

**步骤**：
1. 用 admin 登录。
2. 进入"系统管理 > 角色管理"，创建角色 `viewer`，数据范围=4 仅本人，分配菜单"系统管理 > 用户管理"（仅查询权限）。
3. 进入"系统管理 > 用户管理"，创建用户 `viewer1` / `Init@1234` / 部门=总部 / 角色=viewer，状态=1 待激活。
4. 退出 admin，用 `viewer1` 登录。

**预期结果**：
- viewer1 仅看到"用户管理"菜单，仅能查询不能新增。
- 用户列表只显示自己（数据范围=仅本人，FR-025）。
- 90% 管理员可独立完成此流程（SC-006）。

### 场景 3：SSO 单点登录与单点注销（对齐 spec SC-002）

**步骤**：
1. 用 admin 在 `http://localhost:5173` 登录。
2. 在新标签访问模拟子系统 `http://localhost:5175/protected-page`（需在 auth 服务注册为 SSO Client）。
3. 在一体化平台点击"退出登录"。

**预期结果**：
- 步骤 2：免登直接进入受保护页面（≤ 2 秒，SC-002）。
- 步骤 3：刷新子系统页面，自动跳回一体化登录中心。

### 场景 4：踢人下线（对齐 spec SC-008）

**步骤**：
1. 在浏览器 A 用 `viewer1` 登录，打开子系统受保护页面。
2. 在浏览器 B 用 admin 登录，进入"用户管理"，对 `viewer1` 点击"踢下线"。

**预期结果**：
- 浏览器 A 的子系统页面在 5 秒内自动跳回登录中心，提示"您的账号已被管理员下线"（SC-008）。
- 登录日志中可查到 `viewer1` 的 `login_type=3` 踢人下线记录（FR-020）。

### 场景 5：操作日志查询与脱敏（对齐 spec FR-022、SC-007）

**步骤**：
1. 执行场景 2 创建用户操作。
2. admin 进入"系统管理 > 操作日志"，按 `module=system/user` 检索。

**预期结果**：
- 看到刚才的 CREATE 操作记录，`requestParams` 中 `phone` 字段显示为 `138****8888`（脱敏，FR-022）。
- 查询响应 ≤ 1 秒（SC-007）。

### 场景 6：公开内容发布工作流（对齐 spec FR-029）

**步骤**：
1. 用 `viewer1`（如已分配 `portal:content:create` 权限）登录 admin，进入"门户管理 > 内容管理"。
2. 创建一篇博客，状态=1 草稿。
3. 点击"提交审核"，状态变为 2 待审核。
4. 用 admin 登录（业务审批人），点击"审批通过"，状态变为 3 已发布。
5. 访问 portal `http://localhost:5174/blog/{slug}`。

**预期结果**：
- 步骤 4：触发 RabbitMQ `portal.content.published` 事件，SSG 重新构建该博客静态页。
- 步骤 5：访客无需登录即可看到博客详情，禁用 JS 仍可见正文（FR-017）。
- admin 在"门户管理"将该博客"下架"，访问对应 URL 返回 410 Gone。

### 场景 7：并发编辑冲突（对齐 spec FR-027）

**步骤**：
1. 浏览器 A 与 B 同时用 admin 登录，都进入"角色管理 > 编辑 viewer 角色"。
2. A 修改数据范围并保存成功。
3. B 修改权限点并保存。

**预期结果**：
- B 的保存返回 HTTP 409 + `R.code=40901`，提示"该资源已被他人修改，是否刷新后重试"。
- B 刷新后看到 A 的修改已生效，再次保存成功。

### 场景 8：日志按月分表查询（对齐 spec FR-024、SC-009）

**步骤**：
1. 跨 2 个月份（如 2026-07 与 2026-08）写入 ≥ 100 万条操作日志（用 `spring-cloud-test` 的压测脚本）。
2. admin 在"操作日志查询"页，时间范围选择 2026-07 至 2026-08，按"操作人=admin"检索。

**预期结果**：
- ShardingSphere 路由到 `sys_operation_log_202607` 与 `sys_operation_log_202608` 两张分表。
- 归并查询返回结果，P95 ≤ 3 秒（SC-009）。

### 场景 9：公开门户 SEO 友好（对齐 spec SC-005）

**步骤**：
1. 用 Chrome Lighthouse 对 `http://localhost:5174/blog/{slug}` 跑 SEO 评分。

**预期结果**：
- SEO 评分 ≥ 90（SC-005）。
- HTML 源码包含完整的 `<title>`、`<meta name="description">`、`<meta property="og:title">` 等。

### 场景 10：限流触发（对齐 spec 边界情况）

**步骤**：
1. 用 `ab` 或 `wrk` 对 `/api/auth/login` POST 发起 10 次并发请求（同 IP）。

**预期结果**：
- 第 6 次起返回 HTTP 429 + `R.code=42901` + `Retry-After: 30`。
- 验证码强制要求（前 5 次允许无验证码，超过 5 次必须带验证码）。

---

## 5. 自动化测试

### 5.1 后端单元测试

```bash
cd spring-cloud-alibaba
mvn test
```

预期：所有模块单元测试通过，覆盖率 ≥ 60%。

### 5.2 后端集成测试

```bash
cd spring-cloud-alibaba/spring-cloud-test
mvn verify -Dspring.profiles.active=integration
```

预期：基于 Testcontainers（MySQL/Redis/RabbitMQ）的端到端集成测试通过，覆盖场景 1–7。

### 5.3 前端测试

```bash
cd vue-web-ui
pnpm test:unit   # Vitest 单元测试
pnpm test:e2e    # Playwright E2E 测试（覆盖场景 1–4）
```

### 5.4 契约测试

```bash
cd spring-cloud-alibaba/spring-cloud-test
mvn verify -Dspring.profiles.active=contract
```

预期：基于 springdoc-openapi 生成的 OpenAPI 3 spec，前后端契约一致性校验通过。

---

## 6. 性能压测（对齐 spec SC-003、SC-009）

### 6.1 登录压测

```bash
# 使用 wrk 压测登录接口
wrk -t4 -c100 -d30s -s login.lua http://localhost:8080/api/auth/login
```

预期：500 并发不降级，P95 ≤ 1 秒（SC-003）。

### 6.2 操作日志查询压测

```bash
wrk -t4 -c50 -d30s -s log-query.lua "http://localhost:8080/api/log/operations?startTime=2026-07-01&endTime=2026-08-31"
```

预期：跨 2 个月份分表的查询 P95 ≤ 3 秒（SC-009）。

---

## 7. 验证完成标志

- ✅ 场景 1–10 全部按预期通过
- ✅ 后端单元 + 集成 + 契约测试全部通过
- ✅ 前端单元 + E2E 测试全部通过
- ✅ 性能压测达标（SC-001 ~ SC-009 全部满足）

完成后即可进入下一阶段：`/speckit-tasks` 生成任务清单 → `/speckit-implement` 执行实现。
