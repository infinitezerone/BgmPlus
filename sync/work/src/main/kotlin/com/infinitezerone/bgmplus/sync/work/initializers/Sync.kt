package com.infinitezerone.bgmplus.sync.work.initializers

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
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
 * 1. App 启动初始化时立即入队一次性后台同步任务（对标 NiA startUpSyncWork），
 *    确保新安装 / 无缓存 / 数据为空时立即发起首次静默同步。
 * 2. 根据用户偏好中的 [SyncInterval] 动态注册或注销系统后台周期任务。
 */
object Sync {
    fun initialize(
        context: Context,
        interval: SyncInterval = SyncInterval.WEEKLY,
    ) {
        val workManager = WorkManager.getInstance(context)

        // 启动时入队一次性静默同步（KEEP 保证不重复排队已在运行的任务）
        val startupSyncWork =
            OneTimeWorkRequestBuilder<BgmSyncWorker>()
                .setConstraints(SyncConstraints)
                .addTag(BgmSyncWorker.TAG)
                .build()

        workManager.enqueueUniqueWork(
            BgmSyncWorker.STARTUP_SYNC_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            startupSyncWork,
        )

        reconfigure(context, interval)
    }

    fun reconfigure(
        context: Context,
        interval: SyncInterval,
    ) {
        val workManager = WorkManager.getInstance(context)

        if (interval == SyncInterval.MANUAL_ONLY) {
            // 彻底注销系统后台周期任务
            workManager.cancelUniqueWork(BgmSyncWorker.PERIODIC_SYNC_WORK_NAME)
            return
        }

        val periodicSyncWork =
            PeriodicWorkRequestBuilder<BgmSyncWorker>(
                repeatInterval = interval.hours,
                repeatIntervalTimeUnit = TimeUnit.HOURS,
            ).setConstraints(SyncConstraints)
                .addTag(BgmSyncWorker.TAG)
                .build()

        workManager.enqueueUniquePeriodicWork(
            BgmSyncWorker.PERIODIC_SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicSyncWork,
        )
    }
}
