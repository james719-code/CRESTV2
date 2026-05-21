package com.bdbshs.crest.domain.usecase

import com.bdbshs.crest.ui.viewmodels.ResearchItem
import com.bdbshs.crest.ui.viewmodels.ResearchType
import com.bdbshs.crest.ui.viewmodels.SortOption
import com.bdbshs.crest.ui.viewmodels.Strand
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
        val filtered = researches.filter { researchItem ->
            val queryMatch = if (query.isBlank()) {
                true
            } else {
                researchItem.title.contains(query, ignoreCase = true) ||
                        researchItem.members.any { it.contains(query, ignoreCase = true) }
            }
            
            val typeMatch = selectedType == null || researchItem.type == selectedType
            
            val selectedStrands = strands.filter { it.isSelected }.map { it.name }
            val strandMatch = selectedStrands.isEmpty() || researchItem.strand in selectedStrands
            
            val favoriteMatch = !showFavoritesOnly || researchItem.id in favoriteResearchIds
            
            queryMatch && typeMatch && strandMatch && favoriteMatch
        }

        return when (sortOption) {
            SortOption.DateNewest -> filtered.sortedByDescending { it.createdAt }
            SortOption.DateOldest -> filtered.sortedBy { it.createdAt }
            SortOption.TitleAZ -> filtered.sortedBy { it.title }
            SortOption.TitleZA -> filtered.sortedByDescending { it.title }
        }
    }
}
