package com.infinitezerone.bgmplus.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Subject(
    val id: Long,
    val type: Int = 2,
    val name: String,
    @SerialName("name_cn") val nameCn: String = "",
    val summary: String = "",
    val date: String = "",
    @SerialName("air_date") val airDate: String = "",
    val eps: Int = 0,
    @SerialName("total_episodes") val totalEpisodes: Int = 0,
    val images: SubjectImages? = null,
    val rating: Rating? = null,
    val collection: CollectionCount? = null,
    val tags: List<Tag> = emptyList(),
) {
    val displayName: String
        get() = nameCn.ifBlank { name }
}

@Serializable
data class SubjectImages(
    val large: String = "",
    val common: String = "",
    val medium: String = "",
    val small: String = "",
    val grid: String = "",
) {
    val bestImage: String
        get() = large.ifBlank { common.ifBlank { medium } }
}

@Serializable
data class Rating(
    val score: Double = 0.0,
    val total: Int = 0,
    val rank: Int = 0,
    val count: Map<String, Int> = emptyMap(),
)

@Serializable
data class CollectionCount(
    val wish: Int = 0,
    val collect: Int = 0,
    val doing: Int = 0,
    val onHold: Int = 0,
    val dropped: Int = 0,
)

@Serializable
data class Tag(
    val name: String,
    val count: Int = 0,
)
