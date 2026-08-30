package com.infinitezerone.bgmplus.core.data.repository

import com.infinitezerone.bgmplus.core.common.AppResult
import com.infinitezerone.bgmplus.core.common.TimeUtils
import com.infinitezerone.bgmplus.core.database.dao.AirScheduleDao
import com.infinitezerone.bgmplus.core.database.entity.AirScheduleEntity
import com.infinitezerone.bgmplus.core.model.AirSchedule
import com.infinitezerone.bgmplus.core.model.SiteLink
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
    private val dataService: BangumiDataService,
    private val scheduleDao: AirScheduleDao,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : ScheduleRepository {
    override fun getSchedulesByWeekday(weekday: Int): Flow<List<AirSchedule>> =
        scheduleDao.getSchedulesByWeekday(weekday).map { entities ->
            entities.map { it.toModel(json) }
        }

    override suspend fun refreshSchedules(): AppResult<Unit> {
        return try {
            val bangumiItems = dataService.getBangumiData()
            val entities =
                bangumiItems.mapNotNull { item ->
                    val bgmId = item.bgmSubjectId ?: return@mapNotNull null
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
                    )
                }

            if (entities.isNotEmpty()) {
                scheduleDao.insertSchedules(entities)
            }
            AppResult.Success(Unit)
        } catch (e: Throwable) {
            AppResult.Error(e)
        }
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
