package com.infinitezerone.bgmplus.core.data.repository

import com.infinitezerone.bgmplus.core.common.AppResult
import com.infinitezerone.bgmplus.core.common.UserDataClearable
import com.infinitezerone.bgmplus.core.database.dao.UserCollectionDao
import com.infinitezerone.bgmplus.core.database.entity.UserCollectionEntity
import com.infinitezerone.bgmplus.core.datastore.UserPreferencesDataSource
import com.infinitezerone.bgmplus.core.model.CollectionType
import com.infinitezerone.bgmplus.core.model.UserCollection
import com.infinitezerone.bgmplus.core.network.BangumiApiService
import com.infinitezerone.bgmplus.core.network.BgmNetworkException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

interface CollectionRepository : UserDataClearable {
    /** 观察指定条目的收藏状态（响应式绑定当前活跃账号） */
    fun getCollectionStream(subjectId: Long): Flow<UserCollection?>

    /** 观察指定分类（想看/在看/看过等）的收藏列表 */
    fun getCollectionsByTypeStream(type: CollectionType): Flow<List<UserCollection>>

    /** 从远端拉取指定条目的收藏详情并更新本地 Room 缓存 */
    suspend fun fetchCollection(subjectId: Long): AppResult<UserCollection?>

    /** 更新条目收藏状态（想看/在看/看过、评分、简评等） */
    suspend fun updateCollectionStatus(
        subjectId: Long,
        type: CollectionType,
        rate: Int? = null,
        comment: String? = null,
        private: Boolean = false,
    ): AppResult<Unit>

    /** 更新单集观看进度（看过了 / 撤销） */
    suspend fun updateEpisodeStatus(
        subjectId: Long,
        episodeId: Long,
        isWatched: Boolean,
    ): AppResult<Unit>
}

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionRepositoryImpl(
    private val apiService: BangumiApiService,
    private val userCollectionDao: UserCollectionDao,
    private val userPreferences: UserPreferencesDataSource,
) : CollectionRepository {
    private val activeUserIdFlow: Flow<Long?> =
        userPreferences.userPreferences
            .map { it.activeUserId.takeIf { id -> id != 0L } }
            .distinctUntilChanged()

    override fun getCollectionStream(subjectId: Long): Flow<UserCollection?> =
        activeUserIdFlow.flatMapLatest { userId ->
            if (userId == null) {
                flowOf(null)
            } else {
                userCollectionDao
                    .getCollectionBySubjectId(userId, subjectId)
                    .map { entity -> entity?.asExternalModel() }
            }
        }

    override fun getCollectionsByTypeStream(type: CollectionType): Flow<List<UserCollection>> =
        activeUserIdFlow.flatMapLatest { userId ->
            if (userId == null) {
                flowOf(emptyList())
            } else {
                userCollectionDao
                    .getCollectionsByType(userId, type.value)
                    .map { list -> list.map { it.asExternalModel() } }
            }
        }

    override suspend fun fetchCollection(subjectId: Long): AppResult<UserCollection?> {
        val activeUid = userPreferences.userPreferences.first().activeUserId
        if (activeUid == 0L) return AppResult.Success(null)
        return try {
            val collection = apiService.getCollection(subjectId)
            if (collection != null) {
                userCollectionDao.insertCollection(collection.asEntity(activeUid))
            }
            AppResult.Success(collection)
        } catch (e: BgmNetworkException) {
            AppResult.Error(e, "获取收藏状态失败：${e.message}")
        } catch (e: Exception) {
            AppResult.Error(e, "获取收藏状态异常：${e.message}")
        }
    }

    override suspend fun updateCollectionStatus(
        subjectId: Long,
        type: CollectionType,
        rate: Int?,
        comment: String?,
        private: Boolean,
    ): AppResult<Unit> {
        val activeUid = userPreferences.userPreferences.first().activeUserId
        if (activeUid == 0L) return AppResult.Error(IllegalStateException("未登录账号，无法更新收藏"))
        return try {
            apiService.updateCollection(
                subjectId = subjectId,
                type = type.value,
                rate = rate,
                comment = comment,
                private = private,
            )
            // 远端更新成功后回拉最新状态或直接写入本地 Room
            val collection = apiService.getCollection(subjectId)
            if (collection != null) {
                userCollectionDao.insertCollection(collection.asEntity(activeUid))
            } else {
                userCollectionDao.insertCollection(
                    UserCollectionEntity(
                        userId = activeUid,
                        subjectId = subjectId,
                        subjectType = 2,
                        rate = rate ?: 0,
                        type = type.value,
                        comment = comment.orEmpty(),
                        epStatus = 0,
                        volStatus = 0,
                        updatedAt = "",
                    ),
                )
            }
            AppResult.Success(Unit)
        } catch (e: BgmNetworkException) {
            AppResult.Error(e, "更新收藏状态失败：${e.message}")
        } catch (e: Exception) {
            AppResult.Error(e, "更新收藏状态异常：${e.message}")
        }
    }

    override suspend fun updateEpisodeStatus(
        subjectId: Long,
        episodeId: Long,
        isWatched: Boolean,
    ): AppResult<Unit> =
        try {
            apiService.updateEpisodeStatus(
                subjectId = subjectId,
                episodeId = episodeId,
                type = if (isWatched) 2 else 0,
            )
            AppResult.Success(Unit)
        } catch (e: BgmNetworkException) {
            AppResult.Error(e, "更新章节状态失败：${e.message}")
        } catch (e: Exception) {
            AppResult.Error(e, "更新章节状态异常：${e.message}")
        }

    override suspend fun clearUserData(userId: Long) {
        userCollectionDao.clearByUserId(userId)
    }

    override suspend fun clearAllUserData() {
        userCollectionDao.clearAll()
    }
}

fun UserCollectionEntity.asExternalModel(): UserCollection =
    UserCollection(
        userId = userId,
        subjectId = subjectId,
        subjectType = subjectType,
        rate = rate,
        type = type,
        comment = comment,
        epStatus = epStatus,
        volStatus = volStatus,
        updatedAt = updatedAt,
    )

fun UserCollection.asEntity(userId: Long): UserCollectionEntity =
    UserCollectionEntity(
        userId = userId,
        subjectId = subjectId,
        subjectType = subjectType,
        rate = rate,
        type = type,
        comment = comment,
        epStatus = epStatus,
        volStatus = volStatus,
        updatedAt = updatedAt,
    )
