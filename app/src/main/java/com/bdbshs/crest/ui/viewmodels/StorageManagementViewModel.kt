package com.bdbshs.crest.ui.viewmodels

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bdbshs.crest.data.CachedFileInfo
import com.bdbshs.crest.data.FileCache
import com.bdbshs.crest.data.FirebaseClient
import com.google.firebase.firestore.QueryDocumentSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Represents a cached research item with its associated metadata.
 */
@Immutable
data class CachedResearchItem(
    val fileId: String,
    val title: String,
    val strand: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val isSelected: Boolean = false
) {
    val formattedSize: String get() = FileCache.formatSize(sizeBytes)
}

/**
 * UI state for the Storage Management screen.
 */
@Immutable
data class StorageUiState(
    val isLoading: Boolean = true,
    val pdfCacheSize: Long = 0L,
    val totalAppCacheSize: Long = 0L,
    val cachedResearches: List<CachedResearchItem> = emptyList(),
    val selectedCount: Int = 0,
    val selectedSize: Long = 0L,
    val isDeleting: Boolean = false,
    val showConfirmDialog: Boolean = false,
    val confirmDialogType: ConfirmDialogType = ConfirmDialogType.DELETE_SELECTED,
    val message: String? = null
)

enum class ConfirmDialogType {
    DELETE_SELECTED,
    CLEAR_PDF_CACHE,
    CLEAR_ALL_CACHE
}

/**
 * ViewModel for managing storage and cached files.
 * Uses AndroidViewModel to access application context.
 */
class StorageManagementViewModel(application: Application) : AndroidViewModel(application) {

    private val context get() = getApplication<Application>()
    private val firestore = FirebaseClient.firestore

    private val _uiState = MutableStateFlow(StorageUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadStorageData()
    }

