package com.bdbshs.crest.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.annotation.Keep
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bdbshs.crest.data.AppwriteClient
import com.bdbshs.crest.data.FirebaseClient
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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

class StudentHomeViewModel(application: Application) : AndroidViewModel(application) {

    private val firestore = FirebaseClient.firestore
    private val auth = FirebaseClient.auth
    private val appwriteStorage = AppwriteClient.storage
    private val BUCKET_ID = "686a262b0024b8e10a35"
    private val TAG = "StudentHomeViewModel" // For logging

    private val _uiState = MutableStateFlow(StudentHomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        fetchAllData()
    }

    private fun fetchAllData() {
        val uid = auth.currentUser?.uid ?: return
        _uiState.update { it.copy(isLoading = true, isDownloadingCertificate = false) }

        viewModelScope.launch {
            try {
                val studentDoc = firestore.collection("users/user_details/students").document(uid).get().await()
                val student = studentDoc.toObject(StudentDetails::class.java)

                var group: GroupDetails? = null
                var memberNames: List<String> = emptyList()
                if (student != null && student.groupId.isNotBlank()) {
                    val groupDoc = firestore.collection("groups").document(student.groupId).get().await()
                    group = groupDoc.toObject(GroupDetails::class.java)
                    group?.group_member?.let { uids ->
                        if (uids.isNotEmpty()) {
                            memberNames = uids.chunked(10).flatMap { chunk ->
                                val docs = firestore.collection("users/user_details/students")
                                    .whereIn("__name__", chunk).get().await()
                                docs.documents.mapNotNull { it.getString("name") }
                            }
                        }
                    }
                }

                val qualitativeDocs = firestore.collection("researches/research_details/qualitative").get().await()
                val quantitativeDocs = firestore.collection("researches/research_details/quantitative").get().await()

                // --- THIS IS THE FIX ---
                // Manually parse the documents instead of using the risky toObject() call.
                val allResearches = (qualitativeDocs.documents + quantitativeDocs.documents).mapNotNull { doc ->
                    val title = doc.getString("title")
                    val strand = doc.getString("strand")
                    // Safely get createdAt, whether it's a Timestamp or a Long
                    val createdAt = when(val rawDate = doc.get("createdAt")) {
                        is Timestamp -> rawDate.toDate().time
                        is Long -> rawDate
                        else -> 0L
                    }

                    if (title != null && strand != null) {
                        ResearchItem(
                            id = doc.id,
                            title = title,
                            strand = strand,
                            createdAt = createdAt
                        )
                    } else {
                        null // If essential data is missing, skip this item
                    }
                }
                // --- END OF FIX ---


                val strandCount = allResearches.count { it.strand == student?.strand }
                val recent = allResearches.sortedByDescending { it.createdAt }.take(3)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        studentDetails = student,
                        groupDetails = group,
                        memberNames = memberNames,
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
        val uid = auth.currentUser?.uid ?: return
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
                val groupRef = firestore.collection("groups").document(groupId)
                if (group.group_member.size <= 1) {
                    firestore.batch().apply {
                        delete(groupRef)
                        val studentRef = firestore.collection("users/user_details/students").document(uid)
                        update(studentRef, "groupId", "")
                    }.commit().await()
                } else {
                    firestore.batch().apply {
                        update(groupRef, "group_member", FieldValue.arrayRemove(uid))
                        val studentRef = firestore.collection("users/user_details/students").document(uid)
                        update(studentRef, "groupId", "")
                    }.commit().await()
                }
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
                val groupRef = firestore.collection("groups").document(groupId)
                if (!fileId.isNullOrBlank()) {
                    appwriteStorage.deleteFile(BUCKET_ID, fileId)
                }
                val updates = mapOf(
                    "uploaded" to false, "file_link" to "", "research_type" to ""
                )
                groupRef.update(updates).await()
                fetchAllData()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Failed to unsubmit: ${e.message}") }
            }
        }
    }

    fun createGroup(groupName: String) {
        val uid = auth.currentUser?.uid ?: return
        val student = _uiState.value.studentDetails ?: return
        if (student.groupId.isNotBlank()) {
            _uiState.update { it.copy(error = "You are already in a group.") }
            return
        }
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val newGroupRef = firestore.collection("groups").document()
                val newGroupId = newGroupRef.id
                val newGroup = GroupDetails(group_name = groupName, strand = student.strand, group_member = listOf(uid))
                firestore.batch().apply {
                    set(newGroupRef, newGroup)
                    val studentRef = firestore.collection("users/user_details/students").document(uid)
                    update(studentRef, "groupId", newGroupId)
                }.commit().await()
                fetchAllData()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Failed to create group: ${e.message}") }
            }
        }
    }

    fun joinGroup(groupId: String) {
        val uid = auth.currentUser?.uid ?: return
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
                val groupRef = firestore.collection("groups").document(groupId)
                val groupDoc = groupRef.get().await()
                if (!groupDoc.exists()) throw Exception("Group ID '$groupId' does not exist.")
                val groupStrand = groupDoc.getString("strand")
                if (groupStrand != student.strand) throw Exception("You can only join a group from your own strand (${student.strand}).")
                firestore.batch().apply {
                    update(groupRef, "group_member", FieldValue.arrayUnion(uid))
                    val studentRef = firestore.collection("users/user_details/students").document(uid)
                    update(studentRef, "groupId", groupId)
                }.commit().await()
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