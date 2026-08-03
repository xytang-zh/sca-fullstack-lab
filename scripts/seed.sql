-- ============================================================
-- sca-fullstack-lab 种子数据（用户中心 dashboard 测试用）
-- 执行：mysql -u root -p sca_system < scripts/seed.sql
-- 密码统一：Admin@123（Argon2id 哈希，与客户端 PasswordEncoder 参数一致：
--   salt=16B / key=32B / parallelism=2 / memory=16384KB / iterations=3）
-- 所有 INSERT 使用 ON DUPLICATE KEY UPDATE 保证可重复执行
-- ============================================================

-- ===== 1. 用户（密码 Admin@123）=====
-- 1 admin 已由 V1.0.0 初始化，这里覆盖为 Argon2id 哈希（原 BCrypt 无法被 Argon2id 校验）
UPDATE sys_user SET password = '$argon2id$v=19$m=16384,t=3,p=2$DiiuHn4EwitHMUJ/h53PJg$dJTa44VvioZo/X4ySR8zcexBsakTVmv00LQU63dG+Po', nickname = '超级管理员', bio = '平台管理员，负责内容审核与用户管理。', avatar = NULL WHERE id = 1;

INSERT INTO sys_user (id, username, password, nickname, email, avatar, bio, status, create_time, update_time, version, deleted)
VALUES (2, 'alice01', '$argon2id$v=19$m=16384,t=3,p=2$6iTiCKg16AnK13FyBV2MNg$7vsVLOeqDEn1vLSDzZlvyEFWt9OEAhXqpV63ulf+qA0', 'Alice', 'alice@example.com', NULL, 'Spring Cloud Alibaba 实战爱好者，专注微服务与云原生。', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0)
ON DUPLICATE KEY UPDATE update_time = CURRENT_TIMESTAMP;

INSERT INTO sys_user (id, username, password, nickname, email, avatar, bio, status, create_time, update_time, version, deleted)
VALUES (3, 'bobuser', '$argon2id$v=19$m=16384,t=3,p=2$IYmhkACjzcQz00X+t51s0w$prL6eLnAhtjmidBqUaqIgofyleRHxZTadIGmzUc70dc', 'Bob', 'bob@example.com', NULL, '前端工程师，喜欢 Vue 与 TypeScript。', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0)
ON DUPLICATE KEY UPDATE update_time = CURRENT_TIMESTAMP;

INSERT INTO sys_user (id, username, password, nickname, email, avatar, bio, status, create_time, update_time, version, deleted)
VALUES (4, 'caroluser', '$argon2id$v=19$m=16384,t=3,p=2$AKQXgcnsnmvYGYOzPyvSDw$4NYeS6THMfs6hyqxN4hfXV7MmrgJh1CoZCAxySXqrg4', 'Carol', 'carol@example.com', NULL, '算法工程师，关注 AI 与分布式系统。', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0)
ON DUPLICATE KEY UPDATE update_time = CURRENT_TIMESTAMP;

-- ===== 2. 角色（补充 ADMIN 角色，博客三角色 USER/AUTHOR/ADMIN）=====
INSERT INTO sys_role (id, role_code, role_name, data_scope, sort, status, create_time, update_time, version, deleted)
VALUES (4, 'ADMIN', '管理员', 1, 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0)
ON DUPLICATE KEY UPDATE update_time = CURRENT_TIMESTAMP;

