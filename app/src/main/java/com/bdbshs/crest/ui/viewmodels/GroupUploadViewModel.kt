package com.bdbshs.crest.ui.viewmodels

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bdbshs.crest.data.AppwriteClient
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
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

data class GroupUploadUiState(
    val title: String = "",
    val researchType: ResearchType? = null,
    val selectedFileUri: Uri? = null,
    val selectedFileName: String? = null,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)

class GroupUploadViewModel(application: Application) : AndroidViewModel(application) {

    private val firestore = FirebaseFirestore.getInstance()
    private val appwriteStorage = AppwriteClient.storage
    private val auth = Firebase.auth

    private val BUCKET_ID = "686a262b0024b8e10a35"

    private val _uiState = MutableStateFlow(GroupUploadUiState())
    val uiState = _uiState.asStateFlow()

    fun onTitleChange(newTitle: String) { _uiState.update { it.copy(title = newTitle) } }
    fun onTypeSelected(type: ResearchType) { _uiState.update { it.copy(researchType = type) } }
    fun onFileSelected(uri: Uri?, fileName: String?) { _uiState.update { it.copy(selectedFileUri = uri, selectedFileName = fileName) } }
    fun onFileCleared() { _uiState.update { it.copy(selectedFileUri = null, selectedFileName = null) } }
    fun clearError() { _uiState.update { it.copy(error = null) } }

    fun submitResearchForReview() {
        val currentState = _uiState.value
        if (currentState.title.isBlank() || currentState.researchType == null || currentState.selectedFileUri == null) {
            _uiState.update { it.copy(error = "Please fill all fields and select a file.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                // 1. Get the current user's groupId
                val uid = auth.currentUser?.uid ?: throw Exception("User not logged in.")
                val studentDoc = firestore.collection("users/user_details/students").document(uid).get().await()
                val groupId = studentDoc.getString("groupId")
                if (groupId.isNullOrBlank()) throw Exception("You are not in a group.")

                // 2. Upload file to Appwrite Storage
                val tempFile = withContext(Dispatchers.IO) { createTempFileFromUri(currentState.selectedFileUri) }
                    ?: throw Exception("Failed to prepare file for upload.")
                val inputFile = InputFile.fromFile(file = tempFile)
                val uploadedFile = appwriteStorage.createFile(BUCKET_ID, ID.unique(), inputFile)
                tempFile.delete()

                // 3. Prepare the data to update in the group document
                // Added a new field 'research_title' to store the title for teacher's review
                val groupUpdates = mapOf(
                    "file_link" to uploadedFile.id,
                    "research_title" to currentState.title,
                    "research_type" to currentState.researchType.name,
                    "uploaded" to true
                )

                // 4. Update the group document in Firestore
                firestore.collection("groups").document(groupId).update(groupUpdates).await()

                _uiState.update { it.copy(isLoading = false, isSuccess = true) }

            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Submission failed: ${e.message}") }
            }
        }
    }

    // This helper function needs to be implemented or copied from your other ViewModel
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