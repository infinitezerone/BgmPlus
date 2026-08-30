package com.infinitezerone.bgmplus.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.serialization.NavKeySerializer
import androidx.savedstate.compose.serialization.serializers.MutableStateSerializer

/**
 * 创建可在配置变更与进程死亡后恢复的导航状态（参考 nav3 官方 multiple-backstacks recipe）：
 * 每个顶层 Tab 持有独立返回栈，切 Tab 时互不覆盖；返回在 Tab 根部时回到起始 Tab
 * （exit through home），在起始 Tab 根部时由系统正常退出应用。
 */
@Composable
fun rememberBgmNavState(
    startRoute: TopLevelRoute,
    topLevelRoutes: Set<TopLevelRoute>,
): BgmNavState {
    val topLevelRoute =
        rememberSerializable(
            startRoute,
            topLevelRoutes,
            serializer = MutableStateSerializer(NavKeySerializer()),
        ) {
            mutableStateOf<NavKey>(startRoute)
        }

    // 为每个顶层 Tab 建立独立返回栈
    val backStacks: Map<NavKey, NavBackStack<NavKey>> =
        topLevelRoutes.associateWith { key -> rememberNavBackStack(key) }

    return remember(startRoute, topLevelRoutes) {
        BgmNavState(
            startRoute = startRoute,
            topLevelRoute = topLevelRoute,
            backStacks = backStacks,
        )
    }
}

/**
 * 导航状态持有者；仅通过 [navigateTo]/[goBack] 修改自身状态
 */
class BgmNavState(
    val startRoute: TopLevelRoute,
    topLevelRoute: MutableState<NavKey>,
    private val backStacks: Map<NavKey, NavBackStack<NavKey>>,
) {
    /** 当前选中的顶层 Tab */
    var topLevelRoute: NavKey by topLevelRoute

    /** 当前 Tab 的栈顶 key，即屏幕上可见的目的地 */
    val currentKey: NavKey
        get() = backStacks.getValue(topLevelRoute).lastOrNull() ?: topLevelRoute

    /**
     * 底层 Tab key（已在返回栈表中注册）则切换 Tab；否则压入当前 Tab 的返回栈
     */
    fun navigateTo(key: NavKey) {
        if (key in backStacks.keys) {
            topLevelRoute = key
        } else {
            backStacks.getValue(topLevelRoute).add(key)
        }
    }

    fun goBack() {
        val stack = backStacks.getValue(topLevelRoute)
        val currentRoute = stack.last()
        if (currentRoute == topLevelRoute) {
            // 已在当前 Tab 根部：回到起始 Tab（exit through home）
            topLevelRoute = startRoute
        } else {
            stack.removeLastOrNull()
        }
    }

    /**
     * 将导航状态转换为带装饰器的条目列表供 [androidx.navigation3.ui.NavDisplay] 渲染。
     * 每个 Tab 使用独立的 SaveableStateHolder，保证跨 Tab 的界面状态互不覆盖。
     */
    @Composable
    fun toDecoratedEntries(entryProvider: (NavKey) -> NavEntry<NavKey>): List<NavEntry<NavKey>> {
        val decoratedEntries =
            backStacks.mapValues { (_, stack) ->
                val decorators = listOf(rememberSaveableStateHolderNavEntryDecorator<NavKey>())
                rememberDecoratedNavEntries(
                    backStack = stack,
                    entryDecorators = decorators,
                    entryProvider = entryProvider,
                )
            }

        // 只有起始 Tab 与当前 Tab 处于使用中；其余 Tab 的栈状态仍被保留，只是不参与渲染
        val topLevelRoutesInUse =
            if (topLevelRoute == startRoute) {
                listOf(startRoute)
            } else {
                listOf(startRoute, topLevelRoute)
            }
        return topLevelRoutesInUse.flatMap { decoratedEntries[it] ?: emptyList() }
    }
}
