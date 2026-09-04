package com.infinitezerone.minibgm.feature.subject

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.infinitezerone.minibgm.core.common.AppResult
import com.infinitezerone.minibgm.core.common.onError
import com.infinitezerone.minibgm.core.data.repository.CollectionRepository
import com.infinitezerone.minibgm.core.data.repository.CommunityRepository
import com.infinitezerone.minibgm.core.data.repository.SubjectRepository
import com.infinitezerone.minibgm.core.model.CollectionType
import com.infinitezerone.minibgm.core.model.Episode
import com.infinitezerone.minibgm.core.model.EpisodeComment
import com.infinitezerone.minibgm.core.model.Subject
import com.infinitezerone.minibgm.core.model.SubjectCharacter
import com.infinitezerone.minibgm.core.model.SubjectComment
import com.infinitezerone.minibgm.core.model.SubjectPerson
import com.infinitezerone.minibgm.core.model.SubjectRelation
import com.infinitezerone.minibgm.core.model.SubjectTopic
import com.infinitezerone.minibgm.core.model.UserCollection
import kotlinx.coroutines.async
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
    val characters: List<SubjectCharacter> = emptyList(),
    val persons: List<SubjectPerson> = emptyList(),
    val relations: List<SubjectRelation> = emptyList(),
    val subjectComments: List<SubjectComment> = emptyList(),
    val subjectCommentTotal: Int = 0,
    val subjectTopics: List<SubjectTopic> = emptyList(),
    val episodeComments: Map<Long, List<EpisodeComment>> = emptyMap(),
    val isEpisodeCommentsLoading: Boolean = false,
)

