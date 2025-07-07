package com.bdbshs.crest.ui.viewmodels

import android.app.Application // Needed for AndroidViewModel
import android.net.Uri // For new file selection
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel // For file operations
import androidx.lifecycle.viewModelScope
import com.bdbshs.crest.data.AppwriteClient
import com.bdbshs.crest.data.FirebaseClient
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QueryDocumentSnapshot
import io.appwrite.ID
import io.appwrite.models.InputFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

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

    private val firestore = FirebaseClient.firestore
    private val appwriteStorage = AppwriteClient.storage
    val DOCUMENTS_BUCKET_ID = "686a262b0024b8e10a35" // Use your actual Appwrite bucket ID for documents

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

        documentsListener = firestore.collection("documents") // Assuming your collection is named "documents"
            .orderBy("createdAt", Query.Direction.DESCENDING) // Default sort
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    val message = if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE) {
                        "You are offline. Showing cached documents."
                    } else { "Error: ${e.message}" }
                    _uiState.update { it.copy(isLoading = false, isRefreshing = false, error = message) }
                    return@addSnapshotListener
                }
                val docList = snapshot?.documents?.mapNotNull { mapDocumentToDocumentItem(it as QueryDocumentSnapshot) } ?: emptyList()
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
                var updatedFileLink = document.file_link

                // If a new file is selected, upload it and delete the old one
                if (newFileUri != null) {
                    // 1. Upload new file
                    val tempFile = withContext(Dispatchers.IO) { createTempFileFromUri(newFileUri) }
                        ?: throw Exception("Failed to prepare new file for upload.")
                    val inputFile = InputFile.fromFile(file = tempFile)
                    val uploadedFile = appwriteStorage.createFile(DOCUMENTS_BUCKET_ID, ID.unique(), inputFile)
                    tempFile.delete() // Clean up temp file
                    updatedFileLink = uploadedFile.id

                    // 2. Delete old file (if it exists)
                    if (document.file_link.isNotBlank()) {
                        appwriteStorage.deleteFile(DOCUMENTS_BUCKET_ID, document.file_link)
                    }
                }

                // 3. Update Firestore document
                val updates = mapOf(
                    "name" to newName,
                    "description" to newDescription,
                    "file_link" to updatedFileLink
                )
                firestore.collection("documents").document(document.id).update(updates).await()

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
                // 1. Delete Firestore document
                firestore.collection("documents").document(document.id).delete().await()

                // 2. Delete Appwrite file (if it exists)
                if (document.file_link.isNotBlank()) {
                    appwriteStorage.deleteFile(DOCUMENTS_BUCKET_ID, document.file_link)
                }

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

    private fun mapDocumentToDocumentItem(doc: QueryDocumentSnapshot): DocumentItem? {
        return try {
            doc.toObject(DocumentItem::class.java)?.copy(id = doc.id)
        } catch (e: Exception) {
            Log.e("DocumentsViewModel", "Failed to map document ${doc.id}", e)
            null
        }
    }

    private fun createTempFileFromUri(uri: Uri): File? {
        return try {
            val context = getApplication<Application>().applicationContext
            val inputStream = context.contentResolver.openInputStream(uri)
            val tempFile = File.createTempFile("upload_doc_", "", context.cacheDir) // <-- FIX HERE
            val fileOutputStream = FileOutputStream(tempFile)
            inputStream?.use { input -> fileOutputStream.use { output -> input.copyTo(output) } }
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    override fun onCleared() {
        documentsListener?.remove()
        super.onCleared()
    }
}