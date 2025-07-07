package com.bdbshs.crest.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bdbshs.crest.data.AppwriteClient
import com.bdbshs.crest.data.FirebaseClient
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.collections.emptyList

// --- Data Models for Firestore ---

data class StudentDetails(
    val name: String = "",
    val strand: String = "",
    val groupId: String = "" // The ID of the group they belong to
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
    val groupDetails: GroupDetails? = null, // Null if not in a group
    val totalResearchCount: Int = 0,
    val strandResearchCount: Int = 0,
    val recentResearches: List<ResearchItem> = emptyList(), // Re-use ResearchItem
    val error: String? = null
)

class StudentHomeViewModel : ViewModel() {

    // --- REFACTORED: Use the centralized FirebaseClient ---
    private val firestore = FirebaseClient.firestore
    private val auth = FirebaseClient.auth

    // Appwrite client remains the same
    private val appwriteStorage = AppwriteClient.storage
    private val BUCKET_ID = "686a262b0024b8e10a35"

    private val _uiState = MutableStateFlow(StudentHomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        fetchAllData()
    }

    private fun fetchAllData() {
        val uid = auth.currentUser?.uid ?: return
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                // These .get() calls will now automatically read from the local cache when offline
                val studentDoc = firestore.collection("users/user_details/students").document(uid).get().await()
                val student = studentDoc.toObject(StudentDetails::class.java)
                _uiState.update { it.copy(studentDetails = student) }

                if (student != null && student.groupId.isNotBlank()) {
                    val groupDoc = firestore.collection("groups").document(student.groupId).get().await()
                    val group = groupDoc.toObject(GroupDetails::class.java)
                    _uiState.update { it.copy(groupDetails = group) }
                } else {
                    _uiState.update { it.copy(groupDetails = null) }
                }

                val qualitativeDocs = firestore.collection("researches/research_details/qualitative").get().await()
                val quantitativeDocs = firestore.collection("researches/research_details/quantitative").get().await()
                val allResearches = (qualitativeDocs.documents + quantitativeDocs.documents).mapNotNull {
                    it.toObject(ResearchItem::class.java)?.copy(id = it.id)
                }

                val strandCount = allResearches.count { it.strand == student?.strand }
                val recent = allResearches.sortedByDescending { it.createdAt }.take(3)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        totalResearchCount = allResearches.size,
                        strandResearchCount = strandCount,
                        recentResearches = recent
                    )
                }
            } catch (e: Exception) {
                // --- UPDATED: Better offline error message ---
                _uiState.update { it.copy(isLoading = false, error = "Could not load data. Please check your network connection.") }
            }
        }
    }

    // --- NEW FUNCTION: Unsubmit Research ---
    fun unsubmitResearch() {
        val uid = auth.currentUser?.uid ?: return
        val currentState = _uiState.value
        val groupId = currentState.studentDetails?.groupId
        val fileId = currentState.groupDetails?.file_link

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

                // Prepare the fields to revert
                val updates = mapOf(
                    "uploaded" to false,
                    "file_link" to "",
                    "research_title" to "",
                    "research_type" to ""
                    // We intentionally leave comments intact for the student to review.
                )

                // Update the document
                groupRef.update(updates).await()

                // Refresh the UI to reflect the change
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

                val newGroup = GroupDetails(
                    group_name = groupName,
                    strand = student.strand,
                    group_member = listOf(uid)
                )

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

    fun leaveGroup() {
        val uid = auth.currentUser?.uid ?: return
        val currentState = _uiState.value
        val student = currentState.studentDetails ?: return
        val group = currentState.groupDetails ?: return
        val groupId = student.groupId

        if (groupId.isBlank()) return
        _uiState.update { it.copy(isLoading = true) }

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
                _uiState.update { it.copy(isLoading = false, error = "Failed to leave group: ${e.message}") }
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

                if (!groupDoc.exists()) {
                    throw Exception("Group ID '$groupId' does not exist.")
                }

                val groupStrand = groupDoc.getString("strand")
                if (groupStrand != student.strand) {
                    throw Exception("You can only join a group from your own strand (${student.strand}).")
                }

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