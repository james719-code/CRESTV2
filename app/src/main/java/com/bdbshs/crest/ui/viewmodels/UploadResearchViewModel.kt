package com.bdbshs.crest.ui.viewmodels

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bdbshs.crest.data.AppwriteClient
import com.google.firebase.firestore.FirebaseFirestore
import io.appwrite.ID
import io.appwrite.models.InputFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

// Represents the state of the upload screen's form

@Immutable
data class UploadUiState(
    val title: String = "",
    val members: List<String> = listOf(""), // Start with one empty member field
    val selectedStrand: Strand? = null,
    val selectedType: ResearchType? = null,
    val selectedFileUri: Uri? = null,
    val selectedFileName: String? = null,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)


class UploadResearchViewModel(application: Application) : AndroidViewModel(application) {

    private val BUCKET_ID = "686a262b0024b8e10a35" // Your Appwrite bucket ID

    private val firestore = FirebaseFirestore.getInstance()
    private val appwriteStorage = AppwriteClient.storage

    private val _uiState = MutableStateFlow(UploadUiState())
    val uiState = _uiState.asStateFlow()

    // --- THIS IS THE NEW FUNCTION ---
    fun onStrandSelected(strand: Strand) {
        _uiState.update { it.copy(selectedStrand = strand) }
    }

    // --- Form Field Updaters ---
    fun onTitleChange(newTitle: String) {
        _uiState.update { it.copy(title = newTitle) }
    }

    fun onMemberChange(index: Int, newName: String) {
        _uiState.update { currentState ->
            val updatedMembers = currentState.members.toMutableList()
            updatedMembers[index] = newName
            currentState.copy(members = updatedMembers)
        }
    }

    fun addMemberField() {
        _uiState.update { it.copy(members = it.members + "") }
    }

    fun removeMemberField(index: Int) {
        if (_uiState.value.members.size > 1) { // Always keep at least one field
            _uiState.update { currentState ->
                val updatedMembers = currentState.members.toMutableList()
                updatedMembers.removeAt(index)
                currentState.copy(members = updatedMembers)
            }
        }
    }
    fun onTypeSelected(type: ResearchType) {
        _uiState.update { it.copy(selectedType = type) }
    }

    fun onFileSelected(uri: Uri?, fileName: String?) {
        _uiState.update { it.copy(selectedFileUri = uri, selectedFileName = fileName) }
    }

    fun onFileCleared() {
        _uiState.update { currentState ->
            currentState.copy(
                selectedFileUri = null,
                selectedFileName = null
            )
        }
    }

    // --- Main Upload Logic ---
    fun uploadResearch() {
        val currentState = _uiState.value
        val fileUri = currentState.selectedFileUri
        val fileName = currentState.selectedFileName
        val title = currentState.title
        val strandName = currentState.selectedStrand?.name
        val type = currentState.selectedType
        // Filter out empty member fields before uploading
        val members = currentState.members.filter { it.isNotBlank() }

        // Basic validation
        if (fileUri == null || fileName == null || title.isBlank() || strandName.isNullOrBlank() || type == null || members.isEmpty()) {
            _uiState.update { it.copy(error = "Please fill all required fields and select a file.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                // 1. Create a temporary file from the URI
                val tempFile = withContext(Dispatchers.IO) {
                    createTempFileFromUri(fileUri)
                }

                if (tempFile == null) {
                    _uiState.update { it.copy(isLoading = false, error = "Failed to read the selected file.") }
                    return@launch
                }

                // 2. Upload the temporary file to Appwrite
                val uploadedFileId = withContext(Dispatchers.IO) {
                    // Now we can use InputFile.fromFile()
                    val inputFile = InputFile.fromFile(file = tempFile)
                    val file = appwriteStorage.createFile(BUCKET_ID, ID.unique(), inputFile)

                    // Clean up the temporary file after upload
                    tempFile.delete()

                    file.id
                }

                // 2. Save metadata to Firestore
                withContext(Dispatchers.IO) {
                    val researchData = hashMapOf(
                        "title" to title,
                        "members" to members,
                        "strand" to strandName,
                        "type" to type.name.uppercase(), // Store as uppercase string
                        "unfinished" to false,
                        "views" to 0,
                        "file_link" to uploadedFileId, // The ID from Appwrite
                        "createdAt" to System.currentTimeMillis()
                    )

                    val collectionPath = "researches/research_details/${type.name.lowercase()}"
                    firestore.collection(collectionPath).add(researchData).await()
                }

                // 3. Signal success
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }

            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Upload failed: ${e.message}") }
            }
        }
    }

    private fun createTempFileFromUri(uri: Uri): File? {
        return try {
            val context = getApplication<Application>().applicationContext
            val inputStream = context.contentResolver.openInputStream(uri)
            // Create a temporary file in the app's cache directory
            val tempFile = File.createTempFile("upload_", ".pdf", context.cacheDir)
            val fileOutputStream = FileOutputStream(tempFile)

            inputStream?.use { input ->
                fileOutputStream.use { output ->
                    input.copyTo(output)
                }
            }
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}