package com.infinitezerone.bgmplus.sync.work.initializers

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.infinitezerone.bgmplus.core.model.SyncInterval
import com.infinitezerone.bgmplus.sync.work.workers.BgmSyncWorker
import java.util.concurrent.TimeUnit

val SyncConstraints =
    Constraints
        .Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .setRequiresBatteryNotLow(true)
        .build()

/**
 * 统一调度启动器（对标 NiA Sync.initialize）：
 * 根据用户偏好中的 [SyncInterval] 动态注册或注销系统后台同步任务。
 */
object Sync {
    fun initialize(
        context: Context,
        interval: SyncInterval = SyncInterval.WEEKLY,
    ) {
        reconfigure(context, interval)
    }

    fun reconfigure(
        context: Context,
        interval: SyncInterval,
    ) {
        val workManager = WorkManager.getInstance(context)

        if (interval == SyncInterval.MANUAL_ONLY) {
            // 彻底注销系统后台周期任务
            workManager.cancelUniqueWork(BgmSyncWorker.SYNC_WORK_NAME)
            return
        }

        val periodicSyncWork =
            PeriodicWorkRequestBuilder<BgmSyncWorker>(
                repeatInterval = interval.hours,
                repeatIntervalTimeUnit = TimeUnit.HOURS,
            ).setConstraints(SyncConstraints)
                .build()

        workManager.enqueueUniquePeriodicWork(
            BgmSyncWorker.SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicSyncWork,
        )
    }
}