-- 用户-角色关联：1=ADMIN, 2=AUTHOR, 3=USER, 4=USER
INSERT INTO sys_user_role (user_id, role_id, create_time) VALUES (1, 4, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE create_time = CURRENT_TIMESTAMP;
INSERT INTO sys_user_role (user_id, role_id, create_time) VALUES (2, 3, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE create_time = CURRENT_TIMESTAMP;
INSERT INTO sys_user_role (user_id, role_id, create_time) VALUES (3, 2, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE create_time = CURRENT_TIMESTAMP;
INSERT INTO sys_user_role (user_id, role_id, create_time) VALUES (4, 2, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE create_time = CURRENT_TIMESTAMP;

-- ===== 3. 专栏 =====
INSERT INTO t_column (id, user_id, name, description, cover_image, status, create_time, update_time, version, deleted)
VALUES (3001, 2, '微服务实战', 'Spring Cloud Alibaba 微服务落地笔记', NULL, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0)
ON DUPLICATE KEY UPDATE update_time = CURRENT_TIMESTAMP;
INSERT INTO t_column (id, user_id, name, description, cover_image, status, create_time, update_time, version, deleted)
VALUES (3002, 2, '云原生与容器', 'Docker / K8s / DevOps 实践', NULL, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0)
ON DUPLICATE KEY UPDATE update_time = CURRENT_TIMESTAMP;
INSERT INTO t_column (id, user_id, name, description, cover_image, status, create_time, update_time, version, deleted)
VALUES (3003, 3, '前端工程化', 'Vue / Vite / 构建工具', NULL, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0)
ON DUPLICATE KEY UPDATE update_time = CURRENT_TIMESTAMP;
INSERT INTO t_column (id, user_id, name, description, cover_image, status, create_time, update_time, version, deleted)
VALUES (3004, 4, 'AI 与算法', '机器学习与分布式算法', NULL, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0)
ON DUPLICATE KEY UPDATE update_time = CURRENT_TIMESTAMP;

-- ===== 4. 文章（1001-1010 已发布，1011 草稿，1012 待审核）=====
INSERT INTO t_article (id, title, summary, content_md, status, author_id, slug, cover_image, column_id, views, likes, favorites, comments, publish_time, create_time, update_time, version, deleted)
VALUES (1001, 'Spring Cloud Alibaba 微服务骨架搭建', '从零搭建网关、认证中心与业务服务的完整记录。', '# 微服务骨架\n\n## 网关\n- Spring Cloud Gateway\n- 路由与限流\n\n## 认证中心\n- Sa-Token\n- 验证码登录', 3, 2, 'sca-microservice-skeleton', NULL, 3001, 1280, 156, 89, 24, DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 20 DAY), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0)
ON DUPLICATE KEY UPDATE update_time = CURRENT_TIMESTAMP;
INSERT INTO t_article (id, title, summary, content_md, status, author_id, slug, cover_image, column_id, views, likes, favorites, comments, publish_time, create_time, update_time, version, deleted)
VALUES (1002, 'Sa-Token 登录鉴权与验证码登录', '账号密码 + 文字验证码登录的完整实现与踩坑。', '# 登录鉴权\n\n- 文字验证码生成与校验\n- 一次性消费与过期\n- 防账号枚举', 3, 2, 'sa-token-login-with-captcha', NULL, 3001, 986, 123, 55, 18, DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 15 DAY), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0)
ON DUPLICATE KEY UPDATE update_time = CURRENT_TIMESTAMP;
INSERT INTO t_article (id, title, summary, content_md, status, author_id, slug, cover_image, column_id, views, likes, favorites, comments, publish_time, create_time, update_time, version, deleted)
VALUES (1003, 'Nacos 中配置热更新实践', '@RefreshScope 与配置中心的最佳实践。', '# 配置中心\n\n- Nacos 配置管理\n- @RefreshScope 动态刷新\n- 配置分层', 3, 2, 'nacos-config-refresh', NULL, 3001, 756, 88, 41, 12, DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 12 DAY), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0)
ON DUPLICATE KEY UPDATE update_time = CURRENT_TIMESTAMP;
INSERT INTO t_article (id, title, summary, content_md, status, author_id, slug, cover_image, column_id, views, likes, favorites, comments, publish_time, create_time, update_time, version, deleted)
VALUES (1004, 'Docker 化部署 Spring Boot 服务', '编写 Dockerfile 与 docker-compose 的多阶段构建。', '# Docker 部署\n\n- 多阶段构建减小镜像\n- 健康检查\n- 日志挂载', 3, 2, 'dockerize-spring-boot', NULL, 3002, 654, 77, 33, 9, DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 10 DAY), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0)
ON DUPLICATE KEY UPDATE update_time = CURRENT_TIMESTAMP;
INSERT INTO t_article (id, title, summary, content_md, status, author_id, slug, cover_image, column_id, views, likes, favorites, comments, publish_time, create_time, update_time, version, deleted)
VALUES (1005, 'Kubernetes 入门与资源清单', 'Pod、Deployment、Service 的核心概念。', '# K8s 入门\n\n- Pod 与生命周期\n- Deployment 滚动更新\n- Service 负载均衡', 3, 2, 'kubernetes-basics', NULL, 3002, 543, 66, 28, 7, DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 8 DAY), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0)
ON DUPLICATE KEY UPDATE update_time = CURRENT_TIMESTAMP;
INSERT INTO t_article (id, title, summary, content_md, status, author_id, slug, cover_image, column_id, views, likes, favorites, comments, publish_time, create_time, update_time, version, deleted)
VALUES (1006, 'Vue 3 组合式 API 实战', '<script setup> 与组合式函数的最佳实践。', '# Vue 3 组合式 API\n\n- script setup 语法\n- 组合式函数复用\n- 与 Pinia 联动', 3, 3, 'vue3-composition-api', NULL, 3003, 877, 110, 62, 15, DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 18 DAY), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0)
ON DUPLICATE KEY UPDATE update_time = CURRENT_TIMESTAMP;
INSERT INTO t_article (id, title, summary, content_md, status, author_id, slug, cover_image, column_id, views, likes, favorites, comments, publish_time, create_time, update_time, version, deleted)
VALUES (1007, 'Vite 构建优化与分包策略', 'manualChunks 与构建性能提升。', '# Vite 构建优化\n\n- 手动分包\n- 按需加载\n- 构建缓存', 3, 3, 'vite-build-optimization', NULL, 3003, 432, 55, 21, 6, DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 6 DAY), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0)
ON DUPLICATE KEY UPDATE update_time = CURRENT_TIMESTAMP;
INSERT INTO t_article (id, title, summary, content_md, status, author_id, slug, cover_image, column_id, views, likes, favorites, comments, publish_time, create_time, update_time, version, deleted)
VALUES (1008, 'Naive UI 主题定制与暗黑模式', '基于 Naive UI 的主题配置与暗黑模式切换。', '# Naive UI 主题\n\n- 主题变量\n- 暗黑模式\n- 动态切换', 3, 3, 'naive-ui-theme', NULL, 3003, 321, 44, 17, 4, DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 4 DAY), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0)
ON DUPLICATE KEY UPDATE update_time = CURRENT_TIMESTAMP;
INSERT INTO t_article (id, title, summary, content_md, status, author_id, slug, cover_image, column_id, views, likes, favorites, comments, publish_time, create_time, update_time, version, deleted)
VALUES (1009, '大模型提示词工程实践', '结构化提示词与上下文管理的技巧。', '# 提示词工程\n\n- 角色设定\n- 少样本示例\n- 输出约束', 3, 4, 'prompt-engineering', NULL, 3004, 1120, 142, 78, 20, DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 14 DAY), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0)
ON DUPLICATE KEY UPDATE update_time = CURRENT_TIMESTAMP;
INSERT INTO t_article (id, title, summary, content_md, status, author_id, slug, cover_image, column_id, views, likes, favorites, comments, publish_time, create_time, update_time, version, deleted)
VALUES (1010, '分布式一致性算法概览', 'Raft 与 Paxos 的核心思想简析。', '# 分布式一致性\n\n- Raft 选举与日志复制\n- Paxos 两阶段提交\n- 应用于实践', 3, 4, 'distributed-consensus', NULL, 3004, 689, 92, 47, 11, DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 5 DAY), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0)
ON DUPLICATE KEY UPDATE update_time = CURRENT_TIMESTAMP;
INSERT INTO t_article (id, title, summary, content_md, status, author_id, slug, cover_image, column_id, views, likes, favorites, comments, publish_time, create_time, update_time, version, deleted)
VALUES (1011, '（草稿）Sentinel 限流熔断实战', '草稿中的文章，尚未发布。', '# Sentinel（草稿）\n\n- 限流规则\n- 熔断降级', 1, 2, 'sentinel-draft', NULL, 3001, 0, 0, 0, 0, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0)
ON DUPLICATE KEY UPDATE update_time = CURRENT_TIMESTAMP;
INSERT INTO t_article (id, title, summary, content_md, status, author_id, slug, cover_image, column_id, views, likes, favorites, comments, publish_time, create_time, update_time, version, deleted)
VALUES (1012, '（待审核）Redis 缓存穿透与击穿', '刚提交的文章，等待管理员审核。', '# Redis 缓存\n\n- 穿透与布隆过滤器\n- 击穿与互斥锁', 2, 4, 'redis-cache-penetration', NULL, NULL, 0, 0, 0, 0, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0)
ON DUPLICATE KEY UPDATE update_time = CURRENT_TIMESTAMP;

