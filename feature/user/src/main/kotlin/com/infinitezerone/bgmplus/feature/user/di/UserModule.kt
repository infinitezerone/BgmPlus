package com.infinitezerone.bgmplus.feature.user.di

import com.infinitezerone.bgmplus.feature.user.UserCollectionsViewModel
import com.infinitezerone.bgmplus.feature.user.UserViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val userModule =
    module {
        viewModelOf(::UserViewModel)
        viewModelOf(::UserCollectionsViewModel)
    }
