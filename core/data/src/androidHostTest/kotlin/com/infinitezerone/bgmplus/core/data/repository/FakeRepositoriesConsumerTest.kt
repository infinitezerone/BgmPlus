package com.infinitezerone.bgmplus.core.data.repository

import com.infinitezerone.bgmplus.core.testing.data.sampleAirScheduleList
import com.infinitezerone.bgmplus.core.testing.data.sampleEpisodeList
import com.infinitezerone.bgmplus.core.testing.data.sampleSubject
import com.infinitezerone.bgmplus.core.testing.data.sampleUserProfile
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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 验证从其他模块（如 :core:data 或后续 :feature:*）引入并消费 :core:testing
 * 基础设施时的可用性与行为正确性。
 */
class FakeRepositoriesConsumerTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun verifyTestDispatchersIntegration() {
        val dispatchers = testBgmDispatchers(mainDispatcherRule.testDispatcher)
        assertNotNull(dispatchers.default)
        assertNotNull(dispatchers.io)
        assertNotNull(dispatchers.main)
    }

    @Test
    fun verifyFakeAuthRepositoryInConsumer() =
        runTest {
            val authRepo = FakeAuthRepository(initialLoggedIn = true)
            assertTrue(authRepo.isLoggedIn.first())

            authRepo.logout()
            assertFalse(authRepo.isLoggedIn.first())
            assertEquals(1, authRepo.logoutCallCount)
        }

    @Test
    fun verifyFakeScheduleRepositoryInConsumer() =
        runTest {
            val scheduleRepo = FakeScheduleRepository()
            scheduleRepo.sendSchedules(weekday = 5, schedules = sampleAirScheduleList)

            val schedules = scheduleRepo.getSchedulesByWeekday(5).first()
            assertEquals(1, schedules.size)
            assertEquals("葬送のフリーレン", schedules.first().title)
        }

    @Test
    fun verifyFakeSubjectRepositoryInConsumer() =
        runTest {
            val subjectRepo = FakeSubjectRepository()
            subjectRepo.sendSubject(sampleSubject)
            subjectRepo.sendEpisodes(sampleSubject.id, sampleEpisodeList)

            val subject = subjectRepo.getSubjectStream(sampleSubject.id).first()
            assertNotNull(subject)
            assertEquals(sampleSubject.id, subject.id)

            val episodes = subjectRepo.getEpisodesStream(sampleSubject.id).first()
            assertEquals(2, episodes.size)
            assertEquals("冒険の終わり", episodes.first().name)
        }

    @Test
    fun verifySampleDataIntegrity() {
        assertEquals(1001L, sampleSubject.id)
        assertEquals(2, sampleEpisodeList.size)
        assertEquals(1, sampleAirScheduleList.size)
        assertEquals(42L, sampleUserProfile.id)
    }
}
