# [PROJECT_NAME] 项目宪法
<!-- 示例：Spec Constitution、TaskFlow Constitution 等 -->

## 核心原则

### [PRINCIPLE_1_NAME]
<!-- 示例：一、库优先（Library-First） -->
[PRINCIPLE_1_DESCRIPTION]
<!-- 示例：每个功能先做成独立库；库必须自包含、可独立测试、有文档；用途必须明确——不允许仅为组织结构存在的库 -->

### [PRINCIPLE_2_NAME]
<!-- 示例：二、CLI 接口 -->
[PRINCIPLE_2_DESCRIPTION]
<!-- 示例：每个库通过 CLI 暴露能力；文本进/出协议：stdin/args → stdout，错误 → stderr；支持 JSON + 人类可读格式 -->

### [PRINCIPLE_3_NAME]
<!-- 示例：三、测试先行（不可妥协） -->
[PRINCIPLE_3_DESCRIPTION]
<!-- 示例：TDD 强制：先写测试 → 用户确认 → 测试失败 → 才能实现；严格遵循红-绿-重构循环 -->

### [PRINCIPLE_4_NAME]
<!-- 示例：四、集成测试 -->
[PRINCIPLE_4_DESCRIPTION]
<!-- 示例：需要集成测试的重点场景：新库契约测试、契约变更、服务间通信、共享 schema -->

### [PRINCIPLE_5_NAME]
<!-- 示例：五、可观测性 / 六、版本与破坏性变更 / 七、简洁性 -->
[PRINCIPLE_5_DESCRIPTION]
<!-- 示例：文本 I/O 保证可调试性；必须结构化日志；或：MAJOR.MINOR.BUILD 版本号；或：从简，遵循 YAGNI 原则 -->

## [SECTION_2_NAME]
<!-- 示例：附加约束、安全要求、性能标准等 -->

[SECTION_2_CONTENT]
<!-- 示例：技术栈要求、合规标准、部署策略等 -->

## [SECTION_3_NAME]
<!-- 示例：开发工作流、评审流程、质量门禁等 -->

[SECTION_3_CONTENT]
<!-- 示例：代码评审要求、测试门禁、部署审批流程等 -->

## 治理
<!-- 示例：宪法高于一切其他实践；修订需文档记录、审批、迁移计划 -->

[GOVERNANCE_RULES]
<!-- 示例：所有 PR/评审必须核对合规性；复杂度必须有正当理由；使用 [GUIDANCE_FILE] 作为运行时开发指引 -->

**版本**：[CONSTITUTION_VERSION] | **批准日期**：[RATIFICATION_DATE] | **最后修订**：[LAST_AMENDED_DATE]
<!-- 示例：版本: 2.1.1 | 批准日期: 2025-06-13 | 最后修订: 2025-07-16 -->
