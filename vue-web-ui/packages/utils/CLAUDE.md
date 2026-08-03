# CLAUDE.md — packages/utils 工具函数包

> 工作前**必须**先读取 [`vue-web-ui/CLAUDE.md`](../../CLAUDE.md) 与仓库根 [`sca-fullstack-lab/CLAUDE.md`](../../../CLAUDE.md)。

## 定位

`@sca/utils`：纯函数工具集，无 UI 依赖，被所有应用与包复用。

## 工具清单

| 模块 | 内容 |
|------|------|
| `auth.ts` | Token 管理（存取删，**禁止**应用层直接操作 localStorage） |
| `date.ts` | 日期格式化 |
| `validator.ts` | 表单校验器 |
| `tree.ts` | 树形数据处理（列表转树、过滤） |
| `download.ts` | 文件下载（blob） |

## 规范

1. 纯函数，**禁止**副作用（除 auth.ts 的 localStorage 封装）
2. 每个工具函数**必须**有 JSDoc 注释与类型签名
3. 不依赖 Node API，浏览器环境可用

## 红线

1. ❌ 依赖 UI 库
2. ❌ 直接 `import axios`
3. ❌ 应用层绕过 `auth.ts` 直接操作 Token
