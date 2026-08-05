# CLAUDE.md — sca-fullstack-lab Monorepo 根

> 本项目是一个学习向的项目，主要是为了在实战中学习并了解各个技术和框架，所以代码必须要有详细且清楚易懂的注释。
> 本文档面向 AI 编码助手（Claude Code / Codex / Cursor），用于在仓库根目录下工作时提供统一的工程约束、技术栈基线、跨项目契约与子项目索引。
> 任何 AI 在本仓库（或任意子项目）下生成代码、配置、SQL、文档时，**必须**先读取本文件并严格遵守其中的规范。
> 进入任意子项目（`spring-cloud-alibaba/` 或 `vue-web-ui/`）工作时，**必须**再读取该子项目根目录的 CLAUDE.md。

---

## 1. 项目定位

`sca-fullstack-lab`（企业级一体化智能管理平台）是一个 **前后端分离的 Monorepo**：

- 后端：Spring Cloud Alibaba 微服务体系（1 个网关 + 1 个认证中心 + 3 个业务服务 + 6 个公共子模块）
- 前端：Vue 3 + Vite + pnpm Monorepo（2 个独立部署应用 + 5 个共享包）

| 维度 | 值 |
|------|-----|
| 顶层 groupId | `com.xytang` |
| 后端聚合 artifactId | `spring-cloud-alibaba` |
| 前端应用包名 | `@sca/web` |
| 当前 version | `1.0-SNAPSHOT` |
| JDK 版本 | 21（兼容 17+，建议保持 21） |
| Node 版本 | 20+ |
| 包管理器（前端） | pnpm 9+（**禁止** npm 或 yarn） |
| 编码 | UTF-8 |

---

## 2. 顶层结构

```
sca-fullstack-lab/
├── README.md                       项目说明
├── LICENSE                         开源协议
├── spring-cloud-alibaba/           后端聚合工程（详见该目录下 CLAUDE.md）
│   ├── pom.xml                     父 POM（packaging=pom）
│   ├── src/                        仓库级共享资源
│   │   ├── checkstyle.xml          Checkstyle 规则（阿里规范 + 项目红线）
│   │   └── checkstyle-suppressions.xml
│   ├── spring-cloud-common/        公共能力下沉层（6 个子模块，packaging=pom）
│   ├── spring-cloud-gateway/       网关服务（端口 8080）
│   ├── spring-cloud-auth/          认证中心（端口 8081）
│   └── spring-cloud-services/      业务服务聚合（3 个微服务，packaging=pom）
├── vue-web-ui/                     前端 Monorepo（详见该目录下 CLAUDE.md）
│   ├── package.json                monorepo 根 package
│   ├── pnpm-workspace.yaml         workspace 声明
│   ├── apps/                       2 个独立部署应用（admin / portal）
│   └── packages/                   5 个共享包（ui / api / utils / types / uno-preset）
├── docker/                         基础设施 Compose
│   └── compose/                    docker-compose 文件（MySQL/Redis/Nacos/MinIO 等）
├── docs/                           设计文档（按编号组织）
│   ├── 01-项目概述.md
│   ├── 02-技术栈选型.md
│   ├── ...
│   └── requirements/               需求文档
└── scripts/                        辅助脚本（初始化、批量操作）
```

---

## 3. 技术栈基线（强制约束）

