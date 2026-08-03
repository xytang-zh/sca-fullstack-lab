# CLAUDE.md — packages/uno-preset UnoCSS 预设包

> 工作前**必须**先读取 [`vue-web-ui/CLAUDE.md`](../../CLAUDE.md) 与仓库根 [`sca-fullstack-lab/CLAUDE.md`](../../../CLAUDE.md)。

## 定位

`@sca/uno-preset`：项目自定义 UnoCSS 预设，统一主题变量（颜色、断点、间距），被根级 src 应用复用。

## 预设内容（计划）

| 项 | 说明 |
|----|------|
| theme.colors | 品牌色、功能色（成功/警告/错误），**禁止** hex 直接使用 |
| theme.breakpoints | sm 640 / md 768 / lg 1024 / xl 1280 |
| shortcuts | 常用组合类（如 `btn-primary`、`card`） |
| rules | 自定义规则（如 `text-ellipsis-2` 两行省略） |

## 规范

1. 预设**必须**通过 `presets: [unoPreset()]` 在应用 `uno.config.ts` 中引入
2. 主题色变更**必须**改本包，**禁止**在应用层重复定义
3. 导出 TypeScript 类型（`PresetUnoTheme`），IDE 有提示

## 红线

1. ❌ 在组件里写死 hex 颜色（必须 theme 变量）
2. ❌ 预设依赖业务应用
3. ❌ 修改断点值不通知其他应用