class SubjectDetailViewModel(
    private val subjectRepository: SubjectRepository,
    private val subjectId: Long,
    private val collectionRepository: CollectionRepository? = null,
    private val communityRepository: CommunityRepository? = null,
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

    /** 刷新/重新拉取条目、分集、角色、制作团队、关联作品与收藏数据 */
    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val subjectDeferred = async { subjectRepository.fetchSubjectDetail(subjectId) }
            val episodesDeferred = async { subjectRepository.fetchEpisodes(subjectId) }
            val collectionDeferred = async { collectionRepository?.fetchCollection(subjectId) }
            val charactersDeferred = async { subjectRepository.fetchCharacters(subjectId) }
            val personsDeferred = async { subjectRepository.fetchPersons(subjectId) }
            val relationsDeferred = async { subjectRepository.fetchRelations(subjectId) }
            val subjectCommentsDeferred = async { communityRepository?.getSubjectComments(subjectId, limit = 15) }
            val subjectTopicsDeferred = async { communityRepository?.getSubjectTopics(subjectId, limit = 5) }

            val subjectResult = subjectDeferred.await()
            val episodesResult = episodesDeferred.await()
            val collectionResult = collectionDeferred.await()
            val charactersResult = charactersDeferred.await()
            val personsResult = personsDeferred.await()
            val relationsResult = relationsDeferred.await()
            val subjectCommentsResult = subjectCommentsDeferred.await()
            val subjectTopicsResult = subjectTopicsDeferred.await()

            subjectResult.onError { _, message -> _uiState.update { it.copy(error = message) } }
            episodesResult.onError { _, message -> _uiState.update { it.copy(error = message) } }
            collectionResult?.onError { _, message -> _uiState.update { it.copy(error = message) } }

            val commentsPage = (subjectCommentsResult as? AppResult.Success)?.data
            val topics = (subjectTopicsResult as? AppResult.Success)?.data.orEmpty()

            _uiState.update { current ->
                current.copy(
                    isLoading = false,
                    subject = (subjectResult as? AppResult.Success)?.data ?: current.subject,
                    characters = (charactersResult as? AppResult.Success)?.data ?: current.characters,
                    persons = (personsResult as? AppResult.Success)?.data ?: current.persons,
                    relations = (relationsResult as? AppResult.Success)?.data ?: current.relations,
                    subjectComments = commentsPage?.data ?: current.subjectComments,
                    subjectCommentTotal = commentsPage?.total ?: current.subjectCommentTotal,
                    subjectTopics = if (topics.isNotEmpty()) topics else current.subjectTopics,
                )
            }
        }
    }

    /** 更新条目收藏状态（想看/在看/看过等，支持 0ms 本地即时乐观更新与失败回滚） */
    fun updateCollectionStatus(
        type: CollectionType,
        rate: Int? = null,
        comment: String? = null,
        private: Boolean = false,
    ) {
        val previousCollection = _uiState.value.collection
        val resolvedSubjectType = _uiState.value.subject?.type ?: previousCollection?.subjectType ?: 2
        // 1. 本地立即乐观更新 UI 状态中的 collection
        val optimisticCollection =
            previousCollection?.copy(
                type = type.value,
                rate = rate ?: previousCollection.rate,
                comment = comment ?: previousCollection.comment,
                subjectType = resolvedSubjectType,
            ) ?: UserCollection(
                userId = 0L,
                subjectId = subjectId,
                subjectType = resolvedSubjectType,
                rate = rate ?: 0,
                type = type.value,
                comment = comment.orEmpty(),
                epStatus = 0,
                volStatus = 0,
                updatedAt = "",
            )
        _uiState.update { it.copy(collection = optimisticCollection, error = null) }

        viewModelScope.launch {
            val result =
                collectionRepository?.updateCollectionStatus(
                    subjectId = subjectId,
                    type = type,
                    rate = rate,
                    comment = comment,
                    private = private,
                    subjectType = resolvedSubjectType,
                )
            result?.onError { _, message ->
                // 2. 失败回滚为原状态并提示错误
                _uiState.update { it.copy(collection = previousCollection, error = message) }
            }
        }
    }

    /** 1-tap 快捷追番/移出在看（支持 0ms 本地即时乐观更新与失败回滚） */
    fun toggleWatching() {
        val current = _uiState.value.collection
        val nextType = if (current?.type == CollectionType.DOING.value) CollectionType.DROPPED else CollectionType.DOING
        updateCollectionStatus(nextType)
    }

    /** 单集观看状态打卡（支持即时乐观更新） */
    fun toggleEpisodeWatched(
        episodeId: Long,
        isWatched: Boolean,
        epNumber: Int = 1,
    ) {
        val previousCollection = _uiState.value.collection
        // 乐观更新 UI 状态中的 collection.epStatus
        val newEpStatus = if (isWatched) maxOf(previousCollection?.epStatus ?: 0, epNumber) else maxOf(0, epNumber - 1)
        _uiState.update { state ->
            val updatedCollection =
                state.collection?.copy(
                    epStatus = newEpStatus,
                    type = if (state.collection.type == 0) CollectionType.DOING.value else state.collection.type,
                ) ?: UserCollection(
                    userId = 0L,
                    subjectId = subjectId,
                    subjectType = _uiState.value.subject?.type ?: 2,
                    rate = 0,
                    type = CollectionType.DOING.value,
                    comment = "",
                    epStatus = newEpStatus,
                    volStatus = 0,
                    updatedAt = "",
                )
            state.copy(collection = updatedCollection, error = null)
        }

        viewModelScope.launch {
            val result =
                collectionRepository?.updateEpisodeStatus(
                    subjectId = subjectId,
                    episodeId = episodeId,
                    isWatched = isWatched,
                    epNumber = epNumber,
                )
            result?.onError { _, message ->
                // 回滚
                _uiState.update { it.copy(collection = previousCollection, error = message) }
            }
        }
    }

    /** 按需加载单集吐槽（带本地内存缓存，避免重复网络请求） */
    fun loadEpisodeComments(episodeId: Long) {
        if (_uiState.value.episodeComments.containsKey(episodeId)) return
        val community = communityRepository ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isEpisodeCommentsLoading = true) }
            val result = community.getEpisodeComments(episodeId)
            _uiState.update { state ->
                val comments = (result as? AppResult.Success)?.data.orEmpty()
                state.copy(
                    isEpisodeCommentsLoading = false,
                    episodeComments = state.episodeComments + (episodeId to comments),
                )
            }
        }
    }
}