| 类别 | 技术 | 版本 | 用途 |
|------|------|------|------|
| 后端基座 | Spring Boot | 3.5.0 | 微服务基础 |
| 后端微服务规范 | Spring Cloud | 2025.0.0 | 微服务规范 |
| 后端微服务套件 | Spring Cloud Alibaba | 2025.0.0.0 | Nacos/Sentinel/Seata/Dubbo 集成 |
| 后端认证 | Sa-Token | 1.44.0 | 登录/权限/SSO/OAuth2 |
| 后端 ORM | MyBatis-Plus | 3.5.9 | ORM 增强 |
| 后端多数据源 | dynamic-datasource | 4.3.1 | 多源切换 |
| 后端分布式锁 | Redisson | 4.0.0 | 分布式锁/限流 |
| 后端本地缓存 | Caffeine | 3.2.0 | 本地缓存 |
| 后端分库分表 | Apache ShardingSphere | 5.5.2 | 数据分片 |
| 后端工具集 | Hutool | 5.8.27 | 通用工具 |
| 后端 HTML 解析 | Jsoup | 1.17.2 | XSS 过滤 |
| 后端密码哈希 | Bouncy Castle | 1.78.1 | Argon2id 算法 |
| 后端 API 文档 | springdoc-openapi | 2.6.0 | OpenAPI 3 |
| 后端 API 文档增强 | Knife4j | 4.5.0 | 聚合文档 UI |
| 后端测试容器 | Testcontainers | 1.20.0 | 集成测试 |
| 后端架构守护 | ArchUnit | 1.3.0 | 架构规则测试 |
| 后端 HTTP Mock | WireMock | 3.9.1 | 集成测试 |
| JDK | OpenJDK | 21 | 语言版本 |
| 前端框架 | Vue | 3.5+ | 视图框架 |
| 前端语言 | TypeScript | 5.5+ | 类型安全 |
| 前端构建 | Vite | 5.4+ | 极速 HMR + 构建 |
| 前端 UI 库 | Naive UI | 2.39+ | 组件库 |
| 前端状态 | Pinia | 2.2+ | 状态管理 |
| 前端路由 | Vue Router | 4.4+ | SPA 路由 |
| 前端 HTTP | axios | 1.7+ | API 调用 |
| 前端 CSS 引擎 | UnoCSS | 0.62+ | 原子 CSS |
| 前端图表 | ECharts + vue-echarts | 5.5+ / 7.0+ | 可视化 |
| 前端包管理器 | pnpm | 9+ | Monorepo |
| Node | LTS | 20+ | 运行时 |

> 后端所有依赖版本**必须**在父 POM `spring-cloud-alibaba/pom.xml` 的 `<properties>` 中统一声明，子模块**不允许**自行指定版本。
> 前端所有依赖版本**必须**在 `vue-web-ui/package.json` 或各子应用 `package.json` 中统一处理，**禁止**直接修改 `pnpm-lock.yaml`。

---

## 4. 跨项目契约（前后端必须对齐）

### 4.1 统一响应格式 `R<T>`

所有 HTTP 接口（除 OAuth2 标准 token 端点）**必须**返回 `R<T>` 包装类（来自 `spring-cloud-common-core`）：

```json
{
  "code": 200,
  "message": "success",
  "data": { ... },
  "timestamp": 1722470400000,
  "traceId": "a1b2c3d4e5f6g7h8"
}
```

`code` 为**业务状态码**（与 HTTP 状态码各司其职），按区段划分：

| code 区段 | 含义 | 典型 HTTP 状态 |
|-----------|------|---------------|
| 200 | 成功 | 200 |
| 1xxxx | 参数校验 / 请求格式错误 | 400 |
| 2xxxx | 用户认证 / 权限相关 | 401 / 403 / 404 |
| 3xxxx | 业务规则拦截（可恢复） | 409 / 422 / 429 |
| 4xxxx | 第三方服务错误 | 502 |
| 5xxxx | 系统内部错误（不可恢复） | 500 / 503 / 504 |

分页响应 `data` 统一为 `PageResult<T>`：`{records, total, page, size, pages, hasPrevious, hasNext}`；分页入参统一 `page`/`size`。

前端 axios 响应拦截器**必须**据此处理：`code === 200` 时返回 `data`；`code` 为登录态失效码（未登录/Token 过期/被禁用/被踢下线）时清理登录态并跳转登录页；其余失败显示 `message` 错误提示。

### 4.2 雪花 ID Long → String 序列化

后端 `spring-cloud-common-web` 的 `JacksonConfig` 把所有 `Long` 类型序列化为 String，避免前端 JS Number 精度丢失（最大安全整数 `2^53 - 1`，雪花 ID 超出）。

