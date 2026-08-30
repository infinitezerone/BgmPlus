package com.infinitezerone.bgmplus.feature.subject

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.infinitezerone.bgmplus.core.common.onError
import com.infinitezerone.bgmplus.core.data.repository.SubjectRepository
import com.infinitezerone.bgmplus.core.model.Episode
import com.infinitezerone.bgmplus.core.model.Subject
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
)

class SubjectDetailViewModel(
    private val subjectRepository: SubjectRepository,
    private val subjectId: Long,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SubjectDetailUiState())
    val uiState: StateFlow<SubjectDetailUiState> = _uiState.asStateFlow()

    init {
        // 先拉取条目详情与章节（写入本地库）；错误仅转为文案，不中断流程
        viewModelScope.launch {
            subjectRepository
                .fetchSubjectDetail(subjectId)
                .onError { _, message -> _uiState.update { it.copy(error = message) } }
            subjectRepository
                .fetchEpisodes(subjectId)
                .onError { _, message -> _uiState.update { it.copy(error = message) } }
            _uiState.update { it.copy(isLoading = false) }
        }
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
    }
}
