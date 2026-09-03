package com.infinitezerone.bgmplus.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.ExploreOff
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ViewCarousel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.infinitezerone.bgmplus.core.model.Subject
import com.infinitezerone.bgmplus.feature.search.components.ExploreFilterBottomSheet
import com.infinitezerone.bgmplus.feature.search.components.ImmersiveFeedView
import com.infinitezerone.bgmplus.feature.search.components.WaterfallSubjectCard
import org.koin.androidx.compose.koinViewModel

/**
 * 探索与发现界面：
 * - 沉浸模式：全屏沉浸式卡片上下刷（抖音 / Tinder 风格，无冗余顶部栏，大图铺满，支持下拉刷新与无限滚动）；
 * - 瀑布流模式：双列安利流（小红书 / 小黑盒风格，支持下拉刷新 + 上拉无限分页加载）；
 * - 高级筛选采用 ModalBottomSheet 悬浮抽屉，底层列表平稳不抖动；
 * - 标签采用多行展开自然流动排列，告别单排横划拥挤。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    onSubjectClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onSearchClick: () -> Unit = {},
    viewModel: ExploreViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showFilterBottomSheet by remember { mutableStateOf(false) }
    val filterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val isFilterActive =
        uiState.selectedSeason != CURRENT_SEASON ||
            uiState.selectedTag != null ||
            uiState.selectedCategory != ExploreCategory.ANIME ||
            uiState.selectedSort != ExploreSort.HEAT

    LaunchedEffect(uiState.userMessage) {
        val msg = uiState.userMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (uiState.viewMode == ExploreViewMode.IMMERSIVE) {
            // 1. 全屏沉浸模式
            when {
                uiState.isLoading && uiState.subjects.isEmpty() -> {
                    ExploreLoadingState(modifier = Modifier.fillMaxSize())
                }

                uiState.error != null && uiState.subjects.isEmpty() -> {
                    ExploreErrorState(
                        errorMessage = uiState.error ?: "加载探索内容失败",
                        onRetry = viewModel::retry,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                uiState.subjects.isEmpty() -> {
                    ExploreEmptyState(
                        onReset = { viewModel.onMoodSelect(ExploreMood.TRENDING) },
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                else -> {
                    ImmersiveFeedView(
                        subjects = uiState.subjects,
                        wishedSubjectIds = uiState.wishedSubjectIds,
                        selectedMood = uiState.selectedMood,
                        onMoodSelect = viewModel::onMoodSelect,
                        onSubjectClick = onSubjectClick,
                        onToggleWish = viewModel::toggleWish,
                        onSwitchToWaterfall = { viewModel.onViewModeChange(ExploreViewMode.WATERFALL) },
                        onSearchClick = onSearchClick,
                        onLoadMore = viewModel::loadMore,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        } else {
            // 2. 双列瀑布流模式
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = "📺 刷番发现",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        },
                        actions = {
                            // 切换至全屏沉浸流
                            IconButton(
                                onClick = { viewModel.onViewModeChange(ExploreViewMode.IMMERSIVE) },
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ViewCarousel,
                                    contentDescription = "切换至全屏沉浸流",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }

                            // 展开高级多维筛选抽屉
                            IconButton(onClick = { showFilterBottomSheet = true }) {
                                BadgedBox(
                                    badge = {
                                        if (isFilterActive) {
                                            Badge(containerColor = MaterialTheme.colorScheme.primary)
                                        }
                                    },
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.FilterList,
                                        contentDescription = "高级筛选",
                                        tint =
                                            if (isFilterActive) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                    )
                                }
                            }

                            // 搜索入口
                            IconButton(onClick = onSearchClick) {
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = "搜索",
                                )
                            }
                        },
                        colors =
                            TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                            ),
                    )
                },
                modifier = Modifier.fillMaxSize(),
            ) { innerPadding ->
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                ) {
                    // 心境/场景快捷筛选胶囊栏
                    MoodFilterRow(
                        selectedMood = uiState.selectedMood,
                        onMoodSelect = viewModel::onMoodSelect,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // 如果当前激活了非默认筛选条件，显示快捷标签展示与一键清除栏
                    if (isFilterActive) {
                        ActiveFilterPillRow(
                            selectedSeason = uiState.selectedSeason,
                            selectedCategory = uiState.selectedCategory,
                            selectedTag = uiState.selectedTag,
                            selectedSort = uiState.selectedSort,
                            onClearSeason = { viewModel.onSeasonSelect(CURRENT_SEASON) },
                            onClearCategory = { viewModel.onCategorySelect(ExploreCategory.ANIME) },
                            onClearTag = { viewModel.onTagSelect(null) },
                            onClearSort = { viewModel.onSortSelect(ExploreSort.HEAT) },
                            onResetAll = { viewModel.onMoodSelect(ExploreMood.TRENDING) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    if (uiState.isLoading && uiState.subjects.isNotEmpty()) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }

                    // 瀑布流内容（支持下拉刷新与触底分页加载）
                    PullToRefreshBox(
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = viewModel::refresh,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        when {
                            uiState.isLoading && uiState.subjects.isEmpty() -> {
                                ExploreLoadingState(modifier = Modifier.fillMaxSize())
                            }

                            uiState.error != null && uiState.subjects.isEmpty() -> {
                                ExploreErrorState(
                                    errorMessage = uiState.error ?: "加载探索内容失败",
                                    onRetry = viewModel::retry,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }

                            uiState.subjects.isEmpty() -> {
                                ExploreEmptyState(
                                    onReset = { viewModel.onMoodSelect(ExploreMood.TRENDING) },
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }

                            else -> {
                                WaterfallGridList(
                                    subjects = uiState.subjects,
                                    wishedSubjectIds = uiState.wishedSubjectIds,
                                    hasMore = uiState.hasMore,
                                    isLoadingMore = uiState.isLoadingMore,
                                    onLoadMore = viewModel::loadMore,
                                    onSubjectClick = onSubjectClick,
                                    onToggleWish = viewModel::toggleWish,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    }
                }
            }
        }

        // 高级多维筛选半屏抽屉 (ModalBottomSheet)
        if (showFilterBottomSheet) {
            ExploreFilterBottomSheet(
                sheetState = filterSheetState,
                selectedSeason = uiState.selectedSeason,
                onSeasonSelect = viewModel::onSeasonSelect,
                selectedCategory = uiState.selectedCategory,
                onCategorySelect = viewModel::onCategorySelect,
                selectedTag = uiState.selectedTag,
                onTagSelect = viewModel::onTagSelect,
                onCustomTagSubmit = viewModel::onCustomTagSubmit,
                selectedSort = uiState.selectedSort,
                onSortSelect = viewModel::onSortSelect,
                onResetAll = { viewModel.onMoodSelect(ExploreMood.TRENDING) },
                onDismiss = { showFilterBottomSheet = false },
            )
        }

        // 未登录想看拦截提示弹窗
        if (uiState.showLoginPromptDialog) {
            val context = LocalContext.current
            AlertDialog(
                onDismissRequest = viewModel::dismissLoginPrompt,
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.AccountCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp),
                    )
                },
                title = {
                    Text(
                        text = "请先登录 Bangumi 账号",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                },
                text = {
                    Text(
                        text = "一键「想看 / 追番」需要同步至您的 Bangumi 账号，登录后即可随手收藏、打卡并同步进度。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                confirmButton = {
                    Button(onClick = { viewModel.beginLogin(context) }) {
                        Text("立即登录")
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::dismissLoginPrompt) {
                        Text("稍后再说")
                    }
                },
            )
        }

        // 统一悬浮 Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
        )
    }
}

/** 心境/场景快捷胶囊筛选栏 */
@Composable
private fun MoodFilterRow(
    selectedMood: ExploreMood?,
    onMoodSelect: (ExploreMood) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        items(ExploreMood.entries) { mood ->
            FilterChip(
                selected = selectedMood == mood,
                onClick = { onMoodSelect(mood) },
                label = {
                    Text(
                        text = mood.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selectedMood == mood) FontWeight.Bold else FontWeight.Normal,
                    )
                },
                colors =
                    FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
            )
        }
    }
}