**前端约定**：所有 ID 字段（如 `userId`、`orderId`、`id`）**必须**用 TypeScript `string` 类型接收，**禁止**用 `number`。

### 4.3 HTTP 头透传

| 头 | 方向 | 用途 |
|----|------|------|
| `Authorization: Bearer {token}` | 前端 → 网关 | Sa-Token 访问令牌 |
| `X-Login-Id` | 网关 → 下游服务 | 透传当前登录用户 ID（网关鉴权后写入，下游直接读） |
| `X-Trace-Id` | 网关 → 下游服务 | 全链路追踪 ID（UUID，网关生成） |
| `X-Gray-Version` | 前端 → 网关（可选） | 灰度路由标识 |

**禁止**：网关把原始 Token 透传到下游服务（只透传 `X-Login-Id`）。

### 4.4 RESTful API 对齐

- 路径前缀：`/api/{服务名}/`（如 `/api/system/users`），由网关 `StripPrefix=2` 剥离
- 资源名复数、全小写、短横线分隔
- HTTP 方法语义：GET 查询、POST 新增、PUT 全量更新、PATCH 部分更新、DELETE 删除
- 业务动作用动词子资源：`POST /api/system/users/{id}/disable`
- **禁止**：GET 执行写操作、POST 同时承担新增和更新

详见 `spring-cloud-alibaba/CLAUDE.md` §8 RESTful API 强制规范。

### 4.5 CORS 白名单

- 后端网关 `CorsConfig` 白名单：开发 `http://localhost:5173`、生产 `https://*.example.com`
- **禁止** `Access-Control-Allow-Origin: *`，**必须**显式白名单
- `Allow-Credentials: true` 时**必须**用 `Origin-Patterns` 而非 `Origins`

---

## 5. 端口分配总表

### 5.1 后端服务

| 服务 | HTTP 端口 | Dubbo 端口 | WebSocket | XXL-JOB 执行器 |
|------|----------|-----------|-----------|---------------|
| spring-cloud-gateway | 8080 | - | - | - |
| spring-cloud-auth | 8081 | 20881 | - | 9999 |
| spring-cloud-system | 8082 | 20882 | - | 10000 |
| spring-cloud-article | 8093 | 20893 | - | 10011 |
| spring-cloud-comment | 8094 | 20894 | - | 10012 |

### 5.2 前端 dev server

| 应用 | dev 端口 |
|------|---------|
| 根级应用（`vue-web-ui/src`，@sca/web） | 5173 |

### 5.3 基础设施（Docker Compose）

| 组件 | 端口 | 用途 |
|------|------|------|
| MySQL | 3306 | 业务主库 |
| PostgreSQL | 5432 | pgvector 向量库 |
| Redis | 6379 | 分布式缓存 |
| MongoDB | 27017 | 文档库（对话/日志） |
| ElasticSearch | 9200 / 9300 | 全文检索 |
| Nacos | 8848 / 9848（gRPC） | 注册配置中心 |
| Sentinel Dashboard | 8858 | 限流熔断 |
| MinIO | 9000（API）/ 9001（控制台） | 对象存储 |
| TDengine | 6030 / 6041 | 时序库 |
| XXL-JOB Admin | 8099 | 调度中心 |
| Knife4j 文档 | 8080 | 网关聚合 |
| KKFileView | 8012 | 文件预览 |

---

## 6. 子项目索引

### 6.1 后端聚合工程

详见 [`spring-cloud-alibaba/CLAUDE.md`](./spring-cloud-alibaba/CLAUDE.md)。

