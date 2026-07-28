# 07 · 部署与 DevOps

> 本文给出本项目的 **Docker 化部署**、**Nginx 反向代理**、**CI/CD 流水线**、**线上部署架构**。
> 目标：让你的项目能跑在云服务器上，给招聘方演示。

---

## 7.1 部署架构总览

```
互联网用户 → 域名解析 → 云服务器公网 IP
                         │
                         ▼
                    ┌─ Nginx ─┐
                    │ (443)   │  SSL 终止 + 反向代理
                    └────┬────┘
                         │
        ┌────────────────┼──────────────────┐
        │                │                  │
   ┌────▼────┐    ┌────▼──────┐      ┌────▼──────┐
   │ admin   │    │ portal    │      │ api       │
   │ 静态文件 │    │ 静态文件   │      │ Gateway   │
   │ (前端)  │    │ (前端)    │      │ :8080     │
   └─────────    └───────────┘      └─────┬─────┘
                                          │
                              ┌───────────┴───────────┐
                              │   微服务集群           │
                              │ auth/system/monitor/  │
                              │ workflow/ai/message/  │
                              │ search/file/log/      │
                              │ portal/job            │
                              └───────────┬───────────┘
                                          │
                              ┌───────────┴───────────┐
                              │  基础设施容器          │
                              │ Nacos/Redis/MQ/ES/    │
                              │ Mongo/TDengine/       │
                              │ MinIO/MySQL/PG         │
                              └───────────────────────┘
```

---

## 7.2 Docker Compose 基础设施

### 7.2.1 目录结构

```
aurora-backend/
├── docker/
│   ├── compose/
│   │   ├── docker-compose.infra.yml     (基础设施)
│   │   ├── docker-compose.services.yml  (微服务)
│   │   └── docker-compose.nginx.yml     (Nginx)
│   ├── nginx/
│   │   ├── nginx.conf
│   │   ├── conf.d/
│   │   │   ├── admin.conf
│   │   │   ├── portal.conf
│   │   │   └── api.conf
│   │   └── certs/
│   ├── mysql/
│   │   └── init/
│   ├── redis/
│   │   └── redis.conf
│   ├── rabbitmq/
│   │   └── enabled_plugins
│   ├── elasticsearch/
│   │   └── config/
│   ├── mongodb/
│   ├── tdengine/
│   ├── minio/
│   └── nacos/
```

