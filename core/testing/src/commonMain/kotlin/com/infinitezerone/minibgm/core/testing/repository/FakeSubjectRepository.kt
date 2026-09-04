package com.infinitezerone.minibgm.core.testing.repository

import com.infinitezerone.minibgm.core.common.AppResult
import com.infinitezerone.minibgm.core.data.repository.SubjectRepository
import com.infinitezerone.minibgm.core.model.Episode
import com.infinitezerone.minibgm.core.model.Subject
import com.infinitezerone.minibgm.core.model.SubjectCharacter
import com.infinitezerone.minibgm.core.model.SubjectPerson
import com.infinitezerone.minibgm.core.model.SubjectRelation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeSubjectRepository : SubjectRepository {
    private val subjectsState = MutableStateFlow<Map<Long, Subject>>(emptyMap())
    private val episodesState = MutableStateFlow<Map<Long, List<Episode>>>(emptyMap())
    private val charactersState = MutableStateFlow<Map<Long, List<SubjectCharacter>>>(emptyMap())
    private val personsState = MutableStateFlow<Map<Long, List<SubjectPerson>>>(emptyMap())
    private val relationsState = MutableStateFlow<Map<Long, List<SubjectRelation>>>(emptyMap())

    var fetchSubjectDetailCallCount: Int = 0
        private set
    var fetchEpisodesCallCount: Int = 0
        private set
    var fetchCharactersCallCount: Int = 0
        private set
    var fetchPersonsCallCount: Int = 0
        private set
    var fetchRelationsCallCount: Int = 0
        private set

    var fetchSubjectDetailResult: (Long) -> AppResult<Subject> = { id ->
        subjectsState.value[id]?.let { AppResult.Success(it) }
            ?: AppResult.Error(NoSuchElementException("Subject not found: $id"))
    }

    var fetchEpisodesResult: (Long) -> AppResult<List<Episode>> = { id ->
        AppResult.Success(episodesState.value[id].orEmpty())
    }

    var fetchCharactersResult: (Long) -> AppResult<List<SubjectCharacter>> = { id ->
        AppResult.Success(charactersState.value[id].orEmpty())
    }

    var fetchPersonsResult: (Long) -> AppResult<List<SubjectPerson>> = { id ->
        AppResult.Success(personsState.value[id].orEmpty())
    }

    var fetchRelationsResult: (Long) -> AppResult<List<SubjectRelation>> = { id ->
        AppResult.Success(relationsState.value[id].orEmpty())
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

    fun sendCharacters(
        subjectId: Long,
        characters: List<SubjectCharacter>,
    ) {
        charactersState.value = charactersState.value + (subjectId to characters)
    }

    fun sendPersons(
        subjectId: Long,
        persons: List<SubjectPerson>,
    ) {
        personsState.value = personsState.value + (subjectId to persons)
    }

    fun sendRelations(
        subjectId: Long,
        relations: List<SubjectRelation>,
    ) {
        relationsState.value = relationsState.value + (subjectId to relations)
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

    override suspend fun fetchCharacters(subjectId: Long): AppResult<List<SubjectCharacter>> {
        fetchCharactersCallCount++
        val result = fetchCharactersResult(subjectId)
        if (result is AppResult.Success) {
            sendCharacters(subjectId, result.data)
        }
        return result
    }

    override suspend fun fetchPersons(subjectId: Long): AppResult<List<SubjectPerson>> {
        fetchPersonsCallCount++
        val result = fetchPersonsResult(subjectId)
        if (result is AppResult.Success) {
            sendPersons(subjectId, result.data)
        }
        return result
    }

    override suspend fun fetchRelations(subjectId: Long): AppResult<List<SubjectRelation>> {
        fetchRelationsCallCount++
        val result = fetchRelationsResult(subjectId)
        if (result is AppResult.Success) {
            sendRelations(subjectId, result.data)
        }
        return result
    }
}