/** 生效中的筛选条件快捷展示与一键清除栏 */
@Composable
private fun ActiveFilterPillRow(
    selectedSeason: SeasonOption,
    selectedCategory: ExploreCategory,
    selectedTag: String?,
    selectedSort: ExploreSort,
    onClearSeason: () -> Unit,
    onClearCategory: () -> Unit,
    onClearTag: () -> Unit,
    onClearSort: () -> Unit,
    onResetAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        if (selectedSeason != CURRENT_SEASON) {
            item {
                ActiveFilterChip(
                    text = selectedSeason.label,
                    onClear = onClearSeason,
                )
            }
        }

        if (selectedTag != null) {
            item {
                ActiveFilterChip(
                    text = "#$selectedTag",
                    onClear = onClearTag,
                )
            }
        }

        if (selectedCategory != ExploreCategory.ANIME) {
            item {
                ActiveFilterChip(
                    text = selectedCategory.label,
                    onClear = onClearCategory,
                )
            }
        }

        if (selectedSort != ExploreSort.HEAT) {
            item {
                ActiveFilterChip(
                    text = selectedSort.label,
                    onClear = onClearSort,
                )
            }
        }

        item {
            TextButton(
                onClick = onResetAll,
                contentPadding = PaddingValues(horizontal = 8.dp),
            ) {
                Text(
                    text = "清除全部",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun ActiveFilterChip(
    text: String,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClear,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.SemiBold,
            )
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "移除",
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(12.dp),
            )
        }
    }
}

