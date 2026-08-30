# `:feature:schedule`

## 🎯 模块职责
「放送」Tab：每周放送时间表。按周一~周日切换查看各天番剧卡片（封面、展示名、CST 放送时间、评分），顶栏支持手动刷新。数据经 `ScheduleRepository` 消费（Room 流离线优先 + 手动 refresh），本模块不感知网络与数据库实现。

## 🏛️ 依赖关系
* **依赖的上游**：`:core:model`、`:core:common`、`:core:data`、`:core:designsystem`（均由 `bgmplus.android.feature` 约定插件注入）+ `androidx.navigation3.runtime`（NavKey/entry 契约）
* **被谁依赖**：仅 `:app`（导航聚合 + Koin 装配）
* **禁止依赖**：任何其他 `:feature:*` 模块

## ⚠️ 架构红线与约束
1. 对外只暴露 `ScheduleRoute`、`scheduleEntry()`、`scheduleModule` 三样东西，UI 组件不对外。
2. 条目详情跳转经 `scheduleEntry(onSubjectClick)` 回调交由 `:app` 决定（SubjectDetail 路由仍归属 `:app`），本模块不持有 `NavDisplay`/`NavController`。
3. ViewModel 一律经 entry 内的 `koinViewModel()` 获取（entry 级作用域），不使用 Activity 级注入。
