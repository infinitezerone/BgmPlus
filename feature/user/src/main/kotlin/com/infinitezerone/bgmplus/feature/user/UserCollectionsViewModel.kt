package com.infinitezerone.bgmplus.feature.user

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.infinitezerone.bgmplus.core.common.onError
import com.infinitezerone.bgmplus.core.common.onSuccess
import com.infinitezerone.bgmplus.core.data.repository.AuthRepository
import com.infinitezerone.bgmplus.core.data.repository.CollectionRepository
import com.infinitezerone.bgmplus.core.model.CollectionType
import com.infinitezerone.bgmplus.core.model.UserCollection
import com.infinitezerone.bgmplus.core.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 条目类别筛选（全部、动画、书籍、游戏、音乐）
 */
enum class CollectionSubjectFilter(
    val typeId: Int,
    val label: String,
) {
    ALL(0, "全部"),
    ANIME(2, "动画"),
    BOOK(1, "书籍"),
    GAME(4, "游戏"),
    MUSIC(3, "音乐"),
}

/** 用户收藏列表 UI 状态 */
@Immutable
data class UserCollectionsUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
    val activeProfile: UserProfile? = null,
    val selectedType: CollectionType = CollectionType.DOING,
    val selectedSubjectFilter: CollectionSubjectFilter = CollectionSubjectFilter.ALL,
    val collections: List<UserCollection> = emptyList(),
    val updatingSubjectIds: Set<Long> = emptySet(),
)

class UserCollectionsViewModel(
    private val collectionRepository: CollectionRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(UserCollectionsUiState())
    val uiState: StateFlow<UserCollectionsUiState> = _uiState.asStateFlow()

    private var isInitialized = false

    init {
        viewModelScope.launch {
            authRepository.isLoggedIn.collect { loggedIn ->
                _uiState.update { it.copy(isLoggedIn = loggedIn) }
            }
        }
        viewModelScope.launch {
            authRepository.activeProfile.collect { profile ->
                _uiState.update { it.copy(activeProfile = profile) }
                if (isInitialized && profile != null) {
                    loadCollections(isRefresh = false)
                }
            }
        }
    }

    fun setInitialType(type: CollectionType) {
        _uiState.update { it.copy(selectedType = type) }
        loadCollections(isRefresh = false)
    }

    fun selectType(type: CollectionType) {
        if (_uiState.value.selectedType != type) {
            _uiState.update { it.copy(selectedType = type) }
            loadCollections(isRefresh = false)
        }
    }

    fun selectSubjectFilter(filter: CollectionSubjectFilter) {
        if (_uiState.value.selectedSubjectFilter != filter) {
            _uiState.update { it.copy(selectedSubjectFilter = filter) }
            loadCollections(isRefresh = false)
        }
    }

    fun refresh() {
        loadCollections(isRefresh = true)
    }

    private fun loadCollections(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) {
                _uiState.update { it.copy(isRefreshing = true, error = null) }
            } else {
                _uiState.update { it.copy(isLoading = true, error = null) }
            }

            val profile = _uiState.value.activeProfile ?: authRepository.activeProfile.first()
            val username =
                profile?.username?.ifBlank { profile.id.toString() }
                    ?: profile?.id?.takeIf { it > 0 }?.toString()

            if (username.isNullOrBlank()) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = "请先登录 Bangumi 账号",
                    )
                }
                return@launch
            }

            val type = _uiState.value.selectedType
            val subjectType = _uiState.value.selectedSubjectFilter.typeId

            collectionRepository
                .fetchUserCollections(
                    username = username,
                    subjectType = subjectType,
                    type = type,
                    limit = 50,
                ).onSuccess { data ->
                    _uiState.update {
                        it.copy(
                            collections = data,
                            error = null,
                            isLoading = false,
                            isRefreshing = false,
                        )
                    }
                }.onError { _, message ->
                    _uiState.update {
                        it.copy(
                            error = message,
                            isLoading = false,
                            isRefreshing = false,
                        )
                    }
                }
        }
    }

    fun incrementEpisodeProgress(collection: UserCollection) {
        val nextEp = collection.epStatus + 1
        val total =
            collection.subject?.totalEpisodes?.takeIf { it > 0 }
                ?: collection.subject?.eps?.takeIf { it > 0 }
        if (total != null && nextEp > total) return

        val subjectId = collection.subjectId
        if (_uiState.value.updatingSubjectIds.contains(subjectId)) return

        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    updatingSubjectIds = state.updatingSubjectIds + subjectId,
                    collections =
                        state.collections.map { item ->
                            if (item.subjectId == subjectId) item.copy(epStatus = nextEp) else item
                        },
                )
            }

            val result =
                collectionRepository.updateCollectionStatus(
                    subjectId = subjectId,
                    type = CollectionType.fromValue(collection.type),
                    rate = collection.rate.takeIf { it > 0 },
                    comment = collection.comment.ifBlank { null },
                    epStatus = nextEp,
                )

            result.onError { _, message ->
                _uiState.update { state ->
                    state.copy(
                        collections =
                            state.collections.map { item ->
                                if (item.subjectId == subjectId) item.copy(epStatus = collection.epStatus) else item
                            },
                        error = message,
                    )
                }
            }

            _uiState.update { state ->
                state.copy(updatingSubjectIds = state.updatingSubjectIds - subjectId)
            }
        }
    }
}