/** 双列瀑布流列表（支持上滑触底自动分页加载） */
@Composable
private fun WaterfallGridList(
    subjects: List<Subject>,
    wishedSubjectIds: Set<Long>,
    hasMore: Boolean,
    isLoadingMore: Boolean,
    onLoadMore: () -> Unit,
    onSubjectClick: (Long) -> Unit,
    onToggleWish: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyStaggeredGridState()

    // 监听触底自动触发加载下一页
    LaunchedEffect(gridState, subjects.size, hasMore, isLoadingMore) {
        snapshotFlow {
            val total = gridState.layoutInfo.totalItemsCount
            val lastVisible =
                gridState.layoutInfo.visibleItemsInfo
                    .lastOrNull()
                    ?.index ?: 0
            total > 0 && lastVisible >= total - 6
        }.collect { shouldLoad ->
            if (shouldLoad && hasMore && !isLoadingMore) {
                onLoadMore()
            }
        }
    }

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        state = gridState,
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalItemSpacing = 10.dp,
        modifier = modifier,
    ) {
        items(subjects, key = { it.id }) { subject ->
            WaterfallSubjectCard(
                subject = subject,
                isWished = wishedSubjectIds.contains(subject.id),
                onSubjectClick = onSubjectClick,
                onToggleWish = onToggleWish,
            )
        }

        // 底部加载状态提示
        item(span = StaggeredGridItemSpan.FullLine) {
            if (isLoadingMore) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text(
                            text = "正在探索更多番剧...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else if (!hasMore && subjects.isNotEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "✨ 已经到底啦，共发现 ${subjects.size} 部条目",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ExploreLoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ExploreEmptyState(
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.ExploreOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(64.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "暂无匹配条目",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "当前筛选条件下未发现条目，可尝试重置标签或切换其他场景",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onReset) {
            Text("重置为本季热门")
        }
    }
}

@Composable
private fun ExploreErrorState(
    errorMessage: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "探索加载失败",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("重试")
        }
    }
}
