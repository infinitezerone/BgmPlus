package com.infinitezerone.bgmplus.feature.subject

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.infinitezerone.bgmplus.core.common.onError
import com.infinitezerone.bgmplus.core.data.repository.CollectionRepository
import com.infinitezerone.bgmplus.core.data.repository.SubjectRepository
import com.infinitezerone.bgmplus.core.model.CollectionType
import com.infinitezerone.bgmplus.core.model.Episode
import com.infinitezerone.bgmplus.core.model.Subject
import com.infinitezerone.bgmplus.core.model.UserCollection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 条目详情页 UI 状态 */
data class SubjectDetailUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val subject: Subject? = null,
    val episodes: List<Episode> = emptyList(),
    val collection: UserCollection? = null,
)

class SubjectDetailViewModel(
    private val subjectRepository: SubjectRepository,
    private val subjectId: Long,
    private val collectionRepository: CollectionRepository? = null,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SubjectDetailUiState())
    val uiState: StateFlow<SubjectDetailUiState> = _uiState.asStateFlow()

    init {
        // 先拉取条目详情与章节（写入本地库）；错误仅转为文案，不中断流程
        refresh()
        // 订阅本地库流：fetch 写库后由 Flow 合并进 UiState（数据库为单一数据源）
        viewModelScope.launch {
            subjectRepository.getSubjectStream(subjectId).collect { subject ->
                _uiState.update { it.copy(subject = subject) }
            }
        }
        viewModelScope.launch {
            subjectRepository.getEpisodesStream(subjectId).collect { episodes ->
                _uiState.update { it.copy(episodes = episodes) }
            }
        }
        if (collectionRepository != null) {
            viewModelScope.launch {
                collectionRepository.getCollectionStream(subjectId).collect { collection ->
                    _uiState.update { it.copy(collection = collection) }
                }
            }
        }
    }

    /** 刷新/重新拉取条目、分集与收藏数据 */
    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            subjectRepository
                .fetchSubjectDetail(subjectId)
                .onError { _, message -> _uiState.update { it.copy(error = message) } }
            subjectRepository
                .fetchEpisodes(subjectId)
                .onError { _, message -> _uiState.update { it.copy(error = message) } }
            collectionRepository
                ?.fetchCollection(subjectId)
                ?.onError { _, message -> _uiState.update { it.copy(error = message) } }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    /** 更新条目收藏状态（想看/在看/看过等） */
    fun updateCollectionStatus(
        type: CollectionType,
        rate: Int? = null,
        comment: String? = null,
        private: Boolean = false,
    ) {
        viewModelScope.launch {
            collectionRepository
                ?.updateCollectionStatus(
                    subjectId = subjectId,
                    type = type,
                    rate = rate,
                    comment = comment,
                    private = private,
                )?.onError { _, message ->
                    _uiState.update { it.copy(error = message) }
                }
        }
    }

    /** 单集观看状态打卡 */
    fun toggleEpisodeWatched(
        episodeId: Long,
        isWatched: Boolean,
    ) {
        viewModelScope.launch {
            collectionRepository
                ?.updateEpisodeStatus(
                    subjectId = subjectId,
                    episodeId = episodeId,
                    isWatched = isWatched,
                )?.onError { _, message ->
                    _uiState.update { it.copy(error = message) }
                }
        }
    }
}
