package com.infinitezerone.bgmplus.core.data.di

import com.infinitezerone.bgmplus.core.data.repository.AuthRepository
import com.infinitezerone.bgmplus.core.data.repository.AuthRepositoryImpl
import com.infinitezerone.bgmplus.core.data.repository.ScheduleRepository
import com.infinitezerone.bgmplus.core.data.repository.ScheduleRepositoryImpl
import com.infinitezerone.bgmplus.core.data.repository.SubjectRepository
import com.infinitezerone.bgmplus.core.data.repository.SubjectRepositoryImpl
import com.infinitezerone.bgmplus.core.database.dao.AirScheduleDao
import com.infinitezerone.bgmplus.core.database.dao.EpisodeDao
import com.infinitezerone.bgmplus.core.database.dao.SubjectDao
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
                dataService = get<BangumiDataService>(),
                scheduleDao = get<AirScheduleDao>(),
            )
        }
        single<SubjectRepository> {
            SubjectRepositoryImpl(
                apiService = get<BangumiApiService>(),
                subjectDao = get<SubjectDao>(),
                episodeDao = get<EpisodeDao>(),
            )
        }
        single<AuthRepository> {
            AuthRepositoryImpl(
                tokenService = get<BgmTokenService>(),
                tokenProvider = get<TokenProvider>(),
                userPreferences = get<UserPreferencesDataSource>(),
                authConfig = get<BgmAuthConfig>(),
            )
        }
    }
