package com.infinitezerone.bgmplus.feature.schedule

import com.infinitezerone.bgmplus.core.common.AppResult
import com.infinitezerone.bgmplus.core.model.AirSchedule
import com.infinitezerone.bgmplus.core.model.CollectionType
import com.infinitezerone.bgmplus.core.model.UserCollection
import com.infinitezerone.bgmplus.core.testing.data.sampleAirScheduleList
import com.infinitezerone.bgmplus.core.testing.repository.FakeCollectionRepository
import com.infinitezerone.bgmplus.core.testing.repository.FakeScheduleRepository
import com.infinitezerone.bgmplus.core.testing.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.now().dayOfWeek.value

    @Test
    fun initTriggersRefreshAndEmitsTodaySchedules() =
        runTest {
            val repository = FakeScheduleRepository()
            val collectionRepository = FakeCollectionRepository()
            repository.sendSchedules(weekday = today, schedules = sampleAirScheduleList)
            val viewModel = ScheduleViewModel(repository, collectionRepository)

            val state = viewModel.uiState.first { it.schedules.isNotEmpty() && !it.isLoading }

            assertEquals(1, repository.refreshCallCount)
            assertNull(state.error)
            assertEquals(today, state.selectedWeekday)
            assertEquals(today, state.todayWeekday)
            assertFalse(state.isOfflineCache)
            assertEquals(7, state.dateItems.size)
            assertTrue(state.dateItems.any { it.isToday && it.weekday == today })
            assertEquals(
                "葬送的芙莉莲",
                state.schedules.first().titleCn,
            )
        }

    @Test
    fun selectWeekdaySwitchesScheduleStream() =
        runTest {
            val repository = FakeScheduleRepository()
            val collectionRepository = FakeCollectionRepository()
            repository.sendSchedules(weekday = today, schedules = sampleAirScheduleList)
            val saturdaySchedules =
                listOf(
                    AirSchedule(
                        bgmId = 2002L,
                        title = "オッドタクシー",
                        titleCn = "奇巧计程车",
                        weekday = 6,
                        timeCst = "23:30",
                    ),
                )
            repository.sendSchedules(weekday = 6, schedules = saturdaySchedules)
            val viewModel = ScheduleViewModel(repository, collectionRepository)

            viewModel.selectWeekday(6)

            val state = viewModel.uiState.first { it.selectedWeekday == 6 && it.schedules == saturdaySchedules }
            assertEquals(6, state.selectedWeekday)
            assertEquals(2002L, state.schedules.single().bgmId)
        }

    @Test
    fun watchingFilterAndTimelineSortingWorkCorrectly() =
        runTest {
            val repository = FakeScheduleRepository()
            val collectionRepository = FakeCollectionRepository()

            val fma =
                AirSchedule(
                    bgmId = 101L,
                    title = "钢之炼金术师",
                    titleCn = "钢之炼金术师",
                    weekday = today,
                    timeCst = "23:00",
                    ratingScore = 9.2,
                )
            val frieren =
                AirSchedule(
                    bgmId = 102L,
                    title = "葬送的芙莉莲",
                    titleCn = "葬送的芙莉莲",
                    weekday = today,
                    timeCst = "18:00",
                    ratingScore = 8.8,
                )
            val unknownTime =
                AirSchedule(
                    bgmId = 103L,
                    title = "某部泡面番",
                    titleCn = "某部泡面番",
                    weekday = today,
                    timeCst = "",
                    timeJst = "",
                )

            repository.sendSchedules(weekday = today, schedules = listOf(unknownTime, fma, frieren))

            // 用户正在追 frieren (102L)
            collectionRepository.sendCollection(
                UserCollection(
                    subjectId = 102L,
                    subjectType = 2,
                    type = CollectionType.DOING.value,
                ),
            )

            val viewModel = ScheduleViewModel(repository, collectionRepository)

            val initialState = viewModel.uiState.first { it.watchingSubjectIds.contains(102L) }

            // 1. 验证按时间先后排序：18:00 (frieren) -> 23:00 (fma) -> 无时间 (unknownTime)
            assertEquals(3, initialState.currentDaySchedules.size)
            assertEquals(102L, initialState.currentDaySchedules[0].bgmId)
            assertEquals(101L, initialState.currentDaySchedules[1].bgmId)
            assertEquals(103L, initialState.currentDaySchedules[2].bgmId)

            // 2. 验证今日追番 spotlight 推荐
            assertEquals(1, initialState.todayWatchingSchedules.size)
            assertEquals(102L, initialState.todayWatchingSchedules[0].bgmId)
            assertEquals(1, initialState.getWatchingCountForWeekday(today))
            assertEquals(3, initialState.getTotalCountForWeekday(today))

            // 3. 切换为仅看我追的
            viewModel.toggleOnlyWatching()
            val filteredState = viewModel.uiState.first { it.onlyWatching }
            assertEquals(1, filteredState.currentDaySchedules.size)
            assertEquals(102L, filteredState.currentDaySchedules[0].bgmId)
        }

    @Test
    fun refreshFailureSetsErrorState() =
        runTest {
            val repository = FakeScheduleRepository()
            val collectionRepository = FakeCollectionRepository()
            repository.refreshResult = AppResult.Error(RuntimeException("网络请求失败"))
            val viewModel = ScheduleViewModel(repository, collectionRepository)

            val state = viewModel.uiState.first { it.error != null && !it.isLoading }

            assertTrue(state.error!!.contains("网络请求失败"))
            assertFalse(state.isLoading)
            assertFalse(state.isOfflineCache)
            assertTrue(state.schedules.isEmpty())
        }

    @Test
    fun refreshFailureWithExistingCachePreservesDataAndSetsOfflineState() =
        runTest {
            val repository = FakeScheduleRepository()
            val collectionRepository = FakeCollectionRepository()
            repository.sendSchedules(weekday = today, schedules = sampleAirScheduleList)
            repository.refreshResult = AppResult.Error(RuntimeException("网络连接超时"))
            val viewModel = ScheduleViewModel(repository, collectionRepository)

            val state = viewModel.uiState.first { it.error != null && it.schedules.isNotEmpty() && !it.isLoading }

            assertNotNull(state.error)
            assertTrue(state.error!!.contains("网络连接超时"))
            assertTrue(state.isOfflineCache)
            assertEquals(1, state.schedules.size)
            assertEquals("葬送的芙莉莲", state.schedules.first().titleCn)
        }

    @Test
    fun refreshSuccessClearsPreviousError() =
        runTest {
            val repository = FakeScheduleRepository()
            val collectionRepository = FakeCollectionRepository()
            repository.refreshResult = AppResult.Error(RuntimeException("网络请求失败"))
            val viewModel = ScheduleViewModel(repository, collectionRepository)
            assertTrue(viewModel.uiState.first { it.error != null }.error != null)

            repository.refreshResult = AppResult.Success(Unit)
            viewModel.refresh()

            val state = viewModel.uiState.first { !it.isLoading && it.error == null }
            assertEquals(2, repository.refreshCallCount)
            assertFalse(state.isLoading)
            assertNull(state.error)
            assertFalse(state.isOfflineCache)
        }
}
