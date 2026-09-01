# `:feature:subject`

## 🎯 模块职责
提供带参数的条目详情 UI；通过 `:core:data` 读取和更新条目相关数据。

## 🏛️ 依赖关系
* **依赖的上游**：feature convention plugin 提供的 `:core:*` 依赖与 Navigation 3
* **被谁依赖**：`:app`
* **禁止依赖**：任何其他 `:feature:*` 模块

## ⚠️ 架构红线与约束
1. 不依赖任何其他 feature。
2. 路由参数与返回导航由 `:app` 提供；模块不持有全局导航容器。
3. 参数化 ViewModel 必须由对应导航 entry 创建，保持 entry 级作用域。
