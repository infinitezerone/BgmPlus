package com.infinitezerone.bgmplus.core.network

import kotlinx.coroutines.flow.Flow

/**
 * token 的唯一读写入口（SSOT）。生产实现见 :core:datastore 的
 * KeystoreTokenProvider（AndroidKeyStore 加密落盘）。
 */
interface TokenProvider {
    suspend fun getAccessToken(): String?

    suspend fun getRefreshToken(): String?

    suspend fun saveTokens(
        accessToken: String,
        refreshToken: String,
    )

    suspend fun clearTokens()

    /** token 是否实际存在（响应式）。登录态必须结合它派生：
     *  云备份会把偏好恢复到新设备而加密 token 被排除，仅看偏好会出现"假登录"。 */
    val hasTokens: Flow<Boolean>
}
