package com.bdbshs.crest.ui.viewmodels

import com.bdbshs.crest.data.repository.WorkspaceDeletionRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GroupDeletionViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<WorkspaceDeletionRepository>()
    private val listener = slot<FirebaseAuth.AuthStateListener>()
    private val user = mockk<FirebaseUser> { every { uid } returns "teacher" }
    private val auth = mockk<FirebaseAuth> {
        every { currentUser } returns user
        every { addAuthStateListener(capture(listener)) } just Runs
        every { removeAuthStateListener(any()) } just Runs
    }
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { Dispatchers.resetMain() }

    @Test fun pendingDeletionSurvivesListSnapshotsAndOnlyMarksItsGroup() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        coEvery { repository.deleteGroup("first") } coAnswers { gate.await() }
        val model = GroupDeletionViewModel(repository, auth)
        val first = GroupItem("first", "Science Group")
        val second = GroupItem("second", "Other Group")
        model.deleteGroup(first)
        model.deleteGroup(first)
        runCurrent()
        assertTrue(model.uiState.value.operations.getValue("first").isDeleting)
        assertNull(model.uiState.value.operations["second"])
        assertEquals(listOf("first", "second"), model.uiState.value.visibleGroups(listOf(second)).map { it.id })
        gate.complete(Unit)
        advanceUntilIdle()
        assertTrue(model.uiState.value.operations.isEmpty())
        assertEquals(listOf(second), model.uiState.value.visibleGroups(listOf(first, second)))
        coVerify(exactly = 1) { repository.deleteGroup("first") }
    }

    @Test fun failureLeavesTheGroupOnTheCardAndRetryCanFinish() = runTest(dispatcher) {
        coEvery { repository.deleteGroup("group") } throws IllegalStateException("Connection lost")
        val model = GroupDeletionViewModel(repository, auth)
        val group = GroupItem("group", "Science Group")
        model.deleteGroup(group)
        advanceUntilIdle()
        assertFalse(model.uiState.value.operations.getValue("group").isDeleting)
        assertTrue(model.uiState.value.operations.getValue("group").error!!.contains("Connection lost"))
        assertEquals(listOf(group), model.uiState.value.visibleGroups(emptyList()))
        coEvery { repository.deleteGroup("group") } just Runs
        model.retry("group")
        advanceUntilIdle()
        assertTrue(model.uiState.value.visibleGroups(listOf(group)).isEmpty())
    }

    @Test fun accountChangesClearPendingOperationsAndIgnoreTheirResults() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        coEvery { repository.deleteGroup("group") } coAnswers { gate.await() }
        val model = GroupDeletionViewModel(repository, auth)
        model.deleteGroup(GroupItem("group", "Science Group"))
        runCurrent()
        every { auth.currentUser } returns null
        listener.captured.onAuthStateChanged(auth)
        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(GroupDeletionUiState(), model.uiState.value)
    }
}
