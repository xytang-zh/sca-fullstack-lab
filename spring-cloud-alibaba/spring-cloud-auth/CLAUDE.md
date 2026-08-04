# CLAUDE.md — spring-cloud-auth 认证中心

> 本文档面向 AI 编码助手，用于在 `spring-cloud-auth/` 目录下工作时提供模块约束、技术栈版本、功能清单与开发规范。
> 工作前**必须**先读取父目录的 [`spring-cloud-alibaba/CLAUDE.md`](../CLAUDE.md) 了解全局规范。

---

## 1. 服务定位

`spring-cloud-auth` 是整个 `sca-fullstack-lab` 项目的 **统一身份认证中心（SSO Server）**，承担：

1. **登录认证**：账号密码、手机短信、邮箱验证码、扫码登录
2. **SSO Server**：Sa-Token 模式二，颁发授权码 → Client 用 code 换 token → Server 校验
3. **踢人下线**：基于 Sa-Token 的 `kickout` API，按设备类型精准踢下线
4. **OAuth2 Server**：授权码模式、客户端凭证模式、Refresh Token
5. **会话治理**：在线用户列表、强制注销、Token 续期
6. **风控**：登录失败 5 次锁定 15 分钟、IP 黑名单、异地登录告警

| 维度 | 值                                                              |
|------|----------------------------------------------------------------|
| 服务名 | `spring-cloud-auth`                                            |
| HTTP 端口 | 8081                                                           |
| Dubbo 端口 | 20881                                                          |
| 顶级包 | `com.xytang.auth`                                              |
| 启动类 | `com.xytang.auth.SpringCloudAuthApplication`                   |
| 角色 | SSO Server + OAuth2 Server                                     |
| 依赖 | Redis、Bouncy Castle（Argon2id 密码哈希） |

---

## 2. 模块结构

```
spring-cloud-auth/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/xytang/auth/
    │   │   ├── SpringCloudAuthApplication.java        启动类
    │   │   ├── config/
    │   │   │   ├── SaTokenConfig.java            Sa-Token 主配置
    │   │   │   ├── SaSsoServerConfig.java         SSO Server 配置
    │   │   │   ├── SaOAuth2ServerConfig.java      OAuth2 Server 配置
    │   │   │   ├── RedisConfig.java               Redis 序列化
    │   │   │   ├── RedissonConfig.java            Redisson 客户端
    │   │   │   ├── CaffeineConfig.java            本地缓存
    │   │   │   ├── CaptchaConfig.java             滑动验证码
    │   │   │   ├── WebMvcConfig.java              Web MVC（CORS、拦截器）
    │   │   │   └── XxlJobConfig.java              XXL-JOB 执行器
    │   │   ├── controller/
    │   │   │   ├── AuthController.java            登录/注销/refresh
    │   │   │   ├── SsoServerController.java       SSO Server 入口
    │   │   │   ├── OAuth2ServerController.java     OAuth2 授权入口
    │   │   │   ├── OnlineUserController.java     在线用户管理
    │   │   │   └── CaptchaController.java         验证码
    │   │   ├── service/
    │   │   │   ├── AuthService.java              登录主流程
    │   │   │   ├── SsoServerService.java          SSO 授权码管理
    │   │   │   ├── OAuth2ServerService.java       OAuth2 客户端管理
    │   │   │   ├── OnlineUserService.java         在线用户查询
    │   │   │   ├── CaptchaService.java            验证码生成/校验
    │   │   │   ├── LoginRiskService.java          登录风控（失败计数、IP 黑名单）
    │   │   │   └── impl/                          Service 实现
    │   │   ├── rpc/
    │   │   │   └── UserRpcProvider.java           对外暴露 Dubbo 接口
    │   │   ├── mapper/
    │   │   │   └── SysUserMapper.java            用户查询（仅认证用，CRUD 在 system）
    │   │   ├── entity/
    │   │   │   └── AuthUser.java                  认证用用户实体（不含敏感信息）
    │   │   ├── dto/
    │   │   │   ├── LoginDTO.java                  登录入参
    │   │   │   ├── SmsLoginDTO.java
    │   │   │   ├── EmailLoginDTO.java
    │   │   │   └── OAuth2AuthorizeDTO.java
    │   │   ├── vo/
    │   │   │   ├── LoginVO.java                  登录返回（access_token + refresh_token）
    │   │   │   ├── OnlineUserVO.java
    │   │   │   └── OAuth2TokenVO.java
    │   │   ├── enums/
    │   │   │   ├── LoginTypeEnum.java             PASSWORD/SMS/EMAIL/QRCODE
    │   │   │   └── DeviceTypeEnum.java            PC/APP/WEB/MINI
    │   │   ├── exception/
    │   │   │   ├── AuthException.java            认证异常
    │   │   │   ├── CaptchaException.java
    │   │   │   └── LoginLockedException.java
    │   │   └── constant/
    │   │       ├── AuthConstants.java            Redis Key 前缀
    │   │       └── SaTokenConstants.java
    │   └── resources/
    │       ├── application.yml                    基础配置
    │       ├── application-dev.yml
    │       ├── application-prod.yml
    │       ├── bootstrap.yml                       Nacos 引导
    │       └── mapper/                              MyBatis XML
    └── test/
        └── java/com/xytang/auth/
            ├── SpringCloudAuthApplicationTests.java
            ├── AuthServiceTest.java
            └── SsoServerControllerTest.java
```

