package com.infinitezerone.bgmplus.core.datastore

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow

class UserPreferencesDataSource(
    private val dataStore: DataStore<UserPreferences>,
) {
    val userPreferences: Flow<UserPreferences> = dataStore.data

    /** token 已由 AuthTokensDataSource 落盘后调用，二者共同构成登录态 */
    suspend fun markLoggedIn(userId: Long = 0L) {
        dataStore.updateData { current ->
            current.copy(isLoggedIn = true, userId = userId)
        }
    }

    suspend fun setPendingOAuthVerifier(verifier: String) {
        dataStore.updateData { current ->
            current.copy(pendingOAuthVerifier = verifier)
        }
    }

    suspend fun clearAuth() {
        dataStore.updateData { current ->
            UserPreferences(isDarkMode = current.isDarkMode, notifyBeforeAirMinutes = current.notifyBeforeAirMinutes)
        }
    }

    suspend fun setDarkMode(isDark: Boolean) {
        dataStore.updateData { current ->
            current.copy(isDarkMode = isDark)
        }
    }

    suspend fun setNotifyBeforeAirMinutes(minutes: Int) {
        dataStore.updateData { current ->
            current.copy(notifyBeforeAirMinutes = minutes)
        }
    }
}
