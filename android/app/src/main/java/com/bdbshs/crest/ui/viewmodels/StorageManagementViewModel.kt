package com.bdbshs.crest.ui.viewmodels

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bdbshs.crest.data.FileCache
import com.bdbshs.crest.data.repository.CachedResearchRecord
import com.bdbshs.crest.data.repository.StorageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
 * Uses injected application context for cache operations.
 */
@HiltViewModel
class StorageManagementViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val storageRepository: StorageRepository
) : ViewModel() {

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
                val snapshot = storageRepository.loadStorageSnapshot(context)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        pdfCacheSize = snapshot.pdfCacheSize,
                        totalAppCacheSize = snapshot.totalAppCacheSize,
                        cachedResearches = snapshot.cachedResearches.map { record -> record.toUiCachedItem() },
                        selectedCount = 0,
                        selectedSize = 0L
                    )
                }
            }
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

                val deletedCount = storageRepository.deleteSelectedFiles(context, selectedIds)
                val snapshot = storageRepository.loadStorageSnapshot(context)

                _uiState.update {
                    it.copy(
                        isDeleting = false,
                        pdfCacheSize = snapshot.pdfCacheSize,
                        totalAppCacheSize = snapshot.totalAppCacheSize,
                        cachedResearches = snapshot.cachedResearches.map { record -> record.toUiCachedItem() },
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
                val deletedCount = storageRepository.clearPdfCache(context)
                val snapshot = storageRepository.loadStorageSnapshot(context)

                _uiState.update {
                    it.copy(
                        isDeleting = false,
                        pdfCacheSize = snapshot.pdfCacheSize,
                        totalAppCacheSize = snapshot.totalAppCacheSize,
                        cachedResearches = snapshot.cachedResearches.map { record -> record.toUiCachedItem() },
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
                storageRepository.clearAllCache(context)
                val snapshot = storageRepository.loadStorageSnapshot(context)

                _uiState.update {
                    it.copy(
                        isDeleting = false,
                        pdfCacheSize = snapshot.pdfCacheSize,
                        totalAppCacheSize = snapshot.totalAppCacheSize,
                        cachedResearches = snapshot.cachedResearches.map { record -> record.toUiCachedItem() },
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

private fun CachedResearchRecord.toUiCachedItem(): CachedResearchItem {
    return CachedResearchItem(
        fileId = fileId,
        title = title,
        strand = strand,
        sizeBytes = sizeBytes,
        lastModified = lastModified
    )
}
