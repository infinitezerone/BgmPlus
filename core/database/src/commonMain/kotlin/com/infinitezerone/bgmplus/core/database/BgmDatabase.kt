package com.infinitezerone.bgmplus.core.database

import androidx.room3.Database
import androidx.room3.RoomDatabase
import com.infinitezerone.bgmplus.core.database.dao.AirScheduleDao
import com.infinitezerone.bgmplus.core.database.dao.EpisodeDao
import com.infinitezerone.bgmplus.core.database.dao.SubjectDao
import com.infinitezerone.bgmplus.core.database.dao.UserCollectionDao
import com.infinitezerone.bgmplus.core.database.entity.AirScheduleEntity
import com.infinitezerone.bgmplus.core.database.entity.EpisodeEntity
import com.infinitezerone.bgmplus.core.database.entity.SubjectEntity
import com.infinitezerone.bgmplus.core.database.entity.UserCollectionEntity

@Database(
    entities = [
        SubjectEntity::class,
        EpisodeEntity::class,
        AirScheduleEntity::class,
        UserCollectionEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class BgmDatabase : RoomDatabase() {
    abstract fun subjectDao(): SubjectDao

    abstract fun airScheduleDao(): AirScheduleDao

    abstract fun episodeDao(): EpisodeDao

    abstract fun userCollectionDao(): UserCollectionDao
}