---

## 3. 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.5.0 | 基座 |
| Spring Cloud | 2025.0.0 | 微服务规范 |
| Spring Cloud Alibaba | 2025.0.0.0 | Nacos 注册/配置 |
| Sa-Token | 1.44.0 | 登录/SSO/OAuth2/踢人下线 |
| sa-token-spring-boot3-starter | 1.44.0 | Spring Boot 3 集成 |
| sa-token-sso | 1.44.0 | SSO 三模式 |
| sa-token-oauth2 | 1.44.0 | OAuth2 Server |
| sa-token-redis-jackson | 1.44.0 | Token 持久化 |
| Redisson | 4.0.0 | 分布式锁 + 登录失败计数 |
| Bouncy Castle | 1.78.1 | Argon2id 密码哈希 |
| spring-boot-starter-validation | 3.5.0 | 参数校验 |
| micrometer-registry-prometheus | 3.5.0 | 监控指标 |
| MyBatis-Plus | 3.5.9 | 用户查询 |

> 所有依赖**必须**通过父 POM 的 dependencyManagement 管理版本，子模块只声明 `groupId` 和 `artifactId`。

> ⚠️ 原规划的滑动验证码（tianai-captcha）、邮件验证码（spring-boot-starter-mail）、XXL-JOB 定时任务、Caffeine 本地缓存、Sa-Token alone-redis **当前 POM 未引入**，落地时按需补充。

---

## 4. POM 依赖清单

