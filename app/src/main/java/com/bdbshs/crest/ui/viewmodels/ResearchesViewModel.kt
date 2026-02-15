package com.bdbshs.crest.ui.viewmodels

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bdbshs.crest.data.repository.ResearchRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// --- Data classes and enums ---
@Immutable
data class ResearchItem(
    val id: String = "",
    val title: String = "",
    val members: List<String> = emptyList(),
    val strand: String = "",
    val unfinished: Boolean = false,
    val downloads: Int = 0, // <-- Renamed from 'views'
    val type: ResearchType = ResearchType.QUALITATIVE,
    val file_link: String = "",
    val createdAt: Long = 0L
)

data class Strand(val name: String, val isSelected: Boolean = false)
enum class ResearchType { QUALITATIVE, QUANTITATIVE }
enum class SortOption(val displayName: String) {
    DateNewest("Date (Newest)"),
    DateOldest("Date (Oldest)"),
    TitleAZ("Title (A-Z)"),
    TitleZA("Title (Z-A)")
}

data class ResearchesUiState(
    val currentUserRole: UserType? = null,
    val searchQuery: String = "",
    val isFilterDialogVisible: Boolean = false,
    val qualitativeResearches: List<ResearchItem> = emptyList(),
    val quantitativeResearches: List<ResearchItem> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val selectedResearchType: ResearchType? = null,
    val strands: List<Strand> = listOf(
        Strand("STEM"), Strand("HUMSS"), Strand("ABM"), Strand("TVL"), Strand("GAS")
    ),
    val selectedSortOption: SortOption = SortOption.DateNewest,
    val error: String? = null,
    val isActionDialogVisible: Boolean = false,
    val selectedResearchForAction: ResearchItem? = null,
    val isDeleting: Boolean = false,
    val isOnline: Boolean = true
)