-- ===== 5. 评论（4001-4008 一级评论，4009-4010 二级回复）=====
INSERT INTO t_comment (id, article_id, article_title, user_id, nickname, avatar, parent_id, reply_to_id, reply_to_nickname, content, status, ip, user_agent, likes, create_time, update_time, version, deleted)
VALUES (4001, 1001, 'Spring Cloud Alibaba 微服务骨架搭建', 3, 'Bob', NULL, 0, 0, '', '写得很清晰，正好需要参考。', 2, '127.0.0.1', 'Mozilla/5.0', 3, DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 19 DAY), CURRENT_TIMESTAMP, 0, 0)
ON DUPLICATE KEY UPDATE update_time = CURRENT_TIMESTAMP;
INSERT INTO t_comment (id, article_id, article_title, user_id, nickname, avatar, parent_id, reply_to_id, reply_to_nickname, content, status, ip, user_agent, likes, create_time, update_time, version, deleted)
VALUES (4002, 1001, 'Spring Cloud Alibaba 微服务骨架搭建', 4, 'Carol', NULL, 0, 0, '', '网关部分能再补充一个示例吗？', 2, '127.0.0.1', 'Mozilla/5.0', 5, DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 18 DAY), CURRENT_TIMESTAMP, 0, 0)
ON DUPLICATE KEY UPDATE update_time = CURRENT_TIMESTAMP;
INSERT INTO t_comment (id, article_id, article_title, user_id, nickname, avatar, parent_id, reply_to_id, reply_to_nickname, content, status, ip, user_agent, likes, create_time, update_time, version, deleted)
VALUES (4003, 1002, 'Sa-Token 登录鉴权与验证码登录', 3, 'Bob', NULL, 0, 0, '', '验证码一次性消费这个细节很关键。', 2, '127.0.0.1', 'Mozilla/5.0', 2, DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 14 DAY), CURRENT_TIMESTAMP, 0, 0)
ON DUPLICATE KEY UPDATE update_time = CURRENT_TIMESTAMP;
INSERT INTO t_comment (id, article_id, article_title, user_id, nickname, avatar, parent_id, reply_to_id, reply_to_nickname, content, status, ip, user_agent, likes, create_time, update_time, version, deleted)
VALUES (4004, 1006, 'Vue 3 组合式 API 实战', 2, 'Alice', NULL, 0, 0, '', '组合式函数抽取得很好。', 2, '127.0.0.1', 'Mozilla/5.0', 4, DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 17 DAY), CURRENT_TIMESTAMP, 0, 0)
ON DUPLICATE KEY UPDATE update_time = CURRENT_TIMESTAMP;
INSERT INTO t_comment (id, article_id, article_title, user_id, nickname, avatar, parent_id, reply_to_id, reply_to_nickname, content, status, ip, user_agent, likes, create_time, update_time, version, deleted)
VALUES (4005, 1009, '大模型提示词工程实践', 2, 'Alice', NULL, 0, 0, '', '少样本示例的例子很实用。', 2, '127.0.0.1', 'Mozilla/5.0', 3, DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 13 DAY), CURRENT_TIMESTAMP, 0, 0)
ON DUPLICATE KEY UPDATE update_time = CURRENT_TIMESTAMP;
INSERT INTO t_comment (id, article_id, article_title, user_id, nickname, avatar, parent_id, reply_to_id, reply_to_nickname, content, status, ip, user_agent, likes, create_time, update_time, version, deleted)
VALUES (4006, 1009, '大模型提示词工程实践', 3, 'Bob', NULL, 0, 0, '', '输出约束部分还能再展开。', 2, '127.0.0.1', 'Mozilla/5.0', 1, DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 12 DAY), CURRENT_TIMESTAMP, 0, 0)
ON DUPLICATE KEY UPDATE update_time = CURRENT_TIMESTAMP;
INSERT INTO t_comment (id, article_id, article_title, user_id, nickname, avatar, parent_id, reply_to_id, reply_to_nickname, content, status, ip, user_agent, likes, create_time, update_time, version, deleted)
VALUES (4007, 1010, '分布式一致性算法概览', 3, 'Bob', NULL, 0, 0, '', 'Raft 的选举讲得比较清楚。', 2, '127.0.0.1', 'Mozilla/5.0', 2, DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 4 DAY), CURRENT_TIMESTAMP, 0, 0)
ON DUPLICATE KEY UPDATE update_time = CURRENT_TIMESTAMP;
INSERT INTO t_comment (id, article_id, article_title, user_id, nickname, avatar, parent_id, reply_to_id, reply_to_nickname, content, status, ip, user_agent, likes, create_time, update_time, version, deleted)
VALUES (4008, 1003, 'Nacos 中配置热更新实践', 4, 'Carol', NULL, 0, 0, '', '@RefreshScope 的坑总结得很到位。', 2, '127.0.0.1', 'Mozilla/5.0', 0, DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 11 DAY), CURRENT_TIMESTAMP, 0, 0)
ON DUPLICATE KEY UPDATE update_time = CURRENT_TIMESTAMP;
INSERT INTO t_comment (id, article_id, article_title, user_id, nickname, avatar, parent_id, reply_to_id, reply_to_nickname, content, status, ip, user_agent, likes, create_time, update_time, version, deleted)
VALUES (4009, 1001, 'Spring Cloud Alibaba 微服务骨架搭建', 2, 'Alice', NULL, 4002, 4002, 'Carol', '好的，我后续补充一篇网关专题。', 2, '127.0.0.1', 'Mozilla/5.0', 1, DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 17 DAY), CURRENT_TIMESTAMP, 0, 0)
ON DUPLICATE KEY UPDATE update_time = CURRENT_TIMESTAMP;
INSERT INTO t_comment (id, article_id, article_title, user_id, nickname, avatar, parent_id, reply_to_id, reply_to_nickname, content, status, ip, user_agent, likes, create_time, update_time, version, deleted)
VALUES (4010, 1009, '大模型提示词工程实践', 4, 'Carol', NULL, 4006, 4006, 'Bob', '没问题，后面单独写一篇。', 2, '127.0.0.1', 'Mozilla/5.0', 0, DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 11 DAY), CURRENT_TIMESTAMP, 0, 0)
ON DUPLICATE KEY UPDATE update_time = CURRENT_TIMESTAMP;

