package com.infinitezerone.bgmplus.feature.subject.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.infinitezerone.bgmplus.core.navigation.SubjectDetailRoute
import com.infinitezerone.bgmplus.feature.subject.SubjectDetailScreen

/** 条目详情页的导航条目；由 `:app` 的 BgmNavHost 聚合（NiA 模式，feature 不感知导航容器） */
fun EntryProviderScope<NavKey>.subjectEntry(
    onBackClick: () -> Unit,
    onSubjectClick: (Long) -> Unit = {},
) {
    entry<SubjectDetailRoute> { route ->
        SubjectDetailScreen(
            subjectId = route.subjectId,
            onBackClick = onBackClick,
            onSubjectClick = onSubjectClick,
        )
    }
}
