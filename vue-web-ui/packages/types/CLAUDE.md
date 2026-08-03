# CLAUDE.md — packages/types 全局类型包

> 工作前**必须**先读取 [`vue-web-ui/CLAUDE.md`](../../CLAUDE.md) 与仓库根 [`sca-fullstack-lab/CLAUDE.md`](../../../CLAUDE.md)。

## 定位

`@sca/types`：全局 TS 类型定义，前后端契约的类型化基线。

## 类型清单

| 模块 | 内容 |
|------|------|
| `api.d.ts` | `R<T>`、`PageVO<T>`、分页查询参数 |
| `system.d.ts` | sys_user、sys_role、sys_menu 等 |
| `blog.d.ts` ★ | ArticleVO、CommentVO、CategoryVO、TagVO、LikeRecord（ID 一律 `string`） |

## 规范

1. **ID 字段必须 `string`**（雪花 ID Long→String 契约），**禁止** `number`
2. 类型与后端 `spring-cloud-common-core` 的 VO/DTO 一一对应
3. 枚举值（文章状态、评论状态）用 `union type`，与后端枚举一致
4. 新增类型**必须**按服务分文件，`index.ts` 统一导出

## 红线

1. ❌ 用 `number` 声明 ID 字段
2. ❌ 类型与后端契约不一致（字段名/枚举值漂移）
3. ❌ 放业务逻辑代码
