# 10 · IDEA 与工程结构

> 本文给出**完整的项目目录树**，并手把手教你**用 IDEA 一次打开 /project 目录就能同时识别前后端、同窗口同时运行**。

---

## 10.1 项目顶层目录结构

```
sca-fullstack-lab/                            ← IDEA 打开的就是这个目录
│
├── .idea/                                 (IDEA 工程配置，含运行配置)
│   ├── runConfigurations/                 (预置运行配置)
│   │   ├── Gateway.run.xml
│   │   ├── AuthService.run.xml
│   │   ├── SystemService.run.xml
│   │   ├── Admin Front Dev.run.xml
│   │   ├── Portal Front Dev.run.xml
│   │   └── Start All.run.xml              (Compound，一次启动多个)
│   ├── misc.xml
│   ├── modules.xml
│   ├── vcs.xml
│   └── jsLibraryMappings.xml
│
├── .gitignore
├── .editorconfig
├── .gitattributes
├── README.md                              (项目总入口)
├── package.json                           (前端 monorepo 根 package，仅用于 IDEA 识别前端)
│
├── spring-cloud-alibaba/                  ← 后端 Maven 多模块根
│   ├── pom.xml                            (父 POM)
│   ├── spring-cloud-common/                     (公共模块)
│   │   ├── spring-cloud-common-core/
│   │   ├── spring-cloud-common-redis/
│   │   ├── spring-cloud-common-redisson/
│   │   ├── spring-cloud-common-mybatis/
│   │   ├── spring-cloud-common-datasource/
│   │   ├── spring-cloud-common-mq/
│   │   ├── spring-cloud-common-mongo/
│   │   ├── spring-cloud-common-es/
│   │   ├── spring-cloud-common-ai/
│   │   ├── spring-cloud-common-satoken/
│   │   ├── spring-cloud-common-security/
│   │   ├── spring-cloud-common-web/
│   │   ├── spring-cloud-common-log/
│   │   ├── spring-cloud-common-swagger/
│   │   ├── spring-cloud-common-cache/
│   │   ├── spring-cloud-common-netty/
│   │   └── spring-cloud-common-test/
│   ├── spring-cloud-gateway/                    (网关服务)
│   ├── spring-cloud-auth/                       (认证中心)
│   ├── spring-cloud-services/                   (业务服务)
│   │   ├── spring-cloud-system/
│   │   ├── spring-cloud-monitor/
│   │   ├── spring-cloud-workflow/
│   │   ├── spring-cloud-ai/
│   │   ├── spring-cloud-message/
│   │   ├── spring-cloud-search/
│   │   ├── spring-cloud-file/
│   │   ├── spring-cloud-log/
│   │   ├── spring-cloud-portal/
│   │   ├── spring-cloud-job/
│   │   └── spring-cloud-report/
│   ├── spring-cloud-starters/                   (自定义 Starter)
│   │   ├── spring-cloud-starter-sso-client/
│   │   └── spring-cloud-starter-monitor-agent/
│   └── spring-cloud-test/                       (集成测试)
│
├── vue-web-ui/                            ← 前端 pnpm monorepo 根
│   ├── package.json
│   ├── pnpm-workspace.yaml
│   ├── tsconfig.base.json
│   ├── uno.config.ts
│   ├── apps/
│   │   ├── admin/                         (一体化管理平台)
│   │   │   ├── package.json
│   │   │   ├── vite.config.ts
│   │   │   ├── tsconfig.json
│   │   │   ├── index.html
│   │   │   └── src/
│   │   ├── portal/                        (公开门户 SSG)
│   │   │   ├── package.json
│   │   │   └── ...
│   │   └── flow-web/                      (工作流子系统)
│   │       ├── package.json
│   │       └── ...
│   └── packages/
│       ├── ui/                            (Naive UI 二次封装)
│       ├── api/                           (统一 API 调用)
│       ├── utils/
│       ├── types/
│       └── uno-preset/                   (UnoCSS 预设)
│
├── docker/                                (部署相关)
│   ├── compose/
│   │   ├── docker-compose.infra.yml       (基础设施)
│   │   ├── docker-compose.services.yml    (微服务)
│   │   └── .env.example
│   ├── nginx/
│   │   ├── nginx.conf
│   │   └── conf.d/
│   ├── mysql/init/
│   ├── redis/redis.conf
│   ├── prometheus/
│   ├── grafana/
│   └── xxl-job/
│
├── docs/                                  (文档)
│   ├── 01-项目概述.md
│   ├── 02-技术栈选型.md
│   ├── 03-GitHub调研.md
│   ├── 04-服务架构设计.md
│   ├── 05-单点登录与会话管理.md
│   ├── 06-多数据库与多数据源.md
│   ├── 07-部署与DevOps.md
│   ├── 08-学习路径与实施步骤.md
│   ├── 09-项目需求文档.md
│   └── 10-IDEA与工程结构.md
│
└── scripts/                               (辅助脚本)
    ├── setup.sh                           (环境准备：JDK、Node、Docker 检查)
    ├── start-infra.sh                     (启动基础设施容器)
    ├── start-backend.sh                   (启动后端所有服务)
    ├── start-front.sh                     (启动前端 dev server)
    └── deploy.sh                          (部署线上)
```

