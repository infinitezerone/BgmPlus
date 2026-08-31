package com.infinitezerone.bgmplus.core.testing.repository

import com.infinitezerone.bgmplus.core.common.AppResult
import com.infinitezerone.bgmplus.core.data.repository.ScheduleRepository
import com.infinitezerone.bgmplus.core.model.AirSchedule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeScheduleRepository : ScheduleRepository {
    private val schedulesState = MutableStateFlow<Map<Int, List<AirSchedule>>>(emptyMap())

    var refreshCallCount: Int = 0
        private set
    var refreshResult: AppResult<Unit> = AppResult.Success(Unit)

    fun sendSchedules(
        weekday: Int,
        schedules: List<AirSchedule>,
    ) {
        schedulesState.value =
            schedulesState.value.toMutableMap().apply {
                put(weekday, schedules)
            }
    }

    override fun getSchedulesByWeekday(weekday: Int): Flow<List<AirSchedule>> = schedulesState.map { it[weekday].orEmpty() }

    var syncBangumiDataCallCount: Int = 0
        private set
    var syncBangumiDataResult: AppResult<Unit> = AppResult.Success(Unit)

    override suspend fun refreshSchedules(): AppResult<Unit> {
        refreshCallCount++
        return refreshResult
    }

    override suspend fun syncBangumiData(force: Boolean): AppResult<Unit> {
        syncBangumiDataCallCount++
        return syncBangumiDataResult
    }
}
