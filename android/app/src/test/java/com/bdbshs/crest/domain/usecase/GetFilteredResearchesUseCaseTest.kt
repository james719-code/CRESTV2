package com.bdbshs.crest.domain.usecase

import com.bdbshs.crest.ui.viewmodels.ResearchItem
import com.bdbshs.crest.ui.viewmodels.ResearchType
import com.bdbshs.crest.ui.viewmodels.SortOption
import com.bdbshs.crest.ui.viewmodels.Strand
import org.junit.Assert.assertEquals
import org.junit.Test

class GetFilteredResearchesUseCaseTest {

    private val useCase = GetFilteredResearchesUseCase()

    @Test
    fun `filters by trimmed query selected strand type and favorites`() {
        val researches = listOf(
            research(id = "1", title = "STEM Solar Study", strand = "STEM", type = ResearchType.QUALITATIVE),
            research(id = "2", title = "ABM Budget", strand = "ABM", type = ResearchType.QUALITATIVE),
            research(id = "3", title = "STEM Robotics", strand = "STEM", type = ResearchType.QUANTITATIVE)
        )

        val result = useCase(
            researches = researches,
            query = " solar ",
            selectedType = ResearchType.QUALITATIVE,
            strands = listOf(Strand("STEM", isSelected = true)),
            sortOption = SortOption.DateNewest,
            favoriteResearchIds = setOf("1", "3"),
            showFavoritesOnly = true
        )

        assertEquals(listOf("1"), result.map { it.id })
    }

    @Test
    fun `sorts titles without case sensitivity`() {
        val researches = listOf(
            research(id = "1", title = "banana"),
            research(id = "2", title = "Apple"),
            research(id = "3", title = "cherry")
        )

        val result = useCase(
            researches = researches,
            query = "",
            selectedType = null,
            strands = emptyList(),
            sortOption = SortOption.TitleAZ,
            favoriteResearchIds = emptySet(),
            showFavoritesOnly = false
        )

        assertEquals(listOf("2", "1", "3"), result.map { it.id })
    }

    @Test
    fun `date sorting uses title as deterministic tiebreaker`() {
        val researches = listOf(
            research(id = "1", title = "Beta", createdAt = 2000L),
            research(id = "2", title = "Alpha", createdAt = 2000L),
            research(id = "3", title = "Gamma", createdAt = 1000L)
        )

        val result = useCase(
            researches = researches,
            query = "",
            selectedType = null,
            strands = emptyList(),
            sortOption = SortOption.DateNewest,
            favoriteResearchIds = emptySet(),
            showFavoritesOnly = false
        )

        assertEquals(listOf("2", "1", "3"), result.map { it.id })
    }

    private fun research(
        id: String,
        title: String,
        strand: String = "STEM",
        type: ResearchType = ResearchType.QUALITATIVE,
        createdAt: Long = 0L
    ): ResearchItem {
        return ResearchItem(
            id = id,
            title = title,
            strand = strand,
            type = type,
            createdAt = createdAt,
            members = listOf("Author")
        )
    }
}

