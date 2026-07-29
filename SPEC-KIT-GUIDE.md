# Spec-Kit 详细使用教程（新手向）

> 本文档面向第一次接触 Spec-Kit 的开发者，假设你对"规格驱动开发（Spec-Driven Development, SDD）"没有任何背景。
> 阅读完本文，你将能够独立使用 Spec-Kit 把一个模糊的产品想法，一步步转化为可落地的代码实现。

---

## 目录

1. [Spec-Kit 是什么？为什么要用它？](#1-spec-kit-是什么为什么要用它)
2. [核心概念速览（5 分钟看懂）](#2-核心概念速览5-分钟看懂)
3. [项目目录结构解读](#3-项目目录结构解读)
4. [前置准备：环境检查](#4-前置准备环境检查)
5. [完整工作流：从想法到代码的 6 步](#5-完整工作流从想法到代码的-6-步)
6. [每个命令详解（含示例）](#6-每个命令详解含示例)
7. [可选增强命令（提升规格质量）](#7-可选增强命令提升规格质量)
8. [实战示例：开发"用户登录"功能](#8-实战示例开发用户登录功能)
9. [常见问题 FAQ](#9-常见问题-faq)
10. [最佳实践与避坑指南](#10-最佳实践与避坑指南)

---

## 1. Spec-Kit 是什么？为什么要用它？

### 1.1 一句话定义

**Spec-Kit** 是 GitHub 官方开源的"规格驱动开发（SDD）"工具包，它把"写代码"这件事拆成两个阶段：

- **先写规格（Spec）**：用自然语言描述"要做什么、为什么做、做到什么程度算成功"。
- **再写代码（Code）**：根据规格生成实现计划、任务清单，最后才动手写代码。

### 1.2 它解决什么痛点？

| 没用 Spec-Kit 的常见问题 | 用了 Spec-Kit 之后 |
|---|---|
| 需求模糊就开干，做到一半发现理解错了 | 强制先把"做什么"写清楚，AI 帮你查漏补缺 |
| AI 生成代码后很难追溯"为什么这么实现" | 每段代码都能追溯到 spec.md 的某条需求 |
| 多个 AI 工具（Claude / Copilot / Cursor）口径不一致 | 一份规格文档，喂给任何 AI 都能复用 |
| 改需求时不知道影响范围 | 规格先行，影响范围在 plan.md 里一目了然 |
| 写完才发现漏了边界 case | spec 阶段就强制思考 edge cases 与成功标准 |

### 1.3 谁适合用？

- 经常让 AI（Claude Code / Copilot / Cursor）写代码，但苦于"AI 跑偏"的人
- 个人开发者想给自己的项目建立"需求档案"
- 团队想统一需求与代码的对应关系
- 新手用来训练"先想清楚再动手"的工程思维

---

## 2. 核心概念速览（5 分钟看懂）

Spec-Kit 的核心是 **6 份文档 + 6 条命令**，它们形成一条流水线：

```
想法  →  Constitution  →  Spec  →  Plan  →  Tasks  →  Implement  →  Converge
宪法      规格说明书     设计计划   任务清单    执行实现    收敛对齐
```

### 6 份关键文档

| 文档 | 文件名 | 作用 | 类比 |
|---|---|---|---|
| **宪法** | `.specify/memory/constitution.md` | 项目级根本原则，所有功能都要遵守 | 国家宪法 |
| **规格** | `specs/NNN-feature/spec.md` | 描述"做什么"，不含技术细节 | 产品需求文档 PRD |
| **计划** | `specs/NNN-feature/plan.md` | 描述"怎么做"，技术选型与架构 | 技术设计文档 TDD |
| **任务清单** | `specs/NNN-feature/tasks.md` | 拆成一条条可执行的任务 | 工单 / Jira ticket |
| **研究** | `specs/NNN-feature/research.md` | Phase 0 产出，解决技术疑问 | 技术调研笔记 |
| **数据模型/契约** | `specs/NNN-feature/data-model.md`、`contracts/` | 实体定义与接口契约 | DB schema + API contract |

### 6 条核心命令

| 命令 | 阶段 | 一句话作用 |
|---|---|---|
| `/speckit-constitution` | 0. 立宪 | 制定/更新项目根本原则 |
| `/speckit-specify` | 1. 规格 | 把模糊想法写成 spec.md |
| `/speckit-plan` | 2. 计划 | 把 spec 转成技术实现计划 |
| `/speckit-tasks` | 3. 任务 | 把 plan 拆成有序任务清单 |
| `/speckit-implement` | 4. 实现 | 按 tasks.md 逐条执行写代码 |
| `/speckit-converge` | 5. 收敛 | 检查代码与规格的差距，补齐遗漏 |

另外有 4 条**可选增强**命令（`clarify` / `analyze` / `checklist` / `taskstoissues`），后文详解。

---

## 3. 项目目录结构解读

初始化完成后，项目里多了两个目录：

```
sca-fullstack-lab/
├── .specify/                          # Spec-Kit 的"配置与模板"目录（建议提交到 git）
│   ├── init-options.json              #   初始化选项记录
│   ├── integration.json               #   AI 集成配置（claude）
│   ├── memory/
│   │   └── constitution.md            #   ★ 项目宪法（你将编辑这个文件）
│   ├── templates/                     #   各类文档模板
│   │   ├── constitution-template.md
│   │   ├── spec-template.md
│   │   ├── plan-template.md
│   │   ├── tasks-template.md
│   │   └── checklist-template.md
│   ├── scripts/python/                #   工作流辅助脚本
│   │   ├── setup_plan.py
│   │   ├── setup_tasks.py
│   │   ├── check_prerequisites.py
│   │   └── create_new_feature.py
│   ├── workflows/speckit/             #   完整 SDD 工作流定义
│   └── integrations/                  #   集成清单
│
├── .claude/                           # Claude 集成目录（已加入 .gitignore）
│   └── skills/                        #   10 个 Spec-Kit skills
│       ├── speckit-constitution/SKILL.md
│       ├── speckit-specify/SKILL.md
│       ├── speckit-plan/SKILL.md
│       ├── speckit-tasks/SKILL.md
│       ├── speckit-implement/SKILL.md
│       ├── speckit-converge/SKILL.md
│       ├── speckit-clarify/SKILL.md
│       ├── speckit-analyze/SKILL.md
│       ├── speckit-checklist/SKILL.md
│       └── speckit-taskstoissues/SKILL.md
│
└── specs/                             # ★ 功能规格将生成在这里（尚未创建）
    └── NNN-feature-name/             #   每个功能一个子目录
        ├── spec.md
        ├── plan.md
        ├── research.md
        ├── data-model.md
        ├── contracts/
        ├── quickstart.md
        ├── tasks.md
        └── checklists/
```

**记住三个关键路径**：
- `.specify/memory/constitution.md` — 立宪在这里
- `specs/NNN-feature/` — 每个功能的"档案盒"
- `.claude/skills/speckit-*/SKILL.md` — 命令的"大脑"（出问题可以读它排查）

---

## 4. 前置准备：环境检查

### 4.1 确认 Python 可用

Spec-Kit 的脚本用 Python 写的，需要 Python 3.x：

```bash
python --version
# 或
python3 --version
```

如果输出 `Python 3.x.x` 即可。Windows 下若没有 `python3`，命令里 `python3` 可换成 `python`。

### 4.2 确认 Claude Code 可用

```bash
specify check
```

会列出所有检测到的 AI 工具，只要 `Claude Code (available)` 即可。

### 4.3 确认在项目根目录

所有 `/speckit-*` 命令**必须在项目根目录执行**（即 `D:\WorkSpace\Projects\sca-fullstack-lab`）。

---

## 5. 完整工作流：从想法到代码的 6 步

下面这张流程图建议保存，每次开工前对照着走：

```
┌──────────────────────────────────────────────────────────────┐
│  Step 0  立宪（项目级，做一次即可）                            │
│  /speckit-constitution                                        │
│  产出：.specify/memory/constitution.md                       │
└──────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────┐
│  Step 1  写规格（每个功能都要做）                                │
│  /speckit-specify 我想做 XXX 功能                              │
│  产出：specs/001-xxx/spec.md + checklists/requirements.md     │
└──────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────┐
│  Step 1.5（可选）澄清模糊点                                     │
│  /speckit-clarify                                              │
│  产出：把 [NEEDS CLARIFICATION] 替换成明确答案                  │
└──────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────┐
│  Step 2  写实现计划                                            │
│  /speckit-plan                                                 │
│  产出：specs/001-xxx/plan.md + research.md + data-model.md    │
│        + contracts/ + quickstart.md                            │
└──────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────┐
│  Step 2.5（可选）生成质量检查清单                               │
│  /speckit-checklist                                            │
│  产出：specs/001-xxx/checklists/*.md                          │
└──────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────┐
│  Step 3  生成任务清单                                          │
│  /speckit-tasks                                                │
│  产出：specs/001-xxx/tasks.md                                  │
└──────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────┐
│  Step 3.5（可选）跨文档一致性分析                               │
│  /speckit-analyze                                              │
└──────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────┐
│  Step 4  执行实现                                              │
│  /speckit-implement                                            │
│  产出：实际代码（按 tasks.md 逐条写）                          │
└──────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────┐
│  Step 5  收敛对齐（代码写完或半路都可用）                       │
│  /speckit-converge                                             │
│  产出：补充的 tasks 追加到 tasks.md                            │
└──────────────────────────────────────────────────────────────┘
```

---

## 6. 每个命令详解（含示例）

### 6.1 `/speckit-constitution` — 立宪

#### 作用
制定或更新项目的"根本大法"。所有后续功能都要符合宪法里写明的原则。

#### 何时用
- 项目初始化后，**第一次开工前**用一次
- 以后每次想新增/修改项目原则时再用

#### 怎么用

直接在 Claude Code 对话框输入：

```
/speckit-constitution 我的项目是一个 Spring Cloud Alibaba 微服务实验室，原则是：
1. 模块优先：每个功能先做成独立模块，可独立启动
2. 测试先行：核心逻辑必须有单元测试
3. 可观测性：所有服务必须接入日志与监控
4. 简单优先：YAGNI，不引入用不上的依赖
5. 版本管理：MAJOR.MINOR.PATCH 语义化版本
```

#### 产出

`.specify/memory/constitution.md` 会被填充为完整内容，包含：
- Core Principles（5 条原则）
- Governance（治理规则：版本、修订日期）

#### 关键规则

- 版本号遵循语义化：MAJOR（不兼容变更）/ MINOR（新增原则）/ PATCH（措辞修订）
- 每次修改会自增版本号，并在文件顶部生成 Sync Impact Report
- 如果你的输入里混了"实现某个功能"的请求，命令会拒绝执行，并把它列为 "Next Actions"

---

### 6.2 `/speckit-specify` — 写规格

#### 作用
把一句模糊的"我想做 XXX"，转化成一份结构化的 `spec.md`。

#### 何时用
- 每次开始一个新功能时
- **不用于**修改项目原则（那是 constitution 的事）

#### 怎么用

```
/speckit-specify 我想给用户中心加一个手机号登录功能，支持验证码短信，验证码 5 分钟过期
```

#### 命令在背后做了什么

1. **生成短名**：从描述提取 2-4 词作为目录名，如 `phone-login`
2. **创建目录**：`specs/001-phone-login/`（编号按 sequential 或 timestamp）
3. **复制模板**：把 `spec-template.md` 复制为 `specs/001-phone-login/spec.md`
4. **填充内容**：按你给的描述，填入：
   - User Scenarios & Testing（用户故事 + 验收场景，按 P1/P2/P3 排优先级）
   - Functional Requirements（FR-001、FR-002…）
   - Key Entities（关键实体）
   - Success Criteria（可衡量的成功标准）
   - Assumptions（默认假设）
5. **生成质量清单**：`specs/001-phone-login/checklists/requirements.md`
6. **最多 3 个 NEEDS CLARIFICATION**：如果有关键模糊点，会以表格形式问你 3 个问题，每个给 A/B/C 选项

#### 关键原则

- 规格里**只写 WHAT 和 WHY**，绝不写 HOW
- 不允许出现具体框架名（如 "Spring Boot"）、API 名、数据库类型
- 成功标准必须**可测量**且**与技术无关**（"用户 3 分钟内完成登录"而不是"接口 <200ms"）

#### 示例：好的 vs 坏的 FR

✅ 好：
> **FR-001**: 系统 MUST 允许用户使用手机号 + 短信验证码登录

❌ 坏（包含实现细节）：
> **FR-001**: 系统 MUST 用 Spring Security + JWT 实现 +86 手机号登录，Redis 存验证码

---

### 6.3 `/speckit-clarify` — 可选澄清

#### 作用
针对当前 spec.md，最多问 5 个高针对性的问题，把模糊点消除。

#### 何时用
- spec.md 里 `[NEEDS CLARIFICATION]` 标记太多
- 想在进入 plan 阶段前降低风险

#### 怎么用

```
/speckit-clarify
```
或
```
/speckit-clarify 重点澄清验证码发送频率限制与防刷策略
```

---

### 6.4 `/speckit-plan` — 写实现计划

#### 作用
把 spec.md 转成技术实现计划，并产出 4 份配套文档。

#### 何时用
- spec.md 已经定稿（无未解决 NEEDS CLARIFICATION）
- 准备开始写代码前

#### 怎么用

```
/speckit-plan
```

#### 命令在背后做了什么

1. **运行 setup 脚本**：`python3 .specify/scripts/python/setup_plan.py --json`
2. **加载上下文**：读 spec.md 和 constitution.md
3. **生成 4 份文档**：
   - `research.md` — Phase 0，调研所有 NEEDS CLARIFICATION 并给出结论
   - `data-model.md` — Phase 1，数据实体定义
   - `contracts/` — Phase 1，接口契约（按文件分）
   - `quickstart.md` — Phase 1，快速启动 / 测试场景
   - `plan.md` — 主计划文档，包含 Technical Context、Constitution Check、Project Structure
4. **Constitution Gate**：自动检查计划是否违反宪法，违反就 ERROR

#### 关键产出：`plan.md` 的 Technical Context

这一段会填这些字段（如不清楚会标 `NEEDS CLARIFICATION`）：

| 字段 | 示例 |
|---|---|
| Language/Version | Java 17 |
| Primary Dependencies | Spring Cloud Alibaba 2023.x |
| Storage | MySQL 8 + Redis 7 |
| Testing | JUnit 5 + Mockito |
| Target Platform | Linux server (Docker) |
| Project Type | micro-service |
| Performance Goals | 1000 QPS |
| Constraints | p99 < 200ms |
| Scale/Scope | 10万用户 |

---

### 6.5 `/speckit-checklist` — 可选质量清单

#### 作用
生成自定义质量清单，用来验证**需求文档本身**的清晰度、完整性。

> ⚠️ 注意：清单不是用来"测试代码是否正确"的，而是"测试需求是否写得清楚"的——所以叫"Unit Tests for English"。

#### 怎么用

```
/speckit-checklist 重点关注安全合规与可观测性
```

产出：`specs/001-phone-login/checklists/security-and-observability.md`

---

### 6.6 `/speckit-tasks` — 生成任务清单

#### 作用
把 plan.md + spec.md + data-model.md + contracts 综合起来，生成一份**依赖有序、可并行、按用户故事分组**的任务清单。

#### 怎么用

```
/speckit-tasks
```

#### 产出：`tasks.md` 的结构

```markdown
## Phase 1: Setup (Shared Infrastructure)
- [ ] T001 创建项目结构
- [ ] T002 [P] 配置 linting          # [P] 表示可并行

## Phase 2: Foundational (Blocking Prerequisites)
- [ ] T004 数据库 schema 与迁移
- [ ] T005 [P] 鉴权框架

## Phase 3: User Story 1 - 手机号登录 (Priority: P1) 🎯 MVP
- [ ] T010 发送验证码接口
- [ ] T011 验证码校验接口
- [ ] T012 [P] 防刷限流

## Phase 4: User Story 2 - ...
```

**关键设计**：
- 每个用户故事独立可测（MVP 切片）
- `[P]` 标记的任务可以并行做（互不影响）
- 每个任务都带精确文件路径

---

### 6.7 `/speckit-analyze` — 可选一致性分析

#### 作用
**非破坏性**地分析 spec.md / plan.md / tasks.md 三者的一致性、完整性、对齐度，输出报告。

#### 何时用
- tasks.md 生成后、implement 前
- 担心三份文档互相矛盾

#### 怎么用

```
/speckit-analyze
```

---

### 6.8 `/speckit-implement` — 执行实现

#### 作用
按 tasks.md 逐条执行，真正写代码。

#### 怎么用

```
/speckit-implement
```
或只做某个阶段：
```
/speckit-implement 只执行 Phase 3 的 T010、T011
```

#### 命令在背后做了什么

1. 运行 `check_prerequisites.py` 确认 tasks.md 存在
2. 扫描 `checklists/` 目录里所有清单，统计完成度
3. 按 tasks.md 的依赖顺序，**从前往后**执行每条任务
4. 每完成一条会把 `- [ ]` 改为 `- [x]`
5. 遇到阻塞会停下来报告

---

### 6.9 `/speckit-converge` — 收敛对齐

#### 作用
当代码已经写了一部分（可能没按 tasks.md 走，或者半路加塞了功能），对比 spec/plan/tasks 与实际代码，把**遗漏未做**的工作作为新任务追加到 tasks.md。

#### 何时用
- 代码已经偏离了 tasks.md
- 接手了别人的代码，想对齐规格
- implement 跑到一半发现 spec 还有未覆盖的点

#### 怎么用

```
/speckit-converge
```

---

### 6.10 `/speckit-taskstoissues` — 任务转 Issue

#### 作用
把 tasks.md 里的每条任务转成 GitHub Issue（带依赖顺序、标签）。

#### 何时用
- 团队协作，需要把任务分给不同人
- 想用 GitHub 看板跟踪

#### 怎么用

```
/speckit-taskstoissues
```

---

## 7. 可选增强命令（提升规格质量）

| 命令 | 用在哪一步 | 价值 |
|---|---|---|
| `/speckit-clarify` | specify 之后、plan 之前 | 把模糊点问清楚，避免返工 |
| `/speckit-checklist` | plan 之后 | 生成"需求质量测试清单" |
| `/speckit-analyze` | tasks 之后、implement 之前 | 三文档一致性体检 |
| `/speckit-taskstoissues` | tasks 之后 | 同步到 GitHub Issues |

---

## 8. 实战示例：开发"用户登录"功能

下面是一个完整的对话脚本，你可以照着走一遍。

### Step 0（假设你还没立宪）

```
/speckit-constitution 这是一个 Spring Cloud Alibaba 微服务实验室，
原则：1. 模块独立可启动 2. 测试先行 3. 必须有可观测性 4. YAGNI
```

### Step 1

```
/speckit-specify 给 spring-cloud-auth 模块加手机号验证码登录功能：
- 用户输入手机号，收到 6 位数字验证码
- 验证码 5 分钟过期
- 同一手机号 60 秒内不能重复发送
- 验证通过后返回登录态
```

AI 会问你 1-3 个澄清问题，比如：
> Q1: 验证码发送频率限制
> A. 每手机号 60 秒 1 条，每天 10 条  B. 仅 60 秒限制  C. ...

你回复 `Q1: A` 即可。

### Step 2

```
/speckit-plan
```

AI 会自动填好 Technical Context、生成 research.md、data-model.md 等。

### Step 3

```
/speckit-tasks
```

### Step 4

```
/speckit-implement
```

AI 会按 tasks.md 逐条写代码。你可以随时打断、纠偏。

### Step 5（可选）

```
/speckit-converge
```

检查还有哪些任务没做，追加到 tasks.md。

---

## 9. 常见问题 FAQ

### Q1：`python3` 命令找不到怎么办？

Windows 下通常只有 `python`，没有 `python3`。两种解决办法：

**办法 1（推荐）**：装 Python 时勾选 "Add python.exe to PATH"，并创建 `python3` 别名：
```bash
# 在 Git Bash 里
echo "alias python3=python" >> ~/.bashrc
source ~/.bashrc
```

**办法 2**：手动修改 `.claude/skills/speckit-plan/SKILL.md` 等文件里的 `python3` 为 `python`。但下次 `specify init --force` 会覆盖。

### Q2：spec.md 里有 `[NEEDS CLARIFICATION]` 标记怎么办？

两种方式：

1. **手动改**：直接编辑 spec.md，把 `[NEEDS CLARIFICATION: ...]` 替换成明确答案
2. **用命令**：执行 `/speckit-clarify`，AI 会问你针对性问题并自动回填

### Q3：写到一半想改 spec 怎么办？

直接编辑 `specs/NNN-feature/spec.md`，然后**重新跑** `/speckit-plan`。它会读取最新 spec 重新生成 plan.md。

### Q4：implement 跑到一半挂了怎么办？

没问题，tasks.md 已经把做过的标 `[x]` 了。重新跑 `/speckit-implement`，它会从第一条未完成的任务继续。

### Q5：我能不用 Claude Code，直接用别的 AI 吗？

可以。`.specify/` 是中立的规格目录。你把 spec.md / plan.md / tasks.md 喂给任意 AI（Copilot、Cursor、Codex）都行。Spec-Kit 的"工作流"在 `.claude/skills/`，但"文档"在 `.specify/`，后者可独立使用。

### Q6：`.claude/` 被我加到 `.gitignore` 了，团队成员怎么办？

让团队成员在自己机器上执行：
```bash
specify init . --integration claude --force --script py
```
即可重新生成 `.claude/skills/`。`.specify/` 已提交到 git，他们直接拉取即可。

### Q7：Spec-Kit 版本号在哪看？

`.specify/init-options.json` 里有 `"speckit_version": "0.14.3"`。升级用：
```bash
specify self update
```

### Q8：命令卡住没输出怎么办？

可能是 Windows 下 sh 脚本兼容性问题。初始化时务必加 `--script py`。若 SKILL.md 里仍是 `sh`，手动改成 `py`。

---

## 10. 最佳实践与避坑指南

### ✅ 推荐做法

1. **宪法立得简而精**：3-5 条原则即可，太多反而约束不住
2. **spec 阶段多花时间**：这是返工成本最低的阶段
3. **每个用户故事独立可测**：这样即使只做完 P1，也有可交付的 MVP
4. **commit 节奏对齐工作流**：
   - `constitution` → 1 个 commit
   - `specify` → 1 个 commit
   - `plan` → 1 个 commit
   - `tasks` → 1 个 commit
   - `implement` → 按任务粒度多个 commit
5. **用 `--feature-numbering=sequential`**（默认）：方便排序
6. **implement 前先看 checklists**：清单没满的，先补 spec
7. **中途偏航用 `/speckit-converge` 回归**：比硬改 spec 安全

### ❌ 避坑清单

1. **不要在 spec 里写技术细节**：如"用 Redis 存验证码"——这是 plan 阶段的事
2. **不要跳过 constitution 直接 specify**：虽然能跑，但后续 plan 阶段少了原则校验
3. **不要手动改 tasks.md 的任务 ID**：会破坏依赖追踪
4. **不要在 implement 里做 spec 没写的功能**：先用 converge 补任务
5. **不要把 `.claude/` 提交到 git**：可能含凭证
6. **不要在多个分支同时跑 specify**：`feature.json` 会互相覆盖
7. **不要忽略 `[NEEDS CLARIFICATION]` 标记**：留到 implement 阶段会爆炸

---

## 附录：命令速查卡

| 场景 | 命令 |
|---|---|
| 第一次开工 | `/speckit-constitution <原则>` |
| 新功能 | `/speckit-specify <功能描述>` |
| 澄清模糊点 | `/speckit-clarify` |
| 写实现计划 | `/speckit-plan` |
| 生成质量清单 | `/speckit-checklist <关注点>` |
| 生成任务清单 | `/speckit-tasks` |
| 一致性体检 | `/speckit-analyze` |
| 执行实现 | `/speckit-implement` |
| 收敛对齐 | `/speckit-converge` |
| 同步到 GitHub | `/speckit-taskstoissues` |

---

## 附录：文件位置速查

| 你想看什么 | 去哪找 |
|---|---|
| 项目原则 | `.specify/memory/constitution.md` |
| 某功能的规格 | `specs/NNN-feature/spec.md` |
| 某功能的计划 | `specs/NNN-feature/plan.md` |
| 某功能的任务 | `specs/NNN-feature/tasks.md` |
| 质量清单 | `specs/NNN-feature/checklists/*.md` |
| 命令定义（排错用） | `.claude/skills/speckit-*/SKILL.md` |
| 文档模板 | `.specify/templates/*.md` |
| 工作流脚本 | `.specify/scripts/python/*.py` |

---

> 文档版本：1.0 | 生成日期：2026-07-29 | Spec-Kit 版本：0.14.3
> 如有疑问，可在 Claude Code 中直接询问，或参考 `.claude/skills/` 下的 SKILL.md 原文。
