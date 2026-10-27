package com.bdbshs.crest.ui.viewmodels

import com.bdbshs.crest.data.*
import com.bdbshs.crest.data.repository.PlanningRepository
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlanningViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<PlanningRepository>()
    @Before fun setup() { Dispatchers.setMain(dispatcher); coEvery { repository.capture(any(), any(), any()) } just Runs }
    @After fun cleanup() { Dispatchers.resetMain() }

    @Test fun switchingAccountsCancelsStaleSavesAndClearsPrivateInbox() = runTest(dispatcher) {
        val first = MutableStateFlow(PlanningRecords(loading = false, notifications = listOf(SchoolUpdate("old", "approval", "g", "Private update", "Details"))))
        val second = MutableStateFlow(PlanningRecords(loading = false))
        every { repository.observe("first", true) } returns first
        every { repository.observe("second", false) } returns second
        val gate = CompletableDeferred<Unit>()
        coEvery { repository.save("first", any(), any()) } coAnswers { gate.await() }
        val model = PlanningViewModel(repository)
        model.bind("first", UserType.TEACHER, true); runCurrent()
        assertEquals(1, model.uiState.value.records.notifications.size)
        var completed = false
        model.save(MilestoneInput("g", "Proposal", MilestoneStage.PROPOSAL, "2026-11-01"), null) { completed = true }
        runCurrent()
        model.bind("second", UserType.STUDENT, true); runCurrent()
        gate.complete(Unit); runCurrent()
        assertFalse(completed)
        assertFalse(model.uiState.value.busy)
        assertTrue(model.uiState.value.records.notifications.isEmpty())
        model.bind(null, null, false)
    }
    @Test fun studentsCannotInvokeMilestoneMutationsAndRevocationClearsData() = runTest(dispatcher) {
        every { repository.observe("student", false) } returns flowOf(PlanningRecords(loading = false))
        val model = PlanningViewModel(repository)
        model.bind("student", UserType.STUDENT, true); runCurrent()
        model.save(MilestoneInput("g", "Proposal", MilestoneStage.PROPOSAL, "2026-11-01"), null) {}
        coVerify(exactly = 0) { repository.save(any(), any(), any()) }
        model.bind("student", UserType.STUDENT, false)
        assertTrue(model.uiState.value.records.notifications.isEmpty())
        assertFalse(model.uiState.value.records.loading)
    }
    @Test fun connectionErrorsClearSchoolDataAndOfferRetry() = runTest(dispatcher) {
        every { repository.observe("teacher", true) } returns flow { throw IllegalStateException("Denied") }
        val model = PlanningViewModel(repository)
        model.bind("teacher", UserType.TEACHER, true); runCurrent()
        assertNotNull(model.uiState.value.error)
        assertFalse(model.uiState.value.records.loading)
        model.bind(null, null, false)
    }
}
