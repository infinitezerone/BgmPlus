package com.infinitezerone.bgmplus.core.datastore

import kotlinx.coroutines.test.runTest
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UserPreferencesSerializerTest {
    @Test
    fun defaultValue_isNotLoggedIn() {
        val default = UserPreferencesSerializer.defaultValue
        assertFalse(default.isLoggedIn)
        assertEquals(0L, default.userId)
    }

    @Test
    fun readFrom_emptyStream_returnsDefaultValue() =
        runTest {
            val emptyStream = ByteArrayInputStream(byteArrayOf())
            val result = UserPreferencesSerializer.readFrom(emptyStream)
            assertEquals(UserPreferencesSerializer.defaultValue, result)
        }

    @Test
    fun writeAndRead_preservesUserPreferences() =
        runTest {
            val original =
                UserPreferences(
                    userId = 42L,
                    username = "infinitezerone",
                    nickname = "零一",
                    avatarUrl = "https://lain.bgm.tv/pic/user/l/000/00/00/42.jpg",
                    sign = "Testing",
                    isLoggedIn = true,
                    pendingOAuthVerifier = "test_verifier",
                    isDarkMode = true,
                    notifyBeforeAirMinutes = 30,
                )

            val outputStream = ByteArrayOutputStream()
            UserPreferencesSerializer.writeTo(original, outputStream)

            val inputStream = ByteArrayInputStream(outputStream.toByteArray())
            val readBack = UserPreferencesSerializer.readFrom(inputStream)

            assertEquals(original, readBack)
            assertTrue(readBack.isLoggedIn)
            assertEquals("infinitezerone", readBack.username)
        }
}