包含 4 个一级子模块：
- `spring-cloud-common/` — 公共能力下沉层（6 个子模块），详见 [`spring-cloud-common/CLAUDE.md`](./spring-cloud-alibaba/spring-cloud-common/CLAUDE.md)
- `spring-cloud-gateway/` — 网关服务，详见 [`spring-cloud-gateway/CLAUDE.md`](./spring-cloud-alibaba/spring-cloud-gateway/CLAUDE.md)
- `spring-cloud-auth/` — 认证中心，详见 [`spring-cloud-auth/CLAUDE.md`](./spring-cloud-alibaba/spring-cloud-auth/CLAUDE.md)
- `spring-cloud-services/` — 业务服务聚合（3 个微服务：system/article/comment），详见 [`spring-cloud-services/CLAUDE.md`](./spring-cloud-alibaba/spring-cloud-services/CLAUDE.md)

### 6.2 前端 Monorepo

详见 [`vue-web-ui/CLAUDE.md`](./vue-web-ui/CLAUDE.md)。

包含：
- `src/` — 根级单一应用（包名 `@sca/web`，dev 端口 5173），承载公开博客 + 登录/注册 + dashboard 用户中心 + 管理页
- `packages/ui` — UI 二次封装
- `packages/api` — 统一 API 调用
- `packages/utils` — 工具函数
- `packages/types` — TS 类型
- `packages/uno-preset` — UnoCSS 预设

---

## 7. 全局代码规范

### 7.1 阿里巴巴 Java 开发规范

后端代码**必须**遵守阿里巴巴 Java 开发规范（泰山版）。强制规则由 `spring-cloud-alibaba/src/checkstyle.xml` 落实，绑定到 Maven `validate` 阶段，`failOnViolation=true`。

关键强制规则（违反则 build 失败）：
1. `NeedBraces`：`if`/`else`/`for`/`while`/`do` **必须**加大括号，即使只有一句
2. `Indentation`：缩进 4 空格，**禁止** Tab
3. `LineLength`：行宽 ≤ 120 字符
4. `MethodLength`：方法 ≤ 150 行
5. `ParameterNumber`：参数 ≤ 7 个
6. `NestedIfDepth`：if 嵌套深度 ≤ 3
7. `NestedTryDepth`：try 嵌套深度 ≤ 2
8. `MagicNumber`：禁止魔法数字（除 `-1/0/1/2`，字段声明/注解/hashCode 豁免）
9. `EqualsAvoidNull`：常量在 `equals` 左侧，避免空指针
10. `OneTopLevelClass`：一个 `.java` 文件只能有一个顶层类
11. `EmptyLineSeparator`：包/import/类/方法之间必须有空行
12. `AvoidStarImport`：**禁止** `import xxx.*`
13. `IllegalImport`：**禁止** `sun.*`
14. `IllegalThrows`：**禁止** 抛 `RuntimeException`/`Error`（必须具体业务异常）
15. `IllegalCatch`：**禁止** catch `Exception`/`Throwable`（必须精确捕获）
16. `RegexpSinglelineJava`：**禁止** `System.out/err.println`
17. `RegexpSinglelineJava`：**禁止** `e.printStackTrace()`
18. `RegexpSinglelineJava`：**禁止** `@Autowired` 字段注入（必须构造器注入）
19. `VisibilityModifier`：**禁止** public 字段（除 final）

### 7.2 前端代码规范

- **强制**使用 `<script setup>` 语法，**禁止** Options API
- **强制**用 Setup Store 风格的 Pinia
- **强制**用 `@sca/api` 包的 `request` 实例，**禁止**直接 `import axios`
- **强制**用 UnoCSS 原子类，**禁止**写大段 scoped CSS
- **强制**所有 URL 用环境变量 `import.meta.env.VITE_XXX`，**禁止**硬编码
- **强制**懒加载所有页面组件 `() => import('...')`
- **强制**组件卸载时清理 WebSocket / EventListener
- **强制**页面要尽可能使用 Naive UI 组件
- **强制**分支语句或者判断语句必须使用大括号 {} 包裹，`NeedBraces`：`if`/`else`/`for`/`while`/`do` **必须**加大括号，即使只有一句
- **强制**vue 文件中，从上到下的结构必须是<template></template>、<script setup></script>、<style></style>
- **强制**相同或类似的页面，要保持页面的风格样式统一，可以通过提取出公共样式来实现
- **强制**整个项目的页面风格要尽可能保持一致，需要有一个或多个全局的样式文件

