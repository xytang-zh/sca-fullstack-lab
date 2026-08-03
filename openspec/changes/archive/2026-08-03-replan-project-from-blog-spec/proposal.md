# 依据个人博客需求文档重新规划项目模块与文档体系

## Why

用户提供的《个人博客项目需求文档.md》是一份多用户博客平台（Spring Cloud Alibaba + SA-Token + Dubbo）的完整需求规格说明书，与当前 `sca-fullstack-lab`（企业级一体化智能管理平台）技术栈高度一致，但博客域能力（评论、互动、RSS、搜索增强）在当前项目中缺失或薄弱。同时，当前仓库存在文档缺口：11 个业务服务、前端 apps/packages、docker、docs 均无各自 CLAUDE.md。本次变更以博客需求文档为参照，重新规划模块拆分，并建立"每模块一份 CLAUDE.md"的完整文档体系。

## What Changes

- **模块拆分规划**：经用户确认采用方案 B（博客域拆分），并确认删除与博客无关的服务（workflow/ai/report）与模块（common-ai、starter-sso-client），system 裁剪为 RBAC 核心
- **博客域能力规划**：对照需求文档核心功能清单（用户、博客、评论、互动、搜索、RSS、后台管理），映射到当前项目服务，明确新增/修改/删除
- **CLAUDE.md 文档体系**：为缺失的模块（10 个业务服务、前端 apps/packages、docker、docs 等）补齐 CLAUDE.md，并更新现有文档保持索引一致；父模块文档记录子模块职责
- **技术栈映射**：每个功能点标注所用框架/技术栈，与父 POM 已声明版本对齐

## Capabilities

### New Capabilities

- `project-structure`: 项目模块/服务拆分结构决策（新增/修改/删除清单、端口与职责边界）
- `blog-domain`: 博客内容域行为需求（文章/分类标签/评论/互动/搜索/RSS 的契约与验收标准）
- `module-docs`: CLAUDE.md 文档体系规范（覆盖范围、每模块内容要求、父模块子模块索引规则）

### Modified Capabilities

（无 — `openspec/specs/` 当前为空，无既有 spec）

## Impact

**受影响模块（后端）**：`spring-cloud-alibaba/spring-cloud-services/` 下 10 个保留服务（system/monitor/message/search/file/log/portal/job + 新增 article/comment），**删除** workflow/ai/report 3 个服务；删除 `spring-cloud-common-ai` 与 `spring-cloud-starter-sso-client`；`spring-cloud-alibaba/CLAUDE.md`、`spring-cloud-services/CLAUDE.md` 同步更新

**受影响模块（前端）**：`vue-web-ui/apps/admin`、`vue-web-ui/apps/portal`、`vue-web-ui/packages/*`（api/types/ui/utils）、`vue-web-ui/CLAUDE.md`

**受影响模块（其他）**：`docker/`、`docs/`、`个人博客项目需求文档.md`（作为需求参照归档）、仓库根 `CLAUDE.md`

**不改变**：技术栈版本矩阵、跨端契约（R<T>/雪花 ID/HTTP 头/RESTful）、全局红线

## Non-goals

- 不删除与博客系统相关或经用户确认保留的服务（monitor/message/log/job/file/search/auth/gateway/portal/system）
- 不修改父 POM 依赖版本与红线规范
- 本次变更交付"规划 + 文档 + 删除清单执行"，不做新业务代码实现（文章/评论代码落地由 `/opsx:apply` 后的后续变更承接）
- 不按博客文档改写既有企业级功能需求（system 裁剪仅收敛职责边界，企业功能代码删除由后续变更承接）
