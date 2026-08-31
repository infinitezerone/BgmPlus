package com.infinitezerone.bgmplus.feature.user

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.infinitezerone.bgmplus.core.common.AppResult
import com.infinitezerone.bgmplus.core.data.repository.AuthRepository
import com.infinitezerone.bgmplus.core.data.repository.ScheduleRepository
import com.infinitezerone.bgmplus.core.data.util.SyncManager
import com.infinitezerone.bgmplus.core.datastore.UserPreferencesDataSource
import com.infinitezerone.bgmplus.core.model.SyncInterval
import com.infinitezerone.bgmplus.core.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
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
    val syncInterval: SyncInterval = SyncInterval.WEEKLY,
    val lastSyncTimestamp: Long = 0L,
    val isSyncing: Boolean = false,
)

class UserViewModel(
    private val authRepository: AuthRepository,
    private val scheduleRepository: ScheduleRepository,
    private val userPreferencesDataSource: UserPreferencesDataSource,
    private val syncManager: SyncManager,
) : ViewModel() {
    private val isManualSyncing = MutableStateFlow(false)

    val uiState: StateFlow<UserUiState> =
        combine(
            authRepository.isLoggedIn,
            authRepository.activeProfile,
            authRepository.savedAccounts,
            userPreferencesDataSource.userPreferences,
            syncManager.isSyncing,
            isManualSyncing,
        ) { args: Array<Any?> ->
            val isLoggedIn = args[0] as Boolean
            val activeProfile = args[1] as? UserProfile

            @Suppress("UNCHECKED_CAST")
            val savedAccounts = args[2] as List<UserProfile>
            val prefs = args[3] as com.infinitezerone.bgmplus.core.datastore.UserPreferences
            val workSyncing = args[4] as Boolean
            val manualSyncing = args[5] as Boolean

            UserUiState(
                isLoggedIn = isLoggedIn,
                activeProfile = activeProfile,
                savedAccounts = savedAccounts,
                syncInterval = prefs.syncInterval,
                lastSyncTimestamp = prefs.bangumiDataLastSyncTimestamp,
                isSyncing = workSyncing || manualSyncing,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserUiState())

    val isLoggedIn: StateFlow<Boolean> =
        authRepository.isLoggedIn
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setSyncInterval(interval: SyncInterval) {
        viewModelScope.launch {
            userPreferencesDataSource.setSyncInterval(interval)
        }
    }

    fun syncBangumiDataNow(onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            isManualSyncing.value = true
            val result = scheduleRepository.syncBangumiData(force = false)
            isManualSyncing.value = false
            onComplete(result is AppResult.Success)
        }
    }

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