---

## 10.2 为什么这样设计目录

### 10.2.1 设计目标
1. **IDEA 一次打开根目录**，能识别前后端所有模块
2. **同一窗口同时运行**前后端项目（用 Compound Run Configuration）
3. **目录边界清晰**：backend / front / docker / docs / scripts 各司其职
4. **不互相污染**：Maven 父 POM 不放到根目录（否则 IDEA 只识别后端）

### 10.2.2 关键点：根目录为什么不是 Maven 父 POM

❌ 反例：如果根目录放 `pom.xml`：
```
spring-cloud-project/
├── pom.xml           ← 父 POM 在根目录
├── backend/
└── front/            ← IDEA 会以为 front 是 Maven 子模块，识别错误
```
后果：IDEA 启动时把整个项目当 Maven 工程，前端目录无法被识别为 Node 项目。

✅ 正例：根目录放 `package.json`（前端 monorepo 根） + `backend/pom.xml`（后端父 POM）：
```
spring-cloud-project/
├── package.json      ← 根目录有 package.json，IDEA 识别为前端项目
├── backend/
│   └── pom.xml       ← 后端独立 Maven 工程
└── front/            ← 前端 pnpm workspace
```
IDEA 启动时：
- 自动识别 `backend/pom.xml` → 加载为 Maven 模块
- 自动识别 `package.json` 与 `front/package.json` → 加载为 Node 项目
- 在 Services / Run 面板都能看到

### 10.2.3 关键点：根目录的 package.json 内容

```json
{
  "name": "spring-cloud-project",
  "version": "1.0.0",
  "private": true,
  "packageManager": "pnpm@9.0.0",
  "scripts": {
    "dev:admin": "pnpm --filter admin dev",
    "dev:portal": "pnpm --filter portal dev",
    "dev:flow": "pnpm --filter flow-web dev",
    "build": "pnpm -r build",
    "lint": "pnpm -r lint"
  },
  "devDependencies": {
    "pnpm": "^9.0.0"
  }
}
```

> 这个 package.json 主要作用是：让 IDEA 把根目录识别为 Node 项目，并暴露 dev 命令。
> 真正的前端代码在 `front/` 下，用 pnpm workspace 管理。

---

## 10.3 IDEA 打开方式（按步骤操作）

### 10.3.1 准备环境

| 工具 | 版本要求 |
|------|----------|
| JDK | 17+（推荐 Temurin 17） |
| Maven | 3.9+ |
| Node.js | 20+ |
| pnpm | 9+ |
| IDEA | Ultimate 2024.1+（社区版不支持前端，但可以装 Vue 插件） |
| Docker Desktop | 24+ |
| Git | 2.40+ |

### 10.3.2 第一次打开

1. **File → Open**，选择 `spring-cloud-project` 根目录
2. IDEA 弹窗 "Trust Project?" → 点 **Trust**
3. IDEA 弹窗检测到 `package.json` → 选 **Attach Run Configurations** 或后续手动配置
4. IDEA 弹窗检测到 `backend/pom.xml` → 选 **Load Maven Projects** → 自动加载所有模块
5. **File → Settings → Languages & Frameworks → Node.js**
   - Node interpreter：选本地 Node 20+ 路径
   - Package manager：选 pnpm
6. **File → Settings → Plugins**，确认安装：
   - **Vue**（前端语法支持）
   - **Spring Boot**（后端运行支持）
   - **Docker**（容器集成）
   - **MongoDB**（MongoDB Explorer）

