package com.infinitezerone.bgmplus.core.testing.repository

import com.infinitezerone.bgmplus.core.common.AppResult
import com.infinitezerone.bgmplus.core.data.repository.CollectionRepository
import com.infinitezerone.bgmplus.core.model.CollectionType
import com.infinitezerone.bgmplus.core.model.UserCollection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeCollectionRepository : CollectionRepository {
    private val collectionsState = MutableStateFlow<Map<Long, UserCollection>>(emptyMap())

    var fetchUserCollectionsResult: AppResult<List<UserCollection>>? = null
    var fetchUserCollectionsCallCount: Int = 0
        private set
    var updateCollectionCallCount: Int = 0
        private set
    var updateEpisodeCallCount: Int = 0
        private set
    var clearUserDataCallCount: Int = 0
        private set

    fun sendCollection(collection: UserCollection) {
        collectionsState.value = collectionsState.value + (collection.subjectId to collection)
    }

    override fun getCollectionStream(subjectId: Long): Flow<UserCollection?> = collectionsState.map { it[subjectId] }

    override fun getCollectionsByTypeStream(type: CollectionType): Flow<List<UserCollection>> =
        collectionsState.map { it.values.filter { col -> col.type == type.value } }

    override suspend fun fetchUserCollections(
        username: String,
        subjectType: Int,
        type: CollectionType?,
        limit: Int,
        offset: Int,
    ): AppResult<List<UserCollection>> {
        fetchUserCollectionsCallCount++
        fetchUserCollectionsResult?.let { return it }
        val filtered =
            collectionsState.value.values.filter { col ->
                (type == null || col.type == type.value) &&
                    (subjectType == 0 || col.subjectType == subjectType)
            }
        return AppResult.Success(filtered)
    }

    override suspend fun fetchCollection(subjectId: Long): AppResult<UserCollection?> = AppResult.Success(collectionsState.value[subjectId])

    override suspend fun updateCollectionStatus(
        subjectId: Long,
        type: CollectionType,
        rate: Int?,
        comment: String?,
        private: Boolean,
        epStatus: Int?,
    ): AppResult<Unit> {
        updateCollectionCallCount++
        val current = collectionsState.value[subjectId]
        val updated =
            current?.copy(
                type = type.value,
                rate = rate ?: current.rate,
                comment = comment ?: current.comment,
                epStatus = epStatus ?: current.epStatus,
            ) ?: UserCollection(
                subjectId = subjectId,
                type = type.value,
                rate = rate ?: 0,
                comment = comment.orEmpty(),
                epStatus = epStatus ?: 0,
            )
        collectionsState.value = collectionsState.value + (subjectId to updated)
        return AppResult.Success(Unit)
    }

    override suspend fun updateEpisodeStatus(
        subjectId: Long,
        episodeId: Long,
        isWatched: Boolean,
    ): AppResult<Unit> {
        updateEpisodeCallCount++
        return AppResult.Success(Unit)
    }

    override suspend fun clearUserData(userId: Long) {
        clearUserDataCallCount++
        collectionsState.value = collectionsState.value.filterValues { it.userId != userId }
    }

    override suspend fun clearAllUserData() {
        clearUserDataCallCount++
        collectionsState.value = emptyMap()
    }
}
