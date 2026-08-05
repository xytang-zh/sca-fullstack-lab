# Design: 全仓库代码与配置注释补充

## Context

动机见 proposal.md - Why。本变更是纯注释补充，不改变任何运行行为，因此**没有 spec 需求变更**（`skip_specs: true`）。设计关注的是"如何高效、安全地为一套 166 个 Java + 约 46 个 TS/Vue + 24 个 YAML + 若干 XML/SQL/Docker 的仓库补充符合 `docs/12-注释规范.md` 的注释"。

当前约束：
- 后端 checkstyle 绑定 `mvn validate`，`LineLength≤120`、`NeedBraces`、禁 `System.out` 等规则对注释同样生效（过长注释行会 fail）。
- 前端有 `pnpm typecheck`（`tsc --noEmit`），JSDoc 语法错误会破坏类型检查。
- 注释规范 §1.2 黄金法则：注释解释 **Why** 不解释 **What**，禁止废话/翻译式注释。
- 学习向项目要求注释密度高于常规企业项目（§1.4：公开成员 100%、私有 80%+）。

## Goals / Non-Goals

**Goals:**
- 全仓公开类/方法/接口/枚举/类型/函数达到文档注释 100% 覆盖。
- 所有配置项（YAML/XML/pom）的非自解释键有行尾注释，长配置按功能分组。
- Java/TS 注释语法合法，不破坏 `mvn validate` 与 `pnpm typecheck`。
- 框架集成点（配置类、过滤器、守卫、Store）补"为什么这样集成"说明。

**Non-Goals:**
- 不重构代码、不改命名、不改配置值（注释是解释，不是改动）。
- 不为 `import`、显而易见的一行、`margin:0` 类重置写注释（规范 §3.2.5/§3.5 禁止）。
- 不处理 docs 中已删除服务（workflow/ai/report）历史遗留。

## Decisions

### D1: 补充顺序 = 按"读者学习路径"分层

```
Layer 1  后端公共层  spring-cloud-common/{core,web,mybatis,redis,satoken,dubbo}
Layer 2  网关        spring-cloud-gateway（前端唯一入口，过滤器链最值得学）
Layer 3  认证中心    spring-cloud-auth（Sa-Token 集成、登录/注册/踢人）
Layer 4  业务服务    spring-cloud-services/{system,article,comment}
Layer 5  后端配置    application*.yml / bootstrap*.yml / pom.xml / MyBatis XML
Layer 6  前端        vue-web-ui/src + packages/{api,types,ui,utils}
Layer 7  基础设施    docker-compose / *.sql / Dockerfile / 脚本
```

理由：先公共层再业务层，先"框架怎么被接入"再"业务怎么写"，与规范 §5 标准流程（先读上下文再分析成员清单）一致。分层提交便于按 commit 粒度审查。

**替代方案**：按文件遍历全仓一次性改。被否——文件多、无层次，难以审计与分 commit，且易漏配置类。

### D2: 每文件先盘点"公开成员清单"再写注释

对每个目标文件，先收集类/方法/字段/配置项清单，区分"必须写"（§4.1）与"应当写"（§4.2），再逐条编写。避免边读边写导致漏公开成员。

### D3: 复杂逻辑用"步骤化注释"而非逐行

多步方法用 `// 1.`、`// 2.` 编号，每步注释**意图与边界**（规范 §3.1.4/§3.2.4）。≤2 行且自解释的步骤省略。禁止逐行翻译式注释（§4.3-1）。

### D4: 配置注释的三条硬规则

1. 每个非自解释键 → 行尾注释（含单位/默认值）。
2. 涉及安全/开关的键（如 sa-token `is-kickout`）→ 注释该开关的后果。
3. `${VAR:default}` 占位符 → 注释"变量用途 + 默认值含义"（§3.7.3）。

### D5: 注释质量目标定为 ★★★~★★★★

按 §7 分级：普通成员 ★★★（为什么+边界），关键业务/安全代码（密码哈希、Token 校验、越权拦截）★★★★（再加上下文/错误处理原因）。

### D6: 验证策略（两个门禁）

```
代码   → mvn validate            （checkstyle：行宽/大括号/禁外输出）
        → pnpm typecheck         （JSDoc 语法 / TS 类型）
注释   → 对照 §6 检查清单逐项自检（无废话/无过时/无敏感信息/对齐）
```

注释改动不触发运行行为变更，故不跑单元测试；但 checkstyle/tsc 必须绿。

## Risks / Trade-offs

- [注释与代码不一致（过时注释）] → 逐文件核对当前实现后再写；提交前用 §6.1 清单自检。
- [Java 注释行超 120 字符触发 checkstyle fail] → Javadoc 长描述拆多行 `<p>`，行尾注释控制在行宽内。
- [JSDoc 语法错误破坏 `pnpm typecheck`] → 只写 `/** */` 与 `@param`/`@returns`，不写非法标签；改完跑 tsc。
- [注释密度过高导致难读] → 严格遵循"好命名>注释"与"禁止废话"（§1.2/§4.3），可省略处坚决不写。
- [改动面大、commit 混入代码改动] → 按 Layer 分批，每批单个 `docs(注释): ...` commit，与代码改动隔离。

## Migration Plan

无运行时迁移。按 Layer 1→7 顺序逐层提交，每层完成后 `mvn validate`（后端层）或 `pnpm typecheck`（前端层）验证，再进入下一层。

## Open Questions

无 —— 所有可延迟的未知都已转化为上述决策；若单文件过大或归属不明，以 Layer 内模块为单位拆分 task。