```xml
<dependencies>
    <!-- Spring Boot Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>

    <!-- Spring Cloud Alibaba -->
    <dependency>
        <groupId>com.alibaba.cloud</groupId>
        <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
    </dependency>
    <dependency>
        <groupId>com.alibaba.cloud</groupId>
        <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
    </dependency>
    <dependency>
        <groupId>com.alibaba.cloud</groupId>
        <artifactId>spring-cloud-starter-alibaba-sentinel</artifactId>
    </dependency>

    <!-- Sa-Token 全家桶 -->
    <dependency>
        <groupId>cn.dev33</groupId>
        <artifactId>sa-token-spring-boot3-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>cn.dev33</groupId>
        <artifactId>sa-token-sso</artifactId>
    </dependency>
    <dependency>
        <groupId>cn.dev33</groupId>
        <artifactId>sa-token-oauth2</artifactId>
    </dependency>
    <dependency>
        <groupId>cn.dev33</groupId>
        <artifactId>sa-token-redis-jackson</artifactId>
    </dependency>

    <!-- Redisson -->
    <dependency>
        <groupId>org.redisson</groupId>
        <artifactId>redisson-spring-boot-starter</artifactId>
    </dependency>

    <!-- Bouncy Castle：Argon2id 密码哈希 -->
    <dependency>
        <groupId>org.bouncycastle</groupId>
        <artifactId>bcprov-jdk18on</artifactId>
    </dependency>

    <!-- 内部公共模块 -->
    <dependency>
        <groupId>com.xytang</groupId>
        <artifactId>spring-cloud-common-core</artifactId>
    </dependency>
    <dependency>
        <groupId>com.xytang</groupId>
        <artifactId>spring-cloud-common-web</artifactId>
    </dependency>
    <dependency>
        <groupId>com.xytang</groupId>
        <artifactId>spring-cloud-common-redis</artifactId>
    </dependency>
    <dependency>
        <groupId>com.xytang</groupId>
        <artifactId>spring-cloud-common-satoken</artifactId>
    </dependency>
    <dependency>
        <groupId>com.xytang</groupId>
        <artifactId>spring-cloud-common-mybatis</artifactId>
    </dependency>

    <!-- MyBatis-Plus（仅查用户） -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- 监控 -->
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-registry-prometheus</artifactId>
    </dependency>

    <!-- 测试 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

> ⚠️ 原规划的滑动验证码（tianai-captcha）、邮件验证码（spring-boot-starter-mail）、XXL-JOB 定时任务（xxl-job-core）、Caffeine 本地缓存、Sa-Token alone-redis **当前 POM 未引入**，落地时按需补充。

---

## 5. 核心功能清单

### 5.1 登录模块

| 功能 | 接口 | 说明 |
|------|------|------|
| 账号密码登录 | `POST /sso/login` | 入参 LoginDTO（username、password、captcha、deviceId） |
| 短信登录 | `POST /sso/login/sms` | 入参 SmsLoginDTO（phone、code、deviceId） |
| 邮箱登录 | `POST /sso/login/email` | 入参 EmailLoginDTO（email、code、deviceId） |
| 扫码登录 | `POST /sso/login/qrcode/scan` | 扫码确认 |
| 二维码生成 | `GET /sso/login/qrcode` | 返回二维码 + token |
| 获取验证码 | `GET /sso/captcha` | 滑动验证码图片 |

### 5.2 SSO Server 模块

| 功能 | 接口 | 说明 |
|------|------|------|
| SSO 登录入口 | `GET /sso/auth` | 用户访问，校验 Server 端会话 |
| SSO 登录提交 | `POST /sso/auth` | 账号密码登录 |
| 授权码回调 | `GET /sso/code` | 颁发授权码 code |
| Code 换 Token | `POST /sso/code2session` | Client 调用，code 换 loginId + token |
| 全端注销 | `POST /sso/logout` | 一处注销，全端下线 |
| 单端注销 | `POST /sso/logoutByAlone` | 仅当前 Client 下线 |

### 5.3 OAuth2 模块

| 功能 | 接口 | 说明 |
|------|------|------|
| 授权页 | `GET /oauth2/authorize` | 第三方应用引导用户跳转 |
| 颁发 Token | `POST /oauth2/token` | code 换 access_token |
| 用户信息 | `GET /oauth2/userinfo` | 第三方获取用户信息 |
| Refresh Token | `POST /oauth2/refresh` | refresh_token 换新 access_token |
| 撤销 Token | `POST /oauth2/revoke` | 主动撤销 |

### 5.4 会话治理

| 功能 | 接口 | 说明 |
|------|------|------|
| 在线用户列表 | `GET /sso/online` | 分页查询，支持按用户名/IP/设备过滤 |
| 在线用户详情 | `GET /sso/online/{loginId}` | 单个用户的会话详情 |
| 强制下线 | `POST /sso/kickout` | 按 loginId + deviceType 踢下线 |
| 按 Token 踢下线 | `POST /sso/kickoutByToken` | 按 token 踢下线 |
| 封禁用户 | `POST /sso/ban/{loginId}` | 全端注销 + 标记封禁 |

### 5.5 风控模块

| 功能 | 实现 |
|------|------|
| 登录失败计数 | Redis Key `auth:login:fail:{username}`，TTL 15min，5 次锁定 |
| IP 黑名单 | Redis Key `auth:ip:blacklist:{ip}`，TTL 24h |
| 异地登录告警 | 上次登录 IP 与本次对比，跨省则触发异地登录告警 |
| 滑动验证码 | tianai-captcha，3 次失败后强制验证 |

---

## 6. Sa-Token 关键配置

### 6.1 application-dev.yml

```yaml
sa-token:
  token-name: Authorization
  token-prefix: Bearer
  timeout: 7200              # Access Token 2h
  active-timeout: -1
  is-concurrent: false        # 不允许同账号并发登录（关键：踢人下线）
  is-share: false             # 不共享 Token
  is-kickout: true            # 启用踢人下线
  token-style: random-128
  is-log: false
  jwt-secret-key: ${SA_TOKEN_JWT_SECRET:change-me-in-prod}
  is-read-cookie: false
  is-read-header: true

  # SSO 模式二
  sso:
    mode: 2
    title: SpringCloud 认证中心
    auth-server-url: http://localhost:8081/sso
    is-slo: true              # 单点注销
    slo-url: http://localhost:8081/sso/logout
    client-timeout: 60

  # OAuth2
  oauth2:
    is-code: true
    is-implicit: false         # 隐式模式已废弃
    is-client-credentials: true
    is-password: false         # 密码模式已废弃（OAuth2.1）
    refresh-token-timeout: 604800   # 7d
    access-token-timeout: 7200

  # 单独 Redis（Sa-Token 用独立 Redis，避免与业务缓存冲突）
  alone-redis:
    host: ${REDIS_HOST:127.0.0.1}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:}
    database: 1
