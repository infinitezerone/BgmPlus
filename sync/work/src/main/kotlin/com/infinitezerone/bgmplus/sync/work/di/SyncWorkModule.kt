package com.infinitezerone.bgmplus.sync.work.di

import com.infinitezerone.bgmplus.core.data.util.SyncManager
import com.infinitezerone.bgmplus.sync.work.status.WorkManagerSyncManager
import com.infinitezerone.bgmplus.sync.work.workers.BgmSyncWorker
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.dsl.module

val syncWorkModule =
    module {
        workerOf(::BgmSyncWorker)
        single<SyncManager> { WorkManagerSyncManager(context = get()) }
    }
