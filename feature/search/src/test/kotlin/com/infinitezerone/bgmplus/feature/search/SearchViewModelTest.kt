package com.infinitezerone.bgmplus.feature.search

import com.infinitezerone.bgmplus.core.common.AppResult
import com.infinitezerone.bgmplus.core.testing.data.sampleSubject
import com.infinitezerone.bgmplus.core.testing.repository.FakeSearchRepository
import com.infinitezerone.bgmplus.core.testing.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun initialStateIsEmptyAndNotLoading() {
        val repository = FakeSearchRepository()
        val viewModel = SearchViewModel(repository)

        val state = viewModel.uiState.value
        assertEquals("", state.query)
        assertEquals(0, state.selectedType)
        assertFalse(state.isLoading)
        assertTrue(state.results.isEmpty())
        assertNull(state.error)
        assertEquals(0, repository.searchCallCount)
    }

    @Test
    fun onQueryChangeTriggersDebouncedSearch() =
        runTest {
            val repository = FakeSearchRepository()
            repository.searchResult = AppResult.Success(listOf(sampleSubject))
            val viewModel = SearchViewModel(repository)

            viewModel.onQueryChange("芙莉莲")

            assertEquals("芙莉莲", viewModel.uiState.value.query)
            // 200ms 未满防抖阈值 (300ms) 时，不应触发搜索
            advanceTimeBy(200)
            assertEquals(0, repository.searchCallCount)

            // 推进到达防抖时间后触发搜索
            advanceTimeBy(150)
            advanceUntilIdle()

            assertEquals(1, repository.searchCallCount)
            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertEquals(1, state.results.size)
            assertEquals("葬送的芙莉莲", state.results.first().nameCn)
            assertNull(state.error)
        }

    @Test
    fun onQueryChangeWithBlankResetsResultsAndDoesNotSearch() =
        runTest {
            val repository = FakeSearchRepository()
            repository.searchResult = AppResult.Success(listOf(sampleSubject))
            val viewModel = SearchViewModel(repository)

            viewModel.onQueryChange("芙莉莲")
            advanceUntilIdle()
            assertEquals(1, repository.searchCallCount)
            assertEquals(1, viewModel.uiState.value.results.size)

            viewModel.onQueryChange("   ")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("   ", state.query)
            assertTrue(state.results.isEmpty())
            assertFalse(state.isLoading)
            assertNull(state.error)
            // 空白输入不应再次发起网络搜索
            assertEquals(1, repository.searchCallCount)
        }

    @Test
    fun onTypeSelectSwitchesTypeAndSearchesWhenQueryPresent() =
        runTest {
            val repository = FakeSearchRepository()
            repository.searchResult = AppResult.Success(listOf(sampleSubject))
            val viewModel = SearchViewModel(repository)

            viewModel.onQueryChange("芙莉莲")
            advanceUntilIdle()
            assertEquals(1, repository.searchCallCount)

            // 切换为动画分类 (2)
            viewModel.onTypeSelect(2)
            advanceUntilIdle()

            assertEquals(2, viewModel.uiState.value.selectedType)
            assertEquals(2, repository.searchCallCount)
        }

    @Test
    fun onTypeSelectDoesNotSearchWhenQueryBlank() =
        runTest {
            val repository = FakeSearchRepository()
            val viewModel = SearchViewModel(repository)

            viewModel.onTypeSelect(2)
            advanceUntilIdle()

            assertEquals(2, viewModel.uiState.value.selectedType)
            assertEquals(0, repository.searchCallCount)
        }

    @Test
    fun searchTriggersImmediateSearchWithoutDebounce() =
        runTest {
            val repository = FakeSearchRepository()
            repository.searchResult = AppResult.Success(listOf(sampleSubject))
            val viewModel = SearchViewModel(repository)

            viewModel.onQueryChange("EVA")
            // 立即按回车/触发 search()
            viewModel.search()
            advanceUntilIdle()

            assertEquals(1, repository.searchCallCount)
            assertEquals(1, viewModel.uiState.value.results.size)
        }

    @Test
    fun clearQueryClearsAllState() =
        runTest {
            val repository = FakeSearchRepository()
            repository.searchResult = AppResult.Success(listOf(sampleSubject))
            val viewModel = SearchViewModel(repository)

            viewModel.onQueryChange("芙莉莲")
            advanceUntilIdle()
            assertEquals(1, viewModel.uiState.value.results.size)

            viewModel.clearQuery()

            val state = viewModel.uiState.value
            assertEquals("", state.query)
            assertTrue(state.results.isEmpty())
            assertFalse(state.isLoading)
            assertNull(state.error)
        }

    @Test
    fun searchFailureSetsErrorState() =
        runTest {
            val repository = FakeSearchRepository()
            repository.searchResult = AppResult.Error(RuntimeException("网络连接失败"), "搜索请求失败")
            val viewModel = SearchViewModel(repository)

            viewModel.onQueryChange("测试")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertTrue(state.results.isEmpty())
            assertEquals("搜索请求失败", state.error)
        }

    @Test
    fun searchSuccessClearsPreviousError() =
        runTest {
            val repository = FakeSearchRepository()
            repository.searchResult = AppResult.Error(RuntimeException("网络连接失败"), "搜索请求失败")
            val viewModel = SearchViewModel(repository)

            viewModel.onQueryChange("测试")
            advanceUntilIdle()
            assertEquals("搜索请求失败", viewModel.uiState.value.error)

            repository.searchResult = AppResult.Success(listOf(sampleSubject))
            viewModel.search()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertNull(state.error)
            assertEquals(1, state.results.size)
        }

    @Test
    fun rapidQueryChangesOnlyExecutesLatestQuery() =
        runTest {
            val repository = FakeSearchRepository()
            repository.searchResult = AppResult.Success(listOf(sampleSubject))
            val viewModel = SearchViewModel(repository)

            viewModel.onQueryChange("a")
            advanceTimeBy(100)
            viewModel.onQueryChange("ab")
            advanceTimeBy(100)
            viewModel.onQueryChange("abc")
            advanceTimeBy(100)
            assertEquals(0, repository.searchCallCount)

            advanceTimeBy(300)
            advanceUntilIdle()

            assertEquals(1, repository.searchCallCount)
            assertEquals("abc", viewModel.uiState.value.query)
        }
}
