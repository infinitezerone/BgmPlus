package com.infinitezerone.bgmplus.feature.search

import com.infinitezerone.bgmplus.core.common.AppResult
import com.infinitezerone.bgmplus.core.testing.data.sampleSubject
import com.infinitezerone.bgmplus.core.testing.repository.FakeSearchRepository
import com.infinitezerone.bgmplus.core.testing.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExploreViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun initialLoadTriggersAdvancedSearchSuccessfully() =
        runTest {
            val repository = FakeSearchRepository()
            repository.advancedSearchResult = AppResult.Success(listOf(sampleSubject))
            val viewModel = ExploreViewModel(repository)

            advanceUntilIdle()

            assertEquals(1, repository.advancedSearchCallCount)
            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertFalse(state.isRefreshing)
            assertNull(state.error)
            assertEquals(1, state.subjects.size)
            assertEquals("葬送的芙莉莲", state.subjects.first().nameCn)
            assertEquals(DEFAULT_SEASONS.first(), state.selectedSeason)
            assertEquals(ExploreCategory.ANIME, state.selectedCategory)
            assertEquals(ExploreSort.HEAT, state.selectedSort)
            assertNull(state.selectedTag)
        }

    @Test
    fun onSeasonSelectTriggersNewSearchWithUpdatedAirDate() =
        runTest {
            val repository = FakeSearchRepository()
            repository.advancedSearchResult = AppResult.Success(listOf(sampleSubject))
            val viewModel = ExploreViewModel(repository)
            advanceUntilIdle()

            val summerSeason = DEFAULT_SEASONS.first { it.id == "2024-q3" }
            viewModel.onSeasonSelect(summerSeason)
            advanceUntilIdle()

            assertEquals(2, repository.advancedSearchCallCount)
            assertEquals(summerSeason, viewModel.uiState.value.selectedSeason)
            assertEquals(summerSeason.airDateFilter, repository.lastAdvancedRequest?.filter?.airDate)
        }

    @Test
    fun onCategorySelectUpdatesTypeAndSearches() =
        runTest {
            val repository = FakeSearchRepository()
            repository.advancedSearchResult = AppResult.Success(listOf(sampleSubject))
            val viewModel = ExploreViewModel(repository)
            advanceUntilIdle()

            viewModel.onCategorySelect(ExploreCategory.GAME)
            advanceUntilIdle()

            assertEquals(2, repository.advancedSearchCallCount)
            assertEquals(ExploreCategory.GAME, viewModel.uiState.value.selectedCategory)
            assertEquals(listOf(4), repository.lastAdvancedRequest?.filter?.type)

            viewModel.onCategorySelect(ExploreCategory.ALL)
            advanceUntilIdle()

            assertEquals(3, repository.advancedSearchCallCount)
            assertEquals(ExploreCategory.ALL, viewModel.uiState.value.selectedCategory)
            assertNull(repository.lastAdvancedRequest?.filter?.type)
        }

    @Test
    fun onTagSelectTogglesTagAndSearches() =
        runTest {
            val repository = FakeSearchRepository()
            repository.advancedSearchResult = AppResult.Success(listOf(sampleSubject))
            val viewModel = ExploreViewModel(repository)
            advanceUntilIdle()

            viewModel.onTagSelect("科幻")
            advanceUntilIdle()

            assertEquals(2, repository.advancedSearchCallCount)
            assertEquals("科幻", viewModel.uiState.value.selectedTag)
            assertEquals(listOf("科幻"), repository.lastAdvancedRequest?.filter?.tag)

            // 再次点击相同标签取消选中
            viewModel.onTagSelect("科幻")
            advanceUntilIdle()

            assertEquals(3, repository.advancedSearchCallCount)
            assertNull(viewModel.uiState.value.selectedTag)
            assertNull(repository.lastAdvancedRequest?.filter?.tag)
        }

    @Test
    fun onSortSelectTriggersNewSearchWithUpdatedSortKey() =
        runTest {
            val repository = FakeSearchRepository()
            repository.advancedSearchResult = AppResult.Success(listOf(sampleSubject))
            val viewModel = ExploreViewModel(repository)
            advanceUntilIdle()

            viewModel.onSortSelect(ExploreSort.SCORE)
            advanceUntilIdle()

            assertEquals(2, repository.advancedSearchCallCount)
            assertEquals(ExploreSort.SCORE, viewModel.uiState.value.selectedSort)
            assertEquals("score", repository.lastAdvancedRequest?.sort)
        }

    @Test
    fun searchFailureSetsErrorAndRetryRecovers() =
        runTest {
            val repository = FakeSearchRepository()
            repository.advancedSearchResult = AppResult.Error(RuntimeException("网络故障"), "探索加载失败")
            val viewModel = ExploreViewModel(repository)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertTrue(state.subjects.isEmpty())
            assertEquals("探索加载失败", state.error)

            // 重试恢复
            repository.advancedSearchResult = AppResult.Success(listOf(sampleSubject))
            viewModel.retry()
            advanceUntilIdle()

            val updatedState = viewModel.uiState.value
            assertNull(updatedState.error)
            assertEquals(1, updatedState.subjects.size)
        }

    @Test
    fun refreshSetsRefreshingAndUpdatesSubjects() =
        runTest {
            val repository = FakeSearchRepository()
            repository.advancedSearchResult = AppResult.Success(listOf(sampleSubject))
            val viewModel = ExploreViewModel(repository)
            advanceUntilIdle()

            viewModel.refresh()
            advanceUntilIdle()

            assertEquals(2, repository.advancedSearchCallCount)
            assertFalse(viewModel.uiState.value.isRefreshing)
            assertEquals(1, viewModel.uiState.value.subjects.size)
        }
}
