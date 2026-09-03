package com.infinitezerone.bgmplus.feature.schedule

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.infinitezerone.bgmplus.core.common.onError
import com.infinitezerone.bgmplus.core.common.onSuccess
import com.infinitezerone.bgmplus.core.data.repository.CollectionRepository
import com.infinitezerone.bgmplus.core.data.repository.ScheduleRepository
import com.infinitezerone.bgmplus.core.model.AirSchedule
import com.infinitezerone.bgmplus.core.model.CollectionType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/** 星期与真实日期模型 */
@Immutable
data class WeekdayDateItem(
    val weekday: Int, // 1=周一, ..., 7=周日
    val weekdayLabel: String, // "周一", "周二" ...
    val dateLabel: String, // "9/3"
    val isToday: Boolean,
)

/** 「放送」Tab 的单一不可变 UI 状态 */
@Immutable
data class ScheduleUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val selectedWeekday: Int,
    val todayWeekday: Int = LocalDate.now().dayOfWeek.value,
    val dateItems: List<WeekdayDateItem> = emptyList(),
    val weeklySchedules: Map<Int, List<AirSchedule>> = emptyMap(),
    val watchingSubjectIds: Set<Long> = emptySet(),
    val onlyWatching: Boolean = false,
) {
    /** 兼容旧接口：当前所选星期的原始番剧列表 */
    val schedules: List<AirSchedule>
        get() = weeklySchedules[selectedWeekday].orEmpty()

    /** 是否处于离线缓存展示状态：有网络错误发生但本地有缓存数据 */
    val isOfflineCache: Boolean
        get() = error != null && weeklySchedules.values.any { it.isNotEmpty() }

    /** 当前选中星期经过筛选与时间线排序后的条目 */
    val currentDaySchedules: List<AirSchedule>
        get() = getSortedSchedulesForWeekday(selectedWeekday, onlyWatching)

    /** 获取指定星期过滤并排序后的条目 */
    fun getSortedSchedulesForWeekday(
        weekday: Int,
        onlyWatchingFilter: Boolean = onlyWatching,
    ): List<AirSchedule> {
        val raw = weeklySchedules[weekday].orEmpty()
        val list =
            if (onlyWatchingFilter) {
                raw.filter { watchingSubjectIds.contains(it.bgmId) }
            } else {
                raw
            }
        return list.sortedWith(
            compareBy<AirSchedule> {
                val time = it.timeCst.ifBlank { it.timeJst }
                if (time.isNotBlank()) 0 else 1
            }.thenBy {
                it.timeCst.ifBlank { it.timeJst }
            }.thenByDescending {
                it.ratingScore
            },
        )
    }

    /** 今日正在追番的更新列表（在“今天”视图置顶呈现） */
    val todayWatchingSchedules: List<AirSchedule>
        get() {
            val todayRaw = weeklySchedules[todayWeekday].orEmpty()
            return todayRaw
                .filter { watchingSubjectIds.contains(it.bgmId) }
                .sortedWith(
                    compareBy<AirSchedule> {
                        val time = it.timeCst.ifBlank { it.timeJst }
                        if (time.isNotBlank()) 0 else 1
                    }.thenBy {
                        it.timeCst.ifBlank { it.timeJst }
                    },
                )
        }

    /** 某一天的在追番剧数量 */
    fun getWatchingCountForWeekday(weekday: Int): Int = weeklySchedules[weekday].orEmpty().count { watchingSubjectIds.contains(it.bgmId) }

    /** 某一天的总番剧数量 */
    fun getTotalCountForWeekday(weekday: Int): Int = weeklySchedules[weekday].orEmpty().size
}

/**
 * 每周放送时刻表 ViewModel：
 * - 聚合每周 7 天全部番剧，支持多页面极速手势滑动切换；
 * - 响应式监听用户在看收藏，支持“我追的更新”高亮与独立过滤；
 * - 离线优先展示，支持下拉刷新。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleViewModel(
    private val scheduleRepository: ScheduleRepository,
    private val collectionRepository: CollectionRepository,
) : ViewModel() {
    private val today = LocalDate.now()
    private val initialWeekday = today.dayOfWeek.value

    private val selectedWeekday = MutableStateFlow(initialWeekday)
    private val onlyWatching = MutableStateFlow(false)
    private val isRefreshing = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)

    private val dateItems: List<WeekdayDateItem> =
        run {
            val monday = today.minusDays((initialWeekday - 1).toLong())
            (1..7).map { weekday ->
                val date = monday.plusDays((weekday - 1).toLong())
                val weekdayLabel =
                    when (weekday) {
                        1 -> "周一"
                        2 -> "周二"
                        3 -> "周三"
                        4 -> "周四"
                        5 -> "周五"
                        6 -> "周六"
                        7 -> "周日"
                        else -> ""
                    }
                WeekdayDateItem(
                    weekday = weekday,
                    weekdayLabel = weekdayLabel,
                    dateLabel = "${date.monthValue}/${date.dayOfMonth}",
                    isToday = weekday == initialWeekday,
                )
            }
        }

    // 组合每周 1~7 天的全部放送流
    private val weeklySchedulesFlow: Flow<Map<Int, List<AirSchedule>>> =
        combine(
            (1..7).map { weekday ->
                scheduleRepository.getSchedulesByWeekday(weekday).map { weekday to it }
            },
        ) { pairs ->
            pairs.toMap()
        }

    // 响应式观察用户正在追番的条目 ID 集合
    private val watchingSubjectIdsFlow: Flow<Set<Long>> =
        collectionRepository
            .getCollectionsByTypeStream(CollectionType.DOING)
            .map { list -> list.map { it.subjectId }.toSet() }

    private val filterFlow =
        combine(selectedWeekday, onlyWatching) { weekday, onlyWatch ->
            weekday to onlyWatch
        }

    private val statusFlow =
        combine(isRefreshing, errorMessage) { refreshing, error ->
            refreshing to error
        }

    val uiState: StateFlow<ScheduleUiState> =
        combine(
            weeklySchedulesFlow,
            watchingSubjectIdsFlow,
            filterFlow,
            statusFlow,
        ) { weeklySchedules, watchingIds, (weekday, onlyWatch), (refreshing, error) ->
            ScheduleUiState(
                isLoading = refreshing && weeklySchedules.values.all { it.isEmpty() },
                isRefreshing = refreshing,
                error = error,
                selectedWeekday = weekday,
                todayWeekday = initialWeekday,
                dateItems = dateItems,
                weeklySchedules = weeklySchedules,
                watchingSubjectIds = watchingIds,
                onlyWatching = onlyWatch,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue =
                ScheduleUiState(
                    isLoading = true,
                    isRefreshing = false,
                    selectedWeekday = initialWeekday,
                    todayWeekday = initialWeekday,
                    dateItems = dateItems,
                ),
        )

    init {
        refresh()
    }

    fun selectWeekday(weekday: Int) {
        selectedWeekday.value = weekday.coerceIn(1, 7)
    }

    fun toggleOnlyWatching() {
        onlyWatching.update { !it }
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
