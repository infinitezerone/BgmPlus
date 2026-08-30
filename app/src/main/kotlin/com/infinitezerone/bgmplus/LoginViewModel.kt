package com.infinitezerone.bgmplus

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.infinitezerone.bgmplus.core.common.onError
import com.infinitezerone.bgmplus.core.common.onSuccess
import com.infinitezerone.bgmplus.core.data.repository.AuthRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {
    val isLoggedIn: StateFlow<Boolean> =
        authRepository.isLoggedIn
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** 登录结果提示（登录成功 / 失败原因），展示在主页，下次登录时被覆盖 */
    var message: String? by mutableStateOf(null)
        private set

    fun beginLogin(context: Context) {
        viewModelScope.launch {
            val authorizeUrl = authRepository.beginLogin()
            CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(authorizeUrl))
        }
    }

    fun handleOAuthCallback(
        code: String?,
        state: String?,
    ) {
        viewModelScope.launch {
            authRepository
                .completeLogin(code, state)
                .onSuccess { message = "登录成功 🎉" }
                .onError { _, m -> message = m }
        }
    }

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }
}
