package com.infinitezerone.bgmplus.feature.user

import com.infinitezerone.bgmplus.core.common.AppResult
import com.infinitezerone.bgmplus.core.model.SyncInterval
import com.infinitezerone.bgmplus.core.testing.data.sampleUserProfile
import com.infinitezerone.bgmplus.core.testing.data.sampleUserProfileAlt
import com.infinitezerone.bgmplus.core.testing.datastore.createTestUserPreferencesDataSource
import com.infinitezerone.bgmplus.core.testing.repository.FakeAuthRepository
import com.infinitezerone.bgmplus.core.testing.repository.FakeScheduleRepository
import com.infinitezerone.bgmplus.core.testing.repository.FakeSyncManager
import com.infinitezerone.bgmplus.core.testing.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UserViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun createViewModel(
        authRepo: FakeAuthRepository = FakeAuthRepository(initialLoggedIn = false),
        scheduleRepo: FakeScheduleRepository = FakeScheduleRepository(),
        syncManager: FakeSyncManager = FakeSyncManager(),
    ): Pair<UserViewModel, FakeScheduleRepository> {
        val userPrefs = createTestUserPreferencesDataSource()
        val viewModel =
            UserViewModel(
                authRepository = authRepo,
                scheduleRepository = scheduleRepo,
                userPreferencesDataSource = userPrefs,
                syncManager = syncManager,
            )
        return viewModel to scheduleRepo
    }

    @Test
    fun initialState_notLoggedIn() =
        runTest {
            val (viewModel, _) = createViewModel()

            val state = viewModel.uiState.first()
            assertFalse(state.isLoggedIn)
            assertNull(state.activeProfile)
            assertTrue(state.savedAccounts.isEmpty())
            assertEquals(SyncInterval.WEEKLY, state.syncInterval)
        }

    @Test
    fun loggedInState_emitsActiveProfileAndAccounts() =
        runTest {
            val authRepo =
                FakeAuthRepository(
                    initialLoggedIn = true,
                    initialProfile = sampleUserProfile,
                    initialAccounts = listOf(sampleUserProfile, sampleUserProfileAlt),
                )
            val (viewModel, _) = createViewModel(authRepo = authRepo)

            val state = viewModel.uiState.first { it.isLoggedIn && it.activeProfile != null }
            assertTrue(state.isLoggedIn)
            assertNotNull(state.activeProfile)
            assertEquals("零一", state.activeProfile?.nickname)
            assertEquals(2, state.savedAccounts.size)
        }

    @Test
    fun switchAccount_updatesActiveProfile() =
        runTest {
            val authRepo =
                FakeAuthRepository(
                    initialLoggedIn = true,
                    initialProfile = sampleUserProfile,
                    initialAccounts = listOf(sampleUserProfile, sampleUserProfileAlt),
                )
            val (viewModel, _) = createViewModel(authRepo = authRepo)

            viewModel.switchAccount(sampleUserProfileAlt.id)

            val state = viewModel.uiState.first { it.activeProfile?.id == sampleUserProfileAlt.id }
            assertEquals(1, authRepo.switchAccountCallCount)
            assertEquals("马甲二号", state.activeProfile?.nickname)
            assertEquals(999L, state.activeProfile?.id)
        }

    @Test
    fun logoutSingleAccount_switchesToRemainingAccount() =
        runTest {
            val authRepo =
                FakeAuthRepository(
                    initialLoggedIn = true,
                    initialProfile = sampleUserProfile,
                    initialAccounts = listOf(sampleUserProfile, sampleUserProfileAlt),
                )
            val (viewModel, _) = createViewModel(authRepo = authRepo)

            viewModel.logout(sampleUserProfile.id)

            val state = viewModel.uiState.first { it.activeProfile?.id == sampleUserProfileAlt.id }
            assertEquals(1, authRepo.logoutCallCount)
            assertTrue(state.isLoggedIn)
            assertEquals("马甲二号", state.activeProfile?.nickname)
            assertEquals(1, state.savedAccounts.size)
        }

    @Test
    fun logoutCurrentAccount_clearsLoginStateWhenSingleAccount() =
        runTest {
            val authRepo =
                FakeAuthRepository(
                    initialLoggedIn = true,
                    initialProfile = sampleUserProfile,
                    initialAccounts = listOf(sampleUserProfile),
                )
            val (viewModel, _) = createViewModel(authRepo = authRepo)

            viewModel.logout()

            val state = viewModel.uiState.first { !it.isLoggedIn }
            assertEquals(1, authRepo.logoutCallCount)
            assertFalse(state.isLoggedIn)
            assertNull(state.activeProfile)
            assertTrue(state.savedAccounts.isEmpty())
        }

    @Test
    fun logoutAll_clearsAllAccounts() =
        runTest {
            val authRepo =
                FakeAuthRepository(
                    initialLoggedIn = true,
                    initialProfile = sampleUserProfile,
                    initialAccounts = listOf(sampleUserProfile, sampleUserProfileAlt),
                )
            val (viewModel, _) = createViewModel(authRepo = authRepo)

            viewModel.logoutAll()

            val state = viewModel.uiState.first { !it.isLoggedIn }
            assertEquals(1, authRepo.logoutAllCallCount)
            assertFalse(state.isLoggedIn)
            assertNull(state.activeProfile)
            assertTrue(state.savedAccounts.isEmpty())
        }

    @Test
    fun setSyncInterval_updatesState() =
        runTest {
            val (viewModel, _) = createViewModel()

            viewModel.setSyncInterval(SyncInterval.DAILY)

            val state = viewModel.uiState.first { it.syncInterval == SyncInterval.DAILY }
            assertEquals(SyncInterval.DAILY, state.syncInterval)
        }

    @Test
    fun syncBangumiDataNow_triggersScheduleRepository() =
        runTest {
            val scheduleRepo = FakeScheduleRepository()
            scheduleRepo.syncBangumiDataResult = AppResult.Success(Unit)
            val (viewModel, _) = createViewModel(scheduleRepo = scheduleRepo)

            var callbackSuccess = false
            viewModel.syncBangumiDataNow { success ->
                callbackSuccess = success
            }

            assertTrue(callbackSuccess)
            assertEquals(1, scheduleRepo.syncBangumiDataCallCount)
        }
}
