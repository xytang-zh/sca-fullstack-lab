# Nacos 运维清理清单（simplify-backend-modules）

> 本清单用于人工核对 Nacos 远程配置（`NACOS_ADDR`，默认 127.0.0.1:8848）。仓库内不直接修改远程配置，需在 Nacos 控制台操作。

## 一、需删除的配置（Data ID）

以下 Data ID 对应已删除服务，SHALL 从 Nacos 中删除：

| Data ID | 对应服务 | 状态 |
|---------|---------|------|
| `spring-cloud-monitor.yaml` | monitor（已删） | 删除 |
| `spring-cloud-message.yaml` | message（已删） | 删除 |
| `spring-cloud-search.yaml` | search（已删） | 删除 |
| `spring-cloud-file.yaml` | file（已删） | 删除 |
| `spring-cloud-log.yaml` | log（已删） | 删除 |
| `spring-cloud-portal.yaml` | portal（已删） | 删除 |
| `spring-cloud-job.yaml` | job（已删） | 删除 |

## 二、需修改的配置（Data ID）

| Data ID | 修改内容 |
|---------|---------|
| `spring-cloud-gateway.yaml` | ① 删除路由：`monitor-service`、`message-service`、`search-service`、`file-service`、`log-service`、`portal-service`、`job-service`、`monitor-ws`、`message-ws`、`xxl-job-admin`；② 白名单删除 `/api/portal/**`；③ Sentinel 限流规则中已删服务路由条目删除 |
| `spring-cloud-shared.yaml` | ① 删除 RabbitMQ 相关配置（`spring.rabbitmq.*`，mq 模块已删除且 auth 已移除 MQ）；② Redis/Redisson 配置保留（common-redis 已并入 Redisson） |
| `spring-cloud-auth.yaml` | 删除 RabbitMQ 相关配置（如 `spring.rabbitmq.*`） |

## 三、注意事项

1. `spring-cloud-gateway.yaml` 中 `auth-service`、`system-service`、`article-service`、`comment-service` 路由保留
2. RabbitMQ 若后续无任何服务使用，可同步从 `docker/compose/docker-compose.infra.yml` 移除（见 7.2 待确认）
3. 清空后需触发 Nacos 配置发布，网关动态刷新路由（`spring.cloud.gateway` 配置如有变更需确认刷新机制）