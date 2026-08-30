package com.infinitezerone.bgmplus.feature.subject.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** 条目详情页路由；由 `:app` 的 BgmNavHost 经 subjectEntry() 聚合 */
@Serializable
data class SubjectDetailRoute(
    val subjectId: Long,
) : NavKey
