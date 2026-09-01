# `:feature:schedule`

## 🎯 模块职责
提供放送 Tab 的展示与交互；通过 `:core:data` 消费放送数据，不直接访问网络或数据库。

## 🏛️ 依赖关系
* **依赖的上游**：feature convention plugin 提供的 `:core:*` 依赖与 Navigation 3
* **被谁依赖**：`:app`
* **禁止依赖**：任何其他 `:feature:*` 模块

## ⚠️ 架构红线与约束
1. 不依赖任何其他 feature。
2. 导航意图经回调交由 `:app` 聚合；模块不持有全局导航容器。
3. ViewModel 必须由导航 entry 获取，保持 entry 级作用域。
