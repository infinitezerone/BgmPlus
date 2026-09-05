package com.infinitezerone.minibgm.core.database.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey val id: Long,
    val type: Int,
    val name: String,
    val nameCn: String,
    val summary: String,
    val date: String,
    val eps: Int,
    val totalEpisodes: Int,
    val coverUrl: String,
    val ratingScore: Double,
    val ratingRank: Int,
    val ratingTotal: Int = 0,
    val ratingCountJson: String = "",
    val collectionWish: Int = 0,
    val collectionCollect: Int = 0,
    val collectionDoing: Int = 0,
    val collectionOnHold: Int = 0,
    val collectionDropped: Int = 0,
    val tagsJson: String = "",
    val updatedAt: Long = 0L,
)

@Entity(tableName = "episodes")
data class EpisodeEntity(
    @PrimaryKey val id: Long,
    val subjectId: Long,
    val sort: Float,
    val ep: Float,
    val name: String,
    val nameCn: String,
    val duration: String,
    val airdate: String,
    val type: Int = 0,
    val desc: String = "",
    val comment: Int = 0,
    val isCollected: Boolean = false,
)

@Entity(tableName = "air_schedules")
data class AirScheduleEntity(
    @PrimaryKey val bgmId: Long,
    val title: String,
    val titleCn: String,
    val coverUrl: String,
    val ratingScore: Double,
    val beginUtc: String,
    val weekday: Int,
    val timeCst: String,
    val timeJst: String,
    val sitesJson: String,
    val updatedAt: Long = 0L,
)

@Entity(
    tableName = "user_collections",
    primaryKeys = ["userId", "subjectId"],
)
data class UserCollectionEntity(
    val userId: Long,
    val subjectId: Long,
    val subjectType: Int,
    val rate: Int,
    val type: Int,
    val comment: String,
    val epStatus: Int,
    val volStatus: Int,
    val updatedAt: String,
)
