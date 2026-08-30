package com.infinitezerone.bgmplus.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.infinitezerone.bgmplus.feature.schedule.navigation.ScheduleRoute
import com.infinitezerone.bgmplus.navigation.BgmNavHost
import com.infinitezerone.bgmplus.navigation.TopLevelDestination
import com.infinitezerone.bgmplus.navigation.rememberBgmNavState

@Composable
fun BgmApp(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val navState =
        rememberBgmNavState(
            startRoute = ScheduleRoute,
            topLevelRoutes = TopLevelDestination.entries.map { it.route }.toSet(),
        )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            // 仅当前可见目的地是顶层 Tab 根部时显示底部导航栏，进入二级页面时隐藏
            if (navState.currentKey in navState.topLevelKeys) {
                NavigationBar {
                    TopLevelDestination.entries.forEach { destination ->
                        val selected = destination.route == navState.currentTopLevelKey

                        NavigationBarItem(
                            selected = selected,
                            onClick = { navState.navigateTo(destination.route) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                                    contentDescription = destination.labelText,
                                )
                            },
                            label = { Text(text = destination.labelText) },
                        )
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        BgmNavHost(navState = navState, modifier = Modifier.padding(innerPadding))
    }
}
