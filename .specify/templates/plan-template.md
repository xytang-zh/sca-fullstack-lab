# 实现计划：[FEATURE]

**分支**：`[###-feature-name]` | **日期**：[DATE] | **规格**：[链接]

**输入**：来自 `/specs/[###-feature-name]/spec.md` 的功能规格

**说明**：本模板由 `/speckit-plan` 命令填充；其定义描述了执行工作流。

## 概要

[从功能规格提取：主要需求 + 来自调研的技术方案]

## 技术上下文

<!--
  动作要求：用项目的技术细节替换本节内容。
  这里的结构仅作为迭代过程的参考。
-->

**语言/版本**：[例如 Python 3.11、Swift 5.9、Rust 1.75 或 NEEDS CLARIFICATION]

**主要依赖**：[例如 FastAPI、UIKit、LLVM 或 NEEDS CLARIFICATION]

**存储**：[如适用，例如 PostgreSQL、CoreData、文件 或 N/A]

**测试**：[例如 pytest、XCTest、cargo test 或 NEEDS CLARIFICATION]

**目标平台**：[例如 Linux 服务器、iOS 15+、WASM 或 NEEDS CLARIFICATION]

**项目类型**：[例如 库/CLI/Web 服务/移动应用/编译器/桌面应用 或 NEEDS CLARIFICATION]

**性能目标**：[领域相关，例如 1000 req/s、10k 行/秒、60 fps 或 NEEDS CLARIFICATION]

**约束**：[领域相关，例如 <200ms p99、<100MB 内存、可离线 或 NEEDS CLARIFICATION]

**规模/范围**：[领域相关，例如 1 万用户、100 万行代码、50 个页面 或 NEEDS CLARIFICATION]

## 宪法核对

*门禁：Phase 0 调研前必须通过。Phase 1 设计后再次核对。*

[基于宪法文件确定门禁项]

## 项目结构

### 文档（本功能）

```text
specs/[###-feature]/
├── plan.md              # 本文件（/speckit-plan 命令输出）
├── research.md          # Phase 0 输出（/speckit-plan 命令）
├── data-model.md        # Phase 1 输出（/speckit-plan 命令）
├── quickstart.md        # Phase 1 输出（/speckit-plan 命令）
├── contracts/           # Phase 1 输出（/speckit-plan 命令）
└── tasks.md             # Phase 2 输出（/speckit-tasks 命令——非 /speckit-plan 创建）
```

### 源代码（仓库根）

<!--
  动作要求：用本功能具体的目录结构替换下方占位树。
  删除未使用的选项，并展开所选结构填入真实路径（如 apps/admin、packages/something）。
  最终 plan 不得保留 Option 标签。
-->

```text
# [未使用则删除] 选项 1：单一项目（默认）
src/
├── models/
├── services/
├── cli/
└── lib/

tests/
├── contract/
├── integration/
└── unit/

# [未使用则删除] 选项 2：Web 应用（识别到 "frontend" + "backend" 时）
backend/
├── src/
│   ├── models/
│   ├── services/
│   └── api/
└── tests/

frontend/
├── src/
│   ├── components/
│   ├── pages/
│   └── services/
└── tests/

# [未使用则删除] 选项 3：移动端 + API（识别到 "iOS/Android" 时）
api/
└── [同上 backend]

ios/ 或 android/
└── [平台相关结构：功能模块、UI 流程、平台测试]
```

**结构决策**：[记录所选结构并引用上方记录的真实目录]

## 复杂度追踪

> **仅当宪法核对有需正当理由的违反项时才填写**

| 违反项 | 为何需要 | 为何拒绝更简方案 |
|--------|----------|------------------|
| [例如第 4 个项目] | [当前需求] | [为何 3 个项目不够] |
| [例如仓储模式] | [具体问题] | [为何直接访问数据库不够] |