### 7.2.2 docker-compose.infra.yml

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.4
    container_name: aurora-mysql
    restart: unless-stopped
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      TZ: Asia/Shanghai
    ports: [ "3306:3306" ]
    volumes:
      - mysql_data:/var/lib/mysql
      - ./mysql/init:/docker-entrypoint-initdb.d
      - ./mysql/conf.d:/etc/mysql/conf.d
    command: --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci

  postgres:
    image: pgvector/pgvector:pg16
    container_name: aurora-postgres
    restart: unless-stopped
    environment:
      POSTGRES_USER: ${PG_USER}
      POSTGRES_PASSWORD: ${PG_PASSWORD}
      POSTGRES_DB: aurora_ai
    ports: [ "5432:5432" ]
    volumes:
      - pg_data:/var/lib/postgresql/data
      - ./postgres/init:/docker-entrypoint-initdb.d

  redis:
    image: redis:7.4-alpine
    container_name: aurora-redis
    restart: unless-stopped
    ports: [ "6379:6379" ]
    command: redis-server /etc/redis/redis.conf --requirepass ${REDIS_PASSWORD}
    volumes:
      - redis_data:/data
      - ./redis/redis.conf:/etc/redis/redis.conf

  mongodb:
    image: mongo:7.0
    container_name: aurora-mongo
    restart: unless-stopped
    environment:
      MONGO_INITDB_ROOT_USERNAME: ${MONGO_USER}
      MONGO_INITDB_ROOT_PASSWORD: ${MONGO_PASSWORD}
    ports: [ "27017:27017" ]
    volumes:
      - mongo_data:/data/db

  rabbitmq:
    image: rabbitmq:3.13-management
    container_name: aurora-rabbitmq
    restart: unless-stopped
    environment:
      RABBITMQ_DEFAULT_USER: ${MQ_USER}
      RABBITMQ_DEFAULT_PASS: ${MQ_PASSWORD}
    ports:
      - "5672:5672"
      - "15672:15672"
    volumes:
      - rabbitmq_data:/var/lib/rabbitmq

  elasticsearch:
    image: elasticsearch:8.15.0
    container_name: aurora-es
    restart: unless-stopped
    environment:
      - discovery.type=single-node
      - ES_JAVA_OPTS=-Xms512m -Xmx512m
      - xpack.security.enabled=false
      - ELASTIC_PASSWORD=${ES_PASSWORD}
    ports:
      - "9200:9200"
      - "9300:9300"
    volumes:
      - es_data:/usr/share/elasticsearch/data

  tdengine:
    image: tdengine/tdengine:3.3.2.0
    container_name: aurora-tdengine
    restart: unless-stopped
    ports:
      - "6030:6030"
      - "6041:6041"
      - "6043-6049:6043-6049"
      - "6043-6049:6043-6049/udp"
    volumes:
      - tdengine_data:/var/lib/taos
      - tdengine_log:/var/log/taos

  minio:
    image: minio/minio
    container_name: aurora-minio
    restart: unless-stopped
    environment:
      MINIO_ROOT_USER: ${MINIO_USER}
      MINIO_ROOT_PASSWORD: ${MINIO_PASSWORD}
    command: server /data --console-address ":9001"
    ports:
      - "9000:9000"
      - "9001:9001"
    volumes:
      - minio_data:/data

  nacos:
    image: nacos/nacos-server:v2.4.3
    container_name: aurora-nacos
    restart: unless-stopped
    environment:
      MODE: standalone
      PREFER_HOST_MODE: hostname
      NACOS_AUTH_ENABLE: "true"
      NACOS_AUTH_IDENTITY_KEY: ${NACOS_AUTH_KEY}
      NACOS_AUTH_IDENTITY_VALUE: ${NACOS_AUTH_VALUE}
      NACOS_AUTH_TOKEN: ${NACOS_AUTH_TOKEN}
    ports:
      - "8848:8848"
      - "9848:9848"
    volumes:
      - nacos_logs:/home/nacos/logs

  xxl-job-admin:
    image: xuxueli/xxl-job-admin:3.5.0
    container_name: aurora-xxl-job-admin
    restart: unless-stopped
    depends_on: [ mysql ]
    environment:
      PARAMS: >-
        --spring.datasource.url=jdbc:mysql://mysql:3306/xxl_job?useUnicode=true&characterEncoding=utf8&useSSL=false
        --spring.datasource.username=root
        --spring.datasource.password=${MYSQL_ROOT_PASSWORD}
        --xxl.job.accessToken=${XXL_JOB_ACCESS_TOKEN}
    ports: [ "8099:8099" ]
    volumes:
      - xxl_job_logs:/data/applogs

  prometheus:
    image: prom/prometheus:v2.55.0
    container_name: aurora-prometheus
    restart: unless-stopped
    volumes:
      - ./prometheus/prometheus.yml:/etc/prometheus/prometheus.yml
      - ./prometheus/alert_rules.yml:/etc/prometheus/alert_rules.yml
      - prometheus_data:/prometheus
    ports: [ "9090:9090" ]
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.retention.time=30d'
      - '--web.enable-lifecycle'

  grafana:
    image: grafana/grafana:11.2.0
    container_name: aurora-grafana
    restart: unless-stopped
    environment:
      GF_SECURITY_ADMIN_USER: ${GRAFANA_USER}
      GF_SECURITY_ADMIN_PASSWORD: ${GRAFANA_PASSWORD}
      GF_INSTALL_PLUGINS: "tdengine-datasource"
    ports: [ "3000:3000" ]
    volumes:
      - grafana_data:/var/lib/grafana
      - ./grafana/provisioning:/etc/grafana/provisioning
      - ./grafana/dashboards:/var/lib/grafana/dashboards

volumes:
  mysql_data:
  pg_data:
  redis_data:
  mongo_data:
  rabbitmq_data:
  es_data:
  tdengine_data:
  tdengine_log:
  minio_data:
  nacos_logs:
  xxl_job_logs:
  prometheus_data:
  grafana_data:
```

### 7.2.3 启动基础设施

```bash
cd docker/compose
cp .env.example .env   # 修改密码
docker compose -f docker-compose.infra.yml up -d
```

---

## 7.3 微服务 Dockerfile

### 7.3.1 统一 Dockerfile 模板

```dockerfile
# 构建阶段
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
COPY . .
RUN mvn clean package -pl aurora-services/aurora-system -am -DskipTests

