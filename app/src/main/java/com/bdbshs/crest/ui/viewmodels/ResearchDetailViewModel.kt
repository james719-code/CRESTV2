package com.bdbshs.crest.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bdbshs.crest.data.AppwriteClient
import com.bdbshs.crest.data.FileCache
import com.bdbshs.crest.data.FirebaseClient
import com.bdbshs.crest.navigation.AppDestination
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.bdbshs.crest.data.UserPrefs

private const val BUCKET_ID = "686a262b0024b8e10a35"

// DATA CLASS is unchanged
data class ResearchDetailUiState(
    val researchItem: ResearchItem? = null,
    val pdfBytes: ByteArray? = null,
    val isDetailsLoading: Boolean = true,
    val isPdfLoading: Boolean = false,
    val currentPage: Int = 0,
    val error: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ResearchDetailUiState
        if (researchItem != other.researchItem) return false
        if (pdfBytes != null) {
            if (other.pdfBytes == null) return false
            if (!pdfBytes.contentEquals(other.pdfBytes)) return false
        } else if (other.pdfBytes != null) return false
        if (isDetailsLoading != other.isDetailsLoading) return false
        if (isPdfLoading != other.isPdfLoading) return false
        if (error != other.error) return false
        return true
    }

    override fun hashCode(): Int {
        var result = researchItem?.hashCode() ?: 0
        result = 31 * result + (pdfBytes?.contentHashCode() ?: 0)
        result = 31 * result + isDetailsLoading.hashCode()
        result = 31 * result + isPdfLoading.hashCode()
        result = 31 * result + (error?.hashCode() ?: 0)
        return result
    }
}

class ResearchDetailViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val firestore = FirebaseClient.firestore
    private val appwriteStorage = AppwriteClient.storage
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
            val lastPage = UserPrefs.getLastPageSync(getApplication(), researchId)
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

        qualitativeListener = firestore.collection("researches/research_details/qualitative").document(researchId)
            .addSnapshotListener { snapshot, e ->
                if (snapshot != null && snapshot.exists()) {
                    handleSnapshot(snapshot, e, ResearchType.QUALITATIVE)
                }
            }

        quantitativeListener = firestore.collection("researches/research_details/quantitative").document(researchId)
            .addSnapshotListener { snapshot, e ->
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
            if (research?.file_link != null && _uiState.value.pdfBytes == null) {
                if (FileCache.isFileCached(getApplication(), research.file_link)) {
                    loadPdf(true)
                }
            }
        }
    }

    private fun incrementDownloadCount() {
        // Only run if we haven't already done it for this screen instance
        if (hasIncrementedCount || researchId == null) return
        hasIncrementedCount = true // Set flag to true immediately

        // Find the correct document reference again to be safe
        val qualitativeRef = firestore.collection("researches/research_details/qualitative").document(researchId)
        qualitativeRef.get().addOnSuccessListener { doc ->
            val refToUpdate = if (doc.exists()) {
                qualitativeRef
            } else {
                firestore.collection("researches/research_details/quantitative").document(researchId)
            }
            // Fire-and-forget transaction
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(refToUpdate)
                val newCount = (snapshot.getLong("downloads") ?: 0L) + 1
                transaction.update(refToUpdate, "downloads", newCount)
                null
            }.addOnFailureListener { e ->
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
        if (_uiState.value.pdfBytes != null) return

        _uiState.update { it.copy(isPdfLoading = true, error = null) }
        viewModelScope.launch {
            try {
                // --- THIS IS THE FIX ---
                // Increment the download count only when this function is called.
                incrementDownloadCount()

                val cachedFile = FileCache.getFile(getApplication(), fileId)
                if (cachedFile != null) {
                    _uiState.update { it.copy(isPdfLoading = false, pdfBytes = cachedFile) }
                    return@launch
                }
                if (!isOnline) {
                    throw Exception("File not cached. An internet connection is required to download it.")
                }
                val fileBytes = appwriteStorage.getFileDownload(
                    bucketId = BUCKET_ID,
                    fileId = fileId
                )
                FileCache.saveFile(getApplication(), fileId, fileBytes)
                _uiState.update { it.copy(isPdfLoading = false, pdfBytes = fileBytes) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isPdfLoading = false, error = "Could not load PDF: ${e.message}") }
            }
        }
    }

    // This function is still needed to switch back from the PDF viewer
    fun clearPdfBytes() {
        _uiState.update { it.copy(pdfBytes = null) }
    }

    fun updateCurrentPage(page: Int) {
        if (_uiState.value.currentPage == page) return
        _uiState.update { it.copy(currentPage = page) }
        savedStateHandle["current_page"] = page
        
        // Save to SharedPrefs for persistence across navigation
        if (researchId != null) {
            UserPrefs.saveLastPageSync(getApplication(), researchId, page)
        }
    }
}