### 7.3 Git 提交规范（Conventional Commits）

```
<type>(<scope>): <subject>

<body>

<footer>
```

| type | 用途 |
|------|------|
| feat | 新功能 |
| fix | 修复 bug |
| docs | 文档变更 |
| style | 代码格式（不影响逻辑） |
| refactor | 重构 |
| perf | 性能优化 |
| test | 测试 |
| chore | 构建/工具 |
| ci | CI 配置 |
| build | 构建系统或外部依赖 |

分支策略：Trunk-Based
- `main` — 主干（始终可发布）
- `feature/{模块}-{功能}` — 功能分支，如 `feature/auth-sso`
- `fix/{模块}-{问题}` — 修复分支
- `release/{版本号}` — 发布分支

---

## 8. 全局红线（违反即拒绝）

1. ❌ 在子模块 POM 中覆盖父 POM 的依赖版本
2. ❌ 用 `@Autowired` 字段注入（必须用 `@RequiredArgsConstructor` 构造器注入）
3. ❌ `if`/`else`/`for`/`while` 不加大括号（即使只有一句）
4. ❌ 用 `System.out.println` / `e.printStackTrace()`
5. ❌ 用 `throw new RuntimeException(...)` 而非具体业务异常
6. ❌ 用 `catch (Exception e)` 而非具体异常类型
7. ❌ 在日志/响应中泄露密码、Token、身份证号
8. ❌ 非 RESTful API（如 `POST /api/getUser?id=1`）
9. ❌ 用 GET 执行写操作
10. ❌ 前端用 `number` 类型接收 Long ID（必须 string，避免精度丢失）
11. ❌ 前端直接 `import axios from 'axios'`（必须用 `@sca/api`）
12. ❌ 前端用 Options API（必须 `<script setup>`）
13. ❌ 硬编码 SSO/API 地址（必须环境变量）
14. ❌ 提交 `.env` 文件、密钥、Token 到 Git
15. ❌ 直接 push 到 `main` 分支（必须走 PR）

---

## 9. 常用命令

### 9.1 后端

```bash
# 编译整个聚合工程
mvn clean install -DskipTests

# 编译并测试单个模块
mvn clean test -pl spring-cloud-auth -am

# 启动单个服务（开发模式）
mvn spring-boot:run -pl spring-cloud-auth -Dspring-boot.run.profiles=dev

# 查看依赖树
mvn dependency:tree -pl spring-cloud-system

# 仅跑 checkstyle（阿里规范验证）
mvn checkstyle:check -pl spring-cloud-auth

# 生成可执行 JAR
mvn clean package -pl spring-cloud-system -am -DskipTests
```

### 9.2 前端

```bash
# 安装依赖（在 vue-web-ui 根目录）
pnpm install

# 开发模式启动 admin
pnpm dev:admin

# 构建所有应用
pnpm build

# 类型检查
pnpm typecheck

# Lint
pnpm lint
```

### 9.3 基础设施

```bash
# 启动基础设施容器
docker compose -f docker/compose/docker-compose.infra.yml up -d

# 查看容器状态
docker compose -f docker/compose/docker-compose.infra.yml ps

# 停止并清理
docker compose -f docker/compose/docker-compose.infra.yml down
```

---

## 10. 设计文档索引

仓库根 `docs/` 目录下有完整的设计文档：

| 编号 | 文档 |
|------|------|
| 01 | 项目概述 |
| 02 | 技术栈选型 |
| 03 | GitHub 调研 |
| 04 | 服务架构设计 |
| 05 | 单点登录与会话管理 |
| 06 | 多数据库与多数据源 |
| 07 | 部署与 DevOps |
| 08 | 学习路径与实施步骤 |
| 09 | 项目需求文档 |
| 10 | IDEA 与工程结构 |
| 11 | 数据编码与乱码巡检 |
| 12 | 注释规范 |

> AI 在做架构决策前，建议先读相关设计文档了解上下文。
