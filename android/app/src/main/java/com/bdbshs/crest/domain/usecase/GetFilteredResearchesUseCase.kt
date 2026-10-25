package com.bdbshs.crest.domain.usecase

import com.bdbshs.crest.ui.viewmodels.ResearchItem
import com.bdbshs.crest.ui.viewmodels.ResearchType
import com.bdbshs.crest.ui.viewmodels.SortOption
import com.bdbshs.crest.ui.viewmodels.Strand
import java.util.Locale
import javax.inject.Inject

class GetFilteredResearchesUseCase @Inject constructor() {

    operator fun invoke(
        researches: List<ResearchItem>,
        query: String,
        selectedType: ResearchType?,
        strands: List<Strand>,
        sortOption: SortOption,
        favoriteResearchIds: Set<String>,
        showFavoritesOnly: Boolean
    ): List<ResearchItem> {
        val normalizedQuery = query.trim()
        val selectedStrands = strands
            .asSequence()
            .filter { it.isSelected }
            .map { it.name }
            .toSet()

        val filtered = researches.filter { researchItem ->
            val queryMatch = if (normalizedQuery.isBlank()) {
                true
            } else {
                researchItem.title.contains(normalizedQuery, ignoreCase = true) ||
                        researchItem.members.any { it.contains(normalizedQuery, ignoreCase = true) }
            }
            
            val typeMatch = selectedType == null || researchItem.type == selectedType
            
            val strandMatch = selectedStrands.isEmpty() || researchItem.strand in selectedStrands
            
            val favoriteMatch = !showFavoritesOnly || researchItem.id in favoriteResearchIds
            
            queryMatch && typeMatch && strandMatch && favoriteMatch
        }

        return when (sortOption) {
            SortOption.DateNewest -> filtered.sortedWith(
                compareByDescending<ResearchItem> { it.createdAt }
                    .thenBy { it.title.sortKey() }
            )
            SortOption.DateOldest -> filtered.sortedWith(
                compareBy<ResearchItem> { it.createdAt }
                    .thenBy { it.title.sortKey() }
            )
            SortOption.TitleAZ -> filtered.sortedBy { it.title.sortKey() }
            SortOption.TitleZA -> filtered.sortedByDescending { it.title.sortKey() }
        }
    }

    private fun String.sortKey(): String = trim().lowercase(Locale.ROOT)
}
