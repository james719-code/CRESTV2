package com.bdbshs.crest.ui.viewmodels

import com.bdbshs.crest.data.repository.StudentResearchSummary
import com.bdbshs.crest.data.repository.StudentGroup
import com.bdbshs.crest.data.repository.TeacherRecentResearch
import org.junit.Assert.*
import org.junit.Test

class DashboardResearchMappingTest {
    @Test fun studentRecentPapersRetainAuthorsTypeAndDownloads() {
        val paper = StudentResearchSummary(
            "id", "A quantitative study", "STEM", 1234L,
            members = listOf("A", "B"), type = "quantitative", downloads = 17, unfinished = true
        ).toResearchItem()
        assertEquals(listOf("A", "B"), paper.members)
        assertEquals(ResearchType.QUANTITATIVE, paper.type)
        assertEquals(17, paper.downloads)
        assertTrue(paper.unfinished)
    }

    @Test fun teacherRecentPapersRetainAvailableMetadata() {
        val paper = TeacherRecentResearch(
            "id", "A paper", 1234L, "ABM", listOf("A"), "quantitative", 8, true
        ).toSimpleResearch()
        assertEquals("ABM", paper.strand)
        assertEquals(listOf("A"), paper.members)
        assertEquals(ResearchType.QUANTITATIVE, paper.type)
        assertEquals(8, paper.downloads)
        assertTrue(paper.unfinished)
    }

    @Test fun studentGroupRetainsSubmittedTitleAndFeedback() {
        val group = StudentGroup(
            "Study group", "STEM", "file", listOf("a"), true, "qualitative", false,
            listOf("Revise references"), "The submitted research title"
        ).toUiGroupDetails()
        assertEquals("The submitted research title", group.research_title)
        assertEquals(listOf("Revise references"), group.comments)
        assertTrue(group.uploaded)
        assertFalse(group.accepted_research)
    }
}