### 10.3.3 配置 Run Configurations

#### 10.3.3.1 后端服务运行配置

对每个微服务创建一个 `Spring Boot` Run Configuration：

| 字段 | 值                                                      |
|------|--------------------------------------------------------|
| Name | `Gateway`                                              |
| Module | `spring-cloud-gateway`                                 |
| Main class | `com.spring-cloud.gateway.AuroraGatewayApplication`    |
| Working directory | `$MODULE_WORKING_DIR$`                                 |
| Environment variables | `SPRING_PROFILES_ACTIVE=dev;NACOS_ADDR=127.0.0.1:8848` |
| Use classpath of module | `spring-cloud-gateway`                                 |

同样地为以下服务各创建一个：
- AuthService → `com.spring-cloud.auth.AuroraAuthApplication`
- SystemService → `com.spring-cloud.system.AuroraSystemApplication`
- MonitorService → `com.spring-cloud.monitor.AuroraMonitorApplication`
- WorkflowService → `com.spring-cloud.workflow.AuroraWorkflowApplication`
- AiService → `com.spring-cloud.ai.AuroraAiApplication`
- MessageService → `com.spring-cloud.message.AuroraMessageApplication`
- SearchService → `com.spring-cloud.search.AuroraSearchApplication`
- FileService → `com.spring-cloud.file.AuroraFileApplication`
- LogService → `com.spring-cloud.log.AuroraLogApplication`
- PortalService → `com.spring-cloud.portal.AuroraPortalApplication`
- JobService → `com.spring-cloud.job.AuroraJobApplication`
- ReportService → `com.spring-cloud.report.AuroraReportApplication`

#### 10.3.3.2 前端运行配置

| 字段 | 值                                   |
|------|-------------------------------------|
| Name | `Admin Front Dev`                   |
| Type | `npm`（不是 Node.js！）                  |
| Package.json | `spring-cloud-project/package.json` |
| Command | `run`                               |
| Scripts | `dev:admin`                         |
| Arguments | （空）                                 |
| Run | `$ProjectFileDir$`                  |

同样为 `Portal Front Dev`（Scripts=`dev:portal`）、`Flow Web Dev`（Scripts=`dev:flow`）。

#### 10.3.3.3 一键启动 Compound

`Run → Edit Configurations → + → Compound`：

| 字段 | 值 |
|------|-----|
| Name | `Start All (Dev)` |
| Before launch | (空) |
| Add | 选以下 Run Configurations： |
|  | - `Gateway` |
|  | - `AuthService` |
|  | - `SystemService` |
|  | - `Admin Front Dev` |

> ⚠️ 学习期不建议一次启 13 个服务，会撑爆电脑。
> 默认 Compound 只放：Gateway + Auth + System + Admin 前端 4 个就够用。
> 其他服务按需启动。

#### 10.3.3.4 用 Services 面板批量管理（推荐）

IDEA Ultimate 自带 **Services 面板**（底部）：

1. **View → Tool Windows → Services**
2. 弹出 "Configure services" → 把 `Spring Boot` 类型加进来
3. 所有 Spring Boot Run Configuration 会自动归到 `Spring Boot` 分组下
4. 右键分组 → **Start All** / **Stop All** 批量启停

### 10.3.4 让 IDEA 自动加载我们的 Run Configurations

把 `.idea/runConfigurations/*.xml` 提交到 Git（不是 .idea 全部，但 Run Configuration 文件要提交），团队所有人 clone 后直接就有运行配置。

`.idea/runConfigurations/Gateway.run.xml` 示例：

```xml
<component name="ProjectRunConfigurationManager">
  <configuration default="false" name="Gateway" type="SpringBootApplicationConfigurationType" factoryName="Spring Boot">
    <option name="ACTIVE_PROFILES" value="dev" />
    <module name="spring-cloud-gateway" />
    <option name="SPRING_BOOT_MAIN_CLASS" value="com.spring-cloud.gateway.AuroraGatewayApplication" />
    <option name="VM_PARAMETERS" value="-Dspring.profiles.active=dev" />
    <method v="2">
      <option name="Make" enabled="true" />
    </method>
  </configuration>
</component>
```

`.idea/runConfigurations/Admin Front Dev.run.xml` 示例：

