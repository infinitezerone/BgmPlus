package com.infinitezerone.bgmplus.feature.search

import androidx.compose.runtime.Immutable
import com.infinitezerone.bgmplus.core.model.Subject

/** 搜索分类定义：全部 (0)、动画 (2)、书籍 (1)、游戏 (4)、音乐 (3) */
enum class SearchCategory(
    val type: Int,
    val label: String,
) {
    ALL(0, "全部"),
    ANIME(2, "动画"),
    BOOK(1, "书籍"),
    GAME(4, "游戏"),
    MUSIC(3, "音乐"),
    ;

    companion object {
        fun fromType(type: Int): SearchCategory = entries.firstOrNull { it.type == type } ?: ALL
    }
}

/** 搜索界面的单一不可变 UI 状态 */
@Immutable
data class SearchUiState(
    val query: String = "",
    val selectedType: Int = 0,
    val isLoading: Boolean = false,
    val results: List<Subject> = emptyList(),
    val error: String? = null,
)
