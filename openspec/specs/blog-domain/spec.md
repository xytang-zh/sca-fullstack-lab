## Purpose

定义参照个人博客需求文档规划的博客内容域行为需求，覆盖文章、分类/标签、评论、互动（点赞/收藏）、搜索增强与 RSS 订阅，明确各功能的契约行为与验收标准，作为博客域服务落地与文档撰写的需求基线。

## Requirements

### Requirement: 文章管理

系统 SHALL 支持多用户发布博客文章，包含：Markdown 原文编辑与 HTML 渲染、草稿/待审核/已发布/已驳回状态流转、分类与标签关联、slug 唯一标识、封面图、置顶与阅读量统计。文章内容 SHALL 经过 XSS 过滤后存储。

#### Scenario: 发布文章

- **WHEN** 具有 AUTHOR 或 ADMIN 角色的用户提交已填写的文章（标题、Markdown 内容、分类、标签）
- **THEN** 系统保存文章并渲染 HTML，状态为 PUBLISHED（AUTHOR/ADMIN）或 AUDIT（USER），返回文章详情

#### Scenario: 草稿恢复

- **WHEN** 用户查看自己的草稿文章列表并打开某篇草稿
- **THEN** 系统返回 Markdown 原文，渲染状态为 DRAFT 的文章不对外可见

#### Scenario: 文章状态审核

- **WHEN** 管理员对待审核文章执行通过/驳回操作
- **THEN** 文章状态更新为 APPROVED（转为已发布）或 REJECTED，作者可见审核结果

### Requirement: 分类与标签

系统 SHALL 提供分类（唯一名称、URL 别名、排序、描述）与标签（唯一名称、URL 别名）的管理，文章可关联一个分类与多个标签，列表页 SHALL 支持按分类/标签过滤。

#### Scenario: 分类标签过滤

- **WHEN** 用户以 `category` 或 `tag` 参数请求文章列表
- **THEN** 返回仅含该分类/标签的文章分页结果

### Requirement: 评论管理

系统 SHALL 支持对文章发表评论与二级嵌套回复、评论审核（PENDING/APPROVED/REJECTED/DELETED 状态）、敏感词过滤与 IP/UA 记录，评论内容 SHALL 以过滤后的纯文本存储。

#### Scenario: 发表评论

- **WHEN** 登录用户对某篇文章提交评论内容
- **THEN** 系统过滤敏感词与 XSS 后存储评论，状态为 PENDING 或按配置直接 APPROVED，文章评论数 +1

#### Scenario: 嵌套回复

- **WHEN** 用户对某条一级评论发起回复
- **THEN** 系统记录父评论与被回复者信息，回复在评论列表中以二级缩进展示

#### Scenario: 评论审核

- **WHEN** 管理员对待审核评论执行通过/驳回/删除
- **THEN** 评论状态相应更新，驳回与删除的评论不再对外展示

### Requirement: 互动（点赞与收藏）

系统 SHALL 支持登录用户对文章/评论点赞（幂等，可取消）与收藏文章（幂等，可取消），SHALL 防止同一用户重复点赞/重复收藏，并同步更新文章点赞数与收藏数展示。

#### Scenario: 文章点赞幂等

- **WHEN** 同一用户对同一文章连续两次调用点赞
- **THEN** 第一次成功（点赞数 +1），第二次取消点赞（点赞数 -1），不产生重复记录

#### Scenario: 收藏与取消

- **WHEN** 用户收藏某篇文章后再次执行收藏
- **THEN** 收藏取消，收藏数 -1；用户可在个人中心查看已收藏列表

### Requirement: 全文搜索

系统 SHALL 提供基于 Elasticsearch 的文章全文检索，支持中文分词、搜索建议（前缀自动补全）、结果高亮与分页，且 SHALL 在文章发布/更新/删除后保持索引一致（增量同步 + 定时全量重建兜底）。

#### Scenario: 关键词搜索

- **WHEN** 用户提交关键词与分页参数搜索文章
- **THEN** 返回匹配文章分页结果，标题/摘要命中部分带高亮标记

#### Scenario: 搜索建议

- **WHEN** 用户输入前缀 `Spr`
- **THEN** 返回以 `Spr` 开头或相关的建议词列表（如 Spring）

#### Scenario: 索引一致性兜底

- **WHEN** 增量同步失败且定时补偿任务执行
- **THEN** 系统重试同步或全量重建索引，已发布文章可被搜索

### Requirement: RSS 订阅

系统 SHALL 提供 RSS 2.0 订阅源（最新已发布文章），输出标准 XML 格式，并对订阅源响应进行缓存。

#### Scenario: RSS 输出

- **WHEN** 客户端请求 RSS 地址
- **THEN** 返回 RSS 2.0 XML（含标题、链接、描述、发布时间），且高频请求命中缓存

### Requirement: 博客 RBAC 角色

系统 SHALL 支持 USER/AUTHOR/ADMIN 三种角色参与博客域：USER 仅可阅读与评论；AUTHOR 可直接发布文章；ADMIN 拥有审核、管理用户与全部管理操作。角色数据 SHALL 由 system 服务统一管理，鉴权由网关与各服务基于 Sa-Token 完成。

#### Scenario: 角色权限约束

- **WHEN** USER 角色用户尝试发布文章或执行审核操作
- **THEN** 发布操作进入 AUDIT 状态，审核操作被拒绝（403）
