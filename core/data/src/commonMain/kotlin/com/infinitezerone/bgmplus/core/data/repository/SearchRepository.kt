package com.infinitezerone.bgmplus.core.data.repository

import com.infinitezerone.bgmplus.core.common.AppResult
import com.infinitezerone.bgmplus.core.model.SearchSubjectsRequest
import com.infinitezerone.bgmplus.core.model.Subject
import com.infinitezerone.bgmplus.core.network.BangumiApiService
import com.infinitezerone.bgmplus.core.network.BgmNetworkException

interface SearchRepository {
    /** 搜索条目（动画、书籍、音乐、游戏、三次元） */
    suspend fun searchSubjects(
        query: String,
        type: Int = 2,
        limit: Int = 30,
        offset: Int = 0,
    ): AppResult<List<Subject>>

    /** 高级多维搜索与探索条目 (POST /v0/search/subjects) */
    suspend fun searchSubjectsAdvanced(
        request: SearchSubjectsRequest,
        limit: Int = 30,
        offset: Int = 0,
    ): AppResult<List<Subject>>
}

class SearchRepositoryImpl(
    private val apiService: BangumiApiService,
) : SearchRepository {
    override suspend fun searchSubjects(
        query: String,
        type: Int,
        limit: Int,
        offset: Int,
    ): AppResult<List<Subject>> {
        if (query.isBlank()) return AppResult.Success(emptyList())
        return try {
            val response =
                apiService.searchSubjects(
                    keyword = query,
                    type = type,
                    limit = limit,
                    offset = offset,
                )
            AppResult.Success(response.list)
        } catch (e: BgmNetworkException) {
            AppResult.Error(e, "搜索失败：${e.message}")
        } catch (e: Exception) {
            AppResult.Error(e, "搜索异常：${e.message}")
        }
    }

    override suspend fun searchSubjectsAdvanced(
        request: SearchSubjectsRequest,
        limit: Int,
        offset: Int,
    ): AppResult<List<Subject>> =
        try {
            val response =
                apiService.searchSubjectsAdvanced(
                    request = request,
                    limit = limit,
                    offset = offset,
                )
            AppResult.Success(response.data)
        } catch (e: BgmNetworkException) {
            AppResult.Error(e, "高级搜索失败：${e.message}")
        } catch (e: Exception) {
            AppResult.Error(e, "高级搜索异常：${e.message}")
        }
}
