package com.infinitezerone.bgmplus.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.infinitezerone.bgmplus.LoginViewModel
import com.infinitezerone.bgmplus.ui.screens.ExploreScreen
import com.infinitezerone.bgmplus.ui.screens.RakuenScreen
import com.infinitezerone.bgmplus.ui.screens.ScheduleScreen
import com.infinitezerone.bgmplus.ui.screens.SubjectDetailScreen
import com.infinitezerone.bgmplus.ui.screens.UserScreen

@Composable
fun BgmNavHost(
    navState: BgmNavState,
    loginViewModel: LoginViewModel,
    modifier: Modifier = Modifier,
) {
    NavDisplay(
        entries =
            navState.toDecoratedEntries(
                entryProvider {
                    entry<ScheduleRoute> {
                        ScheduleScreen(
                            onSubjectClick = { subjectId ->
                                navState.navigateTo(SubjectDetailRoute(subjectId))
                            },
                        )
                    }

                    entry<ExploreRoute> {
                        ExploreScreen(
                            onSubjectClick = { subjectId ->
                                navState.navigateTo(SubjectDetailRoute(subjectId))
                            },
                        )
                    }

                    entry<RakuenRoute> {
                        RakuenScreen()
                    }

                    entry<UserRoute> {
                        UserScreen(viewModel = loginViewModel)
                    }

                    entry<SubjectDetailRoute> { route ->
                        SubjectDetailScreen(
                            subjectId = route.subjectId,
                            onBackClick = { navState.goBack() },
                        )
                    }
                },
            ),
        onBack = { navState.goBack() },
        modifier = modifier,
    )
}
