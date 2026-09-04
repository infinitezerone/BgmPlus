package com.infinitezerone.minibgm.feature.search.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.infinitezerone.minibgm.core.navigation.ExploreRoute
import com.infinitezerone.minibgm.feature.search.ExploreScreen

/** 探索发现页面的导航条目；由 `:app` 的 BgmNavHost 聚合（NiA 模式） */
fun EntryProviderScope<NavKey>.exploreEntry(
    onSubjectClick: (Long) -> Unit,
    onSearchClick: () -> Unit = {},
) {
    entry<ExploreRoute> {
        ExploreScreen(
            onSubjectClick = onSubjectClick,
            onSearchClick = onSearchClick,
        )
    }
}
