package com.infinitezerone.bgmplus.core.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

object UserPreferencesSerializer : Serializer<UserPreferences> {
    override val defaultValue: UserPreferences = UserPreferences()

    override suspend fun readFrom(input: InputStream): UserPreferences {
        // 空文件（文件已创建但尚未写入，如首次写入前崩溃遗留 0 字节）按默认值处理，
        // 不走 corruption 恢复路径
        val text = input.readBytes().decodeToString()
        if (text.isBlank()) return defaultValue
        return try {
            Json.decodeFromString(UserPreferences.serializer(), text)
        } catch (exception: SerializationException) {
            throw CorruptionException("Cannot read proto preferences.", exception)
        }
    }

    override suspend fun writeTo(
        t: UserPreferences,
        output: OutputStream,
    ) {
        withContext(Dispatchers.IO) {
            output.write(
                Json.encodeToString(UserPreferences.serializer(), t).encodeToByteArray(),
            )
        }
    }
}
