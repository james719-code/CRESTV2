package com.bdbshs.crest.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bdbshs.crest.data.repository.GroupRepository
import com.bdbshs.crest.data.repository.PdfRepository
import com.bdbshs.crest.navigation.AppDestination
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GroupDetailUiState(
    val isLoading: Boolean = true,
    val group: GroupItem? = null,
    val memberNames: List<String> = emptyList(),
    val error: String? = null,
    val isUpdating: Boolean = false,
    val denialComment: String = "",
    val pdfFilePath: String? = null,
    val isPdfLoading: Boolean = false,
    // --- YOUR REQUESTED VARIABLE ---
    val isShowingPdf: Boolean = false
)

class GroupDetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private var groupListener: ListenerRegistration? = null

    private val _uiState = MutableStateFlow(GroupDetailUiState())
    val uiState = _uiState.asStateFlow()

    private val groupId: String? = savedStateHandle.get<String>(AppDestination.GroupDetail.groupIdArg)

    init {
        if (!groupId.isNullOrBlank()) {
            setupGroupListener()
        } else {
            _uiState.update { it.copy(isLoading = false, error = "Group ID not provided.") }
        }
    }

    // --- PDF Loading now updates both pdfBytes and the isShowingPdf flag ---
    fun loadPdf(isOnline: Boolean) {
        val fileId = _uiState.value.group?.file_link
        if (fileId.isNullOrBlank()) { /* ... error handling ... */ return }
        if (_uiState.value.isShowingPdf && _uiState.value.pdfFilePath != null) return // Don't re-load if already showing

        _uiState.update { it.copy(isPdfLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val resolvedFile = PdfRepository.getOrDownloadPdfFile(getApplication(), fileId, isOnline)

                // On success, update both the data and the flag
                _uiState.update { it.copy(isPdfLoading = false, pdfFilePath = resolvedFile.absolutePath, isShowingPdf = true) }

            } catch (e: Exception) {
                _uiState.update { it.copy(isPdfLoading = false, error = "Could not load PDF: ${e.message}") }
            }
        }
    }

    // --- YOUR REQUESTED FUNCTION ---
    // This function now controls returning to the detail view.
    fun hidePdfViewer() {
        _uiState.update {
            // It's crucial to clear both the flag and the data to prevent memory leaks.
            it.copy(isShowingPdf = false, pdfFilePath = null)
        }
    }

    private fun setupGroupListener() {
        if (groupId == null) return
        _uiState.update { it.copy(isLoading = true) }
        groupListener?.remove()

        groupListener = GroupRepository.observeGroupById(groupId) { snapshot, e ->
                if (e != null) {
                    val message = if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE) {
                        "You are offline. Showing cached details."
                    } else { "Error: ${e.message}" }
                    _uiState.update { it.copy(isLoading = false, error = message) }
                    return@observeGroupById
                }
                if (snapshot != null && snapshot.exists()) {
                    val groupItem = snapshot.toGroupItem()
                    _uiState.update { it.copy(isLoading = false, group = groupItem) }
                    groupItem?.group_member?.let { fetchMemberNames(it) }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Group not found.") }
                }
            }
    }

    private fun fetchMemberNames(uids: List<String>) {
        if (uids.isEmpty()) {
            _uiState.update { it.copy(memberNames = emptyList()) }
            return
        }
        viewModelScope.launch {
            try {
                val names = GroupRepository.fetchStudentNamesByIds(uids)
                _uiState.update { it.copy(memberNames = names) }
            } catch (e: Exception) {
                Log.e("GroupDetailVM", "Failed to fetch member names", e)
            }
        }
    }

    fun onDenialCommentChange(comment: String) {
        _uiState.update { it.copy(denialComment = comment) }
    }

    // --- REWRITTEN: The new, simplified approval logic ---
    fun approveSubmission() {
        val currentState = _uiState.value
        val group = currentState.group
        val memberNames = currentState.memberNames

        if (groupId.isNullOrBlank() || group == null) {
            _uiState.update { it.copy(error = "Group data is missing.") }
            return
        }
        // Ensure we have the names before proceeding.
        if (memberNames.isEmpty() && group.group_member.isNotEmpty()) {
            _uiState.update { it.copy(error = "Could not verify member names. Please check connection and try again.") }
            return
        }

        _uiState.update { it.copy(isUpdating = true) }

        viewModelScope.launch {
            try {
                val researchType = ResearchType.valueOf(group.research_type.uppercase())
                GroupRepository.approveSubmissionWithResearch(
                    groupId = groupId,
                    researchTypeLowercase = researchType.name.lowercase(),
                    researchTitle = group.research_title,
                    memberNames = memberNames,
                    strand = group.strand,
                    fileLink = group.file_link
                )

                _uiState.update { it.copy(isUpdating = false) }

            } catch (e: Exception) {
                _uiState.update { it.copy(isUpdating = false, error = "Approval failed. An internet connection is required.") }
            }
        }
    }

    fun denySubmission() {
        if (groupId == null) return
        val comment = _uiState.value.denialComment
        val fileLink = _uiState.value.group?.file_link
        if (comment.isBlank()) return
        _uiState.update { it.copy(isUpdating = true) }
        viewModelScope.launch {
            try {
                GroupRepository.denySubmission(groupId, fileLink, comment)
                _uiState.update { it.copy(isUpdating = false, denialComment = "") }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isUpdating = false,
                        error = "Denial failed. An internet connection may be required."
                    )
                }
            }
        }
    }

    fun clearPdfBytes() {
        _uiState.update { it.copy(pdfFilePath = null) }
    }

    override fun onCleared() {
        groupListener?.remove()
        super.onCleared()
    }
}

private fun com.google.firebase.firestore.DocumentSnapshot.toGroupItem(): GroupItem? {
    return GroupItem(
        id = id,
        group_name = getString("group_name").orEmpty(),
        strand = getString("strand").orEmpty(),
        group_member = (get("group_member") as? List<*>)?.mapNotNull { it as? String }.orEmpty(),
        uploaded = getBoolean("uploaded") ?: false,
        accepted_research = getBoolean("accepted_research") ?: false,
        research_title = getString("research_title").orEmpty(),
        research_type = getString("research_type").orEmpty(),
        file_link = getString("file_link").orEmpty()
    )
}