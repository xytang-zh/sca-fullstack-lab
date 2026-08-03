# 实施汇总：依据个人博客需求文档重新规划模块与文档体系

> 变更：`replan-project-from-blog-spec`（方案 B：博客域拆分 + 删除清单，经用户确认）

## 模块变更清单

### 新增（2 服务 + 2 文档）

| 模块 | 说明 |
|------|------|
| `spring-cloud-services/spring-cloud-article/` | 博客文章服务（8093/20893/10011）：文章/分类/标签/Markdown/点赞收藏/阅读量/ES 索引同步 |
| `spring-cloud-services/spring-cloud-comment/` | 博客评论服务（8094/20894/10012）：评论/嵌套回复/审核/敏感词过滤 |

> 服务骨架（pom/启动类）由后续变更创建，本变更交付 CLAUDE.md 职责基线。

### 修改（5 处）

| 模块 | 变更 |
|------|------|
| `CLAUDE.md`（根） | 端口表 -3 +2、服务 13→10、flow-web 声明删除 |
| `spring-cloud-alibaba/CLAUDE.md` | 结构图/端口表/计划依赖/通信表/索引同步 |
| `spring-cloud-services/CLAUDE.md` | 清单 11→10、新增 article/comment 章节、system 裁剪标注、子服务索引 |
| `spring-cloud-services/spring-cloud-portal/` | 轻量化：只读门户 + SEO/GEO，写操作移交 article/comment |
| `vue-web-ui/CLAUDE.md` | 2 应用 + 5 包、博客域前端职责、目录结构同步 |

### 删除（5 处）

| 模块 | 理由 |
|------|------|
| `spring-cloud-services/spring-cloud-workflow/` | 需求文档 2.6：审核用状态机，不用工作流引擎 |
| `spring-cloud-services/spring-cloud-ai/` | 需求文档无 AI 功能 |
| `spring-cloud-services/spring-cloud-report/` | 统计面板用 admin 前端 ECharts |
| `spring-cloud-common/spring-cloud-common-ai/` | 随 ai 服务联动删除 |
| `spring-cloud-starters/spring-cloud-starter-sso-client/` | 博客无多系统 SSO 需求（用户确认） |

### 保留（10 个后端服务 + 15 公共子模块 + 1 Starter）

gateway / auth / system（裁剪为 RBAC 核心）/ monitor（用户确认保留）/ message（保留）/ search / file / log（保留）/ portal（轻量化）/ job；15 个 common 子模块；`spring-cloud-starter-monitor-agent`

### 文档体系（43 份 CLAUDE.md）

- 新增 20 份：article、comment、system、monitor、message、search、file、log、portal、job、monitor-agent、apps/admin、apps/portal、packages/ui、packages/api、packages/utils、packages/types、packages/uno-preset、docker、docs
- 更新 5 份：根、后端聚合、services 聚合、vue-web-ui、根端口表
- 覆盖率校验：43/43，索引链接无断链，版本与父 POM 一致

## 待办（后续变更承接）

1. article/comment 服务骨架与业务代码实现（tasks 3.x 之后）
2. portal 博客代码迁移至 article/comment（迁移完成后删除 portal 博客代码）
3. system 企业模块代码裁剪（部门/岗位/字典/参数/通知）
4. docs/ 历史文档中已删服务内容归档或修订（见 docs/CLAUDE.md 说明）
5. 父 POM 移除 Spring AI/Warm-Flow/JimuReport 相关 property（如有残留）
