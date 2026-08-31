package com.infinitezerone.bgmplus.feature.search

import androidx.compose.runtime.Immutable
import com.infinitezerone.bgmplus.core.model.Subject

/** 季度筛选选项 */
@Immutable
data class SeasonOption(
    val id: String,
    val label: String,
    val airDateFilter: List<String>? = null,
)

/** 探索分类定义：动画 (2)、书籍 (1)、游戏 (4)、音乐 (3)、全部 (null) */
enum class ExploreCategory(
    val type: Int?,
    val label: String,
) {
    ANIME(2, "动画"),
    BOOK(1, "书籍"),
    GAME(4, "游戏"),
    MUSIC(3, "音乐"),
    ALL(null, "全部"),
}

/** 排序方式定义 */
enum class ExploreSort(
    val sortKey: String,
    val label: String,
) {
    HEAT("heat", "热门排行"),
    SCORE("score", "评分最高"),
    RANK("rank", "排名优先"),
}

val DEFAULT_SEASONS =
    listOf(
        SeasonOption("2024-q4", "2024 秋季 (10月)", listOf(">=2024-10-01", "<2025-01-01")),
        SeasonOption("2024-q3", "2024 夏季 (7月)", listOf(">=2024-07-01", "<2024-10-01")),
        SeasonOption("2024-q2", "2024 春季 (4月)", listOf(">=2024-04-01", "<2024-07-01")),
        SeasonOption("2024-q1", "2024 冬季 (1月)", listOf(">=2024-01-01", "<2024-04-01")),
        SeasonOption("2025-q1", "2025 冬季 (1月)", listOf(">=2025-01-01", "<2025-04-01")),
        SeasonOption("all", "全部时间", null),
    )

val POPULAR_GENRE_TAGS =
    listOf(
        "奇幻",
        "热血",
        "恋爱",
        "日常",
        "科幻",
        "悬疑",
        "治愈",
        "搞笑",
        "校园",
        "异世界",
        "战斗",
        "冒险",
    )

/** 探索发现界面的单一不可变 UI 状态 */
@Immutable
data class ExploreUiState(
    val selectedSeason: SeasonOption = DEFAULT_SEASONS.first(),
    val selectedCategory: ExploreCategory = ExploreCategory.ANIME,
    val selectedTag: String? = null,
    val selectedSort: ExploreSort = ExploreSort.HEAT,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val subjects: List<Subject> = emptyList(),
    val error: String? = null,
)
