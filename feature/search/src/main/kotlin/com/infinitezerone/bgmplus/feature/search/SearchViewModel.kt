package com.infinitezerone.bgmplus.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.infinitezerone.bgmplus.core.common.AppResult
import com.infinitezerone.bgmplus.core.data.repository.SearchRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 搜索功能 ViewModel：
 * - [onQueryChange] 支持 300ms 防抖自动搜索与空白自动重置；
 * - [onTypeSelect] 切换分类即时以新分类重搜；
 * - [search] 响应软键盘搜索/手动提交，跳过防抖立即执行；
 * - [clearQuery] 一键清空输入与结果。
 */
class SearchViewModel(
    private val searchRepository: SearchRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var debounceJob: Job? = null
    private var searchJob: Job? = null

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        debounceJob?.cancel()

        if (query.isBlank()) {
            searchJob?.cancel()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    results = emptyList(),
                    error = null,
                )
            }
            return
        }

        debounceJob =
            viewModelScope.launch {
                delay(DEBOUNCE_MILLIS)
                performSearch(query, _uiState.value.selectedType)
            }
    }

    fun onTypeSelect(type: Int) {
        if (_uiState.value.selectedType == type) return
        _uiState.update { it.copy(selectedType = type) }
        val currentQuery = _uiState.value.query
        if (currentQuery.isNotBlank()) {
            debounceJob?.cancel()
            performSearch(currentQuery, type)
        }
    }

    fun search() {
        val currentQuery = _uiState.value.query
        if (currentQuery.isNotBlank()) {
            debounceJob?.cancel()
            performSearch(currentQuery, _uiState.value.selectedType)
        }
    }

    fun clearQuery() {
        debounceJob?.cancel()
        searchJob?.cancel()
        _uiState.update {
            it.copy(
                query = "",
                isLoading = false,
                results = emptyList(),
                error = null,
            )
        }
    }

    private fun performSearch(
        query: String,
        type: Int,
    ) {
        searchJob?.cancel()
        searchJob =
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, error = null) }
                when (val result = searchRepository.searchSubjects(query = query.trim(), type = type)) {
                    is AppResult.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                results = result.data,
                                error = null,
                            )
                        }
                    }

                    is AppResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
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
        private const val DEBOUNCE_MILLIS = 300L
    }
}