    /**
     * Loads all storage data including cache sizes and cached research information.
     */
    fun loadStorageData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            withContext(Dispatchers.IO) {
                // Get cache sizes
                val pdfSize = FileCache.getTotalCacheSize(context)
                val totalSize = FileCache.getTotalAppCacheSize(context)

                // Get cached files
                val cachedFiles = FileCache.getAllCachedFiles(context)

                // Fetch research titles from Firestore for cached files
                val cachedResearches = mapCachedFilesToResearches(cachedFiles)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        pdfCacheSize = pdfSize,
                        totalAppCacheSize = totalSize,
                        cachedResearches = cachedResearches,
                        selectedCount = 0,
                        selectedSize = 0L
                    )
                }
            }
        }
    }

    /**
     * Maps cached files to research items by looking up metadata in Firestore.
     */
    private suspend fun mapCachedFilesToResearches(cachedFiles: List<CachedFileInfo>): List<CachedResearchItem> {
        if (cachedFiles.isEmpty()) return emptyList()

        val fileIds = cachedFiles.map { it.fileId }.toSet()
        val researchMap = mutableMapOf<String, Pair<String, String>>() // fileId -> (title, strand)

        try {
            // Search both qualitative and quantitative collections
            val qualDocs = firestore.collection("researches/research_details/qualitative")
                .get().await()
            val quantDocs = firestore.collection("researches/research_details/quantitative")
                .get().await()

            (qualDocs.documents + quantDocs.documents).forEach { doc ->
                val fileLink = doc.getString("file_link") ?: ""
                if (fileLink in fileIds) {
                    researchMap[fileLink] = Pair(
                        doc.getString("title") ?: "Unknown Research",
                        doc.getString("strand") ?: ""
                    )
                }
            }
        } catch (e: Exception) {
            // Continue with just file IDs if Firestore lookup fails
            e.printStackTrace()
        }

        return cachedFiles.map { cachedFile ->
            val (title, strand) = researchMap[cachedFile.fileId] ?: Pair("Cached File", "")
            CachedResearchItem(
                fileId = cachedFile.fileId,
                title = title,
                strand = strand,
                sizeBytes = cachedFile.sizeBytes,
                lastModified = cachedFile.lastModified
            )
        }
    }

    /**
     * Toggles selection of a cached research item.
     */
    fun toggleSelection(fileId: String) {
        _uiState.update { state ->
            val updatedList = state.cachedResearches.map { item ->
                if (item.fileId == fileId) item.copy(isSelected = !item.isSelected) else item
            }
            val selectedItems = updatedList.filter { it.isSelected }
            state.copy(
                cachedResearches = updatedList,
                selectedCount = selectedItems.size,
                selectedSize = selectedItems.sumOf { it.sizeBytes }
            )
        }
    }

    /**
     * Selects or deselects all items.
     */
    fun toggleSelectAll() {
        _uiState.update { state ->
            val allSelected = state.cachedResearches.all { it.isSelected }
            val updatedList = state.cachedResearches.map { it.copy(isSelected = !allSelected) }
            val selectedItems = updatedList.filter { it.isSelected }
            state.copy(
                cachedResearches = updatedList,
                selectedCount = selectedItems.size,
                selectedSize = selectedItems.sumOf { it.sizeBytes }
            )
        }
    }

    /**
     * Shows confirmation dialog for deleting selected items.
     */
    fun showDeleteSelectedDialog() {
        if (_uiState.value.selectedCount > 0) {
            _uiState.update {
                it.copy(showConfirmDialog = true, confirmDialogType = ConfirmDialogType.DELETE_SELECTED)
            }
        }
    }

    /**
     * Shows confirmation dialog for clearing PDF cache.
     */
    fun showClearPdfCacheDialog() {
        _uiState.update {
            it.copy(showConfirmDialog = true, confirmDialogType = ConfirmDialogType.CLEAR_PDF_CACHE)
        }
    }

    /**
     * Shows confirmation dialog for clearing all app cache.
     */
    fun showClearAllCacheDialog() {
        _uiState.update {
            it.copy(showConfirmDialog = true, confirmDialogType = ConfirmDialogType.CLEAR_ALL_CACHE)
        }
    }

    /**
     * Dismisses the confirmation dialog.
     */
    fun dismissDialog() {
        _uiState.update { it.copy(showConfirmDialog = false) }
    }

    /**
     * Confirms and executes the pending delete action.
     */
    fun confirmAction() {
        when (_uiState.value.confirmDialogType) {
            ConfirmDialogType.DELETE_SELECTED -> deleteSelectedItems()
            ConfirmDialogType.CLEAR_PDF_CACHE -> clearPdfCache()
            ConfirmDialogType.CLEAR_ALL_CACHE -> clearAllCache()
        }
    }

    /**
     * Deletes all selected cached items.
     */
    private fun deleteSelectedItems() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, showConfirmDialog = false) }

            withContext(Dispatchers.IO) {
                val selectedIds = _uiState.value.cachedResearches
                    .filter { it.isSelected }
                    .map { it.fileId }

                val deletedCount = FileCache.deleteFiles(context, selectedIds)

                // Reload data after deletion
                val pdfSize = FileCache.getTotalCacheSize(context)
                val totalSize = FileCache.getTotalAppCacheSize(context)
                val cachedFiles = FileCache.getAllCachedFiles(context)
                val cachedResearches = mapCachedFilesToResearches(cachedFiles)

                _uiState.update {
                    it.copy(
                        isDeleting = false,
                        pdfCacheSize = pdfSize,
                        totalAppCacheSize = totalSize,
                        cachedResearches = cachedResearches,
                        selectedCount = 0,
                        selectedSize = 0L,
                        message = "$deletedCount file(s) deleted"
                    )
                }
            }
        }
    }

    /**
     * Clears all PDF cache files.
     */
    private fun clearPdfCache() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, showConfirmDialog = false) }

            withContext(Dispatchers.IO) {
                val deletedCount = FileCache.clearAllCache(context)
                val pdfSize = FileCache.getTotalCacheSize(context)
                val totalSize = FileCache.getTotalAppCacheSize(context)

                _uiState.update {
                    it.copy(
                        isDeleting = false,
                        pdfCacheSize = pdfSize,
                        totalAppCacheSize = totalSize,
                        cachedResearches = emptyList(),
                        selectedCount = 0,
                        selectedSize = 0L,
                        message = "$deletedCount PDF file(s) cleared"
                    )
                }
            }
        }
    }

    /**
     * Clears all app cache including system cache.
     */
    private fun clearAllCache() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, showConfirmDialog = false) }

            withContext(Dispatchers.IO) {
                FileCache.clearAllAppCache(context)
                val pdfSize = FileCache.getTotalCacheSize(context)
                val totalSize = FileCache.getTotalAppCacheSize(context)

                _uiState.update {
                    it.copy(
                        isDeleting = false,
                        pdfCacheSize = pdfSize,
                        totalAppCacheSize = totalSize,
                        cachedResearches = emptyList(),
                        selectedCount = 0,
                        selectedSize = 0L,
                        message = "All cache cleared"
                    )
                }
            }
        }
    }

    /**
     * Clears the message after it's been shown.
     */
    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
