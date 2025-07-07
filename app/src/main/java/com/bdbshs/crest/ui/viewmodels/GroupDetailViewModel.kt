package com.bdbshs.crest.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bdbshs.crest.data.AppwriteClient
import com.bdbshs.crest.data.FileCache
import com.bdbshs.crest.data.FirebaseClient
import com.bdbshs.crest.navigation.AppDestination
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val BUCKET_ID = "686a262b0024b8e10a35"

data class GroupDetailUiState(
    val isLoading: Boolean = true,
    val group: GroupItem? = null,
    val memberNames: List<String> = emptyList(),
    val error: String? = null,
    val isUpdating: Boolean = false,
    val denialComment: String = "",
    val pdfBytes: ByteArray? = null,
    val isPdfLoading: Boolean = false,
    // --- YOUR REQUESTED VARIABLE ---
    val isShowingPdf: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as GroupDetailUiState
        if (isLoading != other.isLoading) return false
        if (group != other.group) return false
        if (memberNames != other.memberNames) return false
        if (error != other.error) return false
        if (isUpdating != other.isUpdating) return false
        if (denialComment != other.denialComment) return false
        if (pdfBytes != null) {
            if (other.pdfBytes == null) return false
            if (!pdfBytes.contentEquals(other.pdfBytes)) return false
        } else if (other.pdfBytes != null) return false
        if (isPdfLoading != other.isPdfLoading) return false
        if (isShowingPdf != other.isShowingPdf) return false // Compare the new flag
        return true
    }

    override fun hashCode(): Int {
        var result = isLoading.hashCode()
        result = 31 * result + (group?.hashCode() ?: 0)
        result = 31 * result + memberNames.hashCode()
        result = 31 * result + (error?.hashCode() ?: 0)
        result = 31 * result + isUpdating.hashCode()
        result = 31 * result + denialComment.hashCode()
        result = 31 * result + (pdfBytes?.contentHashCode() ?: 0)
        result = 31 * result + isPdfLoading.hashCode()
        result = 31 * result + isShowingPdf.hashCode() // Add the new flag
        return result
    }
}

class GroupDetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val firestore = FirebaseClient.firestore
    private val appwriteStorage = AppwriteClient.storage
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
        if (_uiState.value.isShowingPdf) return // Don't re-load if already showing

        _uiState.update { it.copy(isPdfLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val cachedFile = FileCache.getFile(getApplication(), fileId)
                val fileBytes = cachedFile ?: if (isOnline) {
                    appwriteStorage.getFileDownload(bucketId = BUCKET_ID, fileId = fileId).also {
                        FileCache.saveFile(getApplication(), fileId, it)
                    }
                } else {
                    throw Exception("File not cached. An internet connection is required to download it.")
                }

                // On success, update both the data and the flag
                _uiState.update { it.copy(isPdfLoading = false, pdfBytes = fileBytes, isShowingPdf = true) }

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
            it.copy(isShowingPdf = false, pdfBytes = null)
        }
    }

    private fun setupGroupListener() {
        if (groupId == null) return
        _uiState.update { it.copy(isLoading = true) }
        groupListener?.remove()

        groupListener = firestore.collection("groups").document(groupId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    val message = if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE) {
                        "You are offline. Showing cached details."
                    } else { "Error: ${e.message}" }
                    _uiState.update { it.copy(isLoading = false, error = message) }
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val groupItem = snapshot.toObject<GroupItem>()?.copy(id = snapshot.id)
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
                val names = uids.chunked(10).flatMap { chunk ->
                    val studentDocs = firestore.collection("users/user_details/students")
                        .whereIn("__name__", chunk).get().await()
                    studentDocs.documents.mapNotNull { it.getString("name") }
                }
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
                val newResearchData = mapOf(
                    "title" to group.research_title,
                    // --- THIS IS THE FIX ---
                    // Use the fetched memberNames list, not the group_member list of UIDs.
                    "members" to memberNames,
                    "strand" to group.strand,
                    "downloads" to 0,
                    "unfinished" to false,
                    "file_link" to group.file_link,
                    "createdAt" to System.currentTimeMillis()
                )

                val batch = firestore.batch()
                val newResearchRef = firestore.collection("researches/research_details/${researchType.name.lowercase()}").document()
                batch.set(newResearchRef, newResearchData)

                val groupRef = firestore.collection("groups").document(groupId)
                val groupUpdates = mapOf(
                    "accepted_research" to true,
                    "uploaded" to false
                )
                batch.update(groupRef, groupUpdates)

                batch.commit().await()

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
                if (!fileLink.isNullOrBlank()) {
                    appwriteStorage.deleteFile(BUCKET_ID, fileLink)
                }
                val updates = mapOf(
                    "accepted_research" to false,
                    "uploaded" to false,
                    "file_link" to "",
                    "research_title" to "",
                    "research_type" to "",
                    "comments" to FieldValue.arrayUnion(comment)
                )
                firestore.collection("groups").document(groupId).update(updates).await()
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
        _uiState.update { it.copy(pdfBytes = null) }
    }

    override fun onCleared() {
        groupListener?.remove()
        super.onCleared()
    }
}