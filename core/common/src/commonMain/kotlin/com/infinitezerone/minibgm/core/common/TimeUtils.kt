package com.infinitezerone.minibgm.core.common

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
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
        Clock.System
            .now()
            .toEpochMilliseconds()

    /**
     * 根据开播日期（如 "2026-07-02" 或 "2026-07-02T15:00:00.000Z"）计算当前当周所播话数
     */
    fun calculateCurrentEpisode(startDateStr: String): Int {
        if (startDateStr.isBlank()) return 0
        return try {
            val dateStr = startDateStr.substringBefore("T").trim()
            val startDate = LocalDate.parse(dateStr)
            val today =
                Clock.System
                    .now()
                    .toLocalDateTime(timeZoneCst)
                    .date
            val days = startDate.daysUntil(today)
            if (days < 0) {
                1
            } else {
                (days / 7) + 1
            }
        } catch (_: Exception) {
            0
        }
    }

    /**
     * 将秒级时间戳格式化为本地日期字符串 (yyyy-MM-dd)
     */
    fun formatEpochSecondsToDate(epochSeconds: Long): String {
        if (epochSeconds <= 0) return ""
        return try {
            val instant = Instant.fromEpochSeconds(epochSeconds)
            val local = instant.toLocalDateTime(timeZoneCst)
            @Suppress("DEPRECATION")
            "${local.year}-${local.monthNumber.toString().padStart(2, '0')}-${local.dayOfMonth.toString().padStart(2, '0')}"
        } catch (_: Exception) {
            ""
        }
    }
}
