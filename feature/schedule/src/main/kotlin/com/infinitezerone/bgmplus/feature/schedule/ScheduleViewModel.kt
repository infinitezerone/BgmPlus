package com.infinitezerone.bgmplus.feature.schedule

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.infinitezerone.bgmplus.core.common.onError
import com.infinitezerone.bgmplus.core.common.onSuccess
import com.infinitezerone.bgmplus.core.data.repository.ScheduleRepository
import com.infinitezerone.bgmplus.core.model.AirSchedule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** 「放送」Tab 的单一不可变 UI 状态 */
@Immutable
data class ScheduleUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val selectedWeekday: Int,
    val todayWeekday: Int = LocalDate.now().dayOfWeek.value,
    val schedules: List<AirSchedule> = emptyList(),
) {
    /** 是否处于离线缓存展示状态：有网络错误发生但本地有缓存数据 */
    val isOfflineCache: Boolean
        get() = error != null && schedules.isNotEmpty()
}

/**
 * 每周放送时间表：
 * - [selectedWeekday] 变更经 flatMapLatest 切换 Room 流，离线优先展示；
 * - 进入 Tab 时触发一次 [refresh]，失败转成 [ScheduleUiState.error] 文案；
 * - 支持下拉刷新与手动刷新。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleViewModel(
    private val scheduleRepository: ScheduleRepository,
) : ViewModel() {
    // 与 core:common TimeUtils 的 weekday 语义一致：1=周一 … 7=周日
    private val initialWeekday = LocalDate.now().dayOfWeek.value

    private val selectedWeekday = MutableStateFlow(initialWeekday)
    private val isRefreshing = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ScheduleUiState> =
        combine(
            selectedWeekday.flatMapLatest(scheduleRepository::getSchedulesByWeekday),
            isRefreshing,
            errorMessage,
            selectedWeekday,
        ) { schedules, refreshing, error, weekday ->
            ScheduleUiState(
                isLoading = refreshing,
                isRefreshing = refreshing,
                error = error,
                selectedWeekday = weekday,
                todayWeekday = LocalDate.now().dayOfWeek.value,
                schedules = schedules,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue =
                ScheduleUiState(
                    isLoading = true,
                    isRefreshing = false,
                    selectedWeekday = initialWeekday,
                ),
        )

    init {
        refresh()
    }

    fun selectWeekday(weekday: Int) {
        selectedWeekday.value = weekday
    }

    fun refresh() {
        viewModelScope.launch {
            isRefreshing.value = true
            scheduleRepository
                .refreshSchedules()
                .onSuccess { errorMessage.value = null }
                .onError { _, message -> errorMessage.value = message }
            isRefreshing.value = false
        }
    }
}
