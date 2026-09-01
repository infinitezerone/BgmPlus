# BgmPlus

<div align="center">

**现代化 Bangumi (bgm.tv) 番组计划 Android 客户端**

追番时间表 · 番剧详情 · 收藏管理 · 条目搜索

</div>

> 🚧 项目尚处于早期开发阶段，正式版本发布前可自行构建体验。

## ✨ 功能特性

**当前可体验（基础版本）**

- [x] 🔐 **Bangumi 账号登录** — OAuth 授权登录，凭据硬件级加密、仅保存在本机
- [x] 📅 **每周放送时间表** — 按星期浏览与手动刷新
- [x] 📺 **番剧详情** — 条目资料与章节列表
- [x] 📚 **收藏管理** — 浏览收藏与更新观看进度
- [x] 🔍 **搜索与发现** — 关键词、类型筛选与探索入口

**持续完善**

- [ ] 开播提醒、更多收藏操作与同步体验
- [ ] 搜索筛选与发现体验的持续完善

## 📥 获取应用

正式版本尚未发布。当前可通过源码自行构建（需要 JDK 25 与 Android SDK）：

```bash
git clone https://github.com/infinitezerone/BgmPlus.git
cd BgmPlus
./gradlew :app:assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

## 📄 声明

本项目为个人学习用途的开源作品，仅使用 Bangumi 开放 API；账号数据仅存于本机，使用本软件产生的任何问题由使用者自行承担。

## 🙏 致谢

- [Bangumi 番组计划](https://bgm.tv) 与其开放的 API
