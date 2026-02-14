package com.bdbshs.crest.ui.viewmodels

import android.app.Application // Needed for AndroidViewModel
import android.net.Uri // For new file selection
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel // For file operations
import androidx.lifecycle.viewModelScope
import com.bdbshs.crest.data.repository.DocumentRecord
import com.bdbshs.crest.data.repository.DocumentRepository
import com.bdbshs.crest.data.repository.DocumentUpdateInput
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// --- DATA MODELS ---

@Immutable
data class DocumentItem(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val file_link: String = "", // Appwrite File ID
    val createdAt: Long = 0L // Timestamp for sorting
)

enum class DocumentSortOption(val displayName: String) {
    NameAZ("Name (A-Z)"),
    NameZA("Name (Z-A)"),
    DateNewest("Date (Newest)"),
    DateOldest("Date (Oldest)")
}

data class DocumentsUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val allDocuments: List<DocumentItem> = emptyList(),
    val error: String? = null,

    // Search, Sort, Filter
    val searchQuery: String = "",
    val selectedSortOption: DocumentSortOption = DocumentSortOption.DateNewest,

    // Teacher Edit Dialog State
    val isEditDialogVisible: Boolean = false,
    val selectedDocumentForEdit: DocumentItem? = null,
    val editedDocumentName: String = "",
    val editedDocumentDescription: String = "",
    val newSelectedFileUri: Uri? = null,
    val newSelectedFileName: String? = null,
    val isUpdatingDocument: Boolean = false,
    val userRole: UserType? = null // To determine if teacher functions are available
)

// --- VIEWMODEL ---

class DocumentsViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = getApplication<Application>().applicationContext

    private var documentsListener: ListenerRegistration? = null

    private val _uiState = MutableStateFlow(DocumentsUiState(userRole = null)) // Initialize userRole as null
    val uiState = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    @OptIn(FlowPreview::class)
    val filteredAndSortedDocuments: StateFlow<List<DocumentItem>> = combine(
        _uiState,
        _searchQuery.debounce(300L)
    ) { state, query ->
        val filtered = state.allDocuments.filter { document ->
            val queryMatch = if (query.isBlank()) {
                true
            } else {
                document.name.contains(query, ignoreCase = true) ||
                        document.description.contains(query, ignoreCase = true)
            }
            queryMatch
        }

        when (state.selectedSortOption) {
            DocumentSortOption.DateNewest -> filtered.sortedByDescending { it.createdAt }
            DocumentSortOption.DateOldest -> filtered.sortedBy { it.createdAt }
            DocumentSortOption.NameAZ -> filtered.sortedBy { it.name }
            DocumentSortOption.NameZA -> filtered.sortedByDescending { it.name }
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        setupDocumentsListener()
    }

    fun setUserRole(role: UserType?) {
        _uiState.update { it.copy(userRole = role) }
    }

    private fun setupDocumentsListener() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        documentsListener?.remove()

        documentsListener = DocumentRepository.observeDocuments { documents, error ->
                if (error != null) {
                    val message = if (error.code == FirebaseFirestoreException.Code.UNAVAILABLE) {
                        "You are offline. Showing cached documents."
                    } else { "Error: ${error.message}" }
                    _uiState.update { it.copy(isLoading = false, isRefreshing = false, error = message) }
                    return@observeDocuments
                }

                val docList = documents?.map { it.toUiDocumentItem() } ?: emptyList()
                _uiState.update { it.copy(isLoading = false, isRefreshing = false, allDocuments = docList) }
            }
    }

    fun onRefresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        setupDocumentsListener()
    }

    // --- Search & Filter UI Actions ---
    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        _searchQuery.value = query // Trigger debounce
    }

    fun onSortOptionSelected(option: DocumentSortOption) {
        _uiState.update { it.copy(selectedSortOption = option) }
    }

    // --- Teacher Edit Actions ---
    fun onDocumentLongPress(document: DocumentItem) {
        if (_uiState.value.userRole == UserType.TEACHER) {
            _uiState.update {
                it.copy(
                    isEditDialogVisible = true,
                    selectedDocumentForEdit = document,
                    editedDocumentName = document.name,
                    editedDocumentDescription = document.description,
                    newSelectedFileUri = null,
                    newSelectedFileName = null
                )
            }
        }
    }

    fun dismissEditDialog() {
        _uiState.update {
            it.copy(
                isEditDialogVisible = false,
                selectedDocumentForEdit = null,
                editedDocumentName = "",
                editedDocumentDescription = "",
                newSelectedFileUri = null,
                newSelectedFileName = null
            )
        }
    }

    fun onEditedNameChange(name: String) { _uiState.update { it.copy(editedDocumentName = name) } }
    fun onEditedDescriptionChange(description: String) { _uiState.update { it.copy(editedDocumentDescription = description) } }
    fun onNewFileSelected(uri: Uri?, fileName: String?) { _uiState.update { it.copy(newSelectedFileUri = uri, newSelectedFileName = fileName) } }
    fun onNewFileCleared() { _uiState.update { it.copy(newSelectedFileUri = null, newSelectedFileName = null) } }


    fun updateDocument() {
        val document = _uiState.value.selectedDocumentForEdit ?: return
        val newName = _uiState.value.editedDocumentName.trim()
        val newDescription = _uiState.value.editedDocumentDescription.trim()
        val newFileUri = _uiState.value.newSelectedFileUri

        if (newName.isBlank() || newDescription.isBlank()) {
            _uiState.update { it.copy(error = "Name and description cannot be empty.") }
            return
        }

        _uiState.update { it.copy(isUpdatingDocument = true, error = null) }

        viewModelScope.launch {
            try {
                DocumentRepository.updateDocument(
                    context = appContext,
                    input = DocumentUpdateInput(
                        documentId = document.id,
                        name = newName,
                        description = newDescription,
                        currentFileLink = document.file_link,
                        newFileUri = newFileUri
                    )
                )

                dismissEditDialog() // Close dialog on success
                _uiState.update { it.copy(isUpdatingDocument = false) }

            } catch (e: Exception) {
                _uiState.update { it.copy(isUpdatingDocument = false, error = "Update failed: ${e.message}") }
            }
        }
    }

    fun deleteDocument() {
        val document = _uiState.value.selectedDocumentForEdit ?: return
        _uiState.update { it.copy(isUpdatingDocument = true, error = null) }

        viewModelScope.launch {
            try {
                DocumentRepository.deleteDocument(document.id, document.file_link)

                dismissEditDialog() // Close dialog on success
                _uiState.update { it.copy(isUpdatingDocument = false) }

            } catch (e: Exception) {
                _uiState.update { it.copy(isUpdatingDocument = false, error = "Delete failed: ${e.message}") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    override fun onCleared() {
        documentsListener?.remove()
        super.onCleared()
    }
}

private fun DocumentRecord.toUiDocumentItem(): DocumentItem {
    return DocumentItem(
        id = id,
        name = name,
        description = description,
        file_link = fileLink,
        createdAt = createdAt
    )
}