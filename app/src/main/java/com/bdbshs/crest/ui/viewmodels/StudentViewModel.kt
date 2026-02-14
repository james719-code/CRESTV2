package com.bdbshs.crest.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bdbshs.crest.data.repository.AuthRepository
import com.bdbshs.crest.data.repository.StudentGroup
import com.bdbshs.crest.data.repository.StudentProfile
import com.bdbshs.crest.data.repository.StudentRepository
import com.bdbshs.crest.data.repository.StudentResearchSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// --
data class StudentDetails(
    val name: String = "",
    val strand: String = "",
    val groupId: String = ""
)

data class GroupDetails(
    val group_name: String = "",
    val strand: String = "",
    val file_link: String = "",
    val group_member: List<String> = emptyList(),
    val uploaded: Boolean = false,
    val research_type: String = "",
    val accepted_research: Boolean = false,
    val comments: List<String> = emptyList()
)

// --- UI State for the Screen ---
data class StudentHomeUiState(
    val isLoading: Boolean = true,
    val studentDetails: StudentDetails? = null,
    val groupDetails: GroupDetails? = null,
    val memberNames: List<String> = emptyList(), // <-- ADDED: For certificate
    val totalResearchCount: Int = 0,
    val strandResearchCount: Int = 0,
    val recentResearches: List<ResearchItem> = emptyList(),
    val error: String? = null,
    val isDownloadingCertificate: Boolean = false
)

@HiltViewModel
class StudentHomeViewModel @Inject constructor() : ViewModel() {

    private val TAG = "StudentHomeViewModel" // For logging

    private val _uiState = MutableStateFlow(StudentHomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        fetchAllData()
    }

    private fun fetchAllData() {
        val uid = AuthRepository.getCurrentUserUid() ?: return
        _uiState.update { it.copy(isLoading = true, isDownloadingCertificate = false) }

        viewModelScope.launch {
            try {
                val homeData = StudentRepository.fetchHomeData(uid)
                val student = homeData.student?.toUiStudentDetails()
                val group = homeData.group?.toUiGroupDetails()
                val allResearches = homeData.allResearches.map { it.toResearchItem() }

                val strandCount = allResearches.count { it.strand == student?.strand }
                val recent = allResearches.sortedByDescending { it.createdAt }.take(3)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        studentDetails = student,
                        groupDetails = group,
                        memberNames = homeData.memberNames,
                        totalResearchCount = allResearches.size,
                        strandResearchCount = strandCount,
                        recentResearches = recent,
                        error = null // Clear error on success
                    )
                }
            } catch (e: Exception) {
                // --- IMPROVED ERROR HANDLING ---
                // Log the actual exception so you can see what's wrong in Logcat
                Log.e(TAG, "Failed to fetch student home data. Exception: ", e)
                _uiState.update { it.copy(isLoading = false, error = "Could not load data. Please check your network connection or try again later.") }
            }
        }
    }

    fun triggerCertificateDownload() {
        if (_uiState.value.isDownloadingCertificate) return
        val group = _uiState.value.groupDetails
        val members = _uiState.value.memberNames
        if (group == null || members.isEmpty()) {
            _uiState.update { it.copy(error = "Cannot generate certificate. Group data is incomplete.") }
            return
        }
        _uiState.update { it.copy(isDownloadingCertificate = true, error = null) }
    }

    // --- FIX: ADDED THIS NEW FUNCTION ---
    /**
     * Resets the certificate downloading state without performing any other action.
     * This should be called from the UI if the bitmap generation or saving fails.
     */
    fun onCertificateDownloadFailed() {
        _uiState.update { it.copy(isDownloadingCertificate = false) }
    }

    fun leaveGroup(isTriggeredByCertificate: Boolean = false) {
        val uid = AuthRepository.getCurrentUserUid() ?: return
        val currentState = _uiState.value
        val student = currentState.studentDetails ?: return
        val group = currentState.groupDetails ?: return
        val groupId = student.groupId

        if (groupId.isBlank()) return
        if (!isTriggeredByCertificate) {
            _uiState.update { it.copy(isLoading = true) }
        }

        viewModelScope.launch {
            try {
                StudentRepository.leaveGroup(uid, groupId, group.group_member.size)
                fetchAllData()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, isDownloadingCertificate = false, error = "Failed to leave group: ${e.message}") }
            }
        }
    }

    fun unsubmitResearch() {
        val groupId = _uiState.value.studentDetails?.groupId
        val fileId = _uiState.value.groupDetails?.file_link
        if (groupId.isNullOrBlank()) {
            _uiState.update { it.copy(error = "Could not find your group.") }
            return
        }
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                StudentRepository.unsubmitResearch(groupId, fileId)
                fetchAllData()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Failed to unsubmit: ${e.message}") }
            }
        }
    }

    fun createGroup(groupName: String) {
        val uid = AuthRepository.getCurrentUserUid() ?: return
        val student = _uiState.value.studentDetails ?: return
        if (student.groupId.isNotBlank()) {
            _uiState.update { it.copy(error = "You are already in a group.") }
            return
        }
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                StudentRepository.createGroup(uid, student.strand, groupName)
                fetchAllData()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Failed to create group: ${e.message}") }
            }
        }
    }

    fun joinGroup(groupId: String) {
        val uid = AuthRepository.getCurrentUserUid() ?: return
        val student = _uiState.value.studentDetails ?: return
        if (student.groupId.isNotBlank()) {
            _uiState.update { it.copy(error = "You are already in a group.") }
            return
        }
        if (groupId.isBlank()){
            _uiState.update { it.copy(error = "Group ID cannot be empty.") }
            return
        }
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                StudentRepository.joinGroup(uid, student.strand, groupId)
                fetchAllData()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Failed to join group: ${e.message}") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

private fun StudentProfile.toUiStudentDetails(): StudentDetails {
    return StudentDetails(
        name = name,
        strand = strand,
        groupId = groupId
    )
}

private fun StudentGroup.toUiGroupDetails(): GroupDetails {
    return GroupDetails(
        group_name = groupName,
        strand = strand,
        file_link = fileLink,
        group_member = groupMembers,
        uploaded = uploaded,
        research_type = researchType,
        accepted_research = acceptedResearch,
        comments = comments
    )
}

private fun StudentResearchSummary.toResearchItem(): ResearchItem {
    return ResearchItem(
        id = id,
        title = title,
        strand = strand,
        createdAt = createdAt
    )
}