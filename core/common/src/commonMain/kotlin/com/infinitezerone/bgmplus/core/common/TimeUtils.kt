package com.infinitezerone.bgmplus.core.common

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

object TimeUtils {
    private val timeZoneCst = TimeZone.of("Asia/Shanghai")
    private val timeZoneJst = TimeZone.of("Asia/Tokyo")

    fun formatToCstTime(isoUtcString: String): String =
        try {
            val instant = Instant.parse(isoUtcString)
            val local = instant.toLocalDateTime(timeZoneCst)
            val hourStr = local.hour.toString().padStart(2, '0')
            val minStr = local.minute.toString().padStart(2, '0')
            "$hourStr:$minStr"
        } catch (_: Exception) {
            ""
        }

    fun formatToJstTime(isoUtcString: String): String =
        try {
            val instant = Instant.parse(isoUtcString)
            val local = instant.toLocalDateTime(timeZoneJst)
            val hourStr = local.hour.toString().padStart(2, '0')
            val minStr = local.minute.toString().padStart(2, '0')
            "$hourStr:$minStr"
        } catch (_: Exception) {
            ""
        }

    fun getCstWeekday(isoUtcString: String): Int =
        try {
            val instant = Instant.parse(isoUtcString)
            val local = instant.toLocalDateTime(timeZoneCst)
            local.dayOfWeek.ordinal + 1
        } catch (_: Exception) {
            1
        }

    fun nowEpochMillis(): Long =
        kotlin.time.Clock.System
            .now()
            .toEpochMilliseconds()
}
