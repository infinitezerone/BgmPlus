package com.infinitezerone.bgmplus

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.lifecycleScope
import com.infinitezerone.bgmplus.core.common.onError
import com.infinitezerone.bgmplus.core.common.onSuccess
import com.infinitezerone.bgmplus.core.data.repository.AuthRepository
import com.infinitezerone.bgmplus.core.designsystem.theme.BgmPlusTheme
import com.infinitezerone.bgmplus.ui.BgmApp
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val authRepository: AuthRepository by inject()
    private val snackbarHostState = SnackbarHostState()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleOAuthIntent(intent)
        setContent {
            BgmPlusTheme {
                BgmApp(snackbarHostState = snackbarHostState)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleOAuthIntent(intent)
    }

    private fun handleOAuthIntent(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == "bgmplus" && data.host == "oauth" && data.path == "/callback") {
            val code = data.getQueryParameter("code")
            val state = data.getQueryParameter("state")
            // 深链回调是 app 级事件（不依赖任何页面存活），登录结果经全局 Snackbar 反馈
            lifecycleScope.launch {
                authRepository
                    .completeLogin(code, state)
                    .onSuccess { snackbarHostState.showSnackbar("登录成功 🎉") }
                    .onError { _, message -> snackbarHostState.showSnackbar(message) }
            }
        }
    }
}
