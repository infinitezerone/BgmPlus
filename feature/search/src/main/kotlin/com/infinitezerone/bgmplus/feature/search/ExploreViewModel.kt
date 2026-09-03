package com.infinitezerone.bgmplus.feature.search

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.infinitezerone.bgmplus.core.common.AppResult
import com.infinitezerone.bgmplus.core.data.repository.AuthRepository
import com.infinitezerone.bgmplus.core.data.repository.CollectionRepository
import com.infinitezerone.bgmplus.core.data.repository.SearchRepository
import com.infinitezerone.bgmplus.core.model.CollectionType
import com.infinitezerone.bgmplus.core.model.SearchFilter
import com.infinitezerone.bgmplus.core.model.SearchSubjectsRequest
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 探索发现页面 ViewModel：
 * - 支持双模式切换 [onViewModeChange]（双列瀑布流 / 全屏沉浸流）；
 * - 支持心境/场景筛选 [onMoodSelect]（本季热门/高分神作/治愈/热血...）；
 * - 支持未登录拦截并弹窗引导登录 [toggleWish]、[beginLogin]、[dismissLoginPrompt]；
 * - 支持季度/年份/年代全维度时间切换 [onSeasonSelect]；
 * - 支持类型切换 [onCategorySelect]（动画/书籍/游戏/音乐/全部）；
 * - 支持预设与自定义标签筛选 [onTagSelect]、[onCustomTagSubmit]；
 * - 支持排序切换 [onSortSelect]（热门/评分/排名）；
 * - 支持下拉刷新 [refresh]、上拉无限分页加载更多 [loadMore] 与错误重试 [retry]。
 */
