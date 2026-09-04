package com.infinitezerone.minibgm.sync.work.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.infinitezerone.minibgm.core.common.AppResult
import com.infinitezerone.minibgm.core.common.BgmDispatchers
import com.infinitezerone.minibgm.core.data.repository.ScheduleRepository
import kotlinx.coroutines.withContext

/**
 * 周期性后台同步 Worker（对标 NiA SyncWorker）：
 * 在系统满足网络连通与非低电量约束时唤醒，静默刷新时刻表并执行 ETag 304 探测与本地持久化。
 */
class BgmSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val scheduleRepository: ScheduleRepository,
    private val dispatchers: BgmDispatchers,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result =
        withContext(dispatchers.io) {
            Log.d(TAG, "BgmSyncWorker starting doWork... attempt: $runAttemptCount")
            when (val result = scheduleRepository.syncBangumiData(force = false)) {
                is AppResult.Success -> {
                    Log.d(TAG, "BgmSyncWorker succeeded!")
                    Result.success()
                }
                is AppResult.Error -> {
                    Log.e(TAG, "BgmSyncWorker failed with error: ${result.throwable.message}", result.throwable)
                    if (runAttemptCount < 3) Result.retry() else Result.failure()
                }
                is AppResult.Loading -> Result.success()
            }
        }

    companion object {
        const val TAG = "BgmSyncWorker"
        const val STARTUP_SYNC_WORK_NAME = "BgmStartupSyncWork"
        const val PERIODIC_SYNC_WORK_NAME = "BgmPeriodicSyncWork"
        const val MANUAL_SYNC_WORK_NAME = "BgmManualSyncWork"
        const val SYNC_WORK_NAME = "BgmSyncWork"
    }
}
