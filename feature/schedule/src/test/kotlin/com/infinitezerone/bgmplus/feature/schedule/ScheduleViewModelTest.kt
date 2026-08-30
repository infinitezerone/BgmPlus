package com.infinitezerone.bgmplus.feature.schedule

import com.infinitezerone.bgmplus.core.common.AppResult
import com.infinitezerone.bgmplus.core.model.AirSchedule
import com.infinitezerone.bgmplus.core.testing.data.sampleAirScheduleList
import com.infinitezerone.bgmplus.core.testing.repository.FakeScheduleRepository
import com.infinitezerone.bgmplus.core.testing.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
            repository.sendSchedules(weekday = today, schedules = sampleAirScheduleList)
            val viewModel = ScheduleViewModel(repository)

            val state = viewModel.uiState.first { it.schedules.isNotEmpty() && !it.isLoading }

            assertEquals(1, repository.refreshCallCount)
            assertNull(state.error)
            assertEquals(today, state.selectedWeekday)
            assertEquals(
                "葬送的芙莉莲",
                state.schedules.first().titleCn,
            )
        }

    @Test
    fun selectWeekdaySwitchesScheduleStream() =
        runTest {
            val repository = FakeScheduleRepository()
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
            val viewModel = ScheduleViewModel(repository)

            viewModel.selectWeekday(6)

            val state = viewModel.uiState.first { it.selectedWeekday == 6 && it.schedules == saturdaySchedules }
            assertEquals(6, state.selectedWeekday)
            assertEquals(2002L, state.schedules.single().bgmId)
        }

    @Test
    fun refreshFailureSetsErrorState() =
        runTest {
            val repository = FakeScheduleRepository()
            repository.refreshResult = AppResult.Error(RuntimeException("网络请求失败"))
            val viewModel = ScheduleViewModel(repository)

            val state = viewModel.uiState.first { it.error != null && !it.isLoading }

            assertTrue(state.error!!.contains("网络请求失败"))
            assertFalse(state.isLoading)
            assertTrue(state.schedules.isEmpty())
        }

    @Test
    fun refreshSuccessClearsPreviousError() =
        runTest {
            val repository = FakeScheduleRepository()
            repository.refreshResult = AppResult.Error(RuntimeException("网络请求失败"))
            val viewModel = ScheduleViewModel(repository)
            assertTrue(viewModel.uiState.first { it.error != null }.error != null)

            repository.refreshResult = AppResult.Success(Unit)
            viewModel.refresh()

            val state = viewModel.uiState.first { !it.isLoading && it.error == null }
            assertEquals(2, repository.refreshCallCount)
            assertFalse(state.isLoading)
            assertNull(state.error)
        }
}
