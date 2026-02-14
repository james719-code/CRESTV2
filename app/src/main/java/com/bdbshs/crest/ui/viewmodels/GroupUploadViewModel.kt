package com.bdbshs.crest.ui.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bdbshs.crest.data.repository.AuthRepository
import com.bdbshs.crest.data.repository.GroupResearchUploadInput
import com.bdbshs.crest.data.repository.UploadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GroupUploadUiState(
    val title: String = "",
    val researchType: ResearchType? = null,
    val selectedFileUri: Uri? = null,
    val selectedFileName: String? = null,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class GroupUploadViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context
) : ViewModel() {

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
                val uid = AuthRepository.getCurrentUserUid() ?: throw Exception("User not logged in.")
                val groupId = UploadRepository.getStudentGroupId(uid)
                if (groupId.isNullOrBlank()) throw Exception("You are not in a group.")

                // 2. Upload file and update group document metadata
                UploadRepository.submitGroupResearchForReview(
                    context = appContext,
                    input = GroupResearchUploadInput(
                        groupId = groupId,
                        title = currentState.title,
                        researchType = currentState.researchType.name,
                        selectedFileUri = currentState.selectedFileUri
                    )
                )

                _uiState.update { it.copy(isLoading = false, isSuccess = true) }

            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Submission failed: ${e.message}") }
            }
        }
    }
}