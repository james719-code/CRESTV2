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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val BUCKET_ID = "686a262b0024b8e10a35"

data class ResearchDetailUiState(
    val researchItem: ResearchItem? = null,
    val pdfBytes: ByteArray? = null,
    val isDetailsLoading: Boolean = true,
    val isPdfLoading: Boolean = false,
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
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val firestore = FirebaseClient.firestore
    private val appwriteStorage = AppwriteClient.storage

    private var researchListener: ListenerRegistration? = null

    private val _uiState = MutableStateFlow(ResearchDetailUiState())
    val uiState = _uiState.asStateFlow()

    private val researchId: String = savedStateHandle.get<String>(AppDestination.ResearchDetails.researchIdArg)!!
    // Keep a reference to the doc for the download counter
    private var researchDocRef: DocumentReference? = null

    init {
        if (researchId.isNotBlank()) {
            setupResearchListener()
        } else {
            _uiState.update { it.copy(isDetailsLoading = false, error = "Research ID is missing.") }
        }
    }

    private fun setupResearchListener() {
        _uiState.update { it.copy(isDetailsLoading = true, error = null) }
        researchListener?.remove()

        val qualitativeRef = firestore.collection("researches/research_details/qualitative").document(researchId)
        qualitativeRef.get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                researchDocRef = qualitativeRef // Store reference
                attachListener(qualitativeRef, ResearchType.QUALITATIVE)
            } else {
                val quantitativeRef = firestore.collection("researches/research_details/quantitative").document(researchId)
                researchDocRef = quantitativeRef // Store reference
                attachListener(quantitativeRef, ResearchType.QUANTITATIVE)
            }
        }.addOnFailureListener { e ->
            _uiState.update { it.copy(isDetailsLoading = false, error = "Failed to find research: ${e.message}") }
        }
    }

    private fun attachListener(docRef: DocumentReference, type: ResearchType) {
        researchListener = docRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                val errorMessage = if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE) {
                    "You are offline. Showing cached details."
                } else { "Error loading details: ${e.message}" }
                _uiState.update { it.copy(isDetailsLoading = false, error = errorMessage) }
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val research = snapshot.toObject(ResearchItem::class.java)?.copy(
                    id = snapshot.id,
                    type = type
                )
                _uiState.update { it.copy(isDetailsLoading = false, researchItem = research, error = null) }
                // We no longer increment the counter on view
            } else {
                _uiState.update { it.copy(isDetailsLoading = false, error = "Research not found.") }
            }
        }
    }

    // --- NEW: Function to increment the download counter ---
    private fun incrementDownloadCount() {
        researchDocRef?.let { docRef ->
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val newCount = (snapshot.getLong("downloads") ?: 0L) + 1
                transaction.update(docRef, "downloads", newCount)
                null
            }.addOnFailureListener { e ->
                Log.w("ResearchDetailVM", "Failed to increment download count", e)
            }
        }
    }

    override fun onCleared() {
        researchListener?.remove()
        super.onCleared()
    }

    fun loadPdf(isOnline: Boolean) {
        val fileId = _uiState.value.researchItem?.file_link
        if (fileId.isNullOrBlank() || researchId.isBlank()) { /* ... */ return }
        if (_uiState.value.pdfBytes != null) return

        _uiState.update { it.copy(isPdfLoading = true, error = null) }
        viewModelScope.launch {
            try {
                // --- MODIFIED: Increment counter before fetching file ---
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
}