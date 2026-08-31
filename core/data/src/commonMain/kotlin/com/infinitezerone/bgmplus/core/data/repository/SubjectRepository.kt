package com.infinitezerone.bgmplus.core.data.repository

import com.infinitezerone.bgmplus.core.common.AppResult
import com.infinitezerone.bgmplus.core.database.dao.EpisodeDao
import com.infinitezerone.bgmplus.core.database.dao.SubjectDao
import com.infinitezerone.bgmplus.core.database.entity.EpisodeEntity
import com.infinitezerone.bgmplus.core.database.entity.SubjectEntity
import com.infinitezerone.bgmplus.core.model.Episode
import com.infinitezerone.bgmplus.core.model.Subject
import com.infinitezerone.bgmplus.core.model.SubjectCharacter
import com.infinitezerone.bgmplus.core.model.SubjectImages
import com.infinitezerone.bgmplus.core.model.SubjectPerson
import com.infinitezerone.bgmplus.core.model.SubjectRelation
import com.infinitezerone.bgmplus.core.network.BangumiApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface SubjectRepository {
    fun getSubjectStream(id: Long): Flow<Subject?>

    suspend fun fetchSubjectDetail(id: Long): AppResult<Subject>

    fun getEpisodesStream(subjectId: Long): Flow<List<Episode>>

    suspend fun fetchEpisodes(subjectId: Long): AppResult<List<Episode>>

    suspend fun fetchCharacters(subjectId: Long): AppResult<List<SubjectCharacter>>

    suspend fun fetchPersons(subjectId: Long): AppResult<List<SubjectPerson>>

    suspend fun fetchRelations(subjectId: Long): AppResult<List<SubjectRelation>>
}

class SubjectRepositoryImpl(
    private val apiService: BangumiApiService,
    private val subjectDao: SubjectDao,
    private val episodeDao: EpisodeDao,
) : SubjectRepository {
    override fun getSubjectStream(id: Long): Flow<Subject?> =
        subjectDao.getSubjectById(id).map { entity ->
            entity?.let {
                Subject(
                    id = it.id,
                    type = it.type,
                    name = it.name,
                    nameCn = it.nameCn,
                    summary = it.summary,
                    date = it.date,
                    eps = it.eps,
                    totalEpisodes = it.totalEpisodes,
                    images = SubjectImages(large = it.coverUrl),
                )
            }
        }

    override suspend fun fetchSubjectDetail(id: Long): AppResult<Subject> =
        try {
            val subject = apiService.getSubject(id)
            val entity =
                SubjectEntity(
                    id = subject.id,
                    type = subject.type,
                    name = subject.name,
                    nameCn = subject.nameCn,
                    summary = subject.summary,
                    date = subject.date,
                    eps = subject.eps,
                    totalEpisodes = subject.totalEpisodes,
                    coverUrl = subject.images?.bestImage ?: "",
                    ratingScore = subject.rating?.score ?: 0.0,
                    ratingRank = subject.rating?.rank ?: 0,
                )
            subjectDao.insertSubject(entity)
            AppResult.Success(subject)
        } catch (e: Throwable) {
            AppResult.Error(e)
        }

    override fun getEpisodesStream(subjectId: Long): Flow<List<Episode>> =
        episodeDao.getEpisodesBySubjectId(subjectId).map { entities ->
            entities.map {
                Episode(
                    id = it.id,
                    sort = it.sort,
                    ep = it.ep,
                    name = it.name,
                    nameCn = it.nameCn,
                    duration = it.duration,
                    airdate = it.airdate,
                    type = it.type,
                    desc = it.desc,
                    comment = it.comment,
                )
            }
        }

    override suspend fun fetchEpisodes(subjectId: Long): AppResult<List<Episode>> =
        try {
            val response = apiService.getEpisodes(subjectId, limit = 100)
            val entities =
                response.data.map {
                    EpisodeEntity(
                        id = it.id,
                        subjectId = subjectId,
                        sort = it.sort,
                        ep = it.ep,
                        name = it.name,
                        nameCn = it.nameCn,
                        duration = it.duration,
                        airdate = it.airdate,
                        type = it.type,
                        desc = it.desc,
                        comment = it.comment,
                    )
                }
            episodeDao.insertEpisodes(entities)
            AppResult.Success(response.data)
        } catch (e: Throwable) {
            AppResult.Error(e)
        }

    override suspend fun fetchCharacters(subjectId: Long): AppResult<List<SubjectCharacter>> =
        try {
            val response = apiService.getSubjectCharacters(subjectId)
            AppResult.Success(response)
        } catch (e: Throwable) {
            AppResult.Error(e)
        }

    override suspend fun fetchPersons(subjectId: Long): AppResult<List<SubjectPerson>> =
        try {
            val response = apiService.getSubjectPersons(subjectId)
            AppResult.Success(response)
        } catch (e: Throwable) {
            AppResult.Error(e)
        }

    override suspend fun fetchRelations(subjectId: Long): AppResult<List<SubjectRelation>> =
        try {
            val response = apiService.getSubjectRelations(subjectId)
            AppResult.Success(response)
        } catch (e: Throwable) {
            AppResult.Error(e)
        }
}