```

### 6.2 SaTokenConfig.java 关键代码

```java
@Configuration
public class SaTokenConfig {

    @Bean
    public SaSsoServerConfigure ssoServerConfigure() {
        return new SaSsoServerConfigure()
            .setServerLoginHandle((loginId, password) -> {
                // 1. 校验滑动验证码
                captchaService.verify();
                // 2. 登录失败计数检查
                loginRiskService.checkLocked(username);
                // 3. 校验账号密码
                AuthUser user = authService.loginByPassword(loginId, password);
                // 4. StpUtil.login，绑定 deviceId
                StpUtil.login(user.getId(),
                    new SaLoginParameter()
                        .setDeviceType(DeviceTypeEnum.PC.name())
                        .setDeviceId(SaFoxUtil.getRandomString(32))
                        .setTimeout(7200));
                return SaSsoServerProcessor.instance.authorize(...);
            });
    }
}
```

---

## 7. 关键业务流程

### 7.1 SSO 模式二完整流程

```
1. 用户访问 Client A → 校验本地会话（未登录）
2. Client A 跳转 Server /sso/auth?redirect=admin
3. Server 校验 Server 端会话（未登录）→ 返回登录页
4. 用户提交账号密码 + 验证码
5. Server 校验通过 → StpUtil.login(loginId, deviceId)
6. Server 生成授权码 code → 302 跳回 Client A /sso/callback?code=xxx
7. Client A 用 code 调 Server /sso/code2session
8. Server 校验 code → 返回 loginId + token
9. Client A 保存本地会话 → 重定向回原页面
```

### 7.2 踢人下线流程

```java
// 场景一：同账号第二台设备登录，自动踢第一台
// 通过 sa-token: is-kickout: true + is-concurrent: false 实现