-- ===== 6. 点赞记录（t_like_record）=====
INSERT INTO t_like_record (id, article_id, user_id, create_time) VALUES (5001, 1001, 3, DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 5 DAY)) ON DUPLICATE KEY UPDATE create_time = CURRENT_TIMESTAMP;
INSERT INTO t_like_record (id, article_id, user_id, create_time) VALUES (5002, 1001, 4, DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 4 DAY)) ON DUPLICATE KEY UPDATE create_time = CURRENT_TIMESTAMP;
INSERT INTO t_like_record (id, article_id, user_id, create_time) VALUES (5003, 1002, 3, DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 3 DAY)) ON DUPLICATE KEY UPDATE create_time = CURRENT_TIMESTAMP;
INSERT INTO t_like_record (id, article_id, user_id, create_time) VALUES (5004, 1006, 2, DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 6 DAY)) ON DUPLICATE KEY UPDATE create_time = CURRENT_TIMESTAMP;
INSERT INTO t_like_record (id, article_id, user_id, create_time) VALUES (5005, 1009, 2, DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 2 DAY)) ON DUPLICATE KEY UPDATE create_time = CURRENT_TIMESTAMP;
INSERT INTO t_like_record (id, article_id, user_id, create_time) VALUES (5006, 1009, 3, DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 1 DAY)) ON DUPLICATE KEY UPDATE create_time = CURRENT_TIMESTAMP;
INSERT INTO t_like_record (id, article_id, user_id, create_time) VALUES (5007, 1010, 3, DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 1 DAY)) ON DUPLICATE KEY UPDATE create_time = CURRENT_TIMESTAMP;

