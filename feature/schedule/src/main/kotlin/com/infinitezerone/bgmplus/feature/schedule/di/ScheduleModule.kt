package com.infinitezerone.bgmplus.feature.schedule.di

import com.infinitezerone.bgmplus.feature.schedule.ScheduleViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val scheduleModule =
    module {
        viewModelOf(::ScheduleViewModel)
    }
