package com.infinitezerone.bgmplus.core.data.repository

import com.infinitezerone.bgmplus.core.common.AppResult
import com.infinitezerone.bgmplus.core.datastore.UserPreferencesDataSource
import com.infinitezerone.bgmplus.core.network.BgmAuthConfig
import com.infinitezerone.bgmplus.core.network.BgmNetworkException
import com.infinitezerone.bgmplus.core.network.BgmPkce
import com.infinitezerone.bgmplus.core.network.BgmTokenService
import com.infinitezerone.bgmplus.core.network.TokenProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException

interface AuthRepository {
    val isLoggedIn: Flow<Boolean>

    /** 生成并持久化一次性 verifier，返回授权 URL（state 为其指纹，交给 Custom Tabs 打开） */
    suspend fun beginLogin(): String

    /** 深链回调入口：校验 state → 经 Worker 兑换 token → 加密落盘 */
    suspend fun completeLogin(
        code: String?,
        state: String?,
    ): AppResult<Unit>

    suspend fun logout()
}

class AuthRepositoryImpl(
    private val tokenService: BgmTokenService,
    private val tokenProvider: TokenProvider,
    private val userPreferences: UserPreferencesDataSource,
    private val authConfig: BgmAuthConfig,
) : AuthRepository {
    override suspend fun beginLogin(): String {
        // verifier 仅存本地；state 携带其 sha256 指纹，Worker 兑换时校验
        // sha256(verifier)==state（PKCE 等价，bgm.tv 不支持标准 PKCE，见 BgmPkce）。
        // Uuid.random() 底层为 SecureRandom，两次共 ≥256 位随机。
        val verifier = BgmPkce.generateVerifier()
        userPreferences.setPendingOAuthVerifier(verifier)
        return authConfig.buildAuthorizeUrl(state = BgmPkce.challenge(verifier))
    }

    override suspend fun completeLogin(
        code: String?,
        state: String?,
    ): AppResult<Unit> {
        val verifier = userPreferences.userPreferences.first().pendingOAuthVerifier
        when {
            code.isNullOrBlank() || state.isNullOrBlank() ->
                return AppResult.Error(IllegalArgumentException("回调缺少 code 或 state"))
            verifier.isBlank() || BgmPkce.challenge(verifier) != state ->
                return AppResult.Error(IllegalStateException("state 校验失败，疑似伪造回调"))
        }
        return try {
            val tokens = tokenService.exchangeCode(code, state, verifier)
            tokenProvider.saveTokens(tokens.accessToken, tokens.refreshToken)
            userPreferences.markLoggedIn(tokens.userId)
            userPreferences.setPendingOAuthVerifier("")
            AppResult.Success(Unit)
        } catch (e: BgmNetworkException) {
            AppResult.Error(e, "授权码兑换失败：${e.message}")
        } catch (e: SerializationException) {
            AppResult.Error(e, "兑换响应解析失败：${e.message}")
        }
    }

    override suspend fun logout() {
        tokenProvider.clearTokens()
        userPreferences.clearAuth()
    }

    // 登录态要求"偏好已标记"且"token 实际存在"：云备份/设备迁移会把
    // user_preferences.pb 恢复到新设备，而 auth_tokens.pb 被排除在备份之外，
    // 仅看偏好会呈现"已登录但无凭据"的假登录态（API 全 401 且无法刷新）。
    override val isLoggedIn: Flow<Boolean> =
        combine(
            userPreferences.userPreferences.map { it.isLoggedIn },
            tokenProvider.hasTokens,
        ) { markedLoggedIn, hasTokens -> markedLoggedIn && hasTokens }
}