```xml
<component name="ProjectRunConfigurationManager">
  <configuration default="false" name="Admin Front Dev" type="js.build_tools.npm">
    <package-json value="$PROJECT_DIR$/package.json" />
    <command value="run" />
    <scripts value="dev:admin" />
    <node-interpreter value="project" />
    <package-manager value="pnpm" />
    <method v="2" />
  </configuration>
</component>
```

---

## 10.4 后端 Maven 多模块设计

### 10.4.1 父 POM（`backend/pom.xml`）

```xml
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.spring-cloud</groupId>
  <artifactId>spring-cloud-backend</artifactId>
  <version>1.0.0-SNAPSHOT</version>
  <packaging>pom</packaging>

  <modules>
    <module>spring-cloud-common</module>
    <module>spring-cloud-gateway</module>
    <module>spring-cloud-auth</module>
    <module>spring-cloud-services</module>
    <module>spring-cloud-starters</module>
    <module>spring-cloud-test</module>
  </modules>

  <properties>
    <java.version>17</java.version>
    <maven.compiler.source>17</maven.compiler.source>
    <maven.compiler.target>17</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>

    <spring-boot.version>3.5.0</spring-boot.version>
    <spring-cloud.version>2025.0.0</spring-cloud.version>
    <spring-cloud-alibaba.version>2025.0.0</spring-cloud-alibaba.version>
    <sa-token.version>1.44.0</sa-token.version>
    <warm-flow.version>1.8.8</warm-flow.version>
    <spring-ai.version>1.1.0</spring-ai.version>
    <mybatis-plus.version>3.5.9</mybatis-plus.version>
    <dynamic-datasource.version>4.3.1</dynamic-datasource.version>
    <redisson.version>4.0.0</redisson.version>
    <xxl-job.version>3.5.0</xxl-job.version>
    <shardingsphere.version>5.5.2</shardingsphere.version>
    <jimureport.version>2.3.4</jimureport.version>
    <hutool.version>5.8.27</hutool.version>
  </properties>

  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-dependencies</artifactId>
        <version>${spring-boot.version}</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
      <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-dependencies</artifactId>
        <version>${spring-cloud.version}</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
      <dependency>
        <groupId>com.alibaba.cloud</groupId>
        <artifactId>spring-cloud-alibaba-dependencies</artifactId>
        <version>${spring-cloud-alibaba.version}</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
      <!-- 其他依赖版本统一在这里管理 -->
    </dependencies>
  </dependencyManagement>

  <build>
    <pluginManagement>
      <plugins>
        <plugin>
          <groupId>org.springframework.boot</groupId>
          <artifactId>spring-boot-maven-plugin</artifactId>
          <version>${spring-boot.version}</version>
        </plugin>
      </plugins>
    </pluginManagement>
  </build>
</project>
```

### 10.4.2 单个业务服务 POM 示例（`spring-cloud-system`）

```xml
<project>
  <parent>
    <groupId>com.spring-cloud</groupId>
    <artifactId>spring-cloud-services</artifactId>
    <version>1.0.0-SNAPSHOT</version>
  </parent>
  <artifactId>spring-cloud-system</artifactId>
  <dependencies>
    <dependency><groupId>com.spring-cloud</groupId><artifactId>spring-cloud-common-core</artifactId></dependency>
    <dependency><groupId>com.spring-cloud</groupId><artifactId>spring-cloud-common-web</artifactId></dependency>
    <dependency><groupId>com.spring-cloud</groupId><artifactId>spring-cloud-common-mybatis</artifactId></dependency>
    <dependency><groupId>com.spring-cloud</groupId><artifactId>spring-cloud-common-redis</artifactId></dependency>
    <dependency><groupId>com.spring-cloud</groupId><artifactId>spring-cloud-common-satoken</artifactId></dependency>
    <dependency><groupId>com.spring-cloud</groupId><artifactId>spring-cloud-common-swagger</artifactId></dependency>
    <!-- Dubbo -->
    <dependency>
      <groupId>org.apache.dubbo</groupId>
      <artifactId>dubbo-spring-boot-starter</artifactId>
    </dependency>
    <!-- MyBatis-Plus -->
    <dependency>
      <groupId>com.baomidou</groupId>
      <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
    </dependency>
    <!-- EasyExcel -->
    <dependency>
      <groupId>com.alibaba</groupId>
      <artifactId>easyexcel</artifactId>
    </dependency>
  </dependencies>
  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
      </plugin>
    </plugins>
  </build>
</project>
```

