package com.infinitezerone.minibgm.core.network

import com.infinitezerone.minibgm.core.model.BangumiDataItem
import com.infinitezerone.minibgm.core.model.BangumiDataRoot
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
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
    private val cdnUrls: List<String> = DEFAULT_CDN_URLS,
) : BangumiDataService {
    constructor(client: HttpClient, cdnUrl: String) : this(client, listOf(cdnUrl))

    override suspend fun getBangumiData(etag: String?): BangumiDataResult {
        var lastException: Throwable? = null
        for (url in cdnUrls) {
            try {
                val response =
                    client.get(url) {
                        timeout {
                            requestTimeoutMillis = 60_000
                            connectTimeoutMillis = 15_000
                            socketTimeoutMillis = 60_000
                        }
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
            } catch (e: Throwable) {
                lastException = e
            }
        }
        throw lastException ?: IllegalStateException("Failed to fetch bangumi-data from CDN endpoints")
    }

    companion object {
        val DEFAULT_CDN_URLS =
            listOf(
                "https://cdn.jsdelivr.net/npm/bangumi-data@latest/dist/data.json",
                "https://fastly.jsdelivr.net/npm/bangumi-data@latest/dist/data.json",
                "https://gcore.jsdelivr.net/npm/bangumi-data@latest/dist/data.json",
            )
    }
}
