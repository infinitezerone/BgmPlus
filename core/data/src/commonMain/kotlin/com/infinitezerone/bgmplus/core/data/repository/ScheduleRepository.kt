package com.infinitezerone.bgmplus.core.data.repository

import com.infinitezerone.bgmplus.core.common.AppResult
import com.infinitezerone.bgmplus.core.common.TimeUtils
import com.infinitezerone.bgmplus.core.database.dao.AirScheduleDao
import com.infinitezerone.bgmplus.core.database.entity.AirScheduleEntity
import com.infinitezerone.bgmplus.core.datastore.UserPreferencesDataSource
import com.infinitezerone.bgmplus.core.model.AirSchedule
import com.infinitezerone.bgmplus.core.model.BangumiDataSite
import com.infinitezerone.bgmplus.core.model.SiteLink
import com.infinitezerone.bgmplus.core.network.BangumiApiService
import com.infinitezerone.bgmplus.core.network.BangumiDataResult
import com.infinitezerone.bgmplus.core.network.BangumiDataService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
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
    private val userPreferences: UserPreferencesDataSource,
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

            if (calendarDays.isEmpty()) {
                return AppResult.Error(IllegalStateException("Failed to load official schedule calendar"))
            }

            // 2. 获取本地存储的 ETag 指纹，向 CDN 发起 304 条件请求（无更新时 0 字节传输）
            val currentEtag =
                userPreferences.userPreferences
                    .firstOrNull()
                    ?.bangumiDataEtag
                    .orEmpty()
            val bangumiDataResult =
                runCatching { dataService.getBangumiData(currentEtag) }.getOrNull()

            // 3. 读取本地已有的缓存实体，用于在 304 Not Modified 时复用已有播放源和时刻
            val existingEntities = scheduleDao.getAllSchedulesList().associateBy { it.bgmId }
            val entities = mutableListOf<AirScheduleEntity>()

            when (bangumiDataResult) {
                is BangumiDataResult.Success -> {
                    // CDN 有新版本发布 (200 OK)：全量融合新数据，并持久化新的 ETag 指纹
                    val bgmMap =
                        bangumiDataResult.items
                            .filter { it.bgmSubjectId != null }
                            .associateBy { it.bgmSubjectId!! }

                    val newEtag = bangumiDataResult.etag
                    if (!newEtag.isNullOrBlank()) {
                        userPreferences.setBangumiDataEtag(newEtag)
                    }

                    for (day in calendarDays) {
                        val officialWeekday = day.weekday.id
                        for (subject in day.items) {
                            val bgmId = subject.id
                            val dataItem = bgmMap[bgmId]

                            val weekday = officialWeekday
                            val timeCst = dataItem?.let { TimeUtils.formatToCstTime(it.begin) } ?: ""
                            val timeJst = dataItem?.let { TimeUtils.formatToJstTime(it.begin) } ?: ""
                            val beginUtc = dataItem?.begin ?: ""
                            val siteLinks =
                                dataItem?.sites?.mapNotNull { s -> resolveSiteLink(s) } ?: emptyList()

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
                }

                BangumiDataResult.NotModified, null -> {
                    // CDN 无更新 (304 Not Modified) 或网络异常：0 字节传输，复用本地已有的播放源与时间，仅更新官方海报/评分/标题
                    for (day in calendarDays) {
                        val officialWeekday = day.weekday.id
                        for (subject in day.items) {
                            val bgmId = subject.id
                            val existing = existingEntities[bgmId]

                            val coverUrl =
                                (subject.images?.bestImage ?: "").replace("http://", "https://")
                            val titleCn = subject.nameCn.ifBlank { existing?.titleCn ?: "" }

                            entities.add(
                                AirScheduleEntity(
                                    bgmId = bgmId,
                                    title = subject.name,
                                    titleCn = titleCn,
                                    coverUrl = coverUrl,
                                    ratingScore = subject.rating?.score ?: 0.0,
                                    beginUtc = existing?.beginUtc ?: "",
                                    weekday = officialWeekday,
                                    timeCst = existing?.timeCst ?: "",
                                    timeJst = existing?.timeJst ?: "",
                                    sitesJson = existing?.sitesJson ?: "[]",
                                ),
                            )
                        }
                    }
                }
            }

            if (entities.isNotEmpty()) {
                scheduleDao.clearSchedules()
                scheduleDao.insertSchedules(entities)
            }
            AppResult.Success(Unit)
        } catch (e: Throwable) {
            AppResult.Error(e)
        }

    private fun resolveSiteLink(site: BangumiDataSite): SiteLink? {
        val siteKey = site.site.lowercase()
        val id = site.id
        val rawUrl = site.url.ifBlank { "" }

        val (displayName, url) =
            when (siteKey) {
                "bilibili" ->
                    "哔哩哔哩" to
                        rawUrl.ifBlank {
                            if (id.startsWith("http")) {
                                id
                            } else if (id.startsWith("md")) {
                                "https://www.bilibili.com/bangumi/media/$id"
                            } else if (id.startsWith("ss")) {
                                "https://www.bilibili.com/bangumi/play/$id"
                            } else {
                                "https://www.bilibili.com/bangumi/media/md$id"
                            }
                        }
                "gamer", "gamer_hk" -> "巴哈姆特" to rawUrl.ifBlank { "https://ani.gamer.com.tw/animeVideo.php?sn=$id" }
                "iqiyi" -> "爱奇艺" to rawUrl.ifBlank { "https://www.iqiyi.com/v_$id.html" }
                "qq" -> "腾讯视频" to rawUrl.ifBlank { "https://v.qq.com/x/cover/$id.html" }
                "youku" -> "优酷" to rawUrl.ifBlank { "https://v.youku.com/v_show/id_$id.html" }
                "netflix" -> "Netflix" to rawUrl.ifBlank { "https://www.netflix.com/title/$id" }
                "danime" -> "d动画" to rawUrl.ifBlank { "https://animestore.docomo.ne.jp/animestore/ci_pc?workId=$id" }
                "abema" -> "ABEMA" to rawUrl.ifBlank { "https://abema.tv/channels/$id" }
                "unext" -> "U-NEXT" to rawUrl.ifBlank { "https://video.unext.jp/title/$id" }
                "prime" -> "Prime Video" to rawUrl.ifBlank { "https://www.amazon.co.jp/dp/$id" }
                "disneyplus" -> "Disney+" to rawUrl.ifBlank { "https://www.disneyplus.com/series/$id" }
                "crunchyroll" -> "Crunchyroll" to rawUrl.ifBlank { "https://www.crunchyroll.com/series/$id" }
                "muse_tw", "muse_hk" -> "木棉花" to rawUrl.ifBlank { "https://www.youtube.com/playlist?list=$id" }
                "ani_one", "ani_one_asia" -> "羚邦" to rawUrl.ifBlank { "https://www.youtube.com/playlist?list=$id" }
                "nicovideo" -> "NicoNico" to rawUrl.ifBlank { "https://ch.nicovideo.jp/$id" }
                "mikan" -> "蜜柑计划" to rawUrl.ifBlank { "https://mikanani.me/Home/Bangumi/$id" }
                else -> return null // 过滤 mal, anidb, aniList, tmdb, bangumi 等元数据站点
            }

        if (url.isBlank()) return null
        return SiteLink(
            siteName = siteKey,
            displayName = displayName,
            playUrl = url,
        )
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
