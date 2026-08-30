package com.infinitezerone.bgmplus.core.testing.repository

import com.infinitezerone.bgmplus.core.common.AppResult
import com.infinitezerone.bgmplus.core.data.repository.SubjectRepository
import com.infinitezerone.bgmplus.core.model.Episode
import com.infinitezerone.bgmplus.core.model.Subject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeSubjectRepository : SubjectRepository {
    private val subjectsState = MutableStateFlow<Map<Long, Subject>>(emptyMap())
    private val episodesState = MutableStateFlow<Map<Long, List<Episode>>>(emptyMap())

    var fetchSubjectDetailCallCount: Int = 0
        private set
    var fetchEpisodesCallCount: Int = 0
        private set

    var fetchSubjectDetailResult: (Long) -> AppResult<Subject> = { id ->
        subjectsState.value[id]?.let { AppResult.Success(it) }
            ?: AppResult.Error(NoSuchElementException("Subject not found: $id"))
    }

    var fetchEpisodesResult: (Long) -> AppResult<List<Episode>> = { id ->
        AppResult.Success(episodesState.value[id].orEmpty())
    }

    fun sendSubject(subject: Subject) {
        subjectsState.value = subjectsState.value + (subject.id to subject)
    }

    fun sendEpisodes(
        subjectId: Long,
        episodes: List<Episode>,
    ) {
        episodesState.value = episodesState.value + (subjectId to episodes)
    }

    override fun getSubjectStream(id: Long): Flow<Subject?> = subjectsState.map { it[id] }

    override suspend fun fetchSubjectDetail(id: Long): AppResult<Subject> {
        fetchSubjectDetailCallCount++
        val result = fetchSubjectDetailResult(id)
        if (result is AppResult.Success) {
            sendSubject(result.data)
        }
        return result
    }

    override fun getEpisodesStream(subjectId: Long): Flow<List<Episode>> = episodesState.map { it[subjectId].orEmpty() }

    override suspend fun fetchEpisodes(subjectId: Long): AppResult<List<Episode>> {
        fetchEpisodesCallCount++
        val result = fetchEpisodesResult(subjectId)
        if (result is AppResult.Success) {
            sendEpisodes(subjectId, result.data)
        }
        return result
    }
}
