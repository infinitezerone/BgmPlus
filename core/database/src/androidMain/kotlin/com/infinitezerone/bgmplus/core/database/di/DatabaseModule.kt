package com.infinitezerone.bgmplus.core.database.di

import androidx.room3.Room
import com.infinitezerone.bgmplus.core.database.BgmDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule =
    module {
        single {
            Room
                .databaseBuilder(
                    androidContext(),
                    BgmDatabase::class.java,
                    "bgm_plus.db",
                ).fallbackToDestructiveMigration(dropAllTables = true)
                .build()
        }
        single { get<BgmDatabase>().subjectDao() }
        single { get<BgmDatabase>().airScheduleDao() }
        single { get<BgmDatabase>().episodeDao() }
        single { get<BgmDatabase>().userCollectionDao() }
    }
