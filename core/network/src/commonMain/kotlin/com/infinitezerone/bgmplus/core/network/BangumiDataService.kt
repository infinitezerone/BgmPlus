package com.infinitezerone.bgmplus.core.network

import com.infinitezerone.bgmplus.core.model.BangumiDataItem
import com.infinitezerone.bgmplus.core.model.BangumiDataRoot
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode

sealed interface BangumiDataResult {
    data class Success(
        val items: List<BangumiDataItem>,
        val etag: String?,
    ) : BangumiDataResult

    data object NotModified : BangumiDataResult
}

interface BangumiDataService {
    suspend fun getBangumiData(etag: String? = null): BangumiDataResult
}

class BangumiDataServiceImpl(
    private val client: HttpClient,
    private val cdnUrl: String = "https://cdn.jsdelivr.net/npm/bangumi-data@latest/dist/data.json",
) : BangumiDataService {
    override suspend fun getBangumiData(etag: String?): BangumiDataResult {
        val response =
            client.get(cdnUrl) {
                if (!etag.isNullOrBlank()) {
                    header(HttpHeaders.IfNoneMatch, etag)
                }
            }

        if (response.status == HttpStatusCode.NotModified) {
            return BangumiDataResult.NotModified
        }

        val newEtag = response.headers[HttpHeaders.ETag]
        val root: BangumiDataRoot = response.body()
        return BangumiDataResult.Success(root.items, newEtag)
    }
}
