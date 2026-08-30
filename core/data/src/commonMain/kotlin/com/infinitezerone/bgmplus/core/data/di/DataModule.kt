package com.infinitezerone.bgmplus.core.data.di

import com.infinitezerone.bgmplus.core.data.repository.AuthRepository
import com.infinitezerone.bgmplus.core.data.repository.AuthRepositoryImpl
import com.infinitezerone.bgmplus.core.data.repository.ScheduleRepository
import com.infinitezerone.bgmplus.core.data.repository.ScheduleRepositoryImpl
import com.infinitezerone.bgmplus.core.data.repository.SubjectRepository
import com.infinitezerone.bgmplus.core.data.repository.SubjectRepositoryImpl
import org.koin.dsl.module

val dataModule =
    module {
        single<ScheduleRepository> { ScheduleRepositoryImpl(get(), get()) }
        single<SubjectRepository> { SubjectRepositoryImpl(get(), get(), get()) }
        single<AuthRepository> { AuthRepositoryImpl(get(), get(), get(), get()) }
    }
