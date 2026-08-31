package com.infinitezerone.bgmplus.feature.user

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.infinitezerone.bgmplus.core.data.repository.AuthRepository
import com.infinitezerone.bgmplus.core.model.UserProfile
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UserUiState(
    val isLoggedIn: Boolean = false,
    val activeProfile: UserProfile? = null,
    val savedAccounts: List<UserProfile> = emptyList(),
    val isLoading: Boolean = false,
)

class UserViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {
    val uiState: StateFlow<UserUiState> =
        combine(
            authRepository.isLoggedIn,
            authRepository.activeProfile,
            authRepository.savedAccounts,
        ) { isLoggedIn, activeProfile, savedAccounts ->
            UserUiState(
                isLoggedIn = isLoggedIn,
                activeProfile = activeProfile,
                savedAccounts = savedAccounts,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserUiState())

    val isLoggedIn: StateFlow<Boolean> =
        authRepository.isLoggedIn
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** 打开系统浏览器进行 OAuth 授权；使用 Ephemeral 隔离会话确保支持输入账号密码/切换新账号 */
    fun beginLogin(
        context: Context,
        ephemeral: Boolean = true,
    ) {
        viewModelScope.launch {
            val authorizeUrl = authRepository.beginLogin()
            val customTabsIntent =
                CustomTabsIntent
                    .Builder()
                    .setEphemeralBrowsingEnabled(ephemeral)
                    .build()
            customTabsIntent.launchUrl(context, Uri.parse(authorizeUrl))
        }
    }

    fun switchAccount(userId: Long) {
        viewModelScope.launch {
            authRepository.switchAccount(userId)
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }

    fun logout(userId: Long) {
        viewModelScope.launch {
            authRepository.logout(userId)
        }
    }

    fun logoutAll() {
        viewModelScope.launch {
            authRepository.logoutAll()
        }
    }
}
