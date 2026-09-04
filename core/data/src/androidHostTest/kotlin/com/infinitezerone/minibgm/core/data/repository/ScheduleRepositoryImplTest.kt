package com.infinitezerone.minibgm.core.data.repository

import com.infinitezerone.minibgm.core.common.AppResult
import com.infinitezerone.minibgm.core.database.dao.AirScheduleDao
import com.infinitezerone.minibgm.core.database.entity.AirScheduleEntity
import com.infinitezerone.minibgm.core.model.BangumiDataItem
import com.infinitezerone.minibgm.core.model.BangumiDataSite
import com.infinitezerone.minibgm.core.model.SearchSubjectsRequest
import com.infinitezerone.minibgm.core.model.Subject
import com.infinitezerone.minibgm.core.model.SubjectCharacter
import com.infinitezerone.minibgm.core.model.SubjectPerson
import com.infinitezerone.minibgm.core.model.SubjectRelation
import com.infinitezerone.minibgm.core.network.BangumiApiService
import com.infinitezerone.minibgm.core.network.BangumiDataResult
import com.infinitezerone.minibgm.core.network.BangumiDataService
import com.infinitezerone.minibgm.core.network.model.CalendarDayResponse
import com.infinitezerone.minibgm.core.network.model.CalendarWeekday
import com.infinitezerone.minibgm.core.network.model.EpisodePageResponse
import com.infinitezerone.minibgm.core.network.model.PageResponse
import com.infinitezerone.minibgm.core.network.model.SearchSubjectResponse
import com.infinitezerone.minibgm.core.network.model.UserCollectionPageResponse
import com.infinitezerone.minibgm.core.testing.datastore.createTestUserPreferencesDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ScheduleRepositoryImplTest {
    private class FakeAirScheduleDao : AirScheduleDao {
        private val schedulesFlow = MutableStateFlow<List<AirScheduleEntity>>(emptyList())

        override fun getSchedulesByWeekday(weekday: Int): Flow<List<AirScheduleEntity>> =
            schedulesFlow.map { list -> list.filter { it.weekday == weekday } }

        override fun getAllSchedules(): Flow<List<AirScheduleEntity>> = schedulesFlow

        override suspend fun getAllSchedulesList(): List<AirScheduleEntity> = schedulesFlow.value

        override suspend fun insertSchedules(schedules: List<AirScheduleEntity>) {
            val currentMap = schedulesFlow.value.associateBy { it.bgmId }.toMutableMap()
            schedules.forEach { currentMap[it.bgmId] = it }
            schedulesFlow.value = currentMap.values.toList()
        }

        override suspend fun clearSchedules() {
            schedulesFlow.value = emptyList()
        }
    }

    private class FakeBangumiApiService : BangumiApiService {
        var calendarDays: List<CalendarDayResponse> = emptyList()

        override suspend fun getCalendar(): List<CalendarDayResponse> = calendarDays

        override suspend fun getSubject(id: Long): Subject = error("Not implemented")

        override suspend fun getSubjectCharacters(id: Long): List<SubjectCharacter> = error("Not implemented")

        override suspend fun getSubjectPersons(id: Long): List<SubjectPerson> = error("Not implemented")

        override suspend fun getSubjectRelations(id: Long): List<SubjectRelation> = error("Not implemented")

        override suspend fun getEpisodes(
            subjectId: Long,
            limit: Int,
            offset: Int,
        ): EpisodePageResponse = error("Not implemented")

        override suspend fun searchSubjects(
            keyword: String,
            type: Int,
            limit: Int,
            offset: Int,
        ): SearchSubjectResponse = error("Not implemented")

        override suspend fun searchSubjectsAdvanced(
            request: SearchSubjectsRequest,
            limit: Int,
            offset: Int,
        ): PageResponse<Subject> = error("Not implemented")

        override suspend fun getUserCollections(
            username: String,
            subjectType: Int,
            type: Int?,
            limit: Int,
            offset: Int,
        ): UserCollectionPageResponse = error("Not implemented")

        override suspend fun getMe(): com.infinitezerone.minibgm.core.model.UserProfile = error("Not implemented")

        override suspend fun getCollection(
            username: String,
            subjectId: Long,
        ): com.infinitezerone.minibgm.core.model.UserCollection? = error("Not implemented")

        override suspend fun updateCollection(
            subjectId: Long,
            type: Int,
            rate: Int?,
            comment: String?,
            private: Boolean,
            epStatus: Int?,
        ) = error("Not implemented")

        override suspend fun updateEpisodeStatus(
            subjectId: Long,
            episodeId: Long,
            type: Int,
        ) = error("Not implemented")
    }

    private class FakeBangumiDataService : BangumiDataService {
        var dataResult: BangumiDataResult = BangumiDataResult.NotModified
        var calledEtag: String? = null
        var callCount: Int = 0

        override suspend fun getBangumiData(etag: String?): BangumiDataResult {
            callCount++
            calledEtag = etag
            return dataResult
        }
    }

    @Test
    fun refreshSchedules_fetchesCalendarWithoutTouchingCDN() =
        runTest {
            val apiService =
                FakeBangumiApiService().apply {
                    calendarDays =
                        listOf(
                            CalendarDayResponse(
                                weekday = CalendarWeekday(en = "Sun", cn = "星期日", ja = "日", id = 7),
                                items =
                                    listOf(
                                        Subject(
                                            id = 1001L,
                                            name = "无职转生",
                                            nameCn = "无职转生 第三季",
                                        ),
                                    ),
                            ),
                        )
                }
            val dataService = FakeBangumiDataService()
            val dao = FakeAirScheduleDao()
            val userPrefs = createTestUserPreferencesDataSource()

            val repo =
                ScheduleRepositoryImpl(
                    apiService = apiService,
                    dataService = dataService,
                    scheduleDao = dao,
                    userPreferences = userPrefs,
                )

            val result = repo.refreshSchedules()

            assertIs<AppResult.Success<Unit>>(result)
            assertEquals(0, dataService.callCount) // 0 次 CDN 请求
            val stored = dao.getAllSchedulesList()
            assertEquals(1, stored.size)
            assertEquals(1001L, stored[0].bgmId)
            assertEquals(7, stored[0].weekday)
            assertEquals("无职转生 第三季", stored[0].titleCn)
        }

    @Test
    fun syncBangumiData_updatesRoomOnSuccess() =
        runTest {
            val apiService = FakeBangumiApiService()
            val dataService =
                FakeBangumiDataService().apply {
                    dataResult =
                        BangumiDataResult.Success(
                            items =
                                listOf(
                                    BangumiDataItem(
                                        title = "无职转生",
                                        titleTranslate = mapOf("zh-Hans" to listOf("无职转生 第三季")),
                                        begin = "2026-07-05T15:00:00.000Z",
                                        sites =
                                            listOf(
                                                BangumiDataSite(site = "bangumi", id = "1001"),
                                                BangumiDataSite(site = "bilibili", id = "md12345"),
                                            ),
                                    ),
                                ),
                            etag = "W/\"etag-999\"",
                        )
                }
            val dao =
                FakeAirScheduleDao().apply {
                    insertSchedules(
                        listOf(
                            AirScheduleEntity(
                                bgmId = 1001L,
                                title = "无职转生",
                                titleCn = "无职转生",
                                coverUrl = "",
                                ratingScore = 8.5,
                                beginUtc = "",
                                weekday = 7,
                                timeCst = "",
                                timeJst = "",
                                sitesJson = "[]",
                            ),
                        ),
                    )
                }
            val userPrefs = createTestUserPreferencesDataSource()

            val repo =
                ScheduleRepositoryImpl(
                    apiService = apiService,
                    dataService = dataService,
                    scheduleDao = dao,
                    userPreferences = userPrefs,
                )

            val result = repo.syncBangumiData(force = false)

            assertIs<AppResult.Success<Unit>>(result)
            assertEquals(1, dataService.callCount)
            assertEquals("W/\"etag-999\"", userPrefs.userPreferences.first().bangumiDataEtag)
            assertTrue(userPrefs.userPreferences.first().bangumiDataLastSyncTimestamp > 0L)

            val updated = dao.getAllSchedulesList().first { it.bgmId == 1001L }
            assertTrue(updated.sitesJson.contains("哔哩哔哩"))
            assertEquals("23:00", updated.timeCst)
        }

    @Test
    fun syncBangumiData_populatesCalendarFirst_whenDaoIsEmpty() =
        runTest {
            val apiService =
                FakeBangumiApiService().apply {
                    calendarDays =
                        listOf(
                            CalendarDayResponse(
                                weekday = CalendarWeekday(en = "Sun", cn = "星期日", ja = "日", id = 7),
                                items =
                                    listOf(
                                        Subject(
                                            id = 1001L,
                                            name = "无职转生",
                                            nameCn = "无职转生",
                                        ),
                                    ),
                            ),
                        )
                }
            val dataService =
                FakeBangumiDataService().apply {
                    dataResult =
                        BangumiDataResult.Success(
                            items =
                                listOf(
                                    BangumiDataItem(
                                        title = "无职转生",
                                        titleTranslate = mapOf("zh-Hans" to listOf("无职转生 第三季")),
                                        begin = "2026-07-05T15:00:00.000Z",
                                        sites =
                                            listOf(
                                                BangumiDataSite(site = "bangumi", id = "1001"),
                                                BangumiDataSite(site = "bilibili", id = "md12345"),
                                            ),
                                    ),
                                ),
                            etag = "W/\"etag-coldstart\"",
                        )
                }
            val dao = FakeAirScheduleDao() // 完全为空
            val userPrefs = createTestUserPreferencesDataSource()

            val repo =
                ScheduleRepositoryImpl(
                    apiService = apiService,
                    dataService = dataService,
                    scheduleDao = dao,
                    userPreferences = userPrefs,
                )

            val result = repo.syncBangumiData(force = false)

            assertIs<AppResult.Success<Unit>>(result)
            val stored = dao.getAllSchedulesList()
            assertEquals(1, stored.size)
            val item = stored.first()
            assertEquals(1001L, item.bgmId)
            assertTrue(item.sitesJson.contains("哔哩哔哩"))
            assertEquals("23:00", item.timeCst)
            assertEquals("W/\"etag-coldstart\"", userPrefs.userPreferences.first().bangumiDataEtag)
        }

    @Test
    fun syncBangumiData_forcesFetch_whenExistingEntitiesAllHaveEmptySites() =
        runTest {
            val apiService = FakeBangumiApiService()
            val dataService =
                FakeBangumiDataService().apply {
                    dataResult =
                        BangumiDataResult.Success(
                            items =
                                listOf(
                                    BangumiDataItem(
                                        title = "无职转生",
                                        titleTranslate = mapOf("zh-Hans" to listOf("无职转生 第三季")),
                                        begin = "2026-07-05T15:00:00.000Z",
                                        sites =
                                            listOf(
                                                BangumiDataSite(site = "bangumi", id = "1001"),
                                                BangumiDataSite(site = "bilibili", id = "md12345"),
                                            ),
                                    ),
                                ),
                            etag = "W/\"etag-forced\"",
                        )
                }
            val dao =
                FakeAirScheduleDao().apply {
                    insertSchedules(
                        listOf(
                            AirScheduleEntity(
                                bgmId = 1001L,
                                title = "无职转生",
                                titleCn = "无职转生",
                                coverUrl = "",
                                ratingScore = 8.5,
                                beginUtc = "",
                                weekday = 7,
                                timeCst = "",
                                timeJst = "",
                                sitesJson = "[]", // 播放源全部为空
                            ),
                        ),
                    )
                }
            val userPrefs = createTestUserPreferencesDataSource()
            userPrefs.setBangumiDataEtag("W/\"old-stale-etag\"")

            val repo =
                ScheduleRepositoryImpl(
                    apiService = apiService,
                    dataService = dataService,
                    scheduleDao = dao,
                    userPreferences = userPrefs,
                )

            // force = false，但由于本地条目 sitesJson 均为空，应自动强制使用空 ETag 拉取避免 304 死锁
            val result = repo.syncBangumiData(force = false)

            assertIs<AppResult.Success<Unit>>(result)
            assertEquals("", dataService.calledEtag) // 强制使用空 ETag
            val updated = dao.getAllSchedulesList().first { it.bgmId == 1001L }
            assertTrue(updated.sitesJson.contains("哔哩哔哩"))
            assertEquals("W/\"etag-forced\"", userPrefs.userPreferences.first().bangumiDataEtag)
        }
}
