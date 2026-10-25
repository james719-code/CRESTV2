package com.bdbshs.crest.ui.viewmodels

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bdbshs.crest.data.repository.FavoritesRepository
import com.bdbshs.crest.data.repository.ResearchRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
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
    val favoriteResearchIds: Set<String> = emptySet(),
    val favoriteUpdateInProgressIds: Set<String> = emptySet(),
    val showFavoritesOnly: Boolean = false,
    val error: String? = null,
    val isActionDialogVisible: Boolean = false,
    val selectedResearchForAction: ResearchItem? = null,
    val isDeleting: Boolean = false,
    val isOnline: Boolean = true,
    val isLoadingMore: Boolean = false,
    val hasMoreQualitative: Boolean = true,
    val hasMoreQuantitative: Boolean = true
)

@HiltViewModel
class ResearchesViewModel @Inject constructor(
    private val researchRepository: ResearchRepository,
    private val favoritesRepository: FavoritesRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private var qualitativeListener: ListenerRegistration? = null
    private var quantitativeListener: ListenerRegistration? = null
    private var favoritesListener: ListenerRegistration? = null

    private var lastQualitativeDoc: DocumentSnapshot? = null
    private var lastQuantitativeDoc: DocumentSnapshot? = null

    private val _searchQuery = MutableStateFlow("")
    private val _uiState = MutableStateFlow(ResearchesUiState())
    val uiState = _uiState.asStateFlow()

    private val allResearches: StateFlow<List<ResearchItem>> = _uiState.map {
        it.qualitativeResearches + it.quantitativeResearches
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(FlowPreview::class)
    val filteredAndSortedResearches: StateFlow<List<ResearchItem>> = combine(
        allResearches,
        _uiState.map {
            FavoriteFilterInput(
                selectedResearchType = it.selectedResearchType,
                strands = it.strands,
                selectedSortOption = it.selectedSortOption,
                favoriteResearchIds = it.favoriteResearchIds,
                showFavoritesOnly = it.showFavoritesOnly
            )
        }.distinctUntilChanged(),
        _searchQuery.debounce(300L)
    ) { researches, filters, query ->
        val selectedType = filters.selectedResearchType
        val strands = filters.strands
        val sortOption = filters.selectedSortOption
        val filtered = researches.filter { researchItem ->
            val queryMatch = if (query.isBlank()) true else researchItem.title.contains(query, ignoreCase = true) || researchItem.members.any { it.contains(query, ignoreCase = true) }
            val typeMatch = selectedType == null || researchItem.type == selectedType
            val selectedStrands = strands.filter { it.isSelected }.map { it.name }
            val strandMatch = selectedStrands.isEmpty() || researchItem.strand in selectedStrands
            val favoriteMatch = !filters.showFavoritesOnly || researchItem.id in filters.favoriteResearchIds
            queryMatch && typeMatch && strandMatch && favoriteMatch
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
        loadInitialData()
        setupFavoritesListener()
    }

    private fun loadInitialData() {
        _uiState.update { it.copy(isLoading = true, error = null, hasMoreQualitative = true, hasMoreQuantitative = true) }
        lastQualitativeDoc = null
        lastQuantitativeDoc = null
        
        viewModelScope.launch {
            try {
                val qualSnap = researchRepository.fetchQualitativeResearches(limit = 15L)
                val quantSnap = researchRepository.fetchQuantitativeResearches(limit = 15L)
                
                lastQualitativeDoc = qualSnap.documents.lastOrNull()
                lastQuantitativeDoc = quantSnap.documents.lastOrNull()
                
                val qualList = qualSnap.documents.mapNotNull { mapDocumentToResearchItem(it as QueryDocumentSnapshot, ResearchType.QUALITATIVE) }
                val quantList = quantSnap.documents.mapNotNull { mapDocumentToResearchItem(it as QueryDocumentSnapshot, ResearchType.QUANTITATIVE) }
                
                _uiState.update { 
                    it.copy(
                        qualitativeResearches = qualList,
                        quantitativeResearches = quantList,
                        isLoading = false,
                        isRefreshing = false,
                        hasMoreQualitative = qualSnap.documents.size >= 15,
                        hasMoreQuantitative = quantSnap.documents.size >= 15
                    ) 
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = "Failed to load researches: ${e.message}"
                    ) 
                }
            }
        }
    }
    
    fun loadMore() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || (!state.hasMoreQualitative && !state.hasMoreQuantitative)) return
        
        // Disable loadMore when searching for now to simplify logic, as doing a compound server-side search is complex
        if (state.searchQuery.isNotBlank() || state.showFavoritesOnly) return 

        _uiState.update { it.copy(isLoadingMore = true) }
        
        viewModelScope.launch {
            try {
                var newQual = emptyList<ResearchItem>()
                var newQuant = emptyList<ResearchItem>()
                var moreQual = state.hasMoreQualitative
                var moreQuant = state.hasMoreQuantitative

                if (state.hasMoreQualitative) {
                    val qualSnap = researchRepository.fetchQualitativeResearches(limit = 10L, startAfter = lastQualitativeDoc)
                    lastQualitativeDoc = qualSnap.documents.lastOrNull() ?: lastQualitativeDoc
                    newQual = qualSnap.documents.mapNotNull { mapDocumentToResearchItem(it as QueryDocumentSnapshot, ResearchType.QUALITATIVE) }
                    moreQual = qualSnap.documents.size >= 10
                }

                if (state.hasMoreQuantitative) {
                    val quantSnap = researchRepository.fetchQuantitativeResearches(limit = 10L, startAfter = lastQuantitativeDoc)
                    lastQuantitativeDoc = quantSnap.documents.lastOrNull() ?: lastQuantitativeDoc
                    newQuant = quantSnap.documents.mapNotNull { mapDocumentToResearchItem(it as QueryDocumentSnapshot, ResearchType.QUANTITATIVE) }
                    moreQuant = quantSnap.documents.size >= 10
                }
                
                _uiState.update { 
                    it.copy(
                        qualitativeResearches = it.qualitativeResearches + newQual,
                        quantitativeResearches = it.quantitativeResearches + newQuant,
                        isLoadingMore = false,
                        hasMoreQualitative = moreQual,
                        hasMoreQuantitative = moreQuant
                    )
                }

            } catch(e: Exception) {
                _uiState.update { it.copy(isLoadingMore = false, error = "Failed to load more: ${e.message}") }
            }
        }
    }

    override fun onCleared() {
        qualitativeListener?.remove()
        quantitativeListener?.remove()
        favoritesListener?.remove()
        super.onCleared()
    }

    fun onRefresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        loadInitialData()
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

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun resetFilters() {
        _searchQuery.value = ""
        _uiState.update { currentState ->
            currentState.copy(
                searchQuery = "",
                selectedResearchType = null,
                strands = currentState.strands.map { it.copy(isSelected = false) },
                selectedSortOption = SortOption.DateNewest,
                showFavoritesOnly = false
            )
        }
    }

    fun setFavoritesOnly(showFavoritesOnly: Boolean) {
        _uiState.update { it.copy(showFavoritesOnly = showFavoritesOnly) }
    }

    fun toggleResearchFavorite(research: ResearchItem) {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            _uiState.update { it.copy(error = "Sign in required to manage favorites.") }
            return
        }

        val isFavoriteNow = research.id in _uiState.value.favoriteResearchIds
        _uiState.update {
            it.copy(
                favoriteUpdateInProgressIds = it.favoriteUpdateInProgressIds + research.id
            )
        }

        viewModelScope.launch {
            try {
                favoritesRepository.setFavorite(
                    uid = uid,
                    researchId = research.id,
                    researchType = research.type,
                    isFavorite = !isFavoriteNow
                )
            } catch (exception: Exception) {
                _uiState.update { state ->
                    state.copy(error = "Could not update favorites. Please try again.")
                }
            } finally {
                _uiState.update {
                    it.copy(
                        favoriteUpdateInProgressIds = it.favoriteUpdateInProgressIds - research.id
                    )
                }
            }
        }
    }

    private fun setupFavoritesListener() {
        favoritesListener?.remove()

        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            _uiState.update { it.copy(favoriteResearchIds = emptySet()) }
            return
        }

        favoritesListener = favoritesRepository.observeFavoriteIds(uid) { ids, error ->
            if (error != null) {
                _uiState.update { it.copy(error = "Unable to load favorites right now.") }
                return@observeFavoriteIds
            }
            _uiState.update { it.copy(favoriteResearchIds = ids) }
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

private data class FavoriteFilterInput(
    val selectedResearchType: ResearchType?,
    val strands: List<Strand>,
    val selectedSortOption: SortOption,
    val favoriteResearchIds: Set<String>,
    val showFavoritesOnly: Boolean
)