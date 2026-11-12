package com.bdbshs.crest.ui.viewmodels

import com.bdbshs.crest.data.repository.FavoritesRepository
import com.bdbshs.crest.data.repository.ResearchRepository
import com.bdbshs.crest.domain.usecase.GetFilteredResearchesUseCase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ResearchLoadingTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<ResearchRepository>()
    private val auth = mockk<FirebaseAuth> { every { currentUser } returns null }

    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }

    private fun snapshot(ids: List<String>): QuerySnapshot {
        val documents = ids.map { id ->
            val document = mockk<DocumentSnapshot>(relaxed = true)
            every { document.id } returns id
            every { document.getString("title") } returns id
            document
        }
        return mockk { every { this@mockk.documents } returns documents }
    }

    private fun viewModel() = ResearchesViewModel(
        repository, mockk<FavoritesRepository>(), auth, GetFilteredResearchesUseCase()
    )

    private fun filterViewModel(): ResearchesViewModel {
        coEvery { repository.fetchQualitativeResearches(15L, null) } returns snapshot(emptyList())
        coEvery { repository.fetchQuantitativeResearches(15L, null) } returns snapshot(emptyList())
        return viewModel()
    }

    @Test fun `canceling filter choices keeps the current library unchanged`() = runTest(dispatcher) {
        val vm = filterViewModel()
        vm.onStrandCheckedChange("STEM", true)
        vm.showFilterDialog()
        vm.onStrandCheckedChange("STEM", false)
        vm.onStrandCheckedChange("HUMSS", true)
        vm.onResearchTypeSelected(ResearchType.QUANTITATIVE)
        vm.onSortOptionSelected(SortOption.TitleZA)
        assertTrue(vm.uiState.value.strands.first { it.name == "STEM" }.isSelected)
        assertNull(vm.uiState.value.selectedResearchType)
        assertEquals(SortOption.DateNewest, vm.uiState.value.selectedSortOption)
        vm.dismissFilterDialog()
        vm.showFilterDialog()
        assertEquals(ResearchFilterOptions(strands = setOf("STEM")), vm.uiState.value.filterDraft)
        advanceUntilIdle()
    }

    @Test fun `applying filter choices commits them together and preserves the search and favorites`() = runTest(dispatcher) {
        val vm = filterViewModel()
        vm.onSearchQueryChanged("digital")
        vm.setFavoritesOnly(true)
        vm.showFilterDialog()
        vm.onStrandCheckedChange("HUMSS", true)
        vm.onResearchTypeSelected(ResearchType.QUANTITATIVE)
        vm.onSortOptionSelected(SortOption.TitleAZ)
        vm.applyFilters()
        val state = vm.uiState.value
        assertEquals(ResearchType.QUANTITATIVE, state.selectedResearchType)
        assertEquals(setOf("HUMSS"), state.strands.filter { it.isSelected }.map { it.name }.toSet())
        assertEquals(SortOption.TitleAZ, state.selectedSortOption)
        assertEquals("digital", state.searchQuery)
        assertTrue(state.showFavoritesOnly)
        assertFalse(state.isFilterDialogVisible)
        assertNull(state.filterDraft)
        advanceUntilIdle()
    }

    @Test fun `resetting the filter draft preserves query and favorites and can be canceled`() = runTest(dispatcher) {
        val vm = filterViewModel()
        vm.onSearchQueryChanged("digital")
        vm.setFavoritesOnly(true)
        vm.onResearchTypeSelected(ResearchType.QUALITATIVE)
        vm.showFilterDialog()
        vm.resetFilterDraft()
        assertEquals(ResearchFilterOptions(), vm.uiState.value.filterDraft)
        assertEquals("digital", vm.uiState.value.searchQuery)
        assertTrue(vm.uiState.value.showFavoritesOnly)
        vm.dismissFilterDialog()
        assertEquals(ResearchType.QUALITATIVE, vm.uiState.value.selectedResearchType)
        vm.showFilterDialog()
        vm.resetFilterDraft()
        vm.applyFilters()
        assertNull(vm.uiState.value.selectedResearchType)
        assertEquals("digital", vm.uiState.value.searchQuery)
        assertTrue(vm.uiState.value.showFavoritesOnly)
        advanceUntilIdle()
    }

    @Test fun `initial requests run concurrently`() = runTest(dispatcher) {
        val qualitative = CompletableDeferred<QuerySnapshot>()
        val quantitative = CompletableDeferred<QuerySnapshot>()
        coEvery { repository.fetchQualitativeResearches(15L, null) } coAnswers { qualitative.await() }
        coEvery { repository.fetchQuantitativeResearches(15L, null) } coAnswers { quantitative.await() }
        val vm = viewModel()
        runCurrent()
        coVerify(exactly = 1) { repository.fetchQualitativeResearches(15L, null) }
        coVerify(exactly = 1) { repository.fetchQuantitativeResearches(15L, null) }
        assertTrue(vm.uiState.value.isLoading)
        qualitative.complete(snapshot(listOf("qual")))
        quantitative.complete(snapshot(listOf("quant")))
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isLoading)
        assertEquals("qual", vm.uiState.value.qualitativeResearches.single().id)
        assertEquals("quant", vm.uiState.value.quantitativeResearches.single().id)
    }

    @Test fun `refresh cancels old pagination and keeps refreshed papers`() = runTest(dispatcher) {
        coEvery { repository.fetchQualitativeResearches(15L, null) } returns snapshot((1..15).map { "old-$it" })
        coEvery { repository.fetchQuantitativeResearches(15L, null) } returns snapshot(emptyList())
        val page = CompletableDeferred<QuerySnapshot>()
        coEvery { repository.fetchQualitativeResearches(10L, any()) } coAnswers { page.await() }
        val vm = viewModel()
        advanceUntilIdle()
        vm.loadMore()
        runCurrent()
        assertTrue(vm.uiState.value.isLoadingMore)
        coEvery { repository.fetchQualitativeResearches(15L, null) } returns snapshot(listOf("fresh"))
        vm.onRefresh()
        advanceUntilIdle()
        page.complete(snapshot(listOf("stale")))
        advanceUntilIdle()
        assertEquals(listOf("fresh"), vm.uiState.value.qualitativeResearches.map { it.id })
        assertFalse(vm.uiState.value.isLoadingMore)
        assertNull(vm.uiState.value.error)
    }

    @Test fun `pagination skips network requests while offline`() = runTest(dispatcher) {
        coEvery { repository.fetchQualitativeResearches(15L, null) } returns snapshot((1..15).map { "paper-$it" })
        coEvery { repository.fetchQuantitativeResearches(15L, null) } returns snapshot(emptyList())
        coEvery { repository.fetchCachedQualitativeResearches() } returns snapshot((1..15).map { "paper-$it" })
        coEvery { repository.fetchCachedQuantitativeResearches() } returns snapshot(emptyList())
        val vm = viewModel()
        advanceUntilIdle()
        vm.setOnlineStatus(false)
        vm.loadMore()
        advanceUntilIdle()
        coVerify(exactly = 0) { repository.fetchQualitativeResearches(10L, any()) }
        assertFalse(vm.uiState.value.isLoadingMore)
    }

    @Test fun `offline reload reads every cached paper instead of limiting the first page`() = runTest(dispatcher) {
        coEvery { repository.fetchQualitativeResearches(15L, null) } returns snapshot(listOf("online"))
        coEvery { repository.fetchQuantitativeResearches(15L, null) } returns snapshot(emptyList())
        coEvery { repository.fetchCachedQualitativeResearches() } returns snapshot((1..24).map { "cached-$it" })
        coEvery { repository.fetchCachedQuantitativeResearches() } returns snapshot(listOf("cached-quant"))
        val vm = viewModel()
        advanceUntilIdle()
        vm.setOnlineStatus(false)
        advanceUntilIdle()
        assertEquals(24, vm.uiState.value.qualitativeResearches.size)
        assertEquals("cached-quant", vm.uiState.value.quantitativeResearches.single().id)
        assertFalse(vm.uiState.value.isLoading)
        assertFalse(vm.uiState.value.hasMoreQualitative)
        assertFalse(vm.uiState.value.hasMoreQuantitative)
        vm.onRefresh()
        advanceUntilIdle()
        coVerify(exactly = 1) { repository.fetchQualitativeResearches(15L, null) }
        coVerify(exactly = 2) { repository.fetchCachedQualitativeResearches() }
    }

    @Test fun `switching offline cancels an in-flight server load and reconnecting refreshes`() = runTest(dispatcher) {
        val pending = CompletableDeferred<QuerySnapshot>()
        coEvery { repository.fetchQualitativeResearches(15L, null) } coAnswers { pending.await() }
        coEvery { repository.fetchQuantitativeResearches(15L, null) } returns snapshot(emptyList())
        coEvery { repository.fetchCachedQualitativeResearches() } returns snapshot(listOf("saved"))
        coEvery { repository.fetchCachedQuantitativeResearches() } returns snapshot(emptyList())
        val vm = viewModel()
        runCurrent()
        vm.setOnlineStatus(false)
        advanceUntilIdle()
        assertEquals("saved", vm.uiState.value.qualitativeResearches.single().id)
        pending.complete(snapshot(listOf("stale")))
        advanceUntilIdle()
        assertEquals("saved", vm.uiState.value.qualitativeResearches.single().id)
        coEvery { repository.fetchQualitativeResearches(15L, null) } returns snapshot(listOf("refreshed"))
        vm.setOnlineStatus(true)
        advanceUntilIdle()
        assertEquals("refreshed", vm.uiState.value.qualitativeResearches.single().id)
        assertNull(vm.uiState.value.error)
    }
}