# 运行阶段
FROM eclipse-temurin:17-jre
LABEL maintainer="aurora"

WORKDIR /app
COPY --from=builder /build/aurora-services/aurora-system/target/aurora-system.jar app.jar

ENV TZ=Asia/Shanghai
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError"

EXPOSE 8082
ENTRYPOINT exec java $JAVA_OPTS -jar app.jar
```

### 7.3.2 docker-compose.services.yml（示例）

```yaml
version: '3.8'

services:
  gateway:
    build:
      context: ../..
      dockerfile: docker/Dockerfile.gateway
    container_name: aurora-gateway
    restart: unless-stopped
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - NACOS_ADDR=nacos:8848
    ports: [ "8080:8080" ]
    depends_on: [ nacos, redis ]

  auth:
    build:
      context: ../..
      dockerfile: docker/Dockerfile.auth
    container_name: aurora-auth
    restart: unless-stopped
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - NACOS_ADDR=nacos:8848
    ports: [ "8081:8081" ]

  # system / monitor / workflow / ai / ...
```

---

## 7.4 Nginx 配置

### 7.4.1 主配置

```nginx
user nginx;
worker_processes auto;
worker_rlimit_nofile 65535;

events {
  worker_connections 8192;
  use epoll;
}

http {
  include       mime.types;
  default_type  application/octet-stream;

  # 日志格式
  log_format main '$remote_addr - $remote_user [$time_local] '
                  '"$request" $status $body_bytes_sent '
                  '"$http_referer" "$http_user_agent" '
                  'rt=$request_time';

  sendfile        on;
  tcp_nopush      on;
  tcp_nodelay     on;
  keepalive_timeout  65;
  keepalive_requests 1000;

  # Gzip
  gzip on;
  gzip_min_length 1k;
  gzip_comp_level 6;
  gzip_types text/plain text/css application/json application/javascript text/xml application/xml application/xml+rss text/javascript;

  # 限流
  limit_req_zone $binary_remote_addr zone=api:10m rate=10r/s;

  include /etc/nginx/conf.d/*.conf;
}
```

### 7.4.2 一体化平台 admin.conf

```nginx
server {
  listen 443 ssl http2;
  server_name admin.example.com;

  ssl_certificate     /etc/nginx/certs/fullchain.pem;
  ssl_certificate_key /etc/nginx/certs/privkey.pem;
  ssl_protocols TLSv1.2 TLSv1.3;
  ssl_ciphers HIGH:!aNULL:!MD5;

  # 静态资源
  root /var/www/admin;
  index index.html;

  # 静态资源长缓存
  location ~* \.(?:js|css|woff2?|ttf|otf|eot|svg|png|jpg|jpeg|gif|webp|ico)$ {
    expires 1y;
    add_header Cache-Control "public, immutable";
    access_log off;
  }

  # HTML 不缓存
  location = /index.html {
    add_header Cache-Control "no-store, no-cache, must-revalidate";
  }

  # API 反向代理到 Gateway
  location /api/ {
    proxy_pass http://gateway:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;

    # 限流
    limit_req zone=api burst=20 nodelay;

    # 超时
    proxy_connect_timeout 5s;
    proxy_send_timeout 60s;
    proxy_read_timeout 60s;
  }

  # WebSocket
  location /ws/ {
    proxy_pass http://gateway:8080;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_read_timeout 3600s;
  }

  # 健康检查
  location /health {
    access_log off;
    return 200 "ok\n";
  }
}

# HTTP 跳 HTTPS
server {
  listen 80;
  server_name admin.example.com;
  return 301 https://$host$request_uri;
}
```

### 7.4.3 公开门户 portal.conf

```nginx
server {
  listen 443 ssl http2;
  server_name example.com www.example.com;

  ssl_certificate     /etc/nginx/certs/fullchain.pem;
  ssl_certificate_key /etc/nginx/certs/privkey.pem;

  root /var/www/portal;
  index index.html;

  # 博客文章
  location /blog/ {
    try_files $uri $uri/ /index.html;
  }

  # SEO：sitemap
  location = /sitemap.xml {
    root /var/www/portal;
    add_header Content-Type "application/xml";
  }

  # robots
  location = /robots.txt {
    root /var/www/portal;
  }

  # API
  location /api/ {
    proxy_pass http://gateway:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
  }
}
```

---

## 7.5 SSL 证书申请

### 7.5.1 Let's Encrypt（免费）

```bash
# 安装 certbot
apt install certbot python3-certbot-nginx

# 申请证书（自动改 nginx 配置）
certbot --nginx -d admin.example.com -d example.com -d auth.example.com

# 自动续期（crontab）
0 3 * * * certbot renew --quiet
```

### 7.5.2 acme.sh（更轻量）

```bash
curl https://get.acme.sh | sh
~/.acme.sh/acme.sh --issue -d admin.example.com --nginx
~/.acme.sh/acme.sh --install-cert -d admin.example.com \
  --key-file /etc/nginx/certs/privkey.pem \
  --fullchain-file /etc/nginx/certs/fullchain.pem \
  --reloadcmd "nginx -s reload"
```

---

## 7.6 CI/CD 流水线（GitHub Actions）

### 7.6.1 流水线设计

```
push 到 main 分支
  ↓
1. 代码检查（lint、checkstyle）
  ↓
2. 单元测试 + 集成测试
  ↓
3. 构建 JAR + Docker 镜像
  ↓
4. 推送到镜像仓库（阿里云 ACR / DockerHub）
  ↓
5. SSH 到服务器
  ↓
6. docker compose pull && up -d
  ↓
7. 健康检查
```

### 7.6.2 `.github/workflows/build.yml`

```yaml
name: Build & Deploy

on:
  push:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: maven

      - name: Build with Maven
        run: mvn -B clean package -DskipTests

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Login to Registry
        uses: docker/login-action@v3
        with:
          registry: registry.cn-hangzhou.aliyuncs.com
          username: ${{ secrets.DOCKER_USER }}
          password: ${{ secrets.DOCKER_PASS }}

      - name: Build & Push Gateway
        uses: docker/build-push-action@v5
        with:
          context: .
          file: docker/Dockerfile.gateway
          push: true
          tags: registry.cn-hangzhou.aliyuncs.com/aurora/gateway:latest

      # ... 同样构建其他服务

  deploy:
    needs: build
    runs-on: ubuntu-latest
    steps:
      - name: SSH Deploy
        uses: appleboy/ssh-action@v1.0.0
        with:
          host: ${{ secrets.SERVER_HOST }}
          username: ${{ secrets.SERVER_USER }}
          key: ${{ secrets.SERVER_SSH_KEY }}
          script: |
            cd /opt/aurora
            git pull
            docker compose -f docker/compose/docker-compose.services.yml pull
            docker compose -f docker/compose/docker-compose.services.yml up -d
            sleep 30
            curl -f http://localhost:8080/actuator/health || exit 1
```

---

## 7.7 前端部署

### 7.7.1 构建命令

```bash
# 一体化平台
cd aurora-admin
pnpm install
pnpm build           # 产物 dist/

# 公开门户
cd aurora-portal-frontend
pnpm build           # 静态生成 dist/
```

### 7.7.2 部署

把 `dist/` 拷贝到服务器 `/var/www/admin` 和 `/var/www/portal`：

```bash
rsync -avz --delete aurora-admin/dist/ user@server:/var/www/admin/
rsync -avz --delete aurora-portal-frontend/dist/ user@server:/var/www/portal/
```

或用 GitHub Actions 自动部署。

---

## 7.8 服务器配置建议（学习用最低配）

| 资源 | 最低 | 推荐 |
|------|------|------|
| CPU | 2 核 | 4 核 |
| 内存 | 4G（紧张） | 8G |
| 磁盘 | 40G SSD | 80G SSD |
| 带宽 | 3 Mbps | 5 Mbps |
| 系统 | Ubuntu 22.04 LTS | Ubuntu 24.04 LTS |

> ⚠️ 4G 内存跑全套会很卡，建议先跑：MySQL + Redis + Nacos + 网关 + auth + system（其他按需启动）。

**云服务推荐**：
- 阿里云 ECS / 腾讯云 CVM 轻量级（学生价便宜）
- 2C4G 5M 大约 80-100 元/月

---

## 7.9 监控告警（Prometheus + Grafana）

> Prometheus / Grafana 容器已包含在 7.2.2 `docker-compose.infra.yml` 中。本节讲配置与使用。

### 7.9.1 Spring Boot 暴露指标

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics,env
  metrics:
    tags:
      application: ${spring.application.name}
      env: ${spring.profiles.active}
  endpoint:
    health:
      show-details: when_authorized
  prometheus:
    metrics:
      step: 15s   # 暴露指标更新频率
```

### 7.9.2 Prometheus 拉取配置

```yaml
# prometheus/prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s
  external_labels:
    cluster: aurora
    env: prod

scrape_configs:
  - job_name: 'aurora-services'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets:
          - 'gateway:8080'
          - 'auth:8081'
          - 'system:8082'
          - 'monitor:8083'
          - 'workflow:8084'
          - 'ai:8085'
          - 'message:8086'
          - 'search:8087'
          - 'file:8088'
          - 'log:8089'
          - 'portal:8090'
          - 'job:8091'
          - 'report:8092'

  - job_name: 'node-exporter'
    static_configs:
      - targets: [ 'node-exporter:9100' ]

# 告警规则
rule_files:
  - alert_rules.yml

# 告警推送（可选 Alertmanager）
alerting:
  alertmanagers:
    - static_configs:
        - targets: [ 'alertmanager:9093' ]
```

### 7.9.3 告警规则示例

```yaml
# prometheus/alert_rules.yml
groups:
  - name: aurora-alerts
    rules:
      - alert: ServiceDown
        expr: up == 0
        for: 1m
        labels: { severity: critical }
        annotations:
          summary: "服务 {{ $labels.instance }} 已下线"
          description: "{{ $labels.job }} 不可达超过 1 分钟"

      - alert: HighCPUUsage
        expr: process_cpu_usage > 0.8
        for: 5m
        labels: { severity: warning }
        annotations:
          summary: "{{ $labels.application }} CPU 使用率 > 80% 持续 5 分钟"

      - alert: HighJVMMemory
        expr: jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} > 0.85
        for: 5m
        labels: { severity: warning }
        annotations:
          summary: "{{ $labels.application }} 堆内存使用率 > 85%"

      - alert: SlowHTTP
        expr: histogram_quantile(0.99, rate(http_server_requests_seconds_bucket[5m])) > 2
        for: 5m
        labels: { severity: warning }
        annotations:
          summary: "{{ $labels.application }} P99 响应时间 > 2s"
```

### 7.9.4 Grafana 大盘配置

**自动配置数据源（provisioning）**：

```yaml
# grafana/provisioning/datasources/datasources.yml
apiVersion: 1
datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true

  - name: TDengine
    type: tdengine-datasource
    access: proxy
    url: http://tdengine:6041
    jsonData:
      url: http://tdengine:6041
```

**自动导入大盘（provisioning）**：

```yaml
# grafana/provisioning/dashboards/dashboards.yml
apiVersion: 1
providers:
  - name: 'aurora-dashboards'
    folder: 'Aurora'
    type: file
    options:
      path: /var/lib/grafana/dashboards
```

**推荐大盘模板**（直接 import Dashboard ID）：

| 用途 | Dashboard ID |
|------|--------------|
| Spring Boot 2.6+ | 11378 |
| JVM Micrometer | 4701 |
| Node Exporter | 1860 |
| MySQL | 7362 |
| Redis | 11835 |
| RabbitMQ | 10991 |

### 7.9.5 自定义业务指标

```java
@Component
@RequiredArgsConstructor
public class OrderMetrics {
    private final MeterRegistry registry;

    public void recordOrderCreate(Order order) {
        registry.counter("aurora.order.create.total",
                "service", "aurora-order",
                "type", order.getType()
        ).increment();
    }

    public void recordOrderAmount(double amount) {
        registry.timer("aurora.order.amount.duration")
                .record(Duration.ofMillis((long) amount));
    }
}
```

---

## 7.10 JimuReport 部署

### 7.10.1 嵌入式部署（推荐学习）

JimuReport 以 Starter 方式嵌入到 `aurora-report` 服务，无需独立容器。见 `04-服务架构设计.md` 的 4.4.13 节。

### 7.10.2 独立部署（生产可选）

```yaml
# docker-compose.report.yml
services:
  jimureport:
    image: jeecgboot/jimureport:2.3.4
    container_name: aurora-jimureport
    restart: unless-stopped
    environment:
      - SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/jimureport?...
      - SPRING_DATASOURCE_USERNAME=root
      - SPRING_DATASOURCE_PASSWORD=${MYSQL_ROOT_PASSWORD}
      - SPRING_REDIS_HOST=redis
      - SPRING_REDIS_PORT=6379
      - SPRING_REDIS_PASSWORD=${REDIS_PASSWORD}
    ports: [ "8092:8092" ]
    volumes:
      - jimureport_data:/data
```

### 7.10.3 Nginx 反代 JimuReport

```nginx
# 报表子域名
server {
  listen 443 ssl http2;
  server_name report.example.com;

  ssl_certificate     /etc/nginx/certs/fullchain.pem;
  ssl_certificate_key /etc/nginx/certs/privkey.pem;

  location / {
    proxy_pass http://report:8092;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;

    # WebSocket 支持（设计器实时通信）
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
  }
}
```

---

## 7.11 XXL-JOB-Admin 部署

### 7.11.1 容器已包含

见 `docker-compose.infra.yml` 的 `xxl-job-admin` 服务。初始化 SQL：

```bash
# 从 xxl-job 官方仓库下载
wget https://github.com/xuxueli/xxl-job/raw/master/doc/db/tables_xxl_job.sql
docker exec -i aurora-mysql mysql -uroot -p$MYSQL_ROOT_PASSWORD < tables_xxl_job.sql
```

### 7.11.2 配置执行器

每个微服务（aurora-system、aurora-job、aurora-monitor 等）配置：

```yaml
xxl:
  job:
    admin:
      addresses: http://xxl-job-admin:8099
    executor:
      appname: aurora-${spring.application.name}
      address:
      ip:
      port: 9999
      logpath: /data/logs/xxl-job
      logretentiondays: 30
    accessToken: ${XXL_JOB_ACCESS_TOKEN}
```

### 7.11.3 Nginx 反代 XXL-JOB-Admin

```nginx
server {
  listen 443 ssl http2;
  server_name job.example.com;

  ssl_certificate     /etc/nginx/certs/fullchain.pem;
  ssl_certificate_key /etc/nginx/certs/privkey.pem;

  auth_basic "XXL-JOB Admin";
  auth_basic_user_file /etc/nginx/.htpasswd;   # 额外认证保护

  location / {
    proxy_pass http://xxl-job-admin:8099;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
  }
}
```

### 7.11.4 接入流程

1. 微服务加 `xxl-job-core` 依赖 + `XxlJobConfig` 配置类
2. 在 XXL-JOB Admin 的「执行器管理」中新建执行器（appname 与配置一致）
3. 在「任务管理」新建任务，Cron 表达式 + Handler 名（`@XxlJob("xxx")` 注解的 value）
4. 启动任务，观察执行日志

---

## 7.12 日志收集（EFK）

```
微服务 → logback 输出到文件 → Filebeat → Logstash → ElasticSearch → Kibana
```

学习项目可简化：所有服务日志直接输出到 Docker，用 `docker logs aurora-system` 查看。

---

## 7.11 安全加固清单

| 项 | 措施 |
|----|------|
| 防火墙 | ufw 只放 22、80、443 |
| SSH | 禁用 root 登录、改端口、密钥登录 |
| 数据库 | 只绑定 127.0.0.1，禁止公网访问 |
| Redis | 设置密码 + 禁用 KEYS / FLUSHALL |
| Nacos | 启用鉴权、改默认密码 |
| Docker | 不要把 docker.sock 暴露给容器 |
| 文件权限 | application-prod.yml 用环境变量注入密码 |
| HTTPS | 全站强制 HTTPS（HSTS） |
| WAF | 接 Cloudflare 或阿里云 WAF |

---

## 7.12 一键部署脚本示例

```bash
#!/bin/bash
# deploy.sh

set -e

echo ">>> 拉取最新代码"
git pull

echo ">>> 启动基础设施"
docker compose -f docker/compose/docker-compose.infra.yml up -d
sleep 30

echo ">>> 启动微服务"
docker compose -f docker/compose/docker-compose.services.yml up -d --build
sleep 60

echo ">>> 健康检查"
for svc in gateway:8080 auth:8081 system:8082; do
  host="${svc%:*}"
  port="${svc#*:}"
  if curl -fs http://localhost:$port/actuator/health > /dev/null; then
    echo "$host OK"
  else
    echo "$host FAILED" && exit 1
  fi
done

echo ">>> 部署前端"
rsync -avz --delete ../aurora-admin/dist/ /var/www/admin/
rsync -avz --delete ../aurora-portal-frontend/dist/ /var/www/portal/

echo ">>> Reload Nginx"
docker exec aurora-nginx nginx -s reload

echo ">>> 部署完成"
```

---

下一步：[08 · 学习路径与实施步骤](./08-学习路径与实施步骤.md)
