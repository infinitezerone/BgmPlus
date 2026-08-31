package com.infinitezerone.bgmplus.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.infinitezerone.bgmplus.core.common.AppResult
import com.infinitezerone.bgmplus.core.data.repository.SearchRepository
import com.infinitezerone.bgmplus.core.model.SearchFilter
import com.infinitezerone.bgmplus.core.model.SearchSubjectsRequest
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 探索发现页面 ViewModel：
 * - 支持季度切换 [onSeasonSelect]；
 * - 支持类型切换 [onCategorySelect]（动画/书籍/游戏/音乐/全部）；
 * - 支持类型标签筛选 [onTagSelect]（奇幻/热血/科幻...）；
 * - 支持排序切换 [onSortSelect]（热门/评分/排名）；
 * - 支持下拉刷新 [refresh] 与错误重试 [retry]。
 */
class ExploreViewModel(
    private val searchRepository: SearchRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    private var fetchJob: Job? = null

    init {
        loadDiscovery()
    }

    fun onSeasonSelect(season: SeasonOption) {
        if (_uiState.value.selectedSeason == season) return
        _uiState.update { it.copy(selectedSeason = season) }
        loadDiscovery()
    }

    fun onCategorySelect(category: ExploreCategory) {
        if (_uiState.value.selectedCategory == category) return
        _uiState.update { it.copy(selectedCategory = category) }
        loadDiscovery()
    }

    fun onTagSelect(tag: String?) {
        val newTag = if (_uiState.value.selectedTag == tag) null else tag
        _uiState.update { it.copy(selectedTag = newTag) }
        loadDiscovery()
    }

    fun onSortSelect(sort: ExploreSort) {
        if (_uiState.value.selectedSort == sort) return
        _uiState.update { it.copy(selectedSort = sort) }
        loadDiscovery()
    }

    fun refresh() {
        loadDiscovery(isRefresh = true)
    }

    fun retry() {
        loadDiscovery()
    }

    private fun loadDiscovery(isRefresh: Boolean = false) {
        fetchJob?.cancel()
        val currentState = _uiState.value
        fetchJob =
            viewModelScope.launch {
                _uiState.update {
                    if (isRefresh) {
                        it.copy(isRefreshing = true, error = null)
                    } else {
                        it.copy(isLoading = true, error = null)
                    }
                }

                val filter =
                    SearchFilter(
                        type = currentState.selectedCategory.type?.let { listOf(it) },
                        tag = currentState.selectedTag?.let { listOf(it) },
                        airDate = currentState.selectedSeason.airDateFilter,
                        nsfw = false,
                    )
                val request =
                    SearchSubjectsRequest(
                        sort = currentState.selectedSort.sortKey,
                        filter = filter,
                    )

                when (val result = searchRepository.searchSubjectsAdvanced(request = request, limit = 30)) {
                    is AppResult.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isRefreshing = false,
                                subjects = result.data,
                                error = null,
                            )
                        }
                    }

                    is AppResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isRefreshing = false,
                                error = result.message,
                            )
                        }
                    }

                    is AppResult.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                }
            }
    }
}