-- ===== 7. 收藏记录（t_favorite）=====
INSERT INTO t_favorite (id, article_id, user_id, create_time) VALUES (6001, 1001, 3, DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 5 DAY)) ON DUPLICATE KEY UPDATE create_time = CURRENT_TIMESTAMP;
INSERT INTO t_favorite (id, article_id, user_id, create_time) VALUES (6002, 1009, 3, DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 4 DAY)) ON DUPLICATE KEY UPDATE create_time = CURRENT_TIMESTAMP;
INSERT INTO t_favorite (id, article_id, user_id, create_time) VALUES (6003, 1006, 2, DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 3 DAY)) ON DUPLICATE KEY UPDATE create_time = CURRENT_TIMESTAMP;
INSERT INTO t_favorite (id, article_id, user_id, create_time) VALUES (6004, 1002, 4, DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 2 DAY)) ON DUPLICATE KEY UPDATE create_time = CURRENT_TIMESTAMP;

-- ===== 8. 关注关系（t_follow）=====
-- 2 关注 3、4；3 关注 2、4；4 关注 2（形成 2 的粉丝：3、4）
INSERT INTO t_follow (id, follower_id, followee_id, create_time) VALUES (7001, 2, 3, DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 10 DAY)) ON DUPLICATE KEY UPDATE create_time = CURRENT_TIMESTAMP;
INSERT INTO t_follow (id, follower_id, followee_id, create_time) VALUES (7002, 2, 4, DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 9 DAY)) ON DUPLICATE KEY UPDATE create_time = CURRENT_TIMESTAMP;
INSERT INTO t_follow (id, follower_id, followee_id, create_time) VALUES (7003, 3, 2, DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 8 DAY)) ON DUPLICATE KEY UPDATE create_time = CURRENT_TIMESTAMP;
INSERT INTO t_follow (id, follower_id, followee_id, create_time) VALUES (7004, 3, 4, DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 7 DAY)) ON DUPLICATE KEY UPDATE create_time = CURRENT_TIMESTAMP;
INSERT INTO t_follow (id, follower_id, followee_id, create_time) VALUES (7005, 4, 2, DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 6 DAY)) ON DUPLICATE KEY UPDATE create_time = CURRENT_TIMESTAMP;

-- ===== 9. 专栏订阅（t_column_subscribe）=====
INSERT INTO t_column_subscribe (id, user_id, column_id, create_time) VALUES (8001, 3, 3001, DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 5 DAY)) ON DUPLICATE KEY UPDATE create_time = CURRENT_TIMESTAMP;
INSERT INTO t_column_subscribe (id, user_id, column_id, create_time) VALUES (8002, 4, 3002, DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 4 DAY)) ON DUPLICATE KEY UPDATE create_time = CURRENT_TIMESTAMP;
INSERT INTO t_column_subscribe (id, user_id, column_id, create_time) VALUES (8003, 3, 3004, DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 3 DAY)) ON DUPLICATE KEY UPDATE create_time = CURRENT_TIMESTAMP;