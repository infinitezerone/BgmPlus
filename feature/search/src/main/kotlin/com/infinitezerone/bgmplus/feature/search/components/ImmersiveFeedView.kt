package com.infinitezerone.bgmplus.feature.search.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.infinitezerone.bgmplus.core.model.Subject
import com.infinitezerone.bgmplus.feature.search.ExploreMood

/**
 * 真正的全屏沉浸式刷番流（抖音 / Tinder 沉浸形态）：
 * - 全屏海报大图铺满背景与前景渐变；
 * - 顶部浮层控制栏（模式切换、心境胶囊、搜索与分页进度）；
 * - 右侧悬浮功能列（一键追番、完整详情）；
 * - 底部左侧信息流（评分排名胶囊、大标题、播出时间、高票标签、故事简介）；
 * - 支持向下滑动无限自动加载更多。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImmersiveFeedView(
    subjects: List<Subject>,
    wishedSubjectIds: Set<Long>,
    selectedMood: ExploreMood?,
    onMoodSelect: (ExploreMood) -> Unit,
    onSubjectClick: (Long) -> Unit,
    onToggleWish: (Long) -> Unit,
    onSwitchToWaterfall: () -> Unit,
    onSearchClick: () -> Unit,
    onLoadMore: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (subjects.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { subjects.size })

    LaunchedEffect(pagerState.currentPage, subjects.size) {
        if (pagerState.currentPage >= subjects.size - 3) {
            onLoadMore()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        // 1. 全屏上下滑动 Pager
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            val subject = subjects[page]
            val isWished = wishedSubjectIds.contains(subject.id)

            ImmersiveSubjectFullScreenItem(
                subject = subject,
                isWished = isWished,
                onSubjectClick = { onSubjectClick(subject.id) },
                onToggleWish = { onToggleWish(subject.id) },
                modifier = Modifier.fillMaxSize(),
            )
        }

        // 2. 顶部浮层控制栏（带顶部下沉暗色渐变）
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    Color.Black.copy(alpha = 0.85f),
                                    Color.Black.copy(alpha = 0.5f),
                                    Color.Transparent,
                                ),
                        ),
                    ).statusBarsPadding()
                    .padding(top = 4.dp, bottom = 20.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 顶部操作行：切换回瀑布流、标题、搜索、页码
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 切换回瀑布流按钮
                    Surface(
                        onClick = onSwitchToWaterfall,
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.2f),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.GridView,
                                contentDescription = "切换为双列瀑布流",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = "瀑布流",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        }
                    }

                    // 当前翻页进度指示
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.4f),
                    ) {
                        Text(
                            text = "${pagerState.currentPage + 1} / ${subjects.size}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }

                    // 搜索按钮
                    IconButton(
                        onClick = onSearchClick,
                        modifier = Modifier.background(Color.White.copy(alpha = 0.2f), CircleShape).size(36.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "搜索",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                // 顶部心境/场景快捷筛选胶囊流
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(ExploreMood.entries) { mood ->
                        val isSelected = selectedMood == mood
                        Surface(
                            onClick = { onMoodSelect(mood) },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.45f),
                            border =
                                if (isSelected) {
                                    null
                                } else {
                                    androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        Color.White.copy(alpha = 0.2f),
                                    )
                                },
                        ) {
                            Text(
                                text = mood.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ImmersiveSubjectFullScreenItem(
    subject: Subject,
    isWished: Boolean,
    onSubjectClick: () -> Unit,
    onToggleWish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primaryTitle = subject.displayName
    val secondaryTitle = subject.name.takeIf { it.isNotBlank() && it != subject.nameCn }
    val rating = subject.rating
    val score = rating?.score ?: 0.0
    val rank = rating?.rank ?: 0
    val bestImage = subject.images?.bestImage.orEmpty()
    val epsText =
        when {
            subject.totalEpisodes > 0 -> "全 ${subject.totalEpisodes} 话"
            subject.eps > 0 -> "全 ${subject.eps} 话"
            else -> ""
        }
    val dateText = subject.date.ifBlank { subject.airDate }

    var showDetailSheet by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        // 1. 全屏底层大图（高清自适应居中裁切）
        AsyncImage(
            model = bestImage,
            contentDescription = primaryTitle,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        // 2. 沉浸式暗色渐变遮罩（顶部与底部暗化，确保文字和按键极致清晰）
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    Color.Black.copy(alpha = 0.6f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.4f),
                                    Color.Black.copy(alpha = 0.88f),
                                    Color.Black.copy(alpha = 0.96f),
                                ),
                        ),
                    ),
        )

        // 3. 右侧短视频式悬浮动作栏 (TikTok / Douyin Style)
        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // a. 追番 / 想看按钮
            ImmersiveActionButton(
                isHighlighted = isWished,
                icon = if (isWished) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                label = if (isWished) "已追番" else "想看",
                onClick = onToggleWish,
            )

            // b. 查看详情按钮
            ImmersiveActionButton(
                isHighlighted = false,
                icon = Icons.Filled.Info,
                label = "详情",
                onClick = onSubjectClick,
            )
        }

        // 4. 底部左侧信息展示区
        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(0.78f)
                    .padding(start = 16.dp, bottom = 24.dp, end = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // 评分与排名胶囊
            if (score > 0.0 || rank > 0) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (score > 0.0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFB800),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(12.dp),
                                )
                                Text(
                                    text = score.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                )
                            }
                        }
                    }

                    if (rank > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                        ) {
                            Text(
                                text = if (rank <= 100) "Top $rank" else "#$rank",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
            }

            // 主标题
            Text(
                text = primaryTitle,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            // 副标题与播出信息
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (dateText.isNotBlank()) {
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f),
                    )
                }
                if (epsText.isNotBlank()) {
                    Text(
                        text = "· $epsText",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f),
                    )
                }
            }

            // 高票特色标签
            if (subject.tags.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    maxLines = 1,
                ) {
                    subject.tags.take(4).forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.White.copy(alpha = 0.2f),
                        ) {
                            Text(
                                text = tag.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
            }

            // 故事简介（点击弹出全屏简介）
            if (subject.summary.isNotBlank()) {
                Text(
                    text = subject.summary.trim(),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                    lineHeight = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier =
                        Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { showDetailSheet = true },
                )
            }
        }

        // 5. 展开的故事梗概半屏 BottomSheet
        if (showDetailSheet) {
            ModalBottomSheet(
                onDismissRequest = { showDetailSheet = false },
                sheetState = rememberModalBottomSheetState(),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                ) {
                    Text(
                        text = primaryTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = subject.summary.trim(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp,
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

/** 右侧悬浮圆形按钮（抖音 / TikTok 形态） */
@Composable
private fun ImmersiveActionButton(
    isHighlighted: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = if (isHighlighted) 1.1f else 1.0f,
        label = "ButtonScale",
    )
    val bgColor by animateColorAsState(
        targetValue =
            if (isHighlighted) {
                Color(0xFFE91E63)
            } else {
                Color.Black.copy(alpha = 0.5f)
            },
        label = "ButtonBgColor",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = bgColor,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White.copy(alpha = 0.4f)),
            modifier = Modifier.size(48.dp).scale(scale),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}
