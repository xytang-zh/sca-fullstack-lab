# CLAUDE.md — packages/ui UI 二次封装包

> 工作前**必须**先读取 [`vue-web-ui/CLAUDE.md`](../../CLAUDE.md) 与仓库根 [`sca-fullstack-lab/CLAUDE.md`](../../../CLAUDE.md)。

## 定位

`@sca/ui`：Naive UI 二次封装组件库，被根级 src 应用复用。

## 核心组件

| 组件 | 封装内容 |
|------|---------|
| `BasicTable` | 封装 NDataTable（分页、loading、列配置） |
| `BasicForm` | 封装 NForm（校验、栅格布局） |
| `BasicModal` | 封装 NModal（标题、footer 按钮、loading） |
| `PageContainer` | 页面容器（标题 + 内容 + 操作区） |
| `MarkdownView` ★ | Markdown 渲染组件（博客前台详情用，XSS 白名单） |

## 规范

1. 组件 props 用 `withDefaults` 定义默认值
2. 组件名 PascalCase，导出统一走 `index.ts`
3. 样式用 UnoCSS 原子类，**禁止**大段 scoped CSS
4. 组件**必须**透传 attrs 与 slots（`useAttrs` / `v-bind="$attrs"`）
5. 组件**必须**有 TypeScript 类型导出

## 红线

1. ❌ 依赖具体业务模块（只依赖 @sca/types、@sca/utils）
2. ❌ 直接 `import axios`
3. ❌ 用 `number` 接收 ID
4. ❌ 组件卸载不清理监听器
