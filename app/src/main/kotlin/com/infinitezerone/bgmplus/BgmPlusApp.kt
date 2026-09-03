package com.infinitezerone.bgmplus

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.request.crossfade
import com.infinitezerone.bgmplus.core.datastore.UserPreferencesDataSource
import com.infinitezerone.bgmplus.di.appModule
import com.infinitezerone.bgmplus.sync.work.initializers.Sync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class BgmPlusApp :
    Application(),
    SingletonImageLoader.Factory {
    private val appScope = CoroutineScope(Dispatchers.Main.immediate)

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader
            .Builder(context)
            .memoryCache {
                MemoryCache
                    .Builder()
                    .maxSizePercent(context, 0.25)
                    .build()
            }.diskCache {
                DiskCache
                    .Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(250L * 1024 * 1024) // 250 MB 磁盘缓存
                    .build()
            }.crossfade(true)
            .build()

    override fun onCreate() {
        super.onCreate()
        startKoin {
            // release 下仅记录错误，避免 DI 结构信息进入公共日志
            androidLogger(if (BuildConfig.DEBUG) Level.DEBUG else Level.ERROR)
            androidContext(this@BgmPlusApp)
            workManagerFactory()
            modules(appModule())
        }

        // 监听用户设置的同步频率，动态注册或注销 WorkManager 周期任务
        val userPreferences: UserPreferencesDataSource by inject()
        appScope.launch {
            userPreferences.userPreferences
                .map { it.syncInterval }
                .distinctUntilChanged()
                .collect { interval ->
                    Sync.reconfigure(this@BgmPlusApp, interval)
                }
        }
    }
}
