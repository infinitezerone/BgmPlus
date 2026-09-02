package com.infinitezerone.bgmplus.feature.search.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.infinitezerone.bgmplus.feature.search.DEFAULT_SEASONS
import com.infinitezerone.bgmplus.feature.search.ExploreCategory
import com.infinitezerone.bgmplus.feature.search.ExploreSort
import com.infinitezerone.bgmplus.feature.search.SeasonOption
import com.infinitezerone.bgmplus.feature.search.TAG_GROUPS
import com.infinitezerone.bgmplus.feature.search.TimeCategory

/**
 * 高级多维筛选半屏抽屉（Material 3 ModalBottomSheet）：
 * - 不挤压和移动主界面内容，体验丝滑；
 * - 标签采用多行 FlowRow 自然排布，分类清晰，告别单排横划拥挤；
 * - 包含条目分类、播出时间/年代、题材/厂牌/受众三组多维标签矩阵与自定义输入；
 * - 底部提供一键重置与确定筛选。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExploreFilterBottomSheet(
    sheetState: SheetState,
    selectedSeason: SeasonOption,
    onSeasonSelect: (SeasonOption) -> Unit,
    selectedCategory: ExploreCategory,
    onCategorySelect: (ExploreCategory) -> Unit,
    selectedTag: String?,
    onTagSelect: (String?) -> Unit,
    onCustomTagSubmit: (String) -> Unit,
    selectedSort: ExploreSort,
    onSortSelect: (ExploreSort) -> Unit,
    onResetAll: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var customTagText by remember { mutableStateOf("") }
    var selectedTimeCategory by remember { mutableStateOf(selectedSeason.category) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .navigationBarsPadding(),
        ) {
            // 1. 顶部标题栏与重置按钮
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "🎯 多维深度筛选",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )

                TextButton(
                    onClick = onResetAll,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("重置筛选")
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // 2. 筛选内容滚动列表
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Section A: 条目大类
                FilterSection(title = "📂 条目大类") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        ExploreCategory.entries.forEach { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = { onCategorySelect(category) },
                                label = { Text(text = category.label) },
                            )
                        }
                    }
                }

                // Section B: 排序规则
                FilterSection(title = "📊 排序规则") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        ExploreSort.entries.forEach { sort ->
                            FilterChip(
                                selected = selectedSort == sort,
                                onClick = { onSortSelect(sort) },
                                label = { Text(text = sort.label) },
                                leadingIcon =
                                    if (selectedSort == sort) {
                                        {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                            )
                                        }
                                    } else {
                                        null
                                    },
                            )
                        }
                    }
                }

                // Section C: 播出时间 / 年代
                FilterSection(title = "📅 播出时间 / 经典年代") {
                    // 时间大维度切换（按季度 / 按年份年代 / 全部）
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    ) {
                        TimeCategory.entries.forEach { cat ->
                            FilterChip(
                                selected = selectedTimeCategory == cat,
                                onClick = {
                                    selectedTimeCategory = cat
                                    if (cat == TimeCategory.ALL) {
                                        onSeasonSelect(DEFAULT_SEASONS.first { it.category == TimeCategory.ALL })
                                    }
                                },
                                label = { Text(text = cat.label, style = MaterialTheme.typography.labelSmall) },
                            )
                        }
                    }

                    // 具体时间选项 FlowRow
                    if (selectedTimeCategory != TimeCategory.ALL) {
                        val visibleSeasons = DEFAULT_SEASONS.filter { it.category == selectedTimeCategory }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            visibleSeasons.forEach { season ->
                                FilterChip(
                                    selected = selectedSeason.id == season.id,
                                    onClick = { onSeasonSelect(season) },
                                    label = { Text(text = season.label, style = MaterialTheme.typography.labelSmall) },
                                )
                            }
                        }
                    }
                }

                // Section D: 标签多维矩阵（多行展开排布）
                TAG_GROUPS.forEach { group ->
                    FilterSection(title = group.name) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            group.tags.forEach { tag ->
                                val isSelected = selectedTag == tag
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onTagSelect(tag) },
                                    label = { Text(text = tag, style = MaterialTheme.typography.labelSmall) },
                                    colors =
                                        FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        ),
                                )
                            }
                        }
                    }
                }

                // Section E: 自定义标签精准输入
                FilterSection(title = "➕ 自定义特色标签") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            OutlinedTextField(
                                value = customTagText,
                                onValueChange = { customTagText = it },
                                placeholder = { Text("输入任意 Bangumi 标签（如 赛博朋克、机娘、偶像）", fontSize = 12.sp) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions =
                                    KeyboardActions(
                                        onDone = {
                                            if (customTagText.isNotBlank()) {
                                                onCustomTagSubmit(customTagText)
                                                customTagText = ""
                                            }
                                        },
                                    ),
                                modifier = Modifier.weight(1f),
                            )

                            Button(
                                onClick = {
                                    if (customTagText.isNotBlank()) {
                                        onCustomTagSubmit(customTagText)
                                        customTagText = ""
                                    }
                                },
                                enabled = customTagText.isNotBlank(),
                            ) {
                                Text("添加")
                            }
                        }

                        // 如果当前选中的是非预设自定义标签，显示高亮芯片
                        if (selectedTag != null && !TAG_GROUPS.any { g -> g.tags.contains(selectedTag) }) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = "当前自定义生效标签:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Surface(
                                    onClick = { onTagSelect(null) },
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.primary,
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Text(
                                            text = "#$selectedTag",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "清除",
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(12.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // 3. 底部确定按钮
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            ) {
                Text("完成筛选", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FilterSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        content()
    }
}
