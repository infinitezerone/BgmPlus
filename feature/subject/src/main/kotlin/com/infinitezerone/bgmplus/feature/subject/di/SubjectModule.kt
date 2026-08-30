package com.infinitezerone.bgmplus.feature.subject.di

import com.infinitezerone.bgmplus.feature.subject.SubjectDetailViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val subjectModule =
    module {
        // Koin parametersOf 模式：subjectId 由调用方经 parametersOf 传入（Koin 4 Definition 解构写法）
        viewModel { (subjectId: Long) -> SubjectDetailViewModel(get(), subjectId) }
    }
