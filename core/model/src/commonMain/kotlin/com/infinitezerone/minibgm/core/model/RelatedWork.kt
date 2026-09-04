package com.infinitezerone.minibgm.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 角色出演或人物参与制作的关联条目作品（对应 /v0/characters/{id}/subjects 与 /v0/persons/{id}/subjects）
 */
@Serializable
data class RelatedWork(
    val id: Long,
    val name: String,
    @SerialName("name_cn") val nameCn: String = "",
    val staff: String = "",
    val image: String = "",
    val type: Int = 2,
) {
    val displayName: String
        get() = nameCn.ifBlank { name }

    val coverImage: String
        get() = image.replace("http://", "https://")
}
