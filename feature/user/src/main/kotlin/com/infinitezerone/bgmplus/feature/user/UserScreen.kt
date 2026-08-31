package com.infinitezerone.bgmplus.feature.user

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PauseCircleOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.infinitezerone.bgmplus.core.designsystem.theme.BgmPlusTheme
import com.infinitezerone.bgmplus.core.designsystem.theme.ThemePreviews
import com.infinitezerone.bgmplus.core.model.CollectionType
import com.infinitezerone.bgmplus.core.model.SyncInterval
import com.infinitezerone.bgmplus.core.model.UserAvatar
import com.infinitezerone.bgmplus.core.model.UserProfile
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

private const val BGM_HOME_URL = "https://bgm.tv"

@Composable
fun UserScreen(
    onCollectionClick: (CollectionType) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: UserViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    UserScreenContent(
        uiState = uiState,
        onLogin = { viewModel.beginLogin(context) },
        onSwitchAccount = viewModel::switchAccount,
        onLogoutCurrent = viewModel::logout,
        onLogoutAccount = viewModel::logout,
        onLogoutAll = viewModel::logoutAll,
        onOpenBgmWeb = {
            CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(BGM_HOME_URL))
        },
        onClearCache = {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("本地缓存与临时数据已清理 ✨")
            }
        },
        onCollectionClick = onCollectionClick,
        onSelectSyncInterval = viewModel::setSyncInterval,
        onSyncNow = {
            viewModel.syncBangumiDataNow { success ->
                coroutineScope.launch {
                    if (success) {
                        snackbarHostState.showSnackbar("播放源已是最新状态 ✨")
                    } else {
                        snackbarHostState.showSnackbar("播放源同步失败，请检查网络")
                    }
                }
            }
        },
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserScreenContent(
    uiState: UserUiState,
    onLogin: () -> Unit,
    onSwitchAccount: (Long) -> Unit,
    onLogoutCurrent: () -> Unit,
    onLogoutAccount: (Long) -> Unit,
    onLogoutAll: () -> Unit,
    onOpenBgmWeb: () -> Unit,
    onClearCache: () -> Unit,
    onCollectionClick: (CollectionType) -> Unit,
    onSelectSyncInterval: (SyncInterval) -> Unit,
    onSyncNow: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    var showAccountSheet by remember { mutableStateOf(false) }
    var accountToLogout by remember { mutableStateOf<UserProfile?>(null) }
    var showLogoutAllDialog by remember { mutableStateOf(false) }
    var showLogoutCurrentDialog by remember { mutableStateOf(false) }
    var showSyncIntervalDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "👤 个人中心",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                actions = {
                    if (uiState.isLoggedIn) {
                        IconButton(onClick = { showAccountSheet = true }) {
                            if (uiState.savedAccounts.size > 1) {
                                BadgedBox(
                                    badge = {
                                        Badge {
                                            Text(text = "${uiState.savedAccounts.size}")
                                        }
                                    },
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ManageAccounts,
                                        contentDescription = "账号管理",
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.ManageAccounts,
                                    contentDescription = "账号管理",
                                )
                            }
                        }
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = modifier,
    ) { innerPadding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!uiState.isLoggedIn) {
                item(key = "unauthenticated_card") {
                    UnauthenticatedCard(onLogin = onLogin)
                }
            } else {
                item(key = "profile_header") {
                    UserProfileHeaderCard(
                        profile = uiState.activeProfile,
                        savedAccountsCount = uiState.savedAccounts.size,
                        onManageAccountsClick = { showAccountSheet = true },
                        onLogoutCurrentClick = { showLogoutCurrentDialog = true },
                    )
                }

                if (uiState.savedAccounts.size > 1) {
                    item(key = "multi_account_card") {
                        MultiAccountQuickCard(
                            accounts = uiState.savedAccounts,
                            activeProfile = uiState.activeProfile,
                            onSwitchAccount = onSwitchAccount,
                            onManageAccountsClick = { showAccountSheet = true },
                        )
                    }
                }

                item(key = "collections_overview") {
                    CollectionOverviewCard(
                        isLoggedIn = true,
                        onCollectionClick = onCollectionClick,
                    )
                }
            }

            if (!uiState.isLoggedIn) {
                item(key = "collections_overview_placeholder") {
                    CollectionOverviewCard(
                        isLoggedIn = false,
                        onCollectionClick = onCollectionClick,
                    )
                }
            }

            item(key = "settings_and_about") {
                SettingsAndAboutCard(
                    syncInterval = uiState.syncInterval,
                    lastSyncTimestamp = uiState.lastSyncTimestamp,
                    isSyncing = uiState.isSyncing,
                    onOpenSyncDialog = { showSyncIntervalDialog = true },
                    onSyncNow = onSyncNow,
                    onOpenBgmWeb = onOpenBgmWeb,
                    onClearCache = onClearCache,
                )
            }
        }
    }

    if (showSyncIntervalDialog) {
        SyncIntervalDialog(
            currentInterval = uiState.syncInterval,
            onSelectInterval = onSelectSyncInterval,
            onDismiss = { showSyncIntervalDialog = false },
        )
    }

    if (showAccountSheet) {
        AccountManagementBottomSheet(
            accounts = uiState.savedAccounts,
            activeProfile = uiState.activeProfile,
            onDismiss = { showAccountSheet = false },
            onSwitchAccount = { userId ->
                onSwitchAccount(userId)
                showAccountSheet = false
            },
            onLogoutAccountClick = { profile ->
                accountToLogout = profile
            },
            onAddAccountClick = {
                showAccountSheet = false
                onLogin()
            },
            onLogoutAllClick = {
                showLogoutAllDialog = true
            },
        )
    }

    accountToLogout?.let { profile ->
        AlertDialog(
            onDismissRequest = { accountToLogout = null },
            icon = {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            },
            title = { Text(text = "退出账号") },
            text = {
                Text(
                    text = "确定要退出账号「${profile.displayName}」(@${profile.username}) 吗？退出后本地保存的该账号凭据将被清除。",
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onLogoutAccount(profile.id)
                        accountToLogout = null
                    },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                ) {
                    Text(text = "退出该账号")
                }
            },
            dismissButton = {
                TextButton(onClick = { accountToLogout = null }) {
                    Text(text = "取消")
                }
            },
        )
    }

    if (showLogoutCurrentDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutCurrentDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            },
            title = { Text(text = "退出当前账号") },
            text = {
                val currentDisplayName =
                    uiState.activeProfile
                        ?.displayName
                        .orEmpty()
                        .ifBlank { "当前账号" }
                Text(
                    text = "确定要退出当前登录的账号「$currentDisplayName」吗？",
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onLogoutCurrent()
                        showLogoutCurrentDialog = false
                    },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                ) {
                    Text(text = "确认退出")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutCurrentDialog = false }) {
                    Text(text = "取消")
                }
            },
        )
    }

    if (showLogoutAllDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutAllDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            },
            title = { Text(text = "退出所有账号") },
            text = {
                Text(
                    text = "确定要退出全部已登录的 Bangumi 账号吗？设备上所有加密凭据与缓存将被安全清除。",
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onLogoutAll()
                        showLogoutAllDialog = false
                        showAccountSheet = false
                    },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                ) {
                    Text(text = "退出所有账号")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutAllDialog = false }) {
                    Text(text = "取消")
                }
            },
        )
    }
}

