package com.infinitezerone.bgmplus.feature.user.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.infinitezerone.bgmplus.feature.user.UserScreen

/** 「我的」Tab 的导航条目；由 `:app` 的 BgmNavHost 聚合（NiA 模式，feature 不感知导航容器） */
fun EntryProviderScope<NavKey>.userEntry() {
    entry<UserRoute> {
        UserScreen()
    }
}
