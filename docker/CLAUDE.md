# CLAUDE.md — docker 基础设施目录

> 本文档面向 AI 编码助手，用于在 `docker/` 目录下工作时提供基础设施约束、组件清单与运维命令。
> 工作前**必须**先读取仓库根 [`sca-fullstack-lab/CLAUDE.md`](../CLAUDE.md)。

---

## 1. 目录定位

`docker/` 存放基础设施 Docker Compose 编排文件与容器数据卷。博客系统的全部中间件由此一键启动。

```
docker/
└── compose/
    └── docker-compose.infra.yml   基础设施编排（MySQL/Redis/Nacos/ES/MinIO 等）
```

---

## 2. 组件清单（端口）

| 组件 | 端口 | 用途 |
|------|------|------|
| MySQL | 3306 | 业务主库（blog 库：auth/system/article/comment） |
| PostgreSQL | 5432 | （已随 ai 服务停用，可移除） |
| Redis | 6379 | 分布式缓存 / Sa-Token 存储 |
| MongoDB | 27017 | 日志存储（log 服务） |
| ElasticSearch | 9200 / 9300 | 全文检索（search 服务，ik 分词插件计划） |
| RabbitMQ | 5672（AMQP）/ 15672（管理） | 消息队列 |
| Nacos | 8848 / 9848（gRPC） | 注册配置中心 |
| Sentinel Dashboard | 8858 | 限流熔断 |
| MinIO | 9000（API）/ 9001（控制台） | 对象存储（文章图片） |
| XXL-JOB Admin | 8099 | 调度中心 |
| KKFileView | 8012 | 文件预览 |

---

## 3. 常用命令

```bash
# 启动基础设施容器
docker compose -f docker/compose/docker-compose.infra.yml up -d

# 查看容器状态
docker compose -f docker/compose/docker-compose.infra.yml ps

# 查看日志
docker compose -f docker/compose/docker-compose.infra.yml logs -f {service}

# 停止并清理
docker compose -f docker/compose/docker-compose.infra.yml down
```

---

## 4. 规范

1. **禁止**把业务数据库初始化脚本写进本目录（业务 SQL 在各服务 `src/main/resources/db/migration/`）
2. 端口**必须**与仓库根 `CLAUDE.md` §5.3 端口表一致
3. 容器卷**必须**挂载到 `docker/` 下的数据目录，便于备份
4. 新增中间件**必须**同步更新本文件与根 CLAUDE.md 端口表
