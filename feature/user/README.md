# `:feature:user`

## 🎯 模块职责
「我的」Tab：登录/登出入口、账号连接状态展示，以及后续的用户资料、收藏快捷入口等个人信息类 UI。OAuth 回调不属于本模块——深链是 app 级事件，由 `:app` 处理后经全局 Snackbar 反馈，本模块只消费 `AuthRepository` 的登录态。

## 🏛️ 依赖关系
* **依赖的上游**：`:core:model`、`:core:common`、`:core:data`、`:core:designsystem`（均由 `bgmplus.android.feature` 约定插件注入）+ `androidx.navigation3.runtime`（NavKey/entry 契约）
* **被谁依赖**：仅 `:app`（导航聚合 + Koin 装配）
* **禁止依赖**：任何其他 `:feature:*` 模块

## ⚠️ 架构红线与约束
1. 对外只暴露 `UserRoute`、`userEntry()`、`userModule` 三样东西，UI 组件不对外。
2. 导航条目经 `userEntry()` 交由 `:app` 聚合，本模块不持有 `NavDisplay`/`NavController`。
3. ViewModel 一律经 entry 内的 `koinViewModel()` 获取（entry 级作用域），不使用 Activity 级注入。
