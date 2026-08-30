package com.infinitezerone.bgmplus.core.datastore

import com.infinitezerone.bgmplus.core.model.UserAvatar
import com.infinitezerone.bgmplus.core.model.UserProfile
import kotlinx.serialization.Serializable

/**
 * 普通用户偏好。OAuth token 不在此处存储——它们由 AuthTokensDataSource
 * 经 AndroidKeyStore 加密后写入独立文件，并被备份规则整体排除。
 */
@Serializable
data class UserPreferences(
    val userId: Long = 0L,
    val username: String = "",
    val nickname: String = "",
    val avatarUrl: String = "",
    val sign: String = "",
    val isLoggedIn: Boolean = false,
    /** 进行中登录的 PKCE 等价 verifier（其 sha256 指纹作为 OAuth state，见 BgmPkce） */
    val pendingOAuthVerifier: String = "",
    val isDarkMode: Boolean = false,
    val notifyBeforeAirMinutes: Int = 15,
) {
    val userProfile: UserProfile?
        get() =
            if (isLoggedIn) {
                UserProfile(
                    id = userId,
                    username = username,
                    nickname = nickname,
                    avatar = if (avatarUrl.isNotBlank()) UserAvatar(large = avatarUrl) else null,
                    sign = sign,
                )
            } else {
                null
            }
}
