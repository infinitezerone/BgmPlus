package com.infinitezerone.bgmplus.core.data.di

import com.infinitezerone.bgmplus.core.data.repository.AuthRepository
import com.infinitezerone.bgmplus.core.data.repository.AuthRepositoryImpl
import com.infinitezerone.bgmplus.core.data.repository.CollectionRepository
import com.infinitezerone.bgmplus.core.data.repository.CollectionRepositoryImpl
import com.infinitezerone.bgmplus.core.data.repository.ScheduleRepository
import com.infinitezerone.bgmplus.core.data.repository.ScheduleRepositoryImpl
import com.infinitezerone.bgmplus.core.data.repository.SearchRepository
import com.infinitezerone.bgmplus.core.data.repository.SearchRepositoryImpl
import com.infinitezerone.bgmplus.core.data.repository.SubjectRepository
import com.infinitezerone.bgmplus.core.data.repository.SubjectRepositoryImpl
import com.infinitezerone.bgmplus.core.data.util.UserDataCleaner
import com.infinitezerone.bgmplus.core.database.dao.AirScheduleDao
import com.infinitezerone.bgmplus.core.database.dao.EpisodeDao
import com.infinitezerone.bgmplus.core.database.dao.SubjectDao
import com.infinitezerone.bgmplus.core.database.dao.UserCollectionDao
import com.infinitezerone.bgmplus.core.datastore.UserPreferencesDataSource
import com.infinitezerone.bgmplus.core.network.BangumiApiService
import com.infinitezerone.bgmplus.core.network.BangumiDataService
import com.infinitezerone.bgmplus.core.network.BgmAuthConfig
import com.infinitezerone.bgmplus.core.network.BgmTokenService
import com.infinitezerone.bgmplus.core.network.TokenProvider
import org.koin.dsl.module

val dataModule =
    module {
        single<ScheduleRepository> {
            ScheduleRepositoryImpl(
                apiService = get<BangumiApiService>(),
                dataService = get<BangumiDataService>(),
                scheduleDao = get<AirScheduleDao>(),
                userPreferences = get<UserPreferencesDataSource>(),
            )
        }
        single<SubjectRepository> {
            SubjectRepositoryImpl(
                apiService = get<BangumiApiService>(),
                subjectDao = get<SubjectDao>(),
                episodeDao = get<EpisodeDao>(),
            )
        }
        single<CollectionRepository> {
            CollectionRepositoryImpl(
                apiService = get<BangumiApiService>(),
                userCollectionDao = get<UserCollectionDao>(),
                userPreferences = get<UserPreferencesDataSource>(),
            )
        }
        single<SearchRepository> {
            SearchRepositoryImpl(
                apiService = get<BangumiApiService>(),
                userPreferences = get<UserPreferencesDataSource>(),
            )
        }
        single {
            UserDataCleaner(
                clearables =
                    listOf(
                        get<UserPreferencesDataSource>(),
                        get<CollectionRepository>(),
                    ),
            )
        }
        single<AuthRepository> {
            AuthRepositoryImpl(
                tokenService = get<BgmTokenService>(),
                tokenProvider = get<TokenProvider>(),
                userPreferences = get<UserPreferencesDataSource>(),
                authConfig = get<BgmAuthConfig>(),
                apiService = get<BangumiApiService>(),
                userDataCleaner = get<UserDataCleaner>(),
            )
        }
    }
