package com.infinitezerone.bgmplus.core.testing

import com.infinitezerone.bgmplus.core.common.AppResult
import com.infinitezerone.bgmplus.core.testing.data.sampleAirScheduleList
import com.infinitezerone.bgmplus.core.testing.data.sampleEpisodeList
import com.infinitezerone.bgmplus.core.testing.data.sampleSubject
import com.infinitezerone.bgmplus.core.testing.repository.FakeAuthRepository
import com.infinitezerone.bgmplus.core.testing.repository.FakeScheduleRepository
import com.infinitezerone.bgmplus.core.testing.repository.FakeSubjectRepository
import com.infinitezerone.bgmplus.core.testing.util.MainDispatcherRule
import com.infinitezerone.bgmplus.core.testing.util.testBgmDispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FakeRepositoriesTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun testBgmDispatchersCreation() {
        val dispatchers = testBgmDispatchers(mainDispatcherRule.testDispatcher)
        assertNotNull(dispatchers.default)
        assertNotNull(dispatchers.io)
        assertNotNull(dispatchers.main)
    }

    @Test
    fun fakeAuthRepository_loginAndLogoutFlow() =
        runTest {
            val authRepo = FakeAuthRepository(initialLoggedIn = false)
            assertEquals(false, authRepo.isLoggedIn.first())

            val url = authRepo.beginLogin()
            assertTrue(url.isNotBlank())
            assertEquals(1, authRepo.beginLoginCallCount)

            val loginResult = authRepo.completeLogin("code", "state")
            assertIs<AppResult.Success<Unit>>(loginResult)
            assertEquals(1, authRepo.completeLoginCallCount)
            assertEquals(true, authRepo.isLoggedIn.first())

            authRepo.logout()
            assertEquals(1, authRepo.logoutCallCount)
            assertEquals(false, authRepo.isLoggedIn.first())
        }

    @Test
    fun fakeScheduleRepository_emitsAndRefreshes() =
        runTest {
            val scheduleRepo = FakeScheduleRepository()
            assertEquals(emptyList(), scheduleRepo.getSchedulesByWeekday(5).first())

            scheduleRepo.sendSchedules(weekday = 5, schedules = sampleAirScheduleList)
            val result = scheduleRepo.getSchedulesByWeekday(5).first()
            assertEquals(1, result.size)
            assertEquals(1001L, result.first().bgmId)

            val refreshResult = scheduleRepo.refreshSchedules()
            assertIs<AppResult.Success<Unit>>(refreshResult)
            assertEquals(1, scheduleRepo.refreshCallCount)
        }

    @Test
    fun fakeSubjectRepository_fetchesSubjectAndEpisodes() =
        runTest {
            val subjectRepo = FakeSubjectRepository()
            subjectRepo.sendSubject(sampleSubject)
            subjectRepo.sendEpisodes(sampleSubject.id, sampleEpisodeList)

            val subjectStream = subjectRepo.getSubjectStream(sampleSubject.id).first()
            assertNotNull(subjectStream)
            assertEquals("葬送のフリーレン", subjectStream.name)

            val detailResult = subjectRepo.fetchSubjectDetail(sampleSubject.id)
            assertIs<AppResult.Success<com.infinitezerone.bgmplus.core.model.Subject>>(detailResult)
            assertEquals(1, subjectRepo.fetchSubjectDetailCallCount)

            val episodes = subjectRepo.getEpisodesStream(sampleSubject.id).first()
            assertEquals(2, episodes.size)

            val fetchEpisodesResult = subjectRepo.fetchEpisodes(sampleSubject.id)
            assertIs<AppResult.Success<List<com.infinitezerone.bgmplus.core.model.Episode>>>(fetchEpisodesResult)
            assertEquals(1, subjectRepo.fetchEpisodesCallCount)
        }
}
