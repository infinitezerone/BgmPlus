package com.infinitezerone.bgmplus.feature.schedule

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.infinitezerone.bgmplus.core.designsystem.component.CoverImage
import com.infinitezerone.bgmplus.core.model.AirSchedule
import com.infinitezerone.bgmplus.core.model.SiteLink
import com.infinitezerone.bgmplus.feature.schedule.components.ScheduleSourcesBottomSheet
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    onSubjectClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onSearchClick: () -> Unit = {},
) {
    val viewModel: ScheduleViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var selectedScheduleForSources by remember { mutableStateOf<AirSchedule?>(null) }

    val pagerState =
        rememberPagerState(
            initialPage = (uiState.selectedWeekday - 1).coerceIn(0, 6),
            pageCount = { 7 },
        )

    LaunchedEffect(pagerState.currentPage) {
        val targetWeekday = pagerState.currentPage + 1
        if (uiState.selectedWeekday != targetWeekday) {
            viewModel.selectWeekday(targetWeekday)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "📅 放送时刻表",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "搜索条目",
                        )
                    }
                    IconButton(onClick = viewModel::refresh) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "刷新放送表",
                        )
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            ModernDateCapsuleStrip(
                dateItems = uiState.dateItems,
                selectedWeekday = uiState.selectedWeekday,
                onSelectWeekday = { weekday ->
                    viewModel.selectWeekday(weekday)
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(weekday - 1)
                    }
                },
                watchingCountMap = (1..7).associateWith { uiState.getWatchingCountForWeekday(it) },
                totalCountMap = (1..7).associateWith { uiState.getTotalCountForWeekday(it) },
            )

            val currentWeekdayTotal = uiState.getTotalCountForWeekday(uiState.selectedWeekday)
            val currentWeekdayWatching = uiState.getWatchingCountForWeekday(uiState.selectedWeekday)

            FilterAndMetaBar(
                totalCount = currentWeekdayTotal,
                watchingCount = currentWeekdayWatching,
                onlyWatching = uiState.onlyWatching,
                onToggleOnlyWatching = viewModel::toggleOnlyWatching,
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                val weekday = page + 1
                val isToday = weekday == uiState.todayWeekday
                val daySchedules = uiState.getSortedSchedulesForWeekday(weekday)

                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = viewModel::refresh,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    when {
                        uiState.isLoading && uiState.weeklySchedules.isEmpty() -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        daySchedules.isEmpty() && uiState.error != null -> {
                            ScheduleErrorState(
                                errorMessage = uiState.error.orEmpty(),
                                onRetry = viewModel::refresh,
                            )
                        }

                        daySchedules.isEmpty() -> {
                            ScheduleEmptyState(
                                onlyWatching = uiState.onlyWatching,
                                onResetFilter = {
                                    if (uiState.onlyWatching) viewModel.toggleOnlyWatching()
                                },
                                onRefresh = viewModel::refresh,
                            )
                        }

                        else -> {
                            LazyColumn(
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                if (uiState.isOfflineCache) {
                                    item(key = "offline_cache_banner") {
                                        OfflineCacheBanner(onRetry = viewModel::refresh)
                                    }
                                }

                                if (isToday && uiState.todayWatchingSchedules.isNotEmpty() && !uiState.onlyWatching) {
                                    item(key = "today_watching_spotlight") {
                                        TodayWatchingAiringSection(
                                            schedules = uiState.todayWatchingSchedules,
                                            onSubjectClick = onSubjectClick,
                                        )
                                    }
                                }

                                items(daySchedules, key = { it.bgmId }) { schedule ->
                                    val isWatching = uiState.watchingSubjectIds.contains(schedule.bgmId)
                                    TimelineScheduleItem(
                                        schedule = schedule,
                                        isWatching = isWatching,
                                        isToday = isToday,
                                        onSubjectClick = onSubjectClick,
                                        onShowSources = { selectedScheduleForSources = it },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedScheduleForSources != null) {
        ScheduleSourcesBottomSheet(
            schedule = selectedScheduleForSources!!,
            onDismissRequest = { selectedScheduleForSources = null },
            onOpenUrl = { url -> openWebUrl(context, url) },
        )
    }
}

@Composable
private fun ModernDateCapsuleStrip(
    dateItems: List<WeekdayDateItem>,
    selectedWeekday: Int,
    onSelectWeekday: (Int) -> Unit,
    watchingCountMap: Map<Int, Int>,
    totalCountMap: Map<Int, Int>,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        items(dateItems, key = { it.weekday }) { item ->
            val isSelected = item.weekday == selectedWeekday
            val watchingCount = watchingCountMap[item.weekday] ?: 0
            val totalCount = totalCountMap[item.weekday] ?: 0

            DateCapsule(
                item = item,
                isSelected = isSelected,
                watchingCount = watchingCount,
                totalCount = totalCount,
                onClick = { onSelectWeekday(item.weekday) },
            )
        }
    }
}

@Composable
private fun DateCapsule(
    item: WeekdayDateItem,
    isSelected: Boolean,
    watchingCount: Int,
    totalCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor =
        if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else if (item.isToday) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        }

    val contentColor =
        if (isSelected) {
            MaterialTheme.colorScheme.onPrimary
        } else if (item.isToday) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurface
        }

    val borderColor =
        if (item.isToday && !isSelected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        } else {
            Color.Transparent
        }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = if (borderColor != Color.Transparent) androidx.compose.foundation.BorderStroke(1.dp, borderColor) else null,
        modifier = modifier.width(62.dp),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (item.isToday) {
                Surface(
                    shape = CircleShape,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.primary,
                ) {
                    Text(
                        text = "今天",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                    )
                }
            } else {
                Text(
                    text = item.dateLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.75f),
                )
            }

            Text(
                text = item.weekdayLabel,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (isSelected || item.isToday) FontWeight.Bold else FontWeight.Medium,
                color = contentColor,
            )

            if (watchingCount > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = if (isSelected) Color(0xFFFFD54F) else Color(0xFFFF9800),
                        modifier = Modifier.size(10.dp),
                    )
                    Text(
                        text = "$watchingCount",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color(0xFFFFD54F) else Color(0xFFFF9800),
                    )
                }
            } else if (totalCount > 0) {
                Text(
                    text = "${totalCount}部",
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.6f),
                )
            } else {
                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }
}

