# `:core:common`

## 🎯 模块职责
通用工具与基础抽象层，提供全工程通用的协程调度器注入模型（`BgmDispatchers`）、结果封装（`AppResult<T>`）及通用工具类。

## 🏛️ 依赖关系
* **依赖的上游**：`:core:model`
* **被谁依赖**：`:core:network`, `:core:database`, `:core:datastore`, `:core:data`, 所有 `:feature:*`, `:app`

## ⚠️ 架构红线与约束
1. 仅包含纯 Kotlin 通用工具与无状态辅助函数，严禁包含特定业务领域逻辑或 UI 表现逻辑。
