package com.infinitezerone.bgmplus.di

import com.infinitezerone.bgmplus.BuildConfig
import com.infinitezerone.bgmplus.LoginViewModel
import com.infinitezerone.bgmplus.core.common.BgmDispatchers
import com.infinitezerone.bgmplus.core.data.di.dataModule
import com.infinitezerone.bgmplus.core.database.di.databaseModule
import com.infinitezerone.bgmplus.core.datastore.di.datastoreModule
import com.infinitezerone.bgmplus.core.network.di.networkModule
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

fun appModule(enableNetworkLogging: Boolean = BuildConfig.DEBUG) =
    module {
        includes(
            networkModule(enableNetworkLogging),
            databaseModule,
            datastoreModule,
            dataModule,
        )
        single { BgmDispatchers() }
        viewModelOf(::LoginViewModel)
    }
