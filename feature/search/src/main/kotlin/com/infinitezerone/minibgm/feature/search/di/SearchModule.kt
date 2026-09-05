package com.infinitezerone.minibgm.feature.search.di

import com.infinitezerone.minibgm.feature.search.ExploreViewModel
import com.infinitezerone.minibgm.feature.search.SearchViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val searchModule =
    module {
        viewModelOf(::SearchViewModel)
        viewModelOf(::ExploreViewModel)
    }
