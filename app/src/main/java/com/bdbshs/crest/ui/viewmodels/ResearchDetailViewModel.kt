package com.bdbshs.crest.ui.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bdbshs.crest.data.FileCache
import com.bdbshs.crest.navigation.AppDestination
import com.bdbshs.crest.data.repository.PdfRepository
import com.bdbshs.crest.data.repository.ResearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.bdbshs.crest.data.UserPrefs

// DATA CLASS is unchanged
data class ResearchDetailUiState(
    val researchItem: ResearchItem? = null,
    val pdfFilePath: String? = null,
    val isDetailsLoading: Boolean = true,
    val isPdfLoading: Boolean = false,
    val currentPage: Int = 0,
    val error: String? = null
)

@HiltViewModel
class ResearchDetailViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private var qualitativeListener: ListenerRegistration? = null
    private var quantitativeListener: ListenerRegistration? = null

    private val _uiState = MutableStateFlow(ResearchDetailUiState(
        currentPage = savedStateHandle.get<Int>("current_page") ?: 0
    ))
    val uiState = _uiState.asStateFlow()

    private val researchId: String? = savedStateHandle.get<String>(AppDestination.ResearchDetails.researchIdArg)
    // To keep track of whether we've already incremented the count for this session
    private var hasIncrementedCount = false

    init {
        if (!researchId.isNullOrBlank()) {
            setupListeners()
            // Load persistent page position SYNCHRONOUSLY
            val lastPage = UserPrefs.getLastPageSync(appContext, researchId)
            _uiState.update { it.copy(currentPage = lastPage) }
        } else {
            _uiState.update { it.copy(isDetailsLoading = false, error = "Research ID is missing.") }
        }
    }

    private fun setupListeners() {
        if (researchId == null) return
        _uiState.update { it.copy(isDetailsLoading = true, error = null) }
        qualitativeListener?.remove()
        quantitativeListener?.remove()

        qualitativeListener = ResearchRepository.observeQualitativeById(researchId) { snapshot, e ->
                if (snapshot != null && snapshot.exists()) {
                    handleSnapshot(snapshot, e, ResearchType.QUALITATIVE)
                }
            }

        quantitativeListener = ResearchRepository.observeQuantitativeById(researchId) { snapshot, e ->
                if (snapshot != null && snapshot.exists()) {
                    handleSnapshot(snapshot, e, ResearchType.QUANTITATIVE)
                }
            }
    }

    private fun handleSnapshot(snapshot: com.google.firebase.firestore.DocumentSnapshot?, e: FirebaseFirestoreException?, type: ResearchType) {
        if (e != null) {
            val errorMessage = if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE) {
                "You are offline. Showing cached details."
            } else { "Error loading details: ${e.message}" }
            if (_uiState.value.researchItem == null) {
                _uiState.update { it.copy(isDetailsLoading = false, error = errorMessage) }
            }
            return
        }

        if (snapshot != null && snapshot.exists()) {
            val research = snapshot.toObject(ResearchItem::class.java)?.copy(
                id = snapshot.id,
                type = type
            )
            _uiState.update { it.copy(isDetailsLoading = false, researchItem = research, error = null) }
            
            // Auto-load if cached
            if (research?.file_link != null && _uiState.value.pdfFilePath == null) {
                if (FileCache.isFileCached(appContext, research.file_link)) {
                    loadPdf(true)
                }
            }
        }
    }

    private fun incrementDownloadCount() {
        // Only run if we haven't already done it for this screen instance
        if (hasIncrementedCount || researchId == null) return
        hasIncrementedCount = true // Set flag to true immediately

        viewModelScope.launch {
            try {
                ResearchRepository.incrementDownloadCount(researchId)
            } catch (e: Exception) {
                Log.w("ResearchDetailVM", "Failed to increment download count", e)
            }
        }
    }

    override fun onCleared() {
        qualitativeListener?.remove()
        quantitativeListener?.remove()
        super.onCleared()
    }

    fun loadPdf(isOnline: Boolean) {
        val fileId = _uiState.value.researchItem?.file_link
        if (fileId.isNullOrBlank()) {
            _uiState.update { it.copy(error = "No PDF file associated with this research.") }
            return
        }
        if (_uiState.value.pdfFilePath != null) return

        _uiState.update { it.copy(isPdfLoading = true, error = null) }
        viewModelScope.launch {
            try {
                // --- THIS IS THE FIX ---
                // Increment the download count only when this function is called.
                incrementDownloadCount()

                val resolvedFile = PdfRepository.getOrDownloadPdfFile(appContext, fileId, isOnline)
                _uiState.update { it.copy(isPdfLoading = false, pdfFilePath = resolvedFile.absolutePath, error = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isPdfLoading = false, error = "Could not load PDF: ${e.message}") }
            }
        }
    }

    // This function is still needed to switch back from the PDF viewer
    fun clearPdfBytes() {
        _uiState.update { it.copy(pdfFilePath = null) }
    }

    fun updateCurrentPage(page: Int) {
        if (_uiState.value.currentPage == page) return
        _uiState.update { it.copy(currentPage = page) }
        savedStateHandle["current_page"] = page
        
        // Save to SharedPrefs for persistence across navigation
        if (researchId != null) {
            UserPrefs.saveLastPageSync(appContext, researchId, page)
        }
    }
}