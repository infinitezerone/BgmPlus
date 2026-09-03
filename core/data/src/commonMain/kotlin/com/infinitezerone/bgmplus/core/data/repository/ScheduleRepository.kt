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

    /**
     * 前台极速刷新官方日历（50KB）：
     * 100% 不碰 CDN，直接结合本地已有的播放源毫秒级入库，0 额外开销。
     */
    suspend fun refreshSchedules(): AppResult<Unit>

    /**
     * 后台 / 手动同步 CDN 播放源静态数据：
     * 向 CDN 发起 ETag 304 探测；若有更新则批量更新 Room 中动画的播放链接。
     */
    suspend fun syncBangumiData(force: Boolean = false): AppResult<Unit>
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

            // 2. 读取本地已有的缓存实体，复用已同步好的播放源与时刻（0 次 CDN 请求）
            val existingEntities = scheduleDao.getAllSchedulesList().associateBy { it.bgmId }
            val entities = mutableListOf<AirScheduleEntity>()

            for (day in calendarDays) {
                val officialWeekday = day.weekday.id
                for (subject in day.items) {
                    val bgmId = subject.id
                    val existing = existingEntities[bgmId]

                    val coverUrl =
                        (subject.images?.bestImage ?: "").replace("http://", "https://")
                    val titleCn = subject.nameCn.ifBlank { existing?.titleCn ?: "" }
                    val beginUtc =
                        existing?.beginUtc.takeIf { !it.isNullOrBlank() }
                            ?: subject.airDate

                    entities.add(
                        AirScheduleEntity(
                            bgmId = bgmId,
                            title = subject.name,
                            titleCn = titleCn,
                            coverUrl = coverUrl,
                            ratingScore = subject.rating?.score ?: 0.0,
                            beginUtc = beginUtc,
                            weekday = officialWeekday,
                            timeCst = existing?.timeCst ?: "",
                            timeJst = existing?.timeJst ?: "",
                            sitesJson = existing?.sitesJson ?: "[]",
                        ),
                    )
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

    override suspend fun syncBangumiData(force: Boolean): AppResult<Unit> =
        try {
            val currentEtag =
                if (force) {
                    ""
                } else {
                    userPreferences.userPreferences
                        .firstOrNull()
                        ?.bangumiDataEtag
                        .orEmpty()
                }

            val bangumiDataResult =
                runCatching { dataService.getBangumiData(currentEtag) }.getOrNull()

            when (bangumiDataResult) {
                is BangumiDataResult.Success -> {
                    val now = TimeUtils.nowEpochMillis()
                    val newEtag = bangumiDataResult.etag
                    if (!newEtag.isNullOrBlank()) {
                        userPreferences.setBangumiDataEtag(newEtag)
                    }
                    userPreferences.setBangumiDataLastSyncTimestamp(now)

                    val bgmMap =
                        bangumiDataResult.items
                            .filter { it.bgmSubjectId != null }
                            .associateBy { it.bgmSubjectId!! }

                    val existingEntities = scheduleDao.getAllSchedulesList()
                    if (existingEntities.isNotEmpty()) {
                        val updatedEntities =
                            existingEntities.map { entity ->
                                val dataItem = bgmMap[entity.bgmId]
                                if (dataItem != null) {
                                    val siteLinks =
                                        dataItem.sites.mapNotNull { s -> resolveSiteLink(s) }
                                    val timeCst = TimeUtils.formatToCstTime(dataItem.begin)
                                    val timeJst = TimeUtils.formatToJstTime(dataItem.begin)
                                    val titleCn =
                                        entity.titleCn.ifBlank { dataItem.chineseTitle ?: "" }

                                    entity.copy(
                                        titleCn = titleCn,
                                        beginUtc = dataItem.begin,
                                        timeCst = timeCst,
                                        timeJst = timeJst,
                                        sitesJson = json.encodeToString(siteLinks),
                                    )
                                } else {
                                    entity
                                }
                            }
                        scheduleDao.insertSchedules(updatedEntities)
                    }
                    AppResult.Success(Unit)
                }

                is BangumiDataResult.NotModified -> {
                    val now = TimeUtils.nowEpochMillis()
                    userPreferences.setBangumiDataLastSyncTimestamp(now)
                    AppResult.Success(Unit)
                }

                null -> {
                    AppResult.Error(IllegalStateException("Failed to fetch bangumi-data from CDN"))
                }
            }
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

        val calculatedEp = TimeUtils.calculateCurrentEpisode(beginUtc)

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
            nextEpisodeNumber = calculatedEp,
        )
    }
}