class ExploreViewModel(
    private val searchRepository: SearchRepository,
    private val collectionRepository: CollectionRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    private var fetchJob: Job? = null
    private var loadMoreJob: Job? = null

    init {
        observeAuth()
        observeCollections()
        loadDiscovery()
    }

    private fun observeAuth() {
        viewModelScope.launch {
            authRepository.isLoggedIn.collect { loggedIn ->
                _uiState.update { it.copy(isLoggedIn = loggedIn) }
            }
        }
    }

    private fun observeCollections() {
        viewModelScope.launch {
            combine(
                collectionRepository.getCollectionsByTypeStream(CollectionType.WISH),
                collectionRepository.getCollectionsByTypeStream(CollectionType.DOING),
                collectionRepository.getCollectionsByTypeStream(CollectionType.COLLECT),
            ) { wish, doing, collect ->
                (wish + doing + collect).map { it.subjectId }.toSet()
            }.catch {
                // Ignore errors from collection stream
            }.collect { wishedIds ->
                _uiState.update { it.copy(wishedSubjectIds = wishedIds) }
            }
        }
    }

    fun onViewModeChange(mode: ExploreViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
    }

    fun onMoodSelect(mood: ExploreMood) {
        if (_uiState.value.selectedMood == mood) return
        _uiState.update {
            val season =
                when (mood) {
                    ExploreMood.TRENDING -> CURRENT_SEASON
                    else -> ALL_TIME_SEASON
                }
            it.copy(
                selectedMood = mood,
                selectedTag = mood.tag,
                selectedSort = mood.sort,
                selectedSeason = season,
            )
        }
        loadDiscovery()
    }

    fun toggleWish(subjectId: Long) {
        if (!_uiState.value.isLoggedIn) {
            _uiState.update { it.copy(showLoginPromptDialog = true) }
            return
        }

        val isWished = _uiState.value.wishedSubjectIds.contains(subjectId)
        viewModelScope.launch {
            if (isWished) {
                _uiState.update { it.copy(userMessage = "该番剧已在您的追番列表中") }
            } else {
                _uiState.update {
                    it.copy(
                        wishedSubjectIds = it.wishedSubjectIds + subjectId,
                        userMessage = "已加入「想看」列表",
                    )
                }
                when (val result = collectionRepository.updateCollectionStatus(subjectId, CollectionType.WISH)) {
                    is AppResult.Success -> Unit
                    is AppResult.Error -> {
                        _uiState.update {
                            it.copy(
                                userMessage = result.message.ifBlank { "添加收藏失败，请先确认登录状态" },
                            )
                        }
                    }
                    is AppResult.Loading -> Unit
                }
            }
        }
    }

    fun beginLogin(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(showLoginPromptDialog = false) }
            val authorizeUrl = authRepository.beginLogin()
            val customTabsIntent =
                CustomTabsIntent
                    .Builder()
                    .setEphemeralBrowsingEnabled(true)
                    .build()
            customTabsIntent.launchUrl(context, Uri.parse(authorizeUrl))
        }
    }

    fun dismissLoginPrompt() {
        _uiState.update { it.copy(showLoginPromptDialog = false) }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    fun onSeasonSelect(season: SeasonOption) {
        if (_uiState.value.selectedSeason == season) return
        _uiState.update { it.copy(selectedSeason = season, selectedMood = null) }
        loadDiscovery()
    }

    fun onCategorySelect(category: ExploreCategory) {
        if (_uiState.value.selectedCategory == category) return
        _uiState.update { it.copy(selectedCategory = category, selectedMood = null) }
        loadDiscovery()
    }

    fun onTagSelect(tag: String?) {
        val newTag = if (_uiState.value.selectedTag == tag) null else tag
        _uiState.update {
            val season = if (newTag != null && it.selectedSeason == CURRENT_SEASON) ALL_TIME_SEASON else it.selectedSeason
            it.copy(selectedTag = newTag, selectedSeason = season, selectedMood = null)
        }
        loadDiscovery()
    }

    fun onCustomTagSubmit(customTag: String) {
        val trimmed = customTag.trim()
        if (trimmed.isBlank()) return
        _uiState.update {
            val season = if (it.selectedSeason == CURRENT_SEASON) ALL_TIME_SEASON else it.selectedSeason
            it.copy(selectedTag = trimmed, selectedSeason = season, selectedMood = null)
        }
        loadDiscovery()
    }

    fun onSortSelect(sort: ExploreSort) {
        if (_uiState.value.selectedSort == sort) return
        _uiState.update { it.copy(selectedSort = sort, selectedMood = null) }
        loadDiscovery()
    }

    fun refresh() {
        loadDiscovery(isRefresh = true)
    }

    fun retry() {
        loadDiscovery()
    }

    fun loadMore() {
        val currentState = _uiState.value
        if (currentState.isLoading || currentState.isLoadingMore || currentState.isRefreshing || !currentState.hasMore) {
            return
        }

        loadMoreJob?.cancel()
        loadMoreJob =
            viewModelScope.launch {
                _uiState.update { it.copy(isLoadingMore = true) }

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

                val offset = currentState.subjects.size
                when (
                    val result =
                        searchRepository.searchSubjectsAdvanced(
                            request = request,
                            limit = PAGE_SIZE,
                            offset = offset,
                        )
                ) {
                    is AppResult.Success -> {
                        val newSubjects = result.data
                        _uiState.update {
                            val existingIds = it.subjects.map { s -> s.id }.toSet()
                            val uniqueNew = newSubjects.filter { s -> s.id !in existingIds }
                            it.copy(
                                isLoadingMore = false,
                                subjects = it.subjects + uniqueNew,
                                hasMore = newSubjects.size >= PAGE_SIZE,
                                pageOffset = offset + newSubjects.size,
                            )
                        }
                    }

                    is AppResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoadingMore = false,
                                userMessage = "加载更多失败：${result.message}",
                            )
                        }
                    }

                    is AppResult.Loading -> Unit
                }
            }
    }

    private fun loadDiscovery(isRefresh: Boolean = false) {
        fetchJob?.cancel()
        loadMoreJob?.cancel()
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

                when (val result = searchRepository.searchSubjectsAdvanced(request = request, limit = PAGE_SIZE, offset = 0)) {
                    is AppResult.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isRefreshing = false,
                                subjects = result.data,
                                hasMore = result.data.size >= PAGE_SIZE,
                                pageOffset = result.data.size,
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

    companion object {
        private const val PAGE_SIZE = 20
    }
}
