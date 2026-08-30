# `:core:database`

## 🎯 模块职责
负责本地结构化数据的 SQLite 持久化存储与离线缓存，基于 Room 3.0 构建，向数据仓库层提供响应式数据流（`Flow<T>`）与本地 CRUD 操作。

## 🏛️ 依赖关系
* **依赖的上游**：`:core:model`, `:core:common`
* **被谁依赖**：`:core:data`, `:app`
* **禁止依赖**：禁止依赖 `:core:network`, `:core:datastore` 或任何 `:feature:*` 模块。

## ⚠️ 架构红线与约束
1. 本模块为底层数据源，仅供 `:core:data` 仓库层调用，UI 层与 Feature 模块严禁直接访问 DAO。
2. DAO 的读操作统一返回 `Flow<T>`，写操作一律为 `suspend` 函数。
