# `:feature:user`

## 🎯 模块职责
提供用户、认证状态与收藏入口 UI；OAuth 深链回调由 `:app` 处理。

## 🏛️ 依赖关系
* **依赖的上游**：feature convention plugin 提供的 `:core:*` 依赖与 Navigation 3
* **被谁依赖**：`:app`
* **禁止依赖**：任何其他 `:feature:*` 模块

## ⚠️ 架构红线与约束
1. 不依赖任何其他 feature。
2. 导航意图经回调交由 `:app` 聚合；模块不持有全局导航容器。
3. ViewModel 必须由导航 entry 获取，保持 entry 级作用域。
