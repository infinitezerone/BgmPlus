package com.infinitezerone.bgmplus.core.datastore

import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

/** 密文 blob 的持久化格式：文件内容是 Base64 字符串，加解密与校验都在 [AuthTokensDataSource] 内完成 */
internal object AuthBlobSerializer : Serializer<String> {
    override val defaultValue: String = ""

    override suspend fun readFrom(input: InputStream): String = input.readBytes().decodeToString()

    override suspend fun writeTo(
        t: String,
        output: OutputStream,
    ) {
        withContext(Dispatchers.IO) {
            output.write(t.encodeToByteArray())
        }
    }
}

/**
 * OAuth token 的加密存储：独立于普通偏好的 DataStore 文件，
 * 内容为 Base64(IV || AES-GCM(JSON))，并在备份规则中整体排除。
 */
class AuthTokensDataSource(
    private val dataStore: DataStore<String>,
    private val crypto: CryptoManager,
) {
    val tokens: Flow<Pair<String, String>?> = dataStore.data.map { blob -> decode(blob) }

    suspend fun getAccessToken(): String? = current()?.first

    suspend fun getRefreshToken(): String? = current()?.second

    suspend fun saveTokens(
        accessToken: String,
        refreshToken: String,
    ) {
        val json = Json.encodeToString(AuthTokens.serializer(), AuthTokens(accessToken, refreshToken))
        val blob = Base64.encodeToString(crypto.encrypt(json.encodeToByteArray()), Base64.NO_WRAP)
        dataStore.updateData { blob }
    }

    suspend fun clear() {
        dataStore.updateData { "" }
    }

    private suspend fun current(): Pair<String, String>? = decode(dataStore.data.first())

    /** 密文损坏（如跨设备恢复后无法解密）时按"未登录"处理，避免崩溃循环 */
    private fun decode(blob: String): Pair<String, String>? =
        if (blob.isBlank()) {
            null
        } else {
            runCatching {
                val plain = crypto.decrypt(Base64.decode(blob, Base64.NO_WRAP)).decodeToString()
                val tokens = Json.decodeFromString(AuthTokens.serializer(), plain)
                tokens.accessToken to tokens.refreshToken
            }.getOrNull()
        }

    @Serializable
    private data class AuthTokens(
        val accessToken: String,
        val refreshToken: String,
    )
}
