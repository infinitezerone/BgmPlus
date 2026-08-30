package com.infinitezerone.bgmplus.di

import com.infinitezerone.bgmplus.LoginViewModel
import com.infinitezerone.bgmplus.core.data.repository.AuthRepository
import com.infinitezerone.bgmplus.core.data.repository.ScheduleRepository
import com.infinitezerone.bgmplus.core.data.repository.SubjectRepository
import com.infinitezerone.bgmplus.core.testing.repository.FakeAuthRepository
import com.infinitezerone.bgmplus.core.testing.repository.FakeScheduleRepository
import com.infinitezerone.bgmplus.core.testing.repository.FakeSubjectRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get

/**
 * Koin 装配冒烟测试。注意：这里 boot 的是手工 Fake 模块，而非真实的 appModule() ——
 * 真实 datastore/network 模块依赖 androidContext()，纯 JVM 单测无法装配（项目未引入 Robolectric）。
 * 因此本测试不能覆盖"真实 DI 图装配错误"（如泛型擦除导致绑定错位）这类缺陷。
 */
class AppModuleTest : KoinTest {
    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun verifyLoginViewModelWithAuthRepository() =
        runTest {
            val fakeAuth = FakeAuthRepository(initialLoggedIn = false)
            startKoin {
                modules(
                    module {
                        single<AuthRepository> { fakeAuth }
                        single { LoginViewModel(get()) }
                    },
                )
            }

            val viewModel: LoginViewModel = get()
            assertNotNull(viewModel)
            assertEquals(false, viewModel.isLoggedIn.first())
        }

    @Test
    fun verifyRepositoriesResolution() =
        runTest {
            startKoin {
                modules(
                    module {
                        single<AuthRepository> { FakeAuthRepository() }
                        single<ScheduleRepository> { FakeScheduleRepository() }
                        single<SubjectRepository> { FakeSubjectRepository() }
                    },
                )
            }

            val authRepo: AuthRepository = get()
            val scheduleRepo: ScheduleRepository = get()
            val subjectRepo: SubjectRepository = get()

            assertNotNull(authRepo)
            assertNotNull(scheduleRepo)
            assertNotNull(subjectRepo)
        }
}
