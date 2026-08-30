# `:core:network`

## 🎯 模块职责
负责应用与外部网络的通信交互，包括 Bangumi REST v0 API 调用、bangumi-data CDN 数据抓取以及经 Cloudflare Worker 代理的 OAuth Token 交换与刷新。

## 🏛️ 依赖关系
* **依赖的上游**：`:core:model`, `:core:common`
* **被谁依赖**：`:core:data`, `:core:datastore`, `:app`
* **禁止依赖**：禁止依赖 `:core:database` 或任何 `:feature:*` 模块。

## ⚠️ 架构红线与约束
1. 所有对外部网络请求必须通过本模块统一构建的 Client 发出，确保合规注入 `User-Agent` 与超时/错误重试策略。
2. 业务 API 方法严禁暴露或接收 accessToken，所有鉴权均由底层 Auth 拦截器自动注入与透明刷新。
