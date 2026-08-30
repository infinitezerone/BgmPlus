package com.infinitezerone.bgmplus.di

import com.infinitezerone.bgmplus.BuildConfig
import com.infinitezerone.bgmplus.core.common.BgmDispatchers
import com.infinitezerone.bgmplus.core.data.di.dataModule
import com.infinitezerone.bgmplus.core.database.di.databaseModule
import com.infinitezerone.bgmplus.core.datastore.di.datastoreModule
import com.infinitezerone.bgmplus.core.network.di.networkModule
import com.infinitezerone.bgmplus.feature.schedule.di.scheduleModule
import com.infinitezerone.bgmplus.feature.subject.di.subjectModule
import com.infinitezerone.bgmplus.feature.user.di.userModule
import org.koin.dsl.module

fun appModule(enableNetworkLogging: Boolean = BuildConfig.DEBUG) =
    module {
        includes(
            networkModule(enableNetworkLogging),
            databaseModule,
            datastoreModule,
            dataModule,
            scheduleModule,
            userModule,
            subjectModule,
        )
        single { BgmDispatchers() }
    }