### 10.4.3 后端标准服务模块结构

每个业务服务（如 spring-cloud-system）内部目录：

```
spring-cloud-system/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/spring-cloud/system/
    │   │   ├── AuroraSystemApplication.java       (启动类)
    │   │   ├── config/                            (配置类)
    │   │   │   ├── MybatisPlusConfig.java
    │   │   │   ├── RedisConfig.java
    │   │   │   └── ...
    │   │   ├── controller/                        (Controller)
    │   │   │   ├── UserController.java
    │   │   │   └── ...
    │   │   ├── service/                           (Service)
    │   │   │   ├── UserService.java
    │   │   │   ├── impl/
    │   │   │   │   └── UserServiceImpl.java
    │   │   │   └── ...
    │   │   ├── mapper/                            (MyBatis Mapper)
    │   │   │   ├── UserMapper.java
    │   │   │   └── ...
    │   │   ├── entity/                            (实体)
    │   │   │   ├── User.java
    │   │   │   └── ...
    │   │   ├── dto/                               (DTO)
    │   │   ├── vo/                                (VO)
    │   │   ├── enums/                             (枚举)
    │   │   ├── exception/                         (业务异常)
    │   │   ├── listener/                          (MQ 监听器)
    │   │   ├── rpc/                               (Dubbo Provider 实现)
    │   │   │   └── UserRpcProvider.java
    │   │   └── constant/                         (常量)
    │   └── resources/
    │       ├── application.yml
    │       ├── application-dev.yml
    │       ├── application-prod.yml
    │       ├── bootstrap.yml                     (Nacos 引导)
    │       ├── mapper/                            (XML 映射)
    │       │   └── UserMapper.xml
    │       ├── i18n/                              (国际化)
    │       └── db/migration/                     (Flyway 脚本)
    │           ├── V1.0.0__init_sys_tables.sql
    │           └── ...
    └── test/
        └── java/com/spring-cloud/system/
            ├── AuroraSystemApplicationTests.java
            └── ...
```

---

## 10.5 前端 Monorepo 设计

### 10.5.1 `front/pnpm-workspace.yaml`

```yaml
packages:
  - 'apps/*'
  - 'packages/*'
```

### 10.5.2 `front/package.json`

```json
{
  "name": "spring-cloud-front",
  "version": "1.0.0",
  "private": true,
  "type": "module",
  "scripts": {
    "dev:admin": "pnpm --filter @spring-cloud/admin dev",
    "dev:portal": "pnpm --filter @spring-cloud/portal dev",
    "dev:flow": "pnpm --filter @spring-cloud/flow-web dev",
    "build": "pnpm -r build",
    "lint": "pnpm -r lint"
  },
  "devDependencies": {
    "typescript": "^5.5.0",
    "vue-tsc": "^2.0.0"
  }
}
```

### 10.5.3 `front/apps/admin/` 目录

```
admin/
├── package.json
├── vite.config.ts
├── tsconfig.json
├── index.html
└── src/
    ├── main.ts
    ├── App.vue
    ├── api/                                (接口调用)
    │   ├── system/
    │   │   ├── user.ts
    │   │   └── ...
    │   └── request.ts                      (axios 封装)
    ├── components/                         (通用组件)
    ├── layouts/                            (布局)
    │   ├── default/
    │   │   ├── index.vue
    │   │   └── components/
    │   └── ...
    ├── views/                              (页面)
    │   ├── system/
    │   │   ├── user/index.vue
    │   │   ├── role/index.vue
    │   │   └── menu/index.vue
    │   ├── monitor/
    │   ├── workflow/
    │   ├── ai/
    │   ├── message/
    │   └── login/
    ├── router/                             (路由)
    │   ├── index.ts
    │   ├── permission.ts                   (动态路由)
    │   └── routes.ts
    ├── store/                              (Pinia)
    │   ├── user.ts
    │   ├── permission.ts
    │   └── ...
    ├── hooks/                              (组合式函数)
    ├── utils/
    ├── types/                              (TS 类型)
    ├── styles/                             (全局样式)
    │   ├── index.scss
    │   └── variables.scss
    ├── uno.config.ts                       (UnoCSS 配置)
    └── assets/
```

