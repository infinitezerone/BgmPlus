package com.infinitezerone.bgmplus.core.model

import kotlinx.serialization.Serializable

@Serializable
data class BangumiDataRoot(
    val items: List<BangumiDataItem> = emptyList(),
)

@Serializable
data class BangumiDataItem(
    val title: String,
    val titleTranslate: Map<String, List<String>> = emptyMap(),
    val type: String = "tv",
    val lang: String = "ja",
    val officialSite: String = "",
    val begin: String = "",
    val end: String = "",
    val sites: List<BangumiDataSite> = emptyList(),
) {
    val bgmSubjectId: Long?
        get() = sites.firstOrNull { it.site == "bangumi" }?.id?.toLongOrNull()

    val chineseTitle: String
        get() =
            titleTranslate["zh-Hans"]?.firstOrNull()
                ?: titleTranslate["zh-Hant"]?.firstOrNull()
                ?: title
}

@Serializable
data class BangumiDataSite(
    val site: String,
    val id: String = "",
    val url: String = "",
    val begin: String = "",
)
