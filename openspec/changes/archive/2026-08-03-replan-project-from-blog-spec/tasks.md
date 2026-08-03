# 任务清单：依据博客需求文档重新规划模块与文档体系（方案 B + 删除清单）

> 依据 specs/（project-structure、blog-domain、module-docs）与 design.md 实施。
> 本次变更交付：规划 + 删除清单执行 + 全量 CLAUDE.md 文档体系；不写业务代码。

## 1. 删除与博客无关的模块

- [x] 1.1 删除 `spring-cloud-alibaba/spring-cloud-services/spring-cloud-workflow/` 服务目录（`git rm -r`），并从 `spring-cloud-services/pom.xml` `<modules>` 移除；释放端口 8084/20884/10002
  - 验收：`ls spring-cloud-alibaba/spring-cloud-services/ | grep workflow` 无结果；`grep -c workflow spring-cloud-alibaba/spring-cloud-services/pom.xml` = 0
- [x] 1.2 删除 `spring-cloud-alibaba/spring-cloud-services/spring-cloud-ai/` 服务目录，并从聚合 POM 移除；释放端口 8085/20885/10003
  - 验收：`ls spring-cloud-alibaba/spring-cloud-services/ | grep "spring-cloud-ai"` 无结果
- [x] 1.3 删除 `spring-cloud-alibaba/spring-cloud-services/spring-cloud-report/` 服务目录，并从聚合 POM 移除；释放端口 8092/20892/10010
  - 验收：`ls spring-cloud-alibaba/spring-cloud-services/ | grep report` 无结果
- [x] 1.4 删除 `spring-cloud-alibaba/spring-cloud-common/spring-cloud-common-ai/` 子模块，并从 `spring-cloud-common/pom.xml` `<modules>` 移除
  - 验收：`ls spring-cloud-alibaba/spring-cloud-common/ | grep common-ai` 无结果
- [x] 1.5 删除 `spring-cloud-alibaba/spring-cloud-starters/spring-cloud-starter-sso-client/`（默认执行，见 8.2），并从聚合 POM 移除
  - 验收：`ls spring-cloud-alibaba/spring-cloud-starters/ | grep sso-client` 无结果
- [x] 1.6 残留引用检查：全仓 grep 已删服务名（workflow/spring-cloud-ai/report/common-ai/sso-client）的代码与配置引用（POM 依赖、bootstrap.yml、MQ 队列等），列出清单供后续收尾
  - 验收：`git grep -l "spring-cloud-workflow\|spring-cloud-ai\|spring-cloud-report\|common-ai\|sso-client" -- ":!*.md"` 输出清单（允许存在，记录即可）

## 2. 现有文档更新（根 + 后端聚合层）

- [x] 2.1 更新 `CLAUDE.md`（仓库根）：端口分配总表移除 workflow(8084)/ai(8085)/report(8092)、新增 article(8093/20893/10011)、comment(8094/20894/10012)；§1 服务数量 13→10（业务服务）；§6 子项目索引同步
  - 验收：`grep -c "spring-cloud-article" CLAUDE.md` ≥ 1 且 `grep -c "spring-cloud-ai\|spring-cloud-workflow\|spring-cloud-report" CLAUDE.md` = 0
- [x] 2.2 更新 `spring-cloud-alibaba/CLAUDE.md`：顶层模块结构图移除 3 个删除服务、加入 article/comment；端口分配表同步；§3.2 计划引入依赖表移除 Spring AI/Warm-Flow/JimuReport 条目；§15 子模块索引同步
  - 验收：`grep "spring-cloud-article" spring-cloud-alibaba/CLAUDE.md` 命中；`grep -c "Warm-Flow\|JimuReport" spring-cloud-alibaba/CLAUDE.md` = 0
- [x] 2.3 更新 `spring-cloud-alibaba/spring-cloud-services/CLAUDE.md`：子服务清单 11→10（删除 workflow/ai/report 章节 4.3/4.4/4.11，新增 4.9/4.10 为 article/comment）；§4 中 system 服务标注"裁剪为 RBAC 核心"；§5 通信表移除已删服务事件；新增子服务文档索引
  - 验收：`grep -c "spring-cloud-article\|spring-cloud-comment" spring-cloud-alibaba/spring-cloud-services/CLAUDE.md` ≥ 2 且无 workflow/ai/report 服务章节

