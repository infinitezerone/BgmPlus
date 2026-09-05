package com.infinitezerone.minibgm.feature.schedule.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.infinitezerone.minibgm.core.navigation.ScheduleRoute
import com.infinitezerone.minibgm.feature.schedule.ScheduleScreen

/** 「放送」Tab 的导航条目；由 `:app` 的 BgmNavHost 聚合（NiA 模式，feature 不感知导航容器） */
fun EntryProviderScope<NavKey>.scheduleEntry(
    onSubjectClick: (Long) -> Unit,
    onSearchClick: () -> Unit = {},
) {
    entry<ScheduleRoute> {
        ScheduleScreen(
            onSubjectClick = onSubjectClick,
            onSearchClick = onSearchClick,
        )
    }
}
