package com.infinitezerone.bgmplus.core.data.repository

import com.infinitezerone.bgmplus.core.common.AppResult
import com.infinitezerone.bgmplus.core.common.TimeUtils
import com.infinitezerone.bgmplus.core.database.dao.AirScheduleDao
import com.infinitezerone.bgmplus.core.database.entity.AirScheduleEntity
import com.infinitezerone.bgmplus.core.model.AirSchedule
import com.infinitezerone.bgmplus.core.model.SiteLink
import com.infinitezerone.bgmplus.core.network.BangumiApiService
import com.infinitezerone.bgmplus.core.network.BangumiDataService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface ScheduleRepository {
    fun getSchedulesByWeekday(weekday: Int): Flow<List<AirSchedule>>

    suspend fun refreshSchedules(): AppResult<Unit>
}

class ScheduleRepositoryImpl(
    private val apiService: BangumiApiService,
    private val dataService: BangumiDataService,
    private val scheduleDao: AirScheduleDao,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : ScheduleRepository {
    override fun getSchedulesByWeekday(weekday: Int): Flow<List<AirSchedule>> =
        scheduleDao.getSchedulesByWeekday(weekday).map { entities ->
            entities.map { it.toModel(json) }
        }

    override suspend fun refreshSchedules(): AppResult<Unit> =
        try {
            // 1. 获取官方每日放送日历数据（包含高清海报图片、官方评分与排行、星期分类）
            val calendarDays =
                runCatching { apiService.getCalendar() }.getOrNull() ?: emptyList()

            // 2. 获取 bangumi-data 聚合元数据（包含放送站点播放链接与精准 CST 放送时刻）
            val bangumiItems =
                runCatching { dataService.getBangumiData() }.getOrNull() ?: emptyList()

            val bgmMap = bangumiItems.filter { it.bgmSubjectId != null }.associateBy { it.bgmSubjectId!! }

            val entities = mutableListOf<AirScheduleEntity>()
            val processedIds = mutableSetOf<Long>()

            // 优先基于官方日历构建带封面的条目
            for (day in calendarDays) {
                val officialWeekday = day.weekday.id
                for (subject in day.items) {
                    val bgmId = subject.id
                    processedIds.add(bgmId)
                    val dataItem = bgmMap[bgmId]

                    val weekday = dataItem?.let { TimeUtils.getCstWeekday(it.begin) } ?: officialWeekday
                    val timeCst = dataItem?.let { TimeUtils.formatToCstTime(it.begin) } ?: ""
                    val timeJst = dataItem?.let { TimeUtils.formatToJstTime(it.begin) } ?: ""
                    val beginUtc = dataItem?.begin ?: ""

                    val siteLinks =
                        dataItem?.sites?.map { s ->
                            SiteLink(
                                siteName = s.site,
                                displayName = formatSiteName(s.site),
                                playUrl = s.url.ifBlank { "https://${s.site}.com" },
                            )
                        } ?: emptyList()

                    val coverUrl =
                        (subject.images?.bestImage ?: "").replace("http://", "https://")
                    val titleCn = subject.nameCn.ifBlank { dataItem?.chineseTitle ?: "" }

                    entities.add(
                        AirScheduleEntity(
                            bgmId = bgmId,
                            title = subject.name,
                            titleCn = titleCn,
                            coverUrl = coverUrl,
                            ratingScore = subject.rating?.score ?: 0.0,
                            beginUtc = beginUtc,
                            weekday = weekday,
                            timeCst = timeCst,
                            timeJst = timeJst,
                            sitesJson = json.encodeToString(siteLinks),
                        ),
                    )
                }
            }

            // 补充 bangumi-data 中存在但不在官方当前日历里的其余放送条目
            for (item in bangumiItems) {
                val bgmId = item.bgmSubjectId ?: continue
                if (bgmId in processedIds) continue

                val weekday = TimeUtils.getCstWeekday(item.begin)
                val timeCst = TimeUtils.formatToCstTime(item.begin)
                val timeJst = TimeUtils.formatToJstTime(item.begin)

                val siteLinks =
                    item.sites.map { s ->
                        SiteLink(
                            siteName = s.site,
                            displayName = formatSiteName(s.site),
                            playUrl = s.url.ifBlank { "https://${s.site}.com" },
                        )
                    }

                entities.add(
                    AirScheduleEntity(
                        bgmId = bgmId,
                        title = item.title,
                        titleCn = item.chineseTitle,
                        coverUrl = "",
                        ratingScore = 0.0,
                        beginUtc = item.begin,
                        weekday = weekday,
                        timeCst = timeCst,
                        timeJst = timeJst,
                        sitesJson = json.encodeToString(siteLinks),
                    ),
                )
            }

            if (entities.isNotEmpty()) {
                scheduleDao.insertSchedules(entities)
                AppResult.Success(Unit)
            } else if (calendarDays.isEmpty() && bangumiItems.isEmpty()) {
                AppResult.Error(IllegalStateException("Failed to load schedule data from all sources"))
            } else {
                AppResult.Success(Unit)
            }
        } catch (e: Throwable) {
            AppResult.Error(e)
        }

    private fun formatSiteName(site: String): String =
        when (site.lowercase()) {
            "bilibili" -> "哔哩哔哩"
            "bahamut" -> "巴哈姆特"
            "iqiyi" -> "爱奇艺"
            "qq" -> "腾讯视频"
            "youku" -> "优酷"
            "netflix" -> "Netflix"
            else -> site
        }

    private fun AirScheduleEntity.toModel(json: Json): AirSchedule {
        val links: List<SiteLink> =
            try {
                json.decodeFromString(sitesJson)
            } catch (_: Exception) {
                emptyList()
            }

        return AirSchedule(
            bgmId = bgmId,
            title = title,
            titleCn = titleCn,
            coverUrl = coverUrl,
            ratingScore = ratingScore,
            beginUtc = beginUtc,
            weekday = weekday,
            timeCst = timeCst,
            timeJst = timeJst,
            siteLinks = links,
        )
    }
}
