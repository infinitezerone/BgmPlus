package com.infinitezerone.bgmplus.core.datastore.di

import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import com.infinitezerone.bgmplus.core.datastore.AuthBlobSerializer
import com.infinitezerone.bgmplus.core.datastore.AuthTokensDataSource
import com.infinitezerone.bgmplus.core.datastore.CryptoManager
import com.infinitezerone.bgmplus.core.datastore.KeystoreTokenProvider
import com.infinitezerone.bgmplus.core.datastore.UserPreferencesDataSource
import com.infinitezerone.bgmplus.core.datastore.UserPreferencesSerializer
import com.infinitezerone.bgmplus.core.network.TokenProvider
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val datastoreModule =
    module {
        single {
            DataStoreFactory.create(
                serializer = UserPreferencesSerializer,
                produceFile = { androidContext().dataStoreFile("user_preferences.pb") },
            )
        }
        single { UserPreferencesDataSource(get()) }

        // OAuth token 独立加密存储；损坏时按未登录处理（典型场景：备份恢复到新设备，Keystore 密钥不可迁移）
        single {
            DataStoreFactory.create(
                serializer = AuthBlobSerializer,
                produceFile = { androidContext().dataStoreFile("auth_tokens.pb") },
                corruptionHandler = ReplaceFileCorruptionHandler(produceNewData = { "" }),
            )
        }
        single { CryptoManager() }
        single { AuthTokensDataSource(dataStore = get(), crypto = get()) }
        single<TokenProvider> { KeystoreTokenProvider(get()) }
    }
