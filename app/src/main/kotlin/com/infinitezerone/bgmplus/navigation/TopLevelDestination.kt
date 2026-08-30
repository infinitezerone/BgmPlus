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
import com.infinitezerone.bgmplus.feature.user.navigation.UserRoute
import kotlinx.serialization.Serializable

/**
 * 仍归属 `:app` 的顶层 Tab 路由契约；UserRoute、SubjectDetailRoute 已随各自 feature 迁出，
 * schedule 等后续建 feature 时同样迁往各自模块，`:app` 只做聚合引用
 */
@Serializable
data object ScheduleRoute : NavKey

@Serializable
data object ExploreRoute : NavKey

@Serializable
data object RakuenRoute : NavKey

/**
 * 底部导航栏顶层 Tab 配置枚举；route 为各 feature/`:app` 声明的 NavKey
 */
enum class TopLevelDestination(
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val labelText: String,
    val route: NavKey,
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
