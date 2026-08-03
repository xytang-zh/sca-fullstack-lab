## MODIFIED Requirements

### Requirement: 前端根级单应用结构

前端 `vue-web-ui` SHALL 为单一根级应用：应用代码位于 `vue-web-ui/src`，根 `package.json` 即为应用本体（包名 `@sca/web`），dev 端口 SHALL 为 5173；`vue-web-ui/apps` 目录 SHALL 被删除，不再存在 `apps/portal` 与 `apps/admin`；共享代码 SHALL 保留在 `packages/*`（api/types/utils/ui/uno-preset）。

#### Scenario: 应用代码位于根级 src

- **WHEN** 查看 `vue-web-ui/` 目录结构
- **THEN** 存在 `src/`（含 main.ts、App.vue、router、store、views、layouts、components、api 等）与 `packages/`，不存在 `apps/` 目录

#### Scenario: 根 package.json 为应用本体

- **WHEN** 查看 `vue-web-ui/package.json`
- **THEN** `name` 为 `@sca/web`，包含 `dev/build/typecheck/lint` 等应用脚本，并声明 `packages/*` 为 workspace 依赖

#### Scenario: dev 端口为 5173

- **WHEN** 运行 `pnpm dev`
- **THEN** 应用在 `http://localhost:5173` 启动，网关 CORS 白名单包含该端口

#### Scenario: 公共包保留

- **WHEN** 查看 `vue-web-ui/packages/`
- **THEN** `api`、`types`、`utils`、`ui`、`uno-preset` 等公共包保留，且应用通过 `@sca/*` 引用它们