## 3. 新增博客域服务文档（含职责与技术栈映射）

- [x] 3.1 新建 `spring-cloud-alibaba/spring-cloud-services/spring-cloud-article/CLAUDE.md`：定位（文章/分类/标签/点赞/收藏/阅读量/Markdown→HTML/XSS）、核心功能（对照 blog-domain spec）、技术栈（MyBatis-Plus、commonmark-java 计划、Jsoup、Redis/Redisson、Dubbo、Sa-Token）、关键接口草案、RESTful/缓存/事务规范、红线
  - 验收：文件存在且包含"文章""点赞/收藏""commonmark"等关键词
- [x] 3.2 新建 `spring-cloud-alibaba/spring-cloud-services/spring-cloud-comment/CLAUDE.md`：定位（评论/嵌套回复/审核/敏感词过滤/IP 记录）、核心功能、技术栈（MyBatis-Plus、sensitive-word 计划、Jsoup、Dubbo、Sa-Token）、审核状态机描述、红线
  - 验收：文件存在且含"敏感词""嵌套回复""PENDING"关键词

## 4. 补齐保留的 8 个业务服务文档（按 services/CLAUDE.md 职责 + D5 模板）

- [x] 4.1 新建 `spring-cloud-services/spring-cloud-system/CLAUDE.md`：收敛为 RBAC 核心（用户/角色/菜单/权限，博客 USER/AUTHOR/ADMIN 三角色）、技术栈、关键接口表、sys_* 表、红线；部门/岗位/字典/参数/通知标注"已移出职责边界"
  - 验收：文件存在且含"RBAC""sys_user"关键词，且无"字典"作为核心模块描述
- [x] 4.2 新建 `spring-cloud-services/spring-cloud-monitor/CLAUDE.md`：采集/存储(TDengine)/WebSocket 推送(9090)/告警/Prometheus、技术栈（OSHI/Netty/RabbitMQ）、关键接口与 WS 端点
  - 验收：文件存在且含"TDengine""OSHI""9090"关键词
- [x] 4.3 新建 `spring-cloud-services/spring-cloud-message/CLAUDE.md`：MQ 消费/WebSocket(9091)/站内信/评论通知/邮件/短信/客服、技术栈（AMQP/Netty/mail/短信 SDK/Redisson）、关键接口与 WS 端点
  - 验收：文件存在且含"站内信""9091"关键词
- [x] 4.4 新建（覆盖空壳）`spring-cloud-services/spring-cloud-search/CLAUDE.md`：索引/同步/搜索/聚合 + 新增搜索建议（前缀补全）、结果高亮、RSS 2.0（Redis 缓存）、ik 分词（计划）、技术栈
  - 验收：文件含"高亮""搜索建议""RSS""ik"关键词（对应 blog-domain 搜索与 RSS 需求）
- [x] 4.5 新建 `spring-cloud-services/spring-cloud-file/CLAUDE.md`：上传（分片/断点续传/秒传）/下载/预览(KKFileView)/管理/预签名、技术栈（MinIO SDK/Redisson）、关键接口表
  - 验收：文件存在且含"分片""预签名""MinIO"关键词
- [x] 4.6 新建 `spring-cloud-services/spring-cloud-log/CLAUDE.md`：操作/登录/审计日志、技术栈（Spring AOP/MongoDB/RabbitMQ/ShardingSphere 分表）、@OperationLog 规范
  - 验收：文件存在且含"@OperationLog""MongoDB""按月分表"关键词
- [x] 4.7 更新 `spring-cloud-services/spring-cloud-portal/CLAUDE.md`：轻量化边界声明（只读门户聚合 + SEO/GEO，写操作移交 article/comment）、文章/新闻/产品读接口、技术栈
  - 验收：文件含"只读""SEO""GEO"关键词，且声明博客写操作不在本服务
- [x] 4.8 新建 `spring-cloud-services/spring-cloud-job/CLAUDE.md`：XXL-JOB 任务清单（metricsAggregate/aiMessageArchive 移除、articleSyncToEs/统计聚合/RSS 定时生成）、技术栈（XXL-JOB 计划/Dubbo）、调度关系
  - 验收：文件存在且含"XXL-JOB""articleSyncToEsJob"关键词

## 5. 前端文档补齐

