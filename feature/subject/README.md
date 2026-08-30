# `:feature:subject`

## 🎯 模块职责
条目详情页：按 `subjectId` 展示 Bangumi 条目信息（封面、展示名、放送日期、话数、评分/排名、简介）与章节列表（话数、标题、播出日期）。数据经 `SubjectRepository` 先 fetch 后订阅本地流，以数据库为单一数据源。

## 🏛️ 依赖关系
* **依赖的上游**：`:core:model`、`:core:common`、`:core:data`、`:core:designsystem`（均由 `bgmplus.android.feature` 约定插件注入）+ `androidx.navigation3.runtime`（NavKey/entry 契约）
* **被谁依赖**：仅 `:app`（导航聚合 + Koin 装配）
* **禁止依赖**：任何其他 `:feature:*` 模块

## ⚠️ 架构红线与约束
1. 对外只暴露 `SubjectDetailRoute`、`subjectEntry()`、`subjectModule` 三样东西，UI 组件不对外。
2. 导航条目经 `subjectEntry()` 交由 `:app` 聚合，本模块不持有 `NavDisplay`/`NavController`；返回键回调由 `:app` 注入（即 `navState.goBack()`）。
3. ViewModel 一律经 entry 内的 `koinViewModel(parameters = { parametersOf(subjectId) })` 获取（entry 级作用域），不使用 Activity 级注入。
