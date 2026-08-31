package com.infinitezerone.bgmplus.feature.search.di

import com.infinitezerone.bgmplus.feature.search.ExploreViewModel
import com.infinitezerone.bgmplus.feature.search.SearchViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val searchModule =
    module {
        viewModelOf(::SearchViewModel)
        viewModelOf(::ExploreViewModel)
    }