// 场景二：管理员主动踢下线
@PostMapping("/sso/kickout")
public R<Void> kickout(@RequestParam Long loginId,
                       @RequestParam(defaultValue = "PC") String deviceType) {
    StpUtil.kickout(loginId, deviceType);
    return R.ok();
}
```

### 7.3 Token 续期流程

```
1. 前端请求收到 401 code=40101（Token 过期）
2. 前端调 POST /sso/refresh，带 refresh_token
3. Server 校验 refresh_token 有效性
4. Server 颁发新 access_token（2h）+ 新 refresh_token（7d）
5. 前端替换 Token，重试原请求
```

> Refresh Token **必须是一次性使用**（用一次即失效），防止重放攻击。

---

## 8. 数据模型

### 8.1 MySQL 表

> ⚠️ `sys_user` 表由 `spring-cloud-system` 服务**维护**（DDL 与 CRUD），`spring-cloud-auth` **只读**该表做认证。表结构定义在 `spring-cloud-system` 的 Flyway 脚本中。

```sql
-- 仅供参考，实际 DDL 由 spring-cloud-system 维护
CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY COMMENT '雪花 ID',
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL COMMENT 'Argon2id 哈希（Bouncy Castle）',
    phone VARCHAR(20),
    email VARCHAR(100),
    status TINYINT DEFAULT 1 COMMENT '1=正常 0=禁用',
    last_login_ip VARCHAR(50),
    last_login_time DATETIME,
    create_time DATETIME,
    update_time DATETIME,
    del_flag TINYINT DEFAULT 0
);
```

> ⚠️ `password` 字段存储 Argon2id 哈希（含盐 + 参数），长度约 100 字符。**禁止**用 BCrypt/MD5/SHA1。

### 8.2 Redis Key 规范

| Key | 类型 | TTL | 说明 |
|-----|------|-----|------|
| `satoken:login:token:{token}` | String | 2h | Token → loginId |
| `satoken:login:session:{loginId}` | Hash | 2h | 用户会话（含 deviceId、deviceType、ip） |
| `satoken:login:last-active:{loginId}` | String | 2h | 最后活跃时间 |
| `auth:login:fail:{username}` | String | 15min | 登录失败计数 |
| `auth:login:lock:{username}` | String | 15min | 锁定标记 |
| `auth:captcha:{key}` | String | 5min | 滑动验证码 |
| `auth:sms:code:{phone}` | String | 5min | 短信验证码 |
| `auth:email:code:{email}` | String | 5min | 邮箱验证码 |
| `auth:ip:blacklist:{ip}` | String | 24h | IP 黑名单 |
| `auth:oauth2:code:{code}` | String | 60s | OAuth2 授权码 |
| `auth:oauth2:refresh:{token}` | String | 7d | Refresh Token |

---

## 9. 必须遵守的开发规范

### 9.1 安全规范（强制）

1. 密码**必须**用 Argon2id 哈希（Bouncy Castle，OWASP 推荐），**禁止**用 BCrypt/MD5/SHA1
2. **禁止**在日志中打印密码、Token、身份证号（即使调试）
3. 登录失败信息**必须**模糊化：返回"用户名或密码错误"，**不区分**用户名不存在还是密码错误
4. Refresh Token **必须一次性使用**，用后立即失效
5. 验证码**必须**在登录成功后立即失效（即使没到 5 分钟 TTL）
6. 接口**必须**加 `@SaCheckPermission` 或 `@SaCheckRole`，仅 `登录/注销/获取验证码` 允许匿名访问
7. OAuth2 client_secret **必须**用 Argon2id 哈希存储
8. **禁止**跨域 `Access-Control-Allow-Origin: *`，必须显式白名单
9. Token 长度**必须**至少 64 字符（Sa-Token `random-128` 样式）

### 9.2 接口规范（RESTful 强制）

#### 9.2.1 URI 设计

- 业务接口路径前缀：`/sso/**`、`/oauth2/**`、`/auth/**`
- 路径用复数名词：`/sso/users/online`（在线用户列表）
- 业务动作用动词子资源：`POST /sso/kickout`、`POST /sso/users/{id}/ban`

#### 9.2.2 HTTP 方法

| 方法 | 语义 | 用法 |
|------|------|------|
| GET | 查询 | 获取在线用户、获取验证码、OAuth2 authorize |
| POST | 动作 | 登录、注销、踢下线、OAuth2 token |
| PATCH | 部分更新 | 修改密码 |
| DELETE | 删除 | 撤销 Token、删除 OAuth2 Client |

#### 9.2.3 统一响应

所有接口**必须**返回 `R<T>`：

```json
{
  "code": 200,
  "message": "success",
  "data": { "access_token": "xxx", "refresh_token": "yyy" },
  "timestamp": 1722470400000,
  "traceId": "a1b2c3d4e5f6g7h8"
}
```

> **例外**：OAuth2 `token` 接口遵循 RFC 6749，直接返回 `{"access_token":"xxx","token_type":"Bearer","expires_in":7200}`。

#### 9.2.4 状态码

| code | HTTP | 含义 |
|------|------|------|
| 200 | 200 | 成功 |
| 10001 | 400 | 参数错误（缺用户名/密码） |
| 21007 | 401 | Token 过期（可 Refresh） |
| 21008 | 401 | 被踢下线 |
| 21009 | 401 | 未登录 |
| 21010 | 401 | Refresh Token 无效 |
| 20002 | 403 | 无权限 |
| 21001 | 404 | 用户不存在 |
| 21002 | 409 | 用户名已存在 |
| 21006 | 423 | 登录失败次数过多，已锁定 |
| 50000 | 500 | 服务器内部错误 |

### 9.3 编码规范

1. **禁止**在 Controller 写业务逻辑
2. **禁止**在 Service 直接拼接 SQL
3. **必须**用 `@RequiredArgsConstructor` 构造器注入
4. **必须**用 `@Slf4j` 而非 `LoggerFactory.getLogger(...)`
5. **必须**用 `@OperationLog` 注解记录敏感操作（踢下线、封禁、修改密码）
6. **必须**用 `@DistributedLock` 注解防止并发登录问题
7. 异常**必须**继承 `AuthException`，由 `GlobalExceptionHandler` 统一捕获
8. **禁止**用 `throw new RuntimeException(...)`，必须用具体业务异常

### 9.4 测试规范

- **必须**写单元测试：`AuthServiceTest`、`CaptchaServiceTest`、`LoginRiskServiceTest`
- **必须**写集成测试：`SsoServerControllerTest`、`OAuth2ServerControllerTest`（用 Testcontainers 起 Redis）
- 测试覆盖率**必须** ≥ 70%

---

## 10. 配置文件规范

### 10.1 bootstrap.yml

```yaml
spring:
  application:
    name: spring-cloud-auth
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_ADDR:127.0.0.1:8848}
        namespace: ${NACOS_NAMESPACE:public}
      config:
        server-addr: ${NACOS_ADDR:127.0.0.1:8848}
        file-extension: yaml
        shared-configs:
          - data-id: spring-cloud-shared.yaml
            refresh: true
```

### 10.2 application.yml

```yaml
server:
  port: 8081
  servlet:
    context-path: /
  tomcat:
    max-threads: 200
    accept-count: 100

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  metrics:
    tags:
      application: ${spring.application.name}

# Spring 监控（业务接口的 P99/QPS）
spring:
  mvc:
    async:
      request-timeout: 30s
```

### 10.3 Nacos 配置（spring-cloud-auth.yaml）

```yaml
sa-token:
  timeout: 7200
  is-concurrent: false
  is-kickout: true
  sso:
    mode: 2
    auth-server-url: http://auth.example.com/sso
    is-slo: true

auth:
  login:
    max-fail-count: 5
    lock-minutes: 15
  captcha:
    enabled: true
    min-score: 0.8
  ip:
    blacklist-enabled: true
```

---

## 11. 必须实现的接口清单

| # | 方法 | 路径 | 说明 | 鉴权 |
|---|------|------|------|------|
| 1 | POST | `/sso/login` | 账号密码登录 | 匿名 |
| 2 | POST | `/sso/login/sms` | 短信登录 | 匿名 |
| 3 | POST | `/sso/login/email` | 邮箱登录 | 匿名 |
| 4 | GET | `/sso/captcha` | 滑动验证码 | 匿名 |
| 5 | POST | `/sso/logout` | 全端注销 | 已登录 |
| 6 | POST | `/sso/logoutByAlone` | 单端注销 | 已登录 |
| 7 | POST | `/sso/refresh` | Refresh Token | 已登录 |
| 8 | GET | `/sso/auth` | SSO 登录入口 | 匿名 |
| 9 | POST | `/sso/auth` | SSO 登录提交 | 匿名 |
| 10 | GET | `/sso/code` | 颁发授权码 | 已登录 |
| 11 | POST | `/sso/code2session` | Code 换 Token | Client 凭证 |
| 12 | GET | `/sso/online` | 在线用户列表 | @SaCheckRole(ADMIN) |
| 13 | GET | `/sso/online/{loginId}` | 在线用户详情 | @SaCheckRole(ADMIN) |
| 14 | POST | `/sso/kickout` | 强制下线 | @SaCheckPermission(auth:kickout) |
| 15 | POST | `/sso/users/{id}/ban` | 封禁用户 | @SaCheckPermission(auth:ban) |
| 16 | GET | `/oauth2/authorize` | OAuth2 授权页 | 已登录 |
| 17 | POST | `/oauth2/token` | 颁发 Token | Client 凭证 |
| 18 | GET | `/oauth2/userinfo` | 用户信息 | Bearer Token |
| 19 | POST | `/oauth2/refresh` | Refresh Token | Client 凭证 |
| 20 | POST | `/oauth2/revoke` | 撤销 Token | Client 凭证 |
| 21 | GET | `/actuator/health` | 健康检查 | 匿名 |
| 22 | GET | `/actuator/prometheus` | Prometheus 指标 | 内网 |

---

## 12. 红线（违反即拒绝）

1. ❌ 在日志中打印密码、Token、身份证号
2. ❌ 用 BCrypt/MD5/SHA1 存密码（必须 Argon2id）
3. ❌ 登录失败信息区分"用户名不存在"和"密码错误"（必须统一返回）
4. ❌ Refresh Token 多次使用（必须一次性）
5. ❌ 不加 `@SaCheckPermission`/`@SaCheckRole` 就暴露敏感接口
6. ❌ OAuth2 隐式模式（已废弃，OAuth2.1 不再支持）
7. ❌ OAuth2 密码模式（已废弃）
8. ❌ 用 `Access-Control-Allow-Origin: *`（必须显式白名单）
9. ❌ Controller 写业务逻辑（必须放 Service）
10. ❌ 用 `throw new RuntimeException(...)` 而非 AuthException