@HiltViewModel
class ResearchesViewModel @Inject constructor(
    private val researchRepository: ResearchRepository
) : ViewModel() {

    private var qualitativeListener: ListenerRegistration? = null
    private var quantitativeListener: ListenerRegistration? = null

    private val _searchQuery = MutableStateFlow("")
    private val _uiState = MutableStateFlow(ResearchesUiState())
    val uiState = _uiState.asStateFlow()

    private val allResearches: StateFlow<List<ResearchItem>> = _uiState.map {
        it.qualitativeResearches + it.quantitativeResearches
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(FlowPreview::class)
    val filteredAndSortedResearches: StateFlow<List<ResearchItem>> = combine(
        allResearches,
        _uiState.map { Triple(it.selectedResearchType, it.strands, it.selectedSortOption) }.distinctUntilChanged(),
        _searchQuery.debounce(300L)
    ) { researches, filters, query ->
        val (selectedType, strands, sortOption) = filters
        val filtered = researches.filter { researchItem ->
            val queryMatch = if (query.isBlank()) true else researchItem.title.contains(query, ignoreCase = true) || researchItem.members.any { it.contains(query, ignoreCase = true) }
            val typeMatch = selectedType == null || researchItem.type == selectedType
            val selectedStrands = strands.filter { it.isSelected }.map { it.name }
            val strandMatch = selectedStrands.isEmpty() || researchItem.strand in selectedStrands
            queryMatch && typeMatch && strandMatch
        }
        when (sortOption) {
            SortOption.DateNewest -> filtered.sortedByDescending { it.createdAt }
            SortOption.DateOldest -> filtered.sortedBy { it.createdAt }
            SortOption.TitleAZ -> filtered.sortedBy { it.title }
            SortOption.TitleZA -> filtered.sortedByDescending { it.title }
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        setupListeners()
    }

    private fun setupListeners() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        qualitativeListener?.remove()
        quantitativeListener?.remove()

        qualitativeListener = researchRepository.observeQualitative {
            snap, e -> handleSnapshot(snap, e as? FirebaseFirestoreException, ResearchType.QUALITATIVE)
        }

        quantitativeListener = researchRepository.observeQuantitative {
            snap, e -> handleSnapshot(snap, e as? FirebaseFirestoreException, ResearchType.QUANTITATIVE)
        }
    }

    private fun handleSnapshot(snap: QuerySnapshot?, e: FirebaseFirestoreException?, type: ResearchType) {
        if (e != null) {
            val errorMessage = if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE) {
                "You are offline. Showing cached data."
            } else { "Error: ${e.message}" }
            _uiState.update { it.copy(isLoading = false, isRefreshing = false, error = errorMessage) }
            return
        }

        val list = snap?.documents?.mapNotNull { mapDocumentToResearchItem(it as QueryDocumentSnapshot, type) } ?: emptyList()
        if (type == ResearchType.QUALITATIVE) {
            _uiState.update { it.copy(qualitativeResearches = list, isLoading = false, isRefreshing = false) }
        } else {
            _uiState.update { it.copy(quantitativeResearches = list, isLoading = false, isRefreshing = false) }
        }
    }

    override fun onCleared() {
        qualitativeListener?.remove()
        quantitativeListener?.remove()
        super.onCleared()
    }

    fun onRefresh() {
        setupListeners()
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        _searchQuery.value = query
    }

    fun showFilterDialog() = _uiState.update { it.copy(isFilterDialogVisible = true) }
    fun dismissFilterDialog() = _uiState.update { it.copy(isFilterDialogVisible = false) }

    fun onResearchTypeSelected(type: ResearchType) {
        _uiState.update { current ->
            val newType = if (current.selectedResearchType == type) null else type
            current.copy(selectedResearchType = newType)
        }
    }

    fun onStrandCheckedChange(strandName: String, isChecked: Boolean) {
        _uiState.update { current ->
            current.copy(strands = current.strands.map { if (it.name == strandName) it.copy(isSelected = isChecked) else it })
        }
    }

    fun applyFilters() = dismissFilterDialog()
    fun onSortOptionSelected(option: SortOption) = _uiState.update { it.copy(selectedSortOption = option) }

    fun resetFilters() {
        _searchQuery.value = ""
        _uiState.update { currentState ->
            currentState.copy(
                searchQuery = "",
                selectedResearchType = null,
                strands = currentState.strands.map { it.copy(isSelected = false) },
                selectedSortOption = SortOption.DateNewest
            )
        }
    }

    fun onResearchLongPressed(research: ResearchItem) {
        if (_uiState.value.currentUserRole == UserType.TEACHER) {
            _uiState.update { it.copy(isActionDialogVisible = true, selectedResearchForAction = research) }
        }
    }

    fun dismissActionDialog() = _uiState.update { it.copy(isActionDialogVisible = false, selectedResearchForAction = null) }

    fun setUserRole(role: UserType?) {
        _uiState.update { it.copy(currentUserRole = role) }
    }

    fun onDeleteConfirmed() {
        val research = _uiState.value.selectedResearchForAction ?: return
        _uiState.update { it.copy(isDeleting = true) }
        viewModelScope.launch {
            try {
                researchRepository.deleteResearchDocument(research.type.name.lowercase(), research.id)
                if (research.file_link.isNotBlank()) {
                    researchRepository.deleteResearchFile(research.file_link)
                }
                _uiState.update { current ->
                    current.copy(isDeleting = false, isActionDialogVisible = false, selectedResearchForAction = null)
                }
            } catch (ex: Exception) {
                _uiState.update { it.copy(isDeleting = false, error = "Delete failed. Check network.") }
            }
        }
    }

    private fun mapDocumentToResearchItem(doc: QueryDocumentSnapshot, type: ResearchType): ResearchItem? {
        return try {
            val members = (doc.get("members") as? List<*>)
                ?.mapNotNull { it as? String }
                ?: emptyList()

            ResearchItem(
                id = doc.id,
                title = doc.getString("title") ?: "No Title",
                members = members,
                strand = doc.getString("strand") ?: "",
                unfinished = doc.getBoolean("unfinished") ?: false,
                downloads = doc.getLong("downloads")?.toInt() ?: 0, // <-- Renamed from 'views'
                file_link = doc.getString("file_link") ?: "",
                createdAt = doc.getLong("createdAt") ?: 0L,
                type = type
            )
        } catch(e: Exception) {
            Log.e("ResearchesViewModel", "Failed to map document ${doc.id}", e)
            null
        }
    }
}