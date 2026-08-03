# CLAUDE.md — spring-cloud-file 文件服务

> 本文档面向 AI 编码助手，用于在 `spring-cloud-file/` 目录下工作时提供模块约束、技术栈版本、服务职责与开发规范。
> 工作前**必须**先读取父目录 [`spring-cloud-services/CLAUDE.md`](../CLAUDE.md)、后端聚合 [`spring-cloud-alibaba/CLAUDE.md`](../../CLAUDE.md) 与仓库根 [`sca-fullstack-lab/CLAUDE.md`](../../../CLAUDE.md) 了解全局规范。

---

## 1. 模块定位

基于 MinIO 的对象存储服务，博客系统中承担文章封面图、图片上传。支持大文件分片上传、断点续传、预签名 URL。

| 维度 | 值 |
|------|-----|
| 父 POM | `com.xytang:spring-cloud-alibaba:1.0-SNAPSHOT` |
| 顶级包 | `com.xytang.file` |
| 端口 | HTTP 8088 / Dubbo 20888 / XXL-JOB 10006 |

---

## 2. 核心功能

| 模块 | 功能 |
|------|------|
| 上传模块 | 单文件、批量、分片（init/upload/complete 三步） |
| 下载模块 | 普通下载、断点续传、临时链接 |
| 预览模块 | 图片/PDF 直预览，Office 走 KKFileView |
| 管理模块 | 文件 CRUD、回收站、配额 |
| 预签名模块 | 前端直传 MinIO 的签名接口 |
| 秒传模块 | 上传前查 MD5，已存在复用 |

### 2.1 博客场景

- article 服务经 Dubbo 调用上传文章封面图/正文图片
- 前端也可直接走预签名 URL 直传 MinIO

---

## 3. 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| MinIO Java SDK | latest stable（父 POM 未声明，落地时补充） | 对象存储 |
| Spring Boot Web | 3.5.0（父 POM） | HTTP 接口 |
| Redisson | 4.0.0（父 POM） | 分片上传状态管理 |
| KKFileView | 独立容器（端口 8012） | Office 文件预览代理 |

---

## 4. 关键接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/files/upload/init` | 初始化分片上传，返回 uploadId |
| POST | `/api/files/upload/chunks` | 上传一片 |
| POST | `/api/files/upload/complete` | 完成上传，合并文件 |
| POST | `/api/files` | 单文件上传 |
| GET | `/api/files/{id}` | 文件元数据 |
| GET | `/api/files/{id}/download` | 下载 |
| GET | `/api/files/{id}/preview` | 预览（返回 KKFileView URL） |
| GET | `/api/files/presign` | 预签名上传 URL |
| POST | `/api/files/check-md5` | 秒传检查 |
| DELETE | `/api/files/{id}` | 删除（进回收站） |
| POST | `/api/files/{id}/restore` | 从回收站恢复 |

---

## 5. 开发规范（本服务特有）

- 上传文件**必须**校验类型白名单与大小上限
- 分片上传状态**必须**用 Redisson 管理（超时回收）
- 预签名 URL**必须**设置有效期（默认 10 分钟）
- 文件名**必须**重命名为对象键（雪花 ID + 扩展名），**禁止**使用原始文件名

---

## 6. 红线（违反即拒绝）

1. ❌ 子服务 POM 覆盖父 POM 依赖版本
2. ❌ `@Autowired` 字段注入
3. ❌ `throw new RuntimeException` / `catch (Exception e)`
4. ❌ `System.out.println` / `e.printStackTrace()`
5. ❌ 不校验文件类型/大小（可上传任意文件）
6. ❌ 预签名 URL 无有效期
7. ❌ 使用原始文件名作对象键（冲突/注入风险）
8. ❌ 业务配置硬编码（必须放 Nacos）