@Composable
private fun FilterAndMetaBar(
    totalCount: Int,
    watchingCount: Int,
    onlyWatching: Boolean,
    onToggleOnlyWatching: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !onlyWatching,
                onClick = { if (onlyWatching) onToggleOnlyWatching() },
                label = {
                    Text(
                        text = "全部 ($totalCount)",
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors =
                    FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
            )

            FilterChip(
                selected = onlyWatching,
                onClick = { if (!onlyWatching) onToggleOnlyWatching() },
                label = {
                    Text(
                        text = "⭐ 我追的 ($watchingCount)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (onlyWatching) FontWeight.Bold else FontWeight.Normal,
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors =
                    FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
            )
        }

        Text(
            text = "北京时间 CST",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
        )
    }
}

@Composable
private fun TodayWatchingAiringSection(
    schedules: List<AirSchedule>,
    onSubjectClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 8.dp),
            ) {
                Text(
                    text = "🔥 今日我追的更新",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                ) {
                    Text(
                        text = "${schedules.size} 部更新",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                    )
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(schedules, key = { "watching_spotlight_${it.bgmId}" }) { schedule ->
                    TodayWatchingMiniCard(
                        schedule = schedule,
                        onClick = { onSubjectClick(schedule.bgmId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TodayWatchingMiniCard(
    schedule: AirSchedule,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val displayName = schedule.titleCn.ifBlank { schedule.title }
    val time = schedule.timeCst.ifBlank { schedule.timeJst }
    val airStatus = getAirStatus(time, isToday = true)

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.width(130.dp),
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(8.dp)),
            ) {
                CoverImage(
                    url = schedule.coverUrl,
                    contentDescription = displayName,
                    modifier = Modifier.fillMaxSize(),
                )

                val statusText =
                    when (airStatus) {
                        AirStatus.AIRED -> "已开播"
                        AirStatus.AIRING -> "热播中"
                        AirStatus.UPCOMING -> if (time.isNotBlank()) time else "待播"
                        AirStatus.NORMAL -> time
                    }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (airStatus == AirStatus.AIRING) Color(0xFFFF5722) else Color.Black.copy(alpha = 0.75f),
                    modifier =
                        Modifier
                            .align(Alignment.BottomStart)
                            .padding(4.dp),
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            if (schedule.nextEpisodeNumber > 0) {
                Text(
                    text = "第 ${schedule.nextEpisodeNumber} 话",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun TimelineScheduleItem(
    schedule: AirSchedule,
    isWatching: Boolean,
    isToday: Boolean,
    onSubjectClick: (Long) -> Unit,
    onShowSources: (AirSchedule) -> Unit,
    modifier: Modifier = Modifier,
) {
    val time = schedule.timeCst.ifBlank { schedule.timeJst }
    val airStatus = getAirStatus(time, isToday = isToday)

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TimelineTrackRail(
            time = time,
            airStatus = airStatus,
            modifier = Modifier.width(58.dp),
        )

        ScheduleTimelineCard(
            schedule = schedule,
            isWatching = isWatching,
            onSubjectClick = onSubjectClick,
            onShowSources = onShowSources,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun TimelineTrackRail(
    time: String,
    airStatus: AirStatus,
    modifier: Modifier = Modifier,
) {
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)

    Row(
        modifier = modifier.fillMaxHeight(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            modifier =
                Modifier
                    .weight(1f)
                    .padding(top = 3.dp),
        ) {
            Text(
                text = time.ifBlank { "全天" },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                color =
                    when (airStatus) {
                        AirStatus.AIRING -> Color(0xFFFF5722)
                        AirStatus.UPCOMING -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )

            if (airStatus != AirStatus.NORMAL) {
                Text(
                    text =
                        when (airStatus) {
                            AirStatus.AIRED -> "已播"
                            AirStatus.AIRING -> "在播"
                            AirStatus.UPCOMING -> "待播"
                            AirStatus.NORMAL -> ""
                        },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color =
                        when (airStatus) {
                            AirStatus.AIRED -> MaterialTheme.colorScheme.outline
                            AirStatus.AIRING -> Color(0xFFFF5722)
                            AirStatus.UPCOMING -> MaterialTheme.colorScheme.primary
                            AirStatus.NORMAL -> Color.Transparent
                        },
                )
            }
        }

        Box(
            modifier =
                Modifier
                    .width(14.dp)
                    .fillMaxHeight()
                    .drawBehind {
                        val centerX = size.width / 2
                        drawLine(
                            color = outlineVariant,
                            start = Offset(centerX, 0f),
                            end = Offset(centerX, size.height),
                            strokeWidth = 2.dp.toPx(),
                        )
                    },
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(
                modifier =
                    Modifier
                        .padding(top = 7.dp)
                        .size(
                            when (airStatus) {
                                AirStatus.AIRING -> 12.dp
                                AirStatus.UPCOMING -> 10.dp
                                else -> 8.dp
                            },
                        ).clip(CircleShape)
                        .background(
                            when (airStatus) {
                                AirStatus.AIRING -> Color(0xFFFF5722)
                                AirStatus.UPCOMING -> MaterialTheme.colorScheme.primary
                                AirStatus.AIRED -> MaterialTheme.colorScheme.outlineVariant
                                AirStatus.NORMAL -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                            },
                        ),
            )
        }
    }
}

@Composable
private fun ScheduleTimelineCard(
    schedule: AirSchedule,
    isWatching: Boolean,
    onSubjectClick: (Long) -> Unit,
    onShowSources: (AirSchedule) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val displayName = schedule.titleCn.ifBlank { schedule.title }
    val originalTitle = schedule.title.takeIf { it.isNotBlank() && it != displayName }

    val cardColor =
        if (isWatching) {
            MaterialTheme.colorScheme.surfaceContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        }

    val cardBorder =
        if (isWatching) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        } else {
            null
        }

    Card(
        onClick = { onSubjectClick(schedule.bgmId) },
        shape = RoundedCornerShape(14.dp),
        border = cardBorder,
        colors = CardDefaults.cardColors(containerColor = cardColor),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .width(76.dp)
                        .height(106.dp)
                        .clip(RoundedCornerShape(8.dp)),
            ) {
                CoverImage(
                    url = schedule.coverUrl,
                    contentDescription = displayName,
                    modifier = Modifier.fillMaxSize(),
                )

                if (schedule.ratingScore > 0.0) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.72f),
                        shape = RoundedCornerShape(bottomStart = 8.dp),
                        modifier = Modifier.align(Alignment.TopEnd),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFB800),
                                modifier = Modifier.size(10.dp),
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = schedule.ratingScore.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )

                    if (isWatching) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.padding(start = 6.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Bookmark,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(10.dp),
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "在追",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                    }
                }

                if (originalTitle != null) {
                    Text(
                        text = originalTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 1.dp),
                ) {
                    if (schedule.nextEpisodeNumber > 0) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                        ) {
                            Text(
                                text = "第 ${schedule.nextEpisodeNumber} 话",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }

                    if (schedule.timeJst.isNotBlank()) {
                        Text(
                            text = "日本 ${schedule.timeJst}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    }
                }

                // 播放源固定行（零滚动、零手势冲突、优先常用源）
                if (schedule.siteLinks.isNotEmpty()) {
                    val sortedLinks = remember(schedule.siteLinks) { sortSiteLinks(schedule.siteLinks) }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 3.dp),
                    ) {
                        sortedLinks.firstOrNull()?.let { topLink ->
                            Surface(
                                onClick = { openWebUrl(context, topLink.playUrl) },
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                ) {
                                    Text(
                                        text = topLink.displayName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                        contentDescription = null,
                                        modifier = Modifier.size(10.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    )
                                }
                            }
                        }

                        if (sortedLinks.size > 1) {
                            Surface(
                                onClick = { onShowSources(schedule) },
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                ) {
                                    Text(
                                        text = "+${sortedLinks.size - 1} 更多源 ▾",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun sortSiteLinks(links: List<SiteLink>): List<SiteLink> {
    val priorityOrder =
        listOf(
            "bilibili",
            "gamer",
            "gamer_hk",
            "bahamut",
            "iqiyi",
            "qq",
            "youku",
            "mikan",
            "muse_tw",
            "muse_hk",
            "ani_one",
            "ani_one_asia",
            "netflix",
            "disneyplus",
            "crunchyroll",
            "abema",
            "danime",
            "unext",
            "prime",
            "nicovideo",
        )
    return links.distinctBy { it.displayName }.sortedBy { link ->
        val index = priorityOrder.indexOf(link.siteName.lowercase())
        if (index >= 0) index else 100
    }
}

@Composable
private fun OfflineCacheBanner(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
            ),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "⚠️ 离线缓存数据 · 点击重试获取最新",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            IconButton(onClick = onRetry, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "重试",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun ScheduleErrorState(
    errorMessage: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.CloudOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp),
            )
            Text(
                text = "放送表加载失败",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Button(onClick = onRetry) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "重新加载")
            }
        }
    }
}

@Composable
private fun ScheduleEmptyState(
    onlyWatching: Boolean,
    onResetFilter: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = if (onlyWatching) "⭐" else "📺",
                style = MaterialTheme.typography.displayMedium,
            )
            Text(
                text = if (onlyWatching) "今天没有您在追的番剧更新" else "这一天暂无放送计划",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = if (onlyWatching) "您可以切换至全部新番，探索更多当季精彩" else "下拉或点击刷新获取最新数据",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (onlyWatching) {
                Button(onClick = onResetFilter) {
                    Text(text = "查看全部新番")
                }
            } else {
                Button(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "刷新数据")
                }
            }
        }
    }
}

private enum class AirStatus {
    NORMAL,
    UPCOMING,
    AIRING,
    AIRED,
}

private fun getAirStatus(
    timeCst: String,
    isToday: Boolean,
): AirStatus {
    if (!isToday || timeCst.isBlank()) return AirStatus.NORMAL
    val parts = timeCst.split(":")
    if (parts.size < 2) return AirStatus.NORMAL
    val hour = parts[0].toIntOrNull() ?: return AirStatus.NORMAL
    val minute = parts[1].toIntOrNull() ?: return AirStatus.NORMAL

    val now = LocalTime.now()
    val airTime = LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59))

    return if (now.isAfter(airTime.plusMinutes(35))) {
        AirStatus.AIRED
    } else if (now.isAfter(airTime)) {
        AirStatus.AIRING
    } else {
        AirStatus.UPCOMING
    }
}

private fun openWebUrl(
    context: Context,
    url: String,
) {
    if (url.isBlank()) return
    try {
        val uri = Uri.parse(url)
        CustomTabsIntent
            .Builder()
            .setShowTitle(true)
            .build()
            .launchUrl(context, uri)
    } catch (_: Exception) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (_: Exception) {
            // Ignore if no browser can handle
        }
    }
}