- [x] 5.1 更新 `vue-web-ui/CLAUDE.md`：应用与包清单改为 2 应用 + 5 包、博客域前端职责（apps/portal 博客前台、apps/admin 管理后台）、与后端服务映射
  - 验收：`grep -c "apps/\|packages/" vue-web-ui/CLAUDE.md` ≥ 7 且索引相对路径有效
- [x] 5.2 新建 `vue-web-ui/apps/admin/CLAUDE.md`：管理后台功能（用户/角色/菜单/博客审核/统计）、技术栈（Vue3/Naive UI/Pinia/Router/axios 经 @sca/api/ECharts）、路由懒加载规范
  - 验收：文件存在且含"博客审核""@sca/api""Naive UI"关键词
- [x] 5.3 新建 `vue-web-ui/apps/portal/CLAUDE.md`：门户前台功能（文章列表/详情/Markdown 渲染/评论/搜索/点赞收藏）、对接服务清单、SEO 说明
  - 验收：文件存在且含"Markdown""评论""搜索"关键词
- [x] 5.4 新建 `vue-web-ui/packages/ui/CLAUDE.md`：UI 二次封装组件清单、设计规范（UnoCSS 原子类）
- [x] 5.5 新建 `vue-web-ui/packages/api/CLAUDE.md`：request 实例约定、R<T> 拦截规则（code!==200 提示 msg）、ID 必须 string 类型
- [x] 5.6 新建 `vue-web-ui/packages/utils/CLAUDE.md`：工具函数清单与规范
- [x] 5.7 新建 `vue-web-ui/packages/types/CLAUDE.md`：类型定义约定（对齐 R<T>/PageVO 契约）
- [x] 5.8 新建 `vue-web-ui/packages/uno-preset/CLAUDE.md`：UnoCSS 预设配置说明
  - 验收：`ls vue-web-ui/packages/*/CLAUDE.md` 命中 5 个包

## 6. 基础设施与文档目录

- [x] 6.1 新建 `docker/CLAUDE.md`：compose 文件清单、各组件端口（MySQL/Redis/Nacos/ES/MinIO/XXL-JOB 等）、启动/停止命令
  - 验收：文件存在且含"docker-compose.infra.yml""3306""8848"关键词
- [x] 6.2 新建 `docs/CLAUDE.md`：设计文档索引（01-10 + requirements/）、文档编写规范
  - 验收：文件存在且含"01-项目概述"等文档名索引

## 7. 校验与收尾

- [x] 7.1 全仓 CLAUDE.md 覆盖率检查：排除 node_modules/target/dist/.git，核对 根(1) + 后端聚合(1+16+1+1+11+2=32) + 前端(1+2+5=8) + docker/docs(2) = 43 份
  - 验收：`find . -name CLAUDE.md -not -path "*/node_modules/*" -not -path "*/target/*" -not -path "*/dist/*" -not -path "*/.git/*" | wc -l` 输出 43
- [x] 7.2 索引链接有效性检查：抽查根→聚合→叶子导航链，验证相对路径链接指向真实文件
  - 验收：提取 CLAUDE.md 中的相对路径链接逐一 `ls` 命中
- [x] 7.3 技术栈版本一致性抽查：对比模块 CLAUDE.md 中版本号与父 POM `<properties>` / package.json
  - 验收：`grep -E "sa-token|mybatis-plus" spring-cloud-alibaba/pom.xml` 与文档版本一致
- [x] 7.4 通读评审：确认 blog-domain 全部需求（文章/评论/互动/搜索/RSS/RBAC）在对应服务文档中有覆盖映射
  - 验收：对照 specs/blog-domain 逐条核对无遗漏
- [x] 7.5 汇总变更清单（新增 2 / 修改 5 / 删除 5 模块）到变更根目录，供用户确认后提交
  - 验收：`openspec status --change replan-project-from-blog-spec` 显示全部 artifacts done

## 8. 待办确认（Open Questions）

- [x] 8.1 与用户确认 flow-web 应用去留：保留则后续创建应用骨架（不在本变更），不保留则根文档删除其声明（本变更内处理）
  - 验收：用户确认结论已落实到 `vue-web-ui/CLAUDE.md` 或根 `CLAUDE.md`
- [x] 8.2 与用户确认 `spring-cloud-starter-sso-client` 删除（默认删除）：保留则恢复 1.5 变更并从文档恢复其索引
  - 验收：用户确认结论已落实到聚合 POM 与文档
