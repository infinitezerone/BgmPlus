package com.infinitezerone.bgmplus.feature.schedule

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.infinitezerone.bgmplus.core.common.TimeUtils
import com.infinitezerone.bgmplus.core.designsystem.component.CoverImage
import com.infinitezerone.bgmplus.core.model.AirSchedule
import com.infinitezerone.bgmplus.core.model.SiteLink
import org.koin.androidx.compose.koinViewModel

private data class WeekdayTabItem(
    val weekday: Int,
    val label: String,
)

private val Weekdays =
    listOf(
        WeekdayTabItem(weekday = 1, label = "周一"),
        WeekdayTabItem(weekday = 2, label = "周二"),
        WeekdayTabItem(weekday = 3, label = "周三"),
        WeekdayTabItem(weekday = 4, label = "周四"),
        WeekdayTabItem(weekday = 5, label = "周五"),
        WeekdayTabItem(weekday = 6, label = "周六"),
        WeekdayTabItem(weekday = 7, label = "周日"),
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    onSubjectClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onSearchClick: () -> Unit = {},
) {
    val viewModel: ScheduleViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
            WeekdaySelector(
                selectedWeekday = uiState.selectedWeekday,
                todayWeekday = uiState.todayWeekday,
                onSelectWeekday = viewModel::selectWeekday,
            )

            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                when {
                    uiState.isLoading && uiState.schedules.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    uiState.schedules.isEmpty() && uiState.error != null -> {
                        ScheduleErrorState(
                            errorMessage = uiState.error.orEmpty(),
                            onRetry = viewModel::refresh,
                        )
                    }

                    uiState.schedules.isEmpty() -> {
                        ScheduleEmptyState(onRefresh = viewModel::refresh)
                    }

                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            if (uiState.isOfflineCache) {
                                item(key = "offline_cache_banner") {
                                    OfflineCacheBanner(onRetry = viewModel::refresh)
                                }
                            }

                            items(uiState.schedules, key = { it.bgmId }) { schedule ->
                                ScheduleCard(
                                    schedule = schedule,
                                    onSubjectClick = onSubjectClick,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeekdaySelector(
    selectedWeekday: Int,
    todayWeekday: Int,
    onSelectWeekday: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    PrimaryScrollableTabRow(
        selectedTabIndex = (selectedWeekday - 1).coerceIn(0, 6),
        edgePadding = 16.dp,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth(),
    ) {
        Weekdays.forEach { tab ->
            val isSelected = selectedWeekday == tab.weekday
            val isToday = todayWeekday == tab.weekday

            Tab(
                selected = isSelected,
                onClick = { onSelectWeekday(tab.weekday) },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = tab.label,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        )
                        if (isToday) {
                            Surface(
                                color =
                                    if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.primaryContainer
                                    },
                                contentColor =
                                    if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    },
                                shape = CircleShape,
                            ) {
                                Text(
                                    text = "今",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                )
                            }
                        }
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScheduleCard(
    schedule: AirSchedule,
    onSubjectClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val displayName = schedule.titleCn.ifBlank { schedule.title }
    val originalTitle = schedule.title.takeIf { it.isNotBlank() && it != displayName }

    val timeCst =
        schedule.timeCst.ifBlank {
            if (schedule.beginUtc.isNotBlank()) TimeUtils.formatToCstTime(schedule.beginUtc) else ""
        }
    val timeJst =
        schedule.timeJst.ifBlank {
            if (schedule.beginUtc.isNotBlank()) TimeUtils.formatToJstTime(schedule.beginUtc) else ""
        }

    Card(
        onClick = { onSubjectClick(schedule.bgmId) },
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CoverImage(
                url = schedule.coverUrl,
                contentDescription = displayName,
                modifier = Modifier.width(80.dp),
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // 中文主标题
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                // 日文原名副标题
                if (originalTitle != null) {
                    Text(
                        text = originalTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // 放送时间双显（CST 与 JST）与话数信息
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (timeCst.isNotBlank()) {
                        TimeBadge(label = "CST", time = timeCst)
                    }
                    if (timeJst.isNotBlank()) {
                        TimeBadge(label = "JST", time = timeJst)
                    }
                    if (schedule.nextEpisodeNumber > 0) {
                        Text(
                            text = "第 ${schedule.nextEpisodeNumber} 话",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                // 评分徽章
                if (schedule.ratingScore > 0.0) {
                    RatingBadge(score = schedule.ratingScore)
                }

                // 播放平台链接 Chips
                if (schedule.siteLinks.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        schedule.siteLinks.forEach { siteLink ->
                            SiteLinkChip(
                                siteLink = siteLink,
                                onClick = { openWebUrl(context, siteLink.playUrl) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeBadge(
    label: String,
    time: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(4.dp),
        modifier = modifier,
    ) {
        Text(
            text = "$label $time",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun RatingBadge(
    score: Double,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(6.dp),
        modifier = modifier.padding(top = 2.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = score.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun SiteLinkChip(
    siteLink: SiteLink,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AssistChip(
        onClick = onClick,
        label = {
            Text(
                text = siteLink.displayName,
                style = MaterialTheme.typography.labelSmall,
            )
        },
        trailingIcon = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
            )
        },
        colors =
            AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                trailingIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
        border = null,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.height(28.dp),
    )
}

@Composable
private fun OfflineCacheBanner(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
            ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.CloudOff,
                contentDescription = "离线缓存",
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = "网络连接不可用，正在显示本地缓存数据",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = onRetry,
                modifier = Modifier.size(24.dp),
            ) {
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
                text = "📺",
                style = MaterialTheme.typography.displayMedium,
            )
            Text(
                text = "这一天暂无放送计划",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "下拉或点击刷新获取最新数据",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
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
