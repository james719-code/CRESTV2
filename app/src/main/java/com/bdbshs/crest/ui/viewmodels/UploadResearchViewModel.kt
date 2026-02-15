package com.bdbshs.crest.ui.viewmodels

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bdbshs.crest.data.repository.ResearchUploadInput
import com.bdbshs.crest.data.repository.UploadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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


@HiltViewModel
class UploadResearchViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val uploadRepository: UploadRepository
) : ViewModel() {

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
                uploadRepository.uploadResearch(
                    context = appContext,
                    input = ResearchUploadInput(
                        title = title,
                        members = members,
                        strandName = strandName,
                        researchType = type.name,
                        selectedFileUri = fileUri
                    )
                )

                // 3. Signal success
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }

            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Upload failed: ${e.message}") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}