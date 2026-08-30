package com.infinitezerone.bgmplus.feature.user

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.infinitezerone.bgmplus.core.data.repository.AuthRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class UserViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {
    /** 登录态要求"偏好已标记"且"token 实际存在"，语义见 [AuthRepository.isLoggedIn] */
    val isLoggedIn: StateFlow<Boolean> =
        authRepository.isLoggedIn
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** 打开系统浏览器进行 OAuth 授权；回调由 `:app` 深链处理，结果经全局 Snackbar 反馈 */
    fun beginLogin(context: Context) {
        viewModelScope.launch {
            val authorizeUrl = authRepository.beginLogin()
            CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(authorizeUrl))
        }
    }

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }
}
