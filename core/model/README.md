# `:core:model`

## 🎯 模块职责
领域核心数据模型层，定义全应用通用的业务实体（如 Subject、Episode、AirSchedule、UserProfile 等）与纯 Kotlin 数据结构。

## 🏛️ 依赖关系
* **依赖的上游**：无（底层根模块）
* **被谁依赖**：`:core:common`, `:core:network`, `:core:database`, `:core:datastore`, `:core:data`, `:core:designsystem`, 所有 `:feature:*`, `:app`

## ⚠️ 架构红线与约束
1. 保持纯 Kotlin（0 外部系统依赖），严禁引入任何 `android.*`、Ktor、Room 或 Compose/UI 相关依赖。
