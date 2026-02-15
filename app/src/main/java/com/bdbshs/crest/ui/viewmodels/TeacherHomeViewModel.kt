package com.bdbshs.crest.ui.viewmodels

import android.content.Context
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bdbshs.crest.data.repository.AuthRepository
import com.bdbshs.crest.data.repository.TeacherDocumentUploadInput
import com.bdbshs.crest.data.repository.TeacherHomeRepository
import com.bdbshs.crest.data.repository.TeacherRecentResearch
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class DashboardCardItem(
    val title: String,
    val value: String,
    val icon: ImageVector
)

data class SimpleResearch(
    val id: String,
    val title: String,
    val date: Long = 0L
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

@HiltViewModel
class TeacherHomeViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val authRepository: AuthRepository,
    private val teacherHomeRepository: TeacherHomeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeacherHomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        fetchRealData()
    }

    private fun fetchRealData() {
        val uid = authRepository.getCurrentUserUid() ?: return
        _uiState.update { it.copy(isLoading = true, error = null) } // Reset error on refresh

        viewModelScope.launch {
            try {
                val dashboardData = teacherHomeRepository.fetchDashboardData(uid)

                val dashboardItems = listOf(
                    DashboardCardItem("Total Researches", dashboardData.totalResearches, Icons.Default.Description),
                    DashboardCardItem("Total Accounts", dashboardData.totalAccounts, Icons.Default.Group),
                    DashboardCardItem("Pending Researches", dashboardData.pendingResearches, Icons.Default.PendingActions),
                    DashboardCardItem("Pending Accounts", dashboardData.pendingAccounts, Icons.Default.PersonAdd)
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        teacherName = dashboardData.teacherName,
                        dashboardItems = dashboardItems,
                        recentResearches = dashboardData.recentResearches.map { it.toSimpleResearch() },
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
                teacherHomeRepository.uploadTeacherDocument(
                    context = appContext,
                    input = TeacherDocumentUploadInput(
                        name = currentState.uploadDocumentName,
                        description = currentState.uploadDocumentDescription,
                        fileUri = currentState.uploadSelectedFileUri
                            ?: throw IllegalStateException("No file selected.")
                    )
                )

                _uiState.update { it.copy(isUploadingDocument = false, uploadSuccess = true) }

            } catch (e: Exception) {
                _uiState.update { it.copy(isUploadingDocument = false, error = "Upload failed: ${e.message}") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

private fun TeacherRecentResearch.toSimpleResearch(): SimpleResearch {
    return SimpleResearch(
        id = id,
        title = title,
        date = date
    )
}