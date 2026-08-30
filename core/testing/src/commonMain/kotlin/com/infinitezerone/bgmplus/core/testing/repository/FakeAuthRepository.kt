package com.infinitezerone.bgmplus.core.testing.repository

import com.infinitezerone.bgmplus.core.common.AppResult
import com.infinitezerone.bgmplus.core.data.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeAuthRepository(
    initialLoggedIn: Boolean = false,
) : AuthRepository {
    private val _isLoggedIn = MutableStateFlow(initialLoggedIn)
    override val isLoggedIn: Flow<Boolean> = _isLoggedIn.asStateFlow()

    var beginLoginCallCount: Int = 0
        private set
    var completeLoginCallCount: Int = 0
        private set
    var logoutCallCount: Int = 0
        private set

    var mockAuthorizeUrl: String = "https://bgm.tv/oauth/authorize?client_id=test&state=test_state"
    var completeLoginResult: AppResult<Unit> = AppResult.Success(Unit)

    fun setLoggedIn(loggedIn: Boolean) {
        _isLoggedIn.value = loggedIn
    }

    override suspend fun beginLogin(): String {
        beginLoginCallCount++
        return mockAuthorizeUrl
    }

    override suspend fun completeLogin(
        code: String?,
        state: String?,
    ): AppResult<Unit> {
        completeLoginCallCount++
        if (completeLoginResult is AppResult.Success) {
            _isLoggedIn.value = true
        }
        return completeLoginResult
    }

    override suspend fun logout() {
        logoutCallCount++
        _isLoggedIn.value = false
    }
}
