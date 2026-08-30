package com.infinitezerone.bgmplus.core.network

import com.infinitezerone.bgmplus.core.model.BangumiDataItem
import com.infinitezerone.bgmplus.core.model.BangumiDataRoot
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

interface BangumiDataService {
    suspend fun getBangumiData(): List<BangumiDataItem>
}

class BangumiDataServiceImpl(
    private val client: HttpClient,
    private val cdnUrl: String = "https://cdn.jsdelivr.net/npm/bangumi-data@latest/dist/data.json",
) : BangumiDataService {
    override suspend fun getBangumiData(): List<BangumiDataItem> {
        val root: BangumiDataRoot = client.get(cdnUrl).body()
        return root.items
    }
}