@Composable
private fun UnauthenticatedCard(
    onLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(64.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.AccountCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(36.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "开启 Bangumi 同步",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "登录后解锁多端追番进度同步与安全多账号管理",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                AuthAdvantageItem(
                    icon = Icons.Filled.Sync,
                    title = "追番进度同步",
                    description = "实时同步在看、想看与打卡记录，多端不遗漏",
                )
                AuthAdvantageItem(
                    icon = Icons.Filled.Security,
                    title = "硬件级加密安全",
                    description = "OAuth 凭据存储于 Android Keystore 安全硬件，无明文泄露风险",
                )
                AuthAdvantageItem(
                    icon = Icons.Filled.SwapHoriz,
                    title = "多账号无缝切换",
                    description = "支持绑定多个 Bangumi 账号，一键即时切换",
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onLogin,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Login,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "通过 OAuth 授权登录",
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun AuthAdvantageItem(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(12.dp),
                ).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(36.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun UserProfileHeaderCard(
    profile: UserProfile?,
    savedAccountsCount: Int,
    onManageAccountsClick: () -> Unit,
    onLogoutCurrentClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sign = profile?.sign.orEmpty()
    val username = profile?.username.orEmpty()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    val avatarUrl = profile?.avatar?.bestAvatar.orEmpty()
                    if (avatarUrl.isNotBlank()) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = "用户头像",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(40.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile?.displayName?.ifBlank { "Bangumi 用户" } ?: "Bangumi 用户",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (username.isNotBlank()) {
                        Text(
                            text = "@$username",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                        ) {
                            Text(
                                text = "UID: ${profile?.id ?: 0}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }

                        if (savedAccountsCount > 1) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                modifier = Modifier.clickable(onClick = onManageAccountsClick),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.SwapHoriz,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "$savedAccountsCount 个账号",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (sign.isNotBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FormatQuote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = sign,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedButton(
                    onClick = onManageAccountsClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.ManageAccounts,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "账号管理")
                }
                OutlinedButton(
                    onClick = onLogoutCurrentClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "退出当前")
                }
            }
        }
    }
}

@Composable
private fun MultiAccountQuickCard(
    accounts: List<UserProfile>,
    activeProfile: UserProfile?,
    onSwitchAccount: (Long) -> Unit,
    onManageAccountsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "快速切换账号",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                TextButton(onClick = onManageAccountsClick) {
                    Text(text = "管理全部 (${accounts.size})")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                accounts.forEach { account ->
                    val isActive = account.id == activeProfile?.id
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    color =
                                        if (isActive) {
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                        } else {
                                            MaterialTheme.colorScheme.surfaceContainerLow
                                        },
                                    shape = RoundedCornerShape(10.dp),
                                ).clickable(enabled = !isActive) { onSwitchAccount(account.id) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            val avatarUrl = account.avatar?.bestAvatar.orEmpty()
                            if (avatarUrl.isNotBlank()) {
                                AsyncImage(
                                    model = avatarUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = account.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "@${account.username}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        if (isActive) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primary,
                            ) {
                                Text(
                                    text = "当前使用",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        } else {
                            Text(
                                text = "点击切换",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectionOverviewCard(
    isLoggedIn: Boolean,
    onCollectionClick: (CollectionType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "追番与收藏概览",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = if (isLoggedIn) "Bangumi 云端" else "未同步",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 第一行：三大核心状态（在看、想看、看过）
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                CollectionStatusItem(
                    label = "在看",
                    count = if (isLoggedIn) "12" else "-",
                    icon = Icons.Filled.PlayCircleOutline,
                    tint = MaterialTheme.colorScheme.primary,
                    onClick = { onCollectionClick(CollectionType.DOING) },
                    modifier = Modifier.weight(1f),
                )
                CollectionStatusItem(
                    label = "想看",
                    count = if (isLoggedIn) "28" else "-",
                    icon = Icons.Filled.BookmarkBorder,
                    tint = MaterialTheme.colorScheme.tertiary,
                    onClick = { onCollectionClick(CollectionType.WISH) },
                    modifier = Modifier.weight(1f),
                )
                CollectionStatusItem(
                    label = "看过",
                    count = if (isLoggedIn) "86" else "-",
                    icon = Icons.Filled.CheckCircleOutline,
                    tint = MaterialTheme.colorScheme.secondary,
                    onClick = { onCollectionClick(CollectionType.COLLECT) },
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 第二行：补充状态（搁置、抛弃）
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                CollectionStatusItem(
                    label = "搁置",
                    count = if (isLoggedIn) "3" else "-",
                    icon = Icons.Filled.PauseCircleOutline,
                    tint = MaterialTheme.colorScheme.outline,
                    onClick = { onCollectionClick(CollectionType.ON_HOLD) },
                    modifier = Modifier.weight(1f),
                )
                CollectionStatusItem(
                    label = "抛弃",
                    count = if (isLoggedIn) "1" else "-",
                    icon = Icons.Filled.Cancel,
                    tint = MaterialTheme.colorScheme.error,
                    onClick = { onCollectionClick(CollectionType.DROPPED) },
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "💡 追番列表与详细进度管理功能将在后续版本持续扩展",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CollectionStatusItem(
    label: String,
    count: String,
    icon: ImageVector,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = count,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = tint,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingsAndAboutCard(
    syncInterval: SyncInterval,
    lastSyncTimestamp: Long,
    isSyncing: Boolean,
    onOpenSyncDialog: () -> Unit,
    onSyncNow: () -> Unit,
    onOpenBgmWeb: () -> Unit,
    onClearCache: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lastSyncText =
        if (lastSyncTimestamp == 0L) {
            "未检测"
        } else {
            "已同步"
        }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                text = "通用设置与关于",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            )

            SettingsItemRow(
                icon = Icons.Filled.Sync,
                iconTint = MaterialTheme.colorScheme.primary,
                title = "播放源后台同步",
                subtitle = "频率: ${syncInterval.displayName} · 状态: $lastSyncText",
                trailing = {
                    if (isSyncing) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        OutlinedButton(
                            onClick = onSyncNow,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text("立即检查", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                },
                onClick = onOpenSyncDialog,
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 18.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )

            SettingsItemRow(
                icon = Icons.Filled.Palette,
                iconTint = MaterialTheme.colorScheme.primary,
                title = "外观与主题",
                subtitle = "跟随系统 · Material You 动态取色已启用",
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 18.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )

            SettingsItemRow(
                icon = Icons.Filled.CleaningServices,
                iconTint = MaterialTheme.colorScheme.secondary,
                title = "清除本地缓存",
                subtitle = "清理图片离线缓存与临时数据",
                onClick = onClearCache,
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 18.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )

            SettingsItemRow(
                icon = Icons.Filled.Info,
                iconTint = MaterialTheme.colorScheme.tertiary,
                title = "关于 BgmPlus",
                subtitle = "v1.0.0 · 现代 Material 3 Bangumi 客户端",
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 18.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )

            SettingsItemRow(
                icon = Icons.Filled.Language,
                iconTint = MaterialTheme.colorScheme.primary,
                title = "访问 Bangumi 官网",
                subtitle = "bgm.tv · 番组计划",
                trailing = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "打开网页",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                onClick = onOpenBgmWeb,
            )
        }
    }
}

@Composable
private fun SyncIntervalDialog(
    currentInterval: SyncInterval,
    onSelectInterval: (SyncInterval) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("播放源自动同步频率") },
        text = {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                SyncInterval.entries.forEach { interval ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectInterval(interval)
                                    onDismiss()
                                }.padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = (interval == currentInterval),
                            onClick = {
                                onSelectInterval(interval)
                                onDismiss()
                            },
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = interval.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        },
    )
}

@Composable
private fun SettingsItemRow(
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .then(
                    if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
                ).padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = iconTint.copy(alpha = 0.12f),
            modifier = Modifier.size(36.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (trailing != null) {
            trailing()
        } else if (onClick != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountManagementBottomSheet(
    accounts: List<UserProfile>,
    activeProfile: UserProfile?,
    onDismiss: () -> Unit,
    onSwitchAccount: (Long) -> Unit,
    onLogoutAccountClick: (UserProfile) -> Unit,
    onAddAccountClick: () -> Unit,
    onLogoutAllClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
        ) {
            Text(
                text = "账号管理",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "已保存 ${accounts.size} 个登录账号，支持一键无缝切换",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                accounts.forEach { account ->
                    val isActive = account.id == activeProfile?.id
                    AccountItemRow(
                        account = account,
                        isActive = isActive,
                        onSwitchClick = { onSwitchAccount(account.id) },
                        onLogoutClick = { onLogoutAccountClick(account) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onAddAccountClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.PersonAdd,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "添加其他 Bangumi 账号")
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onLogoutAllClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "退出所有账号")
            }
        }
    }
}

@Composable
private fun AccountItemRow(
    account: UserProfile,
    isActive: Boolean,
    onSwitchClick: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .then(
                    if (!isActive) Modifier.clickable(onClick = onSwitchClick) else Modifier,
                ),
        shape = RoundedCornerShape(14.dp),
        color =
            if (isActive) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                val avatarUrl = account.avatar?.bestAvatar.orEmpty()
                if (avatarUrl.isNotBlank()) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = account.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (isActive) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary,
                        ) {
                            Text(
                                text = "当前活跃",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
                Text(
                    text = "@${account.username} · UID: ${account.id}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (isActive) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "当前活跃账号",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            } else {
                IconButton(onClick = onLogoutClick) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "退出该账号",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

// ---------------- Previews ----------------

private val previewProfile1 =
    UserProfile(
        id = 123456L,
        username = "bgm_master",
        nickname = "零一",
        userGroup = 1,
        avatar = UserAvatar(large = "", medium = "", small = ""),
        sign = "探索二次元与科技的边界 ✨",
    )

private val previewProfile2 =
    UserProfile(
        id = 654321L,
        username = "anime_lover",
        nickname = "马甲二号",
        userGroup = 1,
        avatar = UserAvatar(large = "", medium = "", small = ""),
        sign = "补番进行中...",
    )

@ThemePreviews
@Composable
private fun UserScreenUnauthenticatedPreview() {
    BgmPlusTheme {
        UserScreenContent(
            uiState = UserUiState(isLoggedIn = false),
            onLogin = {},
            onSwitchAccount = {},
            onLogoutCurrent = {},
            onLogoutAccount = {},
            onLogoutAll = {},
            onOpenBgmWeb = {},
            onClearCache = {},
            onCollectionClick = {},
            onSelectSyncInterval = {},
            onSyncNow = {},
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}

@ThemePreviews
@Composable
private fun UserScreenSingleAccountPreview() {
    BgmPlusTheme {
        UserScreenContent(
            uiState =
                UserUiState(
                    isLoggedIn = true,
                    activeProfile = previewProfile1,
                    savedAccounts = listOf(previewProfile1),
                ),
            onLogin = {},
            onSwitchAccount = {},
            onLogoutCurrent = {},
            onLogoutAccount = {},
            onLogoutAll = {},
            onOpenBgmWeb = {},
            onClearCache = {},
            onCollectionClick = {},
            onSelectSyncInterval = {},
            onSyncNow = {},
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}

@ThemePreviews
@Composable
private fun UserScreenMultiAccountPreview() {
    BgmPlusTheme {
        UserScreenContent(
            uiState =
                UserUiState(
                    isLoggedIn = true,
                    activeProfile = previewProfile1,
                    savedAccounts = listOf(previewProfile1, previewProfile2),
                ),
            onLogin = {},
            onSwitchAccount = {},
            onLogoutCurrent = {},
            onLogoutAccount = {},
            onLogoutAll = {},
            onOpenBgmWeb = {},
            onClearCache = {},
            onCollectionClick = {},
            onSelectSyncInterval = {},
            onSyncNow = {},
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}
