package com.infinitezerone.bgmplus

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.infinitezerone.bgmplus.core.designsystem.theme.BgmPlusTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {
    private val loginViewModel: LoginViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleOAuthIntent(intent)
        setContent {
            BgmPlusTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    HomeScreen(
                        viewModel = loginViewModel,
                        modifier = Modifier.padding(innerPadding),
                    )
                }
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

@Composable
fun HomeScreen(
    viewModel: LoginViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    val message = viewModel.message

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = "🌸 BgmPlus - Modern Bangumi Schedule Client")
            Text(text = if (isLoggedIn) "已登录" else "未登录")
            Button(onClick = { viewModel.beginLogin(context) }) {
                Text(text = if (isLoggedIn) "重新登录" else "登录 Bangumi")
            }
            if (isLoggedIn) {
                TextButton(onClick = { viewModel.logout() }) {
                    Text(text = "退出登录")
                }
            }
            message?.let { Text(text = it) }
        }
    }
}
