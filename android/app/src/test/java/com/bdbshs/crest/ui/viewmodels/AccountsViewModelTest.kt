package com.bdbshs.crest.ui.viewmodels

import com.bdbshs.crest.data.repository.AccountRecord
import com.bdbshs.crest.data.repository.AccountRepository
import com.bdbshs.crest.data.repository.UserRole
import com.bdbshs.crest.data.repository.WorkspaceDeletionRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val accounts = mockk<AccountRepository>()
    private val deletion = mockk<WorkspaceDeletionRepository>()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { Dispatchers.resetMain() }

    private fun model(declined: Boolean = false, accepted: Boolean = false): AccountsViewModel {
        coEvery { accounts.fetchAllAccounts() } returns listOf(
            AccountRecord.Student(uid = "student", name = "Student", declined = declined, accepted = accepted)
        )
        return AccountsViewModel(accounts, deletion)
    }

    @Test fun decliningKeepsTheProfileForLaterDeletion() = runTest(dispatcher) {
        coEvery { accounts.denyAccount("student", UserRole.STUDENT) } just Runs
        val model = model()
        advanceUntilIdle()
        model.onAccountClicked(model.uiState.value.allAccounts.single())
        model.denySelectedAccount()
        advanceUntilIdle()
        val student = model.uiState.value.allAccounts.single() as AccountItem.Student
        assertTrue(student.declined)
        assertFalse(student.accepted)
        assertFalse(model.uiState.value.isActionDialogVisible)
        coVerify(exactly = 0) { deletion.deleteDeclinedStudent(any()) }
    }

    @Test fun failedDeletionRetainsTheProfileAndCanBeRetried() = runTest(dispatcher) {
        val model = model(declined = true)
        advanceUntilIdle()
        model.onAccountClicked(model.uiState.value.allAccounts.single())
        coEvery { deletion.deleteDeclinedStudent("student") } throws IllegalStateException("Connection lost")
        model.deleteSelectedAccount()
        advanceUntilIdle()
        assertEquals(1, model.uiState.value.allAccounts.size)
        assertTrue(model.uiState.value.isActionDialogVisible)
        assertFalse(model.uiState.value.isUpdatingAccount)
        assertNotNull(model.uiState.value.error)
        coEvery { deletion.deleteDeclinedStudent("student") } just Runs
        model.deleteSelectedAccount()
        advanceUntilIdle()
        assertTrue(model.uiState.value.allAccounts.isEmpty())
        assertFalse(model.uiState.value.isActionDialogVisible)
    }

    @Test fun approvedAndPendingProfilesCannotBeDeleted() = runTest(dispatcher) {
        for (accepted in listOf(false, true)) {
            val model = model(accepted = accepted)
            advanceUntilIdle()
            model.onAccountClicked(model.uiState.value.allAccounts.single())
            model.deleteSelectedAccount()
            advanceUntilIdle()
            assertEquals(1, model.uiState.value.allAccounts.size)
        }
        coVerify(exactly = 0) { deletion.deleteDeclinedStudent(any()) }
    }
}
