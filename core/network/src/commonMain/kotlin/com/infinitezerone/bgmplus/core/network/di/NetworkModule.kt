package com.infinitezerone.bgmplus.core.network.di

import com.infinitezerone.bgmplus.core.network.BangumiApiService
import com.infinitezerone.bgmplus.core.network.BangumiApiServiceImpl
import com.infinitezerone.bgmplus.core.network.BangumiDataService
import com.infinitezerone.bgmplus.core.network.BangumiDataServiceImpl
import com.infinitezerone.bgmplus.core.network.BgmAuthConfig
import com.infinitezerone.bgmplus.core.network.BgmHttpClient
import com.infinitezerone.bgmplus.core.network.BgmTokenPair
import com.infinitezerone.bgmplus.core.network.BgmTokenService
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

private val tokenClient = named("bgm_token_client")

/**
 * @param enableNetworkLogging 仅由 app 层传入 BuildConfig.DEBUG
 */
fun networkModule(enableNetworkLogging: Boolean = false): Module =
    module {
        single { BgmAuthConfig() }
        // token client：无 Auth 插件，专供 BgmTokenService 访问 Worker
        single(tokenClient) {
            BgmHttpClient.create(tokenProvider = get(), enableLogging = enableNetworkLogging)
        }
        single {
            BgmTokenService(client = get(tokenClient), config = get())
        }
        // 业务 client：Bearer 自动注入 + 401 时经 Worker 刷新重试
        single {
            BgmHttpClient.create(
                tokenProvider = get(),
                enableLogging = enableNetworkLogging,
                // 凭据被 OAuth 拒绝（invalid_grant 等 4xx）降级为 null：client 据此
                // 清除本地凭据（isLoggedIn 随之翻转，自动登出闭环）并沿用原始 401
                // 上抛 Unauthorized；5xx/网络异常等瞬时故障原样上抛以免误登出
                // （降级策略见 refreshOrNull）
                tokenRefresher = { oldRefreshToken ->
                    get<BgmTokenService>().refreshOrNull(oldRefreshToken)?.let { refreshed ->
                        BgmTokenPair(
                            accessToken = refreshed.accessToken,
                            // bgm.tv 刷新接口可能不回传新 refresh token，此时沿用旧的
                            refreshToken = refreshed.refreshToken.ifBlank { oldRefreshToken },
                        )
                    }
                },
            )
        }
        single<BangumiApiService> { BangumiApiServiceImpl(get()) }
        single<BangumiDataService> { BangumiDataServiceImpl(get()) }
    }