### 10.5.4 `front/apps/admin/vite.config.ts`

```typescript
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import UnoCSS from 'unocss/vite'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import NaiveUiResolver from 'unplugin-vue-components/resolvers'
import path from 'node:path'

export default defineConfig({
  plugins: [
    vue(),
    UnoCSS(),
    AutoImport({
      imports: ['vue', 'vue-router', 'pinia'],
      dts: 'src/auto-imports.d.ts'
    }),
    Components({
      resolvers: [NaiveUiResolver()],
      dts: 'src/components.d.ts'
    })
  ],
  resolve: {
    alias: { '@': path.resolve(__dirname, 'src') }
  },
  server: {
    port: 5173,
    host: '0.0.0.0',
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
      '/ws': { target: 'ws://localhost:8080', ws: true }
    }
  }
})
```

### 10.5.5 `front/apps/admin/package.json`

```json
{
  "name": "@spring-cloud/admin",
  "version": "1.0.0",
  "private": true,
  "scripts": {
    "dev": "vite",
    "build": "vue-tsc && vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "vue": "^3.5.0",
    "vue-router": "^4.4.0",
    "pinia": "^2.2.0",
    "naive-ui": "^2.39.0",
    "@vicons/ionicons5": "^0.12.0",
    "axios": "^1.7.0",
    "@vueuse/core": "^11.0.0",
    "echarts": "^5.5.0",
    "vue-echarts": "^7.0.0"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^5.0.0",
    "@unocss/preset-uno": "^0.62.0",
    "@unocss/preset-attributify": "^0.62.0",
    "unplugin-auto-import": "^0.18.0",
    "unplugin-vue-components": "^0.27.0",
    "typescript": "^5.5.0",
    "vue-tsc": "^2.0.0",
    "vite": "^5.4.0",
    "sass": "^1.77.0"
  }
}
```

---

## 10.6 `.gitignore`（根目录）

```gitignore
# IDE
.idea/*
!.idea/runConfigurations/
!.idea/codeStyles/
!.idea/inspectionProfiles/
!.idea/vcs.xml
!.idea/misc.xml
!.idea/jsLibraryMappings.xml
!.idea/modules.xml
.vscode/

# Java / Maven
target/
*.iml
*.ipr
*.iws
.mvn/
mvnw
HELP.md

# Node / Front
node_modules/
dist/
dist-ssr/
*.local
.pnpm-store/
.cache/

# Logs
logs/
*.log
npm-debug.log*
yarn-debug.log*
pnpm-debug.log*

# Env
.env
.env.local
!.env.example

# OS
.DS_Store
Thumbs.db

# Test
coverage/

# Docker
docker/data/
```

> 注意：`.idea/runConfigurations/` 不忽略（团队共享运行配置）。

---

## 10.7 让 IDEA 自动识别的最终秘诀

### 10.7.1 检查清单

打开项目后，按以下清单验证 IDEA 是否正确识别：

| 检查项 | 验证方法                                            |
|--------|-------------------------------------------------|
| 后端 Maven 模块 | 左侧 Project 树里能看到所有 spring-cloud-* 模块            |
| 前端 Node 项目 | 左侧 Project 树里能看到 `node_modules`（pnpm install 后） |
| Spring Boot Run Config | 顶部 Run 下拉里能看到 `Gateway`、`AuthService` 等         |
| npm Run Config | 顶部 Run 下拉里能看到 `Admin Front Dev`                 |
| Vue 语法高亮 | `.vue` 文件有颜色                                    |
| Java 语法 | `.java` 文件能跳转、能编译                               |
| Services 面板 | 底部 Services 能看到 Spring Boot 分组                  |

### 10.7.2 常见问题

| 问题 | 解决 |
|------|------|
| 后端模块没加载 | 右键 `backend/pom.xml` → Maven → Reload Project |
| 前端不识别 | 确认根目录 `package.json` 存在；File → Settings → Languages & Frameworks → Node.js 配置 |
| Vue 文件没有高亮 | Settings → Plugins → 安装 Vue 插件，重启 |
| Compound 启动报"找不到模块" | 先手动启动每个服务一次（编译 classpath），再 Compound |
| npm 配置丢失 | 右键 `package.json` → Show npm Scripts |
| pnpm 命令找不到 | File → Settings → Languages & Frameworks → Node.js → Package manager: pnpm |

