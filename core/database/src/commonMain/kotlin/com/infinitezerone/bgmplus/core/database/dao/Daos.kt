package com.infinitezerone.bgmplus.core.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.infinitezerone.bgmplus.core.database.entity.AirScheduleEntity
import com.infinitezerone.bgmplus.core.database.entity.EpisodeEntity
import com.infinitezerone.bgmplus.core.database.entity.SubjectEntity
import com.infinitezerone.bgmplus.core.database.entity.UserCollectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects WHERE id = :id")
    fun getSubjectById(id: Long): Flow<SubjectEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubjects(subjects: List<SubjectEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: SubjectEntity)
}

@Dao
interface AirScheduleDao {
    @Query("SELECT * FROM air_schedules WHERE weekday = :weekday ORDER BY timeCst ASC")
    fun getSchedulesByWeekday(weekday: Int): Flow<List<AirScheduleEntity>>

    @Query("SELECT * FROM air_schedules")
    fun getAllSchedules(): Flow<List<AirScheduleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedules(schedules: List<AirScheduleEntity>)

    @Query("DELETE FROM air_schedules")
    suspend fun clearSchedules()
}

@Dao
interface EpisodeDao {
    @Query("SELECT * FROM episodes WHERE subjectId = :subjectId ORDER BY sort ASC")
    fun getEpisodesBySubjectId(subjectId: Long): Flow<List<EpisodeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEpisodes(episodes: List<EpisodeEntity>)
}

@Dao
interface UserCollectionDao {
    @Query("SELECT * FROM user_collections WHERE type = :type ORDER BY updatedAt DESC")
    fun getCollectionsByType(type: Int): Flow<List<UserCollectionEntity>>

    @Query("SELECT * FROM user_collections WHERE subjectId = :subjectId")
    fun getCollectionBySubjectId(subjectId: Long): Flow<UserCollectionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollections(collections: List<UserCollectionEntity>)
}
