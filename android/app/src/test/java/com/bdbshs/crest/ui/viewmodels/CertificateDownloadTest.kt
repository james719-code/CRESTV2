package com.bdbshs.crest.ui.viewmodels

import com.bdbshs.crest.data.repository.AuthRepository
import com.bdbshs.crest.data.repository.StudentGroup
import com.bdbshs.crest.data.repository.StudentHomeData
import com.bdbshs.crest.data.repository.StudentProfile
import com.bdbshs.crest.data.repository.StudentRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CertificateDownloadTest {
    private val dispatcher = StandardTestDispatcher()
    private val auth = mockk<AuthRepository> { every { getCurrentUserUid() } returns "student" }
    private val repository = mockk<StudentRepository>()
    private val group = StudentGroup("Research group", "STEM", "file", listOf("student"), false,
        "qualitative", true, emptyList(), "An approved research manuscript")

    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { Dispatchers.resetMain() }

    private fun viewModel(group: StudentGroup = this.group, members: List<String> = listOf("Andrea Santos")): StudentHomeViewModel {
        coEvery { repository.fetchHomeData("student") } returns StudentHomeData(
            StudentProfile("Andrea Santos", "STEM", "group"), group, members, 0, 0, emptyList()
        )
        return StudentHomeViewModel(auth, repository)
    }

    @Test fun certificateRequiresTeacherApproval() = runTest(dispatcher) {
        val vm = viewModel(group.copy(acceptedResearch = false))
        advanceUntilIdle()
        assertFalse(vm.triggerCertificateDownload())
        assertFalse(vm.uiState.value.isDownloadingCertificate)
        assertTrue(vm.uiState.value.error.orEmpty().contains("approved"))
    }

    @Test fun downloadCannotStartWithoutResearchTitleOrNamedResearchers() = runTest(dispatcher) {
        val noTitle = viewModel(group.copy(researchTitle = " "))
        advanceUntilIdle()
        assertFalse(noTitle.triggerCertificateDownload())
        val noNames = viewModel(members = listOf(" ", ""))
        advanceUntilIdle()
        assertFalse(noNames.triggerCertificateDownload())
    }

    @Test fun duplicateRequestsAreBlockedAndFailedExportCanBeRetriedWithoutLeavingGroup() = runTest(dispatcher) {
        val vm = viewModel()
        assertFalse(vm.triggerCertificateDownload())
        advanceUntilIdle()
        assertTrue(vm.triggerCertificateDownload())
        assertFalse(vm.triggerCertificateDownload())
        vm.onCertificateDownloadFailed()
        assertTrue(vm.triggerCertificateDownload())
        coVerify(exactly = 0) { repository.leaveGroup(any(), any(), any()) }
    }
}