### 10.7.3 推荐启动顺序（开发期）

1. 启动基础设施容器：`docker compose -f docker/compose/docker-compose.infra.yml up -d`
2. 启动 Redis、Nacos（已包含在 infra）
3. 启动后端 Gateway → Auth → System → 其他按需
4. 启动前端 `Admin Front Dev`
5. 浏览器打开 `http://localhost:5173`，登录，看到首页

---

## 10.8 一键启动脚本（`scripts/start-all.sh`）

```bash
#!/usr/bin/env bash
set -e

ROOT="$(cd "$(dirname "$0")/.." && pwd)"

echo ">>> [1/4] 启动基础设施容器"
docker compose -f "$ROOT/docker/compose/docker-compose.infra.yml" up -d
sleep 30  # 等 Nacos、MySQL 启动

echo ">>> [2/4] 启动后端核心服务（Gateway + Auth + System）"
# 用 mvn spring-boot:run 后台启动
cd "$ROOT/backend/spring-cloud-gateway" && mvn spring-boot:run -Dspring-boot.run.profiles=dev &
cd "$ROOT/backend/spring-cloud-auth"    && mvn spring-boot:run -Dspring-boot.run.profiles=dev &
cd "$ROOT/backend/spring-cloud-services/spring-cloud-system" && mvn spring-boot:run -Dspring-boot.run.profiles=dev &

echo ">>> [3/4] 等后端服务就绪"
sleep 40

echo ">>> [4/4] 启动前端 admin dev"
cd "$ROOT/front" && pnpm dev:admin &

echo ">>> 完成！"
echo "前端：http://localhost:5173"
echo "后端网关：http://localhost:8080"
echo "认证中心：http://localhost:8081"
echo "API 文档：http://localhost:8080/doc.html"
```

---

## 10.9 工程规范约定

### 10.9.1 包命名规范

```
com.xytang
  ├── common          (公共模块)
  │   ├── core
  │   ├── redis
  │   └── ...
  ├── gateway         (网关)
  ├── auth            (认证)
  ├── system          (系统管理)
  ├── monitor         (监控)
  ├── workflow        (工作流)
  ├── ai              (AI)
  ├── message         (消息)
  ├── search          (搜索)
  ├── file            (文件)
  ├── log             (日志)
  ├── portal          (门户)
  ├── job             (定时任务)
  └── report          (报表)
```

### 10.9.2 类命名规范

| 类型 | 后缀 | 示例 |
|------|------|------|
| 启动类 | `Aurora{服务}Application` | `AuroraSystemApplication` |
| Controller | `{业务}Controller` | `UserController` |
| Service 接口 | `{业务}Service` | `UserService` |
| Service 实现 | `{业务}ServiceImpl` | `UserServiceImpl` |
| Mapper | `{业务}Mapper` | `UserMapper` |
| Entity | `{业务}` (无后缀) | `User` |
| DTO | `{业务}DTO` | `UserDTO` |
| VO | `{业务}VO` | `UserVO` |
| Config | `{功能}Config` | `RedisConfig` |
| Exception | `{业务}Exception` | `BusinessException` |
| Listener | `{事件}Listener` | `UserLoginListener` |
| Dubbo Provider | `{业务}RpcProvider` | `UserRpcProvider` |

### 10.9.3 前端命名规范

| 类型 | 风格 | 示例 |
|------|------|------|
| 组件文件 | PascalCase.vue | `UserList.vue` |
| 组合式函数 | camelCase + use 前缀 | `useUser.ts` |
| 类型文件 | kebab-case | `user.d.ts` |
| 工具函数 | camelCase | `formatDate.ts` |
| API 模块 | kebab-case 目录 | `api/system/user.ts` |
| Pinia Store | camelCase + use 前缀 | `useUserStore.ts` |

---

## 10.10 团队协作约定

1. **Git 分支策略**：采用 Trunk-Based，主干 `main`，功能分支 `feature/xxx`
2. **提交规范**：Conventional Commits（`feat:` / `fix:` / `docs:` / `refactor:` / `chore:`）
3. **代码评审**：所有 PR 必须至少 1 人 review
4. **CI**：GitHub Actions 自动跑 lint + test + build
5. **文档同步**：改了功能必须改对应文档（09 章节里的服务接口）

---

下一步：[返回 README](../README.md)
