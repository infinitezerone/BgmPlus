package com.infinitezerone.bgmplus.feature.subject

import com.infinitezerone.bgmplus.core.common.AppResult
import com.infinitezerone.bgmplus.core.testing.data.sampleEpisodeList
import com.infinitezerone.bgmplus.core.testing.data.sampleSubject
import com.infinitezerone.bgmplus.core.testing.repository.FakeSubjectRepository
import com.infinitezerone.bgmplus.core.testing.util.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SubjectDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun fetchSuccess_subjectAndEpisodesEnterUiState() =
        runTest {
            val repository =
                FakeSubjectRepository().apply {
                    sendSubject(sampleSubject)
                    sendEpisodes(sampleSubject.id, sampleEpisodeList)
                }

            val viewModel = SubjectDetailViewModel(repository, sampleSubject.id)
            val state = viewModel.uiState.value

            assertFalse(state.isLoading)
            assertEquals(sampleSubject, state.subject)
            assertEquals(sampleEpisodeList, state.episodes)
            assertNull(state.error)
        }

    @Test
    fun subjectId_isForwardedToBothRepositoryCalls() =
        runTest {
            var requestedDetailId: Long? = null
            var requestedEpisodesId: Long? = null
            val repository =
                FakeSubjectRepository().apply {
                    fetchSubjectDetailResult = { id ->
                        requestedDetailId = id
                        AppResult.Success(sampleSubject.copy(id = id))
                    }
                    fetchEpisodesResult = { id ->
                        requestedEpisodesId = id
                        AppResult.Success(emptyList())
                    }
                }

            SubjectDetailViewModel(repository, 7777L)

            assertEquals(7777L, requestedDetailId)
            assertEquals(7777L, requestedEpisodesId)
        }

    @Test
    fun fetchError_setsErrorAndKeepsUiStateIntact() =
        runTest {
            val repository =
                FakeSubjectRepository().apply {
                    fetchSubjectDetailResult = { AppResult.Error(IllegalStateException("条目请求失败")) }
                }

            val viewModel = SubjectDetailViewModel(repository, sampleSubject.id)
            val state = viewModel.uiState.value

            assertFalse(state.isLoading)
            assertEquals("条目请求失败", state.error)
            assertNull(state.subject)
            assertTrue(state.episodes.isEmpty())
        }

    @Test
    fun streamUpdates_mergeIntoUiStateAfterFetchFailure() =
        runTest {
            val repository =
                FakeSubjectRepository().apply {
                    // 两次 fetch 均失败：数据只能经本地库流到达
                    fetchSubjectDetailResult = { AppResult.Error(IllegalStateException("离线")) }
                    fetchEpisodesResult = { AppResult.Error(IllegalStateException("离线")) }
                }
            val viewModel = SubjectDetailViewModel(repository, sampleSubject.id)

            repository.sendSubject(sampleSubject)
            repository.sendEpisodes(sampleSubject.id, sampleEpisodeList)

            val state = viewModel.uiState.value
            assertEquals(sampleSubject, state.subject)
            assertEquals(sampleEpisodeList, state.episodes)
            assertEquals("离线", state.error)
            assertFalse(state.isLoading)
        }
}
