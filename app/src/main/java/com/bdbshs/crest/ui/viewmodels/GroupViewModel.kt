package com.bdbshs.crest.ui.viewmodels

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bdbshs.crest.data.AppwriteClient
import com.bdbshs.crest.data.FirebaseClient
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QueryDocumentSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// --- DATA MODELS & ENUMS ---

@Immutable
data class GroupItem(
    val id: String = "",
    val group_name: String = "",
    val strand: String = "",
    val group_member: List<String> = emptyList(),
    val uploaded: Boolean = false,
    val accepted_research: Boolean = false,
    val research_title: String = "",
    val research_type: String = "",
    val file_link: String = ""
)

enum class GroupSortOption(val displayName: String) {
    NameAZ("Name (A-Z)"),
    NameZA("Name (Z-A)"),
    Strand("Strand")
}

data class GroupsUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val allGroups: List<GroupItem> = emptyList(), // Store the full, unfiltered list
    val error: String? = null,
    val selectedGroup: GroupItem? = null,
    val isActionDialogVisible: Boolean = false,
    val isUpdating: Boolean = false,
    val denialComment: String = "",

    // --- NEW: State for Search, Sort, and Filter ---
    val searchQuery: String = "",
    val selectedSortOption: GroupSortOption = GroupSortOption.NameAZ,
    val showPendingOnly: Boolean = false,
    val showAcceptedOnly: Boolean = false
)

// --- VIEWMODEL ---

class GroupsViewModel : ViewModel() {

    private val firestore = FirebaseClient.firestore
    private val appwriteStorage = AppwriteClient.storage
    private val BUCKET_ID = "686a262b0024b8e10a35"
    private var groupsListener: ListenerRegistration? = null

    private val _uiState = MutableStateFlow(GroupsUiState())
    val uiState = _uiState.asStateFlow()

    // --- NEW: A debounced search query flow ---
    private val _searchQuery = MutableStateFlow("")

    @OptIn(FlowPreview::class)
    val filteredAndSortedGroups: StateFlow<List<GroupItem>> = combine(
        _uiState, // Reacts to changes in filters and the allGroups list
        _searchQuery.debounce(300L) // Reacts to debounced search queries
    ) { state, query ->
        val filtered = state.allGroups.filter { group ->
            // Condition 1: Search Query (matches group name)
            val queryMatch = if (query.isBlank()) true else group.group_name.contains(query, ignoreCase = true)

            // Condition 2: Pending Only Filter
            val pendingMatch = if (state.showPendingOnly) group.uploaded && !group.accepted_research else true

            // Condition 3: Accepted Only Filter
            val acceptedMatch = if (state.showAcceptedOnly) group.accepted_research else true

            queryMatch && pendingMatch && acceptedMatch
        }

        // Apply sorting
        when (state.selectedSortOption) {
            GroupSortOption.NameAZ -> filtered.sortedBy { it.group_name }
            GroupSortOption.NameZA -> filtered.sortedByDescending { it.group_name }
            GroupSortOption.Strand -> filtered.sortedBy { it.strand }
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    init {
        setupGroupsListener()
    }

    private fun setupGroupsListener() {
        _uiState.update { it.copy(isLoading = true) }
        groupsListener?.remove()

        groupsListener = firestore.collection("groups")
            .orderBy("group_name", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) { /* ... error handling ... */ return@addSnapshotListener }
                val groupList = snapshot?.documents?.mapNotNull { mapDocumentToGroupItem(it as QueryDocumentSnapshot) } ?: emptyList()
                _uiState.update { it.copy(isLoading = false, isRefreshing = false, allGroups = groupList) }
            }
    }

    // --- NEW: Handlers for UI actions ---
    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        _searchQuery.value = query // Update the debounced flow
    }

    fun onSortOptionSelected(option: GroupSortOption) {
        _uiState.update { it.copy(selectedSortOption = option) }
    }

    fun onShowPendingOnlyToggled(isChecked: Boolean) {
        _uiState.update { it.copy(showPendingOnly = isChecked, showAcceptedOnly = if (isChecked) false else it.showAcceptedOnly) }
    }

    fun onShowAcceptedOnlyToggled(isChecked: Boolean) {
        _uiState.update { it.copy(showAcceptedOnly = isChecked, showPendingOnly = if (isChecked) false else it.showPendingOnly) }
    }

    fun onRefresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        setupGroupsListener()
    }

    fun onGroupSelected(group: GroupItem) {
        // Only show dialog for groups that have a submission pending
        if (group.uploaded && !group.accepted_research) {
            _uiState.update { it.copy(selectedGroup = group, isActionDialogVisible = true) }
        }
    }

    fun onDenialCommentChange(comment: String) {
        _uiState.update { it.copy(denialComment = comment) }
    }

    fun dismissActionDialog() {
        _uiState.update { it.copy(isActionDialogVisible = false, selectedGroup = null, denialComment = "", isUpdating = false) }
    }

    fun approveSubmission() {
        val group = _uiState.value.selectedGroup ?: return
        _uiState.update { it.copy(isUpdating = true) }

        viewModelScope.launch {
            try {
                val updates = mapOf("accepted_research" to true, "uploaded" to false)
                firestore.collection("groups").document(group.id).update(updates).await()
                dismissActionDialog() // Success
            } catch (e: Exception) {
                _uiState.update { it.copy(isUpdating = false, error = "Approval failed: ${e.message}") }
            }
        }
    }

    fun denySubmission() {
        val group = _uiState.value.selectedGroup ?: return
        val comment = _uiState.value.denialComment
        if (comment.isBlank()) return
        _uiState.update { it.copy(isUpdating = true) }

        viewModelScope.launch {
            try {
                // This will fail offline, which is expected.
                if (group.file_link.isNotBlank()) {
                    appwriteStorage.deleteFile(BUCKET_ID, group.file_link)
                }

                val updates = mapOf(
                    "accepted_research" to false,
                    "uploaded" to false,
                    "file_link" to "",
                    "research_title" to "",
                    "research_type" to "",
                    "comments" to FieldValue.arrayUnion(comment)
                )
                firestore.collection("groups").document(group.id).update(updates).await()
                dismissActionDialog() // Success
            } catch (e: Exception) {
                _uiState.update { it.copy(isUpdating = false, error = "Denial failed. An internet connection may be required.") }
            }
        }
    }

    private fun mapDocumentToGroupItem(doc: QueryDocumentSnapshot): GroupItem? {
        return try {
            doc.toObject(GroupItem::class.java).copy(id = doc.id)
        } catch (e: Exception) {
            Log.e("GroupsViewModel", "Failed to map document ${doc.id}", e)
            null
        }
    }

    override fun onCleared() {
        groupsListener?.remove()
        super.onCleared()
    }
}