package com.bdbshs.crest.ui.viewmodels

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.AndroidViewModel // Use AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bdbshs.crest.data.AppwriteClient
import com.bdbshs.crest.data.FirebaseClient
import com.google.firebase.firestore.Query // Import Firestore Query
import io.appwrite.ID
import io.appwrite.models.InputFile
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Immutable
data class DashboardCardItem(
    val title: String,
    val value: String,
    val icon: ImageVector
)

data class SimpleResearch(
    val id: String,
    val title: String
)

data class TeacherHomeUiState(
    val dashboardItems: List<DashboardCardItem> = emptyList(),
    val recentResearches: List<SimpleResearch> = emptyList(),
    val teacherName: String = "",
    val isLoading: Boolean = true,
    val error: String? = null,
    // Document Upload State
    val isUploadDialogVisible: Boolean = false,
    val uploadDocumentName: String = "",
    val uploadDocumentDescription: String = "",
    val uploadSelectedFileUri: Uri? = null,
    val uploadSelectedFileName: String? = null,
    val isUploadingDocument: Boolean = false,
    val uploadSuccess: Boolean = false
)

// Changed from ViewModel to AndroidViewModel to use getApplication()
class TeacherHomeViewModel(application: Application) : AndroidViewModel(application) {

    private val firestore = FirebaseClient.firestore
    private val auth = FirebaseClient.auth
    private val appwriteStorage = AppwriteClient.storage

    // Make sure this bucket ID is correct for your documents
    private val DOCUMENTS_BUCKET_ID = "686a262b0024b8e10a35"

    private val _uiState = MutableStateFlow(TeacherHomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        fetchRealData()
    }

    private fun fetchRealData() {
        val uid = auth.currentUser?.uid ?: return
        _uiState.update { it.copy(isLoading = true, error = null) } // Reset error on refresh

        viewModelScope.launch {
            try {
                // 1. Fetch Teacher's Name
                val teacherNameDeferred = async {
                    firestore.collection("users/user_details/teachers").document(uid).get().await()
                        .getString("name") ?: "Teacher"
                }

                // 2. Fetch Dashboard Counts
                val totalResearchesDeferred = async {
                    (firestore.collection("researches/research_details/qualitative").get().await().size() +
                            firestore.collection("researches/research_details/quantitative").get().await().size()).toString()
                }
                val totalAccountsDeferred = async {
                    (firestore.collection("users/user_details/students").get().await().size() +
                            firestore.collection("users/user_details/teachers").get().await().size()).toString()
                }
                val pendingResearchesDeferred = async {
                    firestore.collection("groups").whereEqualTo("uploaded", true)
                        .whereEqualTo("accepted_research", false).get().await().size().toString()
                }
                val pendingAccountsDeferred = async {
                    val pendingStudents = firestore.collection("users/user_details/students")
                        .whereEqualTo("accepted", false).get().await().size()
                    val pendingTeachers = firestore.collection("users/user_details/teachers")
                        .whereEqualTo("access", false).get().await().size()
                    (pendingStudents + pendingTeachers).toString()
                }

                // 3. Fetch Recent Researches (simplified for home screen)
                val recentResearchesDeferred = async {
                    val qualitative = firestore.collection("researches/research_details/qualitative")
                        .orderBy("createdAt", Query.Direction.DESCENDING)
                        .limit(3).get().await()
                    val quantitative = firestore.collection("researches/research_details/quantitative")
                        .orderBy("createdAt", Query.Direction.DESCENDING)
                        .limit(3).get().await()

                    (qualitative.documents + quantitative.documents)
                        .mapNotNull { doc -> doc.getString("title")?.let { SimpleResearch(doc.id, it) } }
                        .sortedByDescending { it.id } // Crude sort for combining recent
                        .take(5)
                }

                // Await all results and update the UI state once
                val dashboardItems = listOf(
                    DashboardCardItem("Total Researches", totalResearchesDeferred.await(), Icons.Default.Description),
                    DashboardCardItem("Total Accounts", totalAccountsDeferred.await(), Icons.Default.Group),
                    DashboardCardItem("Pending Researches", pendingResearchesDeferred.await(), Icons.Default.PendingActions),
                    DashboardCardItem("Pending Accounts", pendingAccountsDeferred.await(), Icons.Default.PersonAdd)
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        teacherName = teacherNameDeferred.await(),
                        dashboardItems = dashboardItems,
                        recentResearches = recentResearchesDeferred.await(),
                        error = null // Clear error on successful fetch
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = "Could not load dashboard. Please check your network connection.")
                }
            }
        }
    }

    fun onRefresh() {
        fetchRealData() // Re-fetch on refresh
    }

    // --- Document Upload Functions ---

    fun showUploadDialog() {
        _uiState.update {
            it.copy(
                isUploadDialogVisible = true,
                uploadDocumentName = "",
                uploadDocumentDescription = "",
                uploadSelectedFileUri = null,
                uploadSelectedFileName = null,
                uploadSuccess = false, // Reset success state for a new upload
                error = null // Clear any previous error
            )
        }
    }

    fun dismissUploadDialog() {
        _uiState.update {
            it.copy(
                isUploadDialogVisible = false,
                uploadDocumentName = "",
                uploadDocumentDescription = "",
                uploadSelectedFileUri = null,
                uploadSelectedFileName = null,
                uploadSuccess = false // Clear success state on dismiss
            )
        }
    }

    fun onUploadDocumentNameChange(name: String) { _uiState.update { it.copy(uploadDocumentName = name) } }
    fun onUploadDocumentDescriptionChange(description: String) { _uiState.update { it.copy(uploadDocumentDescription = description) } }
    fun onUploadFileSelected(uri: Uri?, fileName: String?) { _uiState.update { it.copy(uploadSelectedFileUri = uri, uploadSelectedFileName = fileName) } }
    fun onUploadFileCleared() { _uiState.update { it.copy(uploadSelectedFileUri = null, uploadSelectedFileName = null) } }

    fun uploadDocument() {
        val currentState = _uiState.value
        if (currentState.uploadDocumentName.isBlank() || currentState.uploadDocumentDescription.isBlank() || currentState.uploadSelectedFileUri == null) {
            _uiState.update { it.copy(error = "Please fill all fields and select a file.") }
            return
        }

        _uiState.update { it.copy(isUploadingDocument = true, error = null, uploadSuccess = false) }

        viewModelScope.launch {
            try {
                // 1. Upload file to Appwrite Storage
                val tempFile = withContext(Dispatchers.IO) { createTempFileFromUri(currentState.uploadSelectedFileUri) }
                    ?: throw Exception("Failed to prepare file for upload.")
                val inputFile = InputFile.fromFile(file = tempFile)
                // Use the correct DOCUMENTS_BUCKET_ID
                val uploadedFile = appwriteStorage.createFile(DOCUMENTS_BUCKET_ID, ID.unique(), inputFile)
                tempFile.delete() // Clean up temp file

                // 2. Add document metadata to Firestore
                val documentData = mapOf(
                    "name" to currentState.uploadDocumentName,
                    "description" to currentState.uploadDocumentDescription,
                    "file_link" to uploadedFile.id,
                    "createdAt" to System.currentTimeMillis()
                )
                firestore.collection("documents").add(documentData).await()

                _uiState.update { it.copy(isUploadingDocument = false, uploadSuccess = true) }

            } catch (e: Exception) {
                _uiState.update { it.copy(isUploadingDocument = false, error = "Upload failed: ${e.message}") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    // Helper function to create temp file from Uri
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
}