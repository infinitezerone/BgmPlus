package com.infinitezerone.bgmplus.feature.user

import com.infinitezerone.bgmplus.core.testing.data.sampleUserProfile
import com.infinitezerone.bgmplus.core.testing.data.sampleUserProfileAlt
import com.infinitezerone.bgmplus.core.testing.repository.FakeAuthRepository
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

    @Test
    fun initialState_notLoggedIn() =
        runTest {
            val authRepo = FakeAuthRepository(initialLoggedIn = false)
            val viewModel = UserViewModel(authRepo)

            val state = viewModel.uiState.first()
            assertFalse(state.isLoggedIn)
            assertNull(state.activeProfile)
            assertTrue(state.savedAccounts.isEmpty())
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
            val viewModel = UserViewModel(authRepo)

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
            val viewModel = UserViewModel(authRepo)

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
            val viewModel = UserViewModel(authRepo)

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
            val viewModel = UserViewModel(authRepo)

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
            val viewModel = UserViewModel(authRepo)

            viewModel.logoutAll()

            val state = viewModel.uiState.first { !it.isLoggedIn }
            assertEquals(1, authRepo.logoutAllCallCount)
            assertFalse(state.isLoggedIn)
            assertNull(state.activeProfile)
            assertTrue(state.savedAccounts.isEmpty())
        }
}
