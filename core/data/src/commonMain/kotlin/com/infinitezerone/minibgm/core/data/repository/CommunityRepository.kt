package com.infinitezerone.minibgm.core.data.repository

import com.infinitezerone.minibgm.core.common.AppResult
import com.infinitezerone.minibgm.core.model.EpisodeComment
import com.infinitezerone.minibgm.core.model.SubjectCommentPage
import com.infinitezerone.minibgm.core.model.SubjectTopic
import com.infinitezerone.minibgm.core.network.BangumiCommunityService

/**
 * 社区数据仓库（单集吐槽、条目全站短评流、条目讨论版）
 */
interface CommunityRepository {
    /** 获取单集吐槽列表 */
    suspend fun getEpisodeComments(episodeId: Long): AppResult<List<EpisodeComment>>

    /** 获取条目全站短评流 */
    suspend fun getSubjectComments(
        subjectId: Long,
        limit: Int = 20,
        offset: Int = 0,
    ): AppResult<SubjectCommentPage>

    /** 获取条目关联讨论版帖子列表 */
    suspend fun getSubjectTopics(
        subjectId: Long,
        limit: Int = 10,
        offset: Int = 0,
    ): AppResult<List<SubjectTopic>>
}

class CommunityRepositoryImpl(
    private val communityService: BangumiCommunityService,
) : CommunityRepository {
    override suspend fun getEpisodeComments(episodeId: Long): AppResult<List<EpisodeComment>> =
        try {
            AppResult.Success(communityService.getEpisodeComments(episodeId))
        } catch (e: Exception) {
            AppResult.Error(e, e.message ?: "获取单集吐槽失败")
        }

    override suspend fun getSubjectComments(
        subjectId: Long,
        limit: Int,
        offset: Int,
    ): AppResult<SubjectCommentPage> =
        try {
            AppResult.Success(communityService.getSubjectComments(subjectId, limit, offset))
        } catch (e: Exception) {
            AppResult.Error(e, e.message ?: "获取条目短评失败")
        }

    override suspend fun getSubjectTopics(
        subjectId: Long,
        limit: Int,
        offset: Int,
    ): AppResult<List<SubjectTopic>> =
        try {
            val page = communityService.getSubjectTopics(subjectId, limit, offset)
            AppResult.Success(page.data)
        } catch (e: Exception) {
            AppResult.Error(e, e.message ?: "获取条目讨论版失败")
        }
}
