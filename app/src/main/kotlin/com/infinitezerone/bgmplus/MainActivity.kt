package com.infinitezerone.bgmplus

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.infinitezerone.bgmplus.core.designsystem.theme.BgmPlusTheme
import com.infinitezerone.bgmplus.ui.BgmApp
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {
    private val loginViewModel: LoginViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleOAuthIntent(intent)
        setContent {
            BgmPlusTheme {
                BgmApp(loginViewModel = loginViewModel)
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
            loginViewModel.handleOAuthCallback(
                code = data.getQueryParameter("code"),
                state = data.getQueryParameter("state"),
            )
        }
    }
}
