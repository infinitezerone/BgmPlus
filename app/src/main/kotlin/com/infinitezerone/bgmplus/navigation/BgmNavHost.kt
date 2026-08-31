package com.infinitezerone.bgmplus.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.infinitezerone.bgmplus.core.navigation.BgmNavState
import com.infinitezerone.bgmplus.core.navigation.ExploreRoute
import com.infinitezerone.bgmplus.core.navigation.RakuenRoute
import com.infinitezerone.bgmplus.core.navigation.SearchRoute
import com.infinitezerone.bgmplus.core.navigation.SubjectDetailRoute
import com.infinitezerone.bgmplus.feature.schedule.navigation.scheduleEntry
import com.infinitezerone.bgmplus.feature.search.navigation.searchEntry
import com.infinitezerone.bgmplus.feature.subject.navigation.subjectEntry
import com.infinitezerone.bgmplus.feature.user.navigation.userEntry
import com.infinitezerone.bgmplus.ui.screens.ExploreScreen
import com.infinitezerone.bgmplus.ui.screens.RakuenScreen

@Composable
fun BgmNavHost(
    navState: BgmNavState,
    modifier: Modifier = Modifier,
) {
    NavDisplay(
        entries =
            navState.toEntries(
                entryProvider {
                    scheduleEntry(
                        onSubjectClick = { subjectId ->
                            navState.navigateTo(SubjectDetailRoute(subjectId))
                        },
                        onSearchClick = {
                            navState.navigateTo(SearchRoute)
                        },
                    )

                    userEntry()

                    subjectEntry(onBackClick = { navState.goBack() })

                    searchEntry(
                        onSubjectClick = {
                            navState.navigateTo(SubjectDetailRoute(it))
                        },
                        onBackClick = { navState.goBack() },
                    )

                    entry<ExploreRoute> {
                        ExploreScreen(
                            onSubjectClick = { subjectId ->
                                navState.navigateTo(SubjectDetailRoute(subjectId))
                            },
                            onSearchClick = {
                                navState.navigateTo(SearchRoute)
                            },
                        )
                    }

                    entry<RakuenRoute> {
                        RakuenScreen()
                    }
                },
            ),
        onBack = { navState.goBack() },
        modifier = modifier,
    )
}
