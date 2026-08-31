package com.infinitezerone.bgmplus.core.testing.repository

import com.infinitezerone.bgmplus.core.common.AppResult
import com.infinitezerone.bgmplus.core.data.repository.SearchRepository
import com.infinitezerone.bgmplus.core.model.Subject

class FakeSearchRepository : SearchRepository {
    var searchCallCount: Int = 0
        private set
    var searchResult: AppResult<List<Subject>> = AppResult.Success(emptyList())

    override suspend fun searchSubjects(
        query: String,
        type: Int,
        limit: Int,
        offset: Int,
    ): AppResult<List<Subject>> {
        searchCallCount++
        return searchResult
    }
}
