package com.infinitezerone.bgmplus.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * 顶层主 Tab 路由契约的公共父类型：仅用于收紧 [TopLevelDestination.route] 的类型边界，
 * 条目详情等二级路由不在此列
 */
sealed interface TopLevelRoute : NavKey

/**
 * 顶层主 Tab 路由契约定义；Navigation 3 的 NavKey 必须 @Serializable 以支持返回栈状态保存
 */
@Serializable
data object ScheduleRoute : TopLevelRoute

@Serializable
data object ExploreRoute : TopLevelRoute

@Serializable
data object RakuenRoute : TopLevelRoute

@Serializable
data object UserRoute : TopLevelRoute

/**
 * 二级页面路由契约定义（条目详情）
 */
@Serializable
data class SubjectDetailRoute(
    val subjectId: Long,
) : NavKey

/**
 * 底部导航栏顶层 Tab 配置枚举
 */
enum class TopLevelDestination(
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val labelText: String,
    val route: TopLevelRoute,
) {
    SCHEDULE(
        selectedIcon = Icons.Filled.CalendarMonth,
        unselectedIcon = Icons.Outlined.CalendarMonth,
        labelText = "放送",
        route = ScheduleRoute,
    ),
    EXPLORE(
        selectedIcon = Icons.Filled.Explore,
        unselectedIcon = Icons.Outlined.Explore,
        labelText = "探索",
        route = ExploreRoute,
    ),
    RAKUEN(
        selectedIcon = Icons.Filled.Forum,
        unselectedIcon = Icons.Outlined.Forum,
        labelText = "超展开",
        route = RakuenRoute,
    ),
    USER(
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person,
        labelText = "我的",
        route = UserRoute,
    ),
}
