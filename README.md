# BgmPlus

<div align="center">

**现代化 Bangumi (bgm.tv) 番组计划 Android 客户端**

追番时间表 · 番剧详情 · 收藏管理 · 条目搜索

基于 Kotlin Multiplatform + Jetpack Compose (Material 3 Expressive) 构建

</div>

> 🚧 项目尚处于早期开发阶段，正式版本发布前可自行构建体验。

## ✨ 功能特性

**已实现**

- 🔐 **Bangumi 账号登录** — 标准 OAuth 2.0 授权登录，凭据硬件级加密安全存储

**开发计划**

- 📅 **每周放送时间表** — 按星期分组浏览每日更新番剧，开播提醒
- 📺 **番剧详情** — 条目资料、章节列表、观看进度跟踪
- 📚 **收藏管理** — 想看 / 看过 / 在看状态与 Bangumi 账号同步
- 🔍 **搜索与发现** — 按关键词与标签查找番剧

## 📥 获取应用

正式版本尚未发布。当前可通过源码自行构建：

```bash
git clone https://github.com/infinitezerone/BgmPlus.git
cd BgmPlus
./gradlew :app:assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

环境要求：JDK 25 与 Android SDK。

## 🛡️ 安全与隐私

- OAuth 凭据兑换经自维护的服务端代理完成，`client_secret` 永不进入 APK；
- 登录链路内置防伪造与防回调拦截校验；
- Token 使用 AndroidKeyStore 硬件密钥加密存储，并被排除出系统备份；
- Release 构建关闭日志输出并启用 R8 混淆。

## 🧱 技术栈

Kotlin Multiplatform · Jetpack Compose (Material 3 Expressive) · Ktor 3 · Room 3 · DataStore · Coil 3 · Koin 4 · Cloudflare Workers（登录代理）

## 🔨 参与开发

构建流程、模块架构与编码规范见 [AGENTS.md](AGENTS.md)。快速命令：

```bash
./gradlew spotlessCheck        # 代码风格检查
./gradlew allTests             # 全量单元测试
./gradlew :app:assembleDebug   # 编译调试 APK
```

## 🙏 致谢

- [Bangumi 番组计划](https://bgm.tv) 与其开放的 API
- [Now in Android](https://github.com/android/nowinandroid) — 架构参考
