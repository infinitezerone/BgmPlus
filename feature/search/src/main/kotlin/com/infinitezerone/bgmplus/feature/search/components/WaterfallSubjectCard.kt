package com.infinitezerone.bgmplus.feature.search.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.infinitezerone.bgmplus.core.designsystem.component.CoverImage
import com.infinitezerone.bgmplus.core.model.Subject

/**
 * 双列安利瀑布流卡片（小红书 / 小黑盒形态）：
 * 1. 超清海报视觉呈现与主色渐变氛围；
 * 2. 硬核评分与排名徽章（如 ★ 8.8 · Top 30）；
 * 3. 高票特色标签胶囊（Top Tags）与核心简介摘录；
 * 4. 零阻力快捷动作（右下角常驻一键「+ 想看」微交互）。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WaterfallSubjectCard(
    subject: Subject,
    isWished: Boolean,
    onSubjectClick: (Long) -> Unit,
    onToggleWish: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val primaryTitle = subject.displayName
    val rating = subject.rating
    val rank = rating?.rank ?: 0
    val score = rating?.score ?: 0.0

    // 摘录简介首句作为安利文案
    val summaryQuote =
        subject.summary
            .lineSequence()
            .map { it.trim() }
            .firstOrNull { it.isNotBlank() }
            ?.take(48)

    Card(
        onClick = { onSubjectClick(subject.id) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 1. 封面图区域（带评分/排名角标）
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                CoverImage(
                    url = subject.images?.bestImage.orEmpty(),
                    contentDescription = primaryTitle,
                    cornerRadius = 16.dp,
                    aspectRatio = 0.72f,
                    modifier = Modifier.fillMaxWidth(),
                )

                // 评分与排名胶囊（左上角浮层）
                if (score > 0.0 || rank > 0) {
                    Row(
                        modifier =
                            Modifier
                                .padding(8.dp)
                                .align(Alignment.TopStart),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (score > 0.0) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Black.copy(alpha = 0.72f),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = null,
                                        tint = Color(0xFFFFB800),
                                        modifier = Modifier.size(12.dp),
                                    )
                                    Text(
                                        text = score.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                    )
                                }
                            }
                        }

                        if (rank > 0) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                            ) {
                                Text(
                                    text = if (rank <= 100) "Top $rank" else "#$rank",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                )
                            }
                        }
                    }
                }
            }

            // 2. 信息与安利文案区
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
            ) {
                // 标题
                Text(
                    text = primaryTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 高票标签胶囊
                if (subject.tags.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        maxLines = 1,
                    ) {
                        subject.tags.take(3).forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                            ) {
                                Text(
                                    text = tag.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // 简介引言安利词
                if (!summaryQuote.isNullOrBlank()) {
                    Text(
                        text = summaryQuote,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // 底部操作栏：零阻力「+ 想看」按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val dateText = subject.date.ifBlank { subject.airDate }
                    if (dateText.isNotBlank()) {
                        Text(
                            text = dateText.take(7),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    // 想看按钮
                    WishButton(
                        isWished = isWished,
                        onClick = { onToggleWish(subject.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun WishButton(
    isWished: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor by animateColorAsState(
        targetValue =
            if (isWished) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHighest
            },
        label = "WishContainerColor",
    )
    val contentColor by animateColorAsState(
        targetValue =
            if (isWished) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        label = "WishContentColor",
    )
    val scale by animateFloatAsState(
        targetValue = if (isWished) 1.05f else 1.0f,
        label = "WishScale",
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        modifier = modifier.scale(scale),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Icon(
                imageVector = if (isWished) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                contentDescription = if (isWished) "已想看" else "想看",
                tint = contentColor,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = if (isWished) "已追" else "+ 想看",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor,
            )
        }
    }
}
