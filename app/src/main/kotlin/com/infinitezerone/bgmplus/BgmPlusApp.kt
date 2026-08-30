package com.infinitezerone.bgmplus

import android.app.Application
import com.infinitezerone.bgmplus.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class BgmPlusApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            // release 下仅记录错误，避免 DI 结构信息进入公共日志
            androidLogger(if (BuildConfig.DEBUG) Level.DEBUG else Level.ERROR)
            androidContext(this@BgmPlusApp)
            modules(appModule())
        }
    }
}
