# `:feature:search`

## 🎯 模块职责
「搜索」模块：支持全站条目搜索（动画、书籍、游戏、音乐等），支持按类型过滤筛选、实时防抖搜索、搜索结果卡片展示（封面、双语标题、评分、年份）及多状态处理（引导、加载、空结果、错误重试）。数据经 `SearchRepository` 消费，本模块不感知网络与数据库实现。

## 🏛️ 依赖关系
* **依赖的上游**：`:core:model`、`:core:common`、`:core:data`、`:core:designsystem`（均由 `bgmplus.android.feature` 约定插件注入）+ `androidx.navigation3.runtime`（NavKey/entry 契约）
* **被谁依赖**：仅 `:app`（导航聚合 + Koin 装配）
* **禁止依赖**：任何其他 `:feature:*` 模块

## ⚠️ 架构红线与约束
1. 对外只暴露 `SearchRoute`、`searchEntry()`、`searchModule` 三样契约，UI 内部组件不对外。
2. 条目详情跳转经 `searchEntry(onSubjectClick)` 回调交由 `:app` 路由决定，本模块不持有 `NavDisplay`/`NavController`。
3. ViewModel 一律经 entry 内的 `koinViewModel()` 获取，不使用 Activity 级注入。
