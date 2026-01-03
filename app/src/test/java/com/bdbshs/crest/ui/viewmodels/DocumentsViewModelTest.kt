package com.bdbshs.crest.ui.viewmodels

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for DocumentsUiState and related data classes.
 * Tests UI state management and document sorting/filtering logic.
 */
class DocumentsViewModelTest {

    @Test
    fun `initial state has correct default values`() {
        // Given/When
        val state = DocumentsUiState()

        // Then
        assertTrue("isLoading should be true initially", state.isLoading)
        assertFalse("isRefreshing should be false initially", state.isRefreshing)
        assertTrue("allDocuments should be empty initially", state.allDocuments.isEmpty())
        assertEquals("searchQuery should be empty", "", state.searchQuery)
        assertEquals("default sort should be DateNewest", DocumentSortOption.DateNewest, state.selectedSortOption)
    }

    @Test
    fun `DocumentItem equality works correctly`() {
        // Given
        val doc1 = DocumentItem(
            id = "1",
            name = "Test Doc",
            description = "Description",
            file_link = "file123",
            createdAt = 1000L
        )
        val doc2 = DocumentItem(
            id = "1",
            name = "Test Doc",
            description = "Description",
            file_link = "file123",
            createdAt = 1000L
        )

        // Then
        assertEquals("Same documents should be equal", doc1, doc2)
    }

    @Test
    fun `DocumentSortOption has correct display names`() {
        // Then
        assertEquals("Name (A-Z)", DocumentSortOption.NameAZ.displayName)
        assertEquals("Name (Z-A)", DocumentSortOption.NameZA.displayName)
        assertEquals("Date (Newest)", DocumentSortOption.DateNewest.displayName)
        assertEquals("Date (Oldest)", DocumentSortOption.DateOldest.displayName)
    }

    @Test
    fun `DocumentItem copy works correctly`() {
        // Given
        val original = DocumentItem(
            id = "1",
            name = "Original",
            description = "Original Desc",
            file_link = "file123",
            createdAt = 1000L
        )

        // When
        val copied = original.copy(name = "Updated Name")

        // Then
        assertEquals("ID should be preserved", "1", copied.id)
        assertEquals("Name should be updated", "Updated Name", copied.name)
        assertEquals("Description should be preserved", "Original Desc", copied.description)
    }

    @Test
    fun `state copy updates only specified fields`() {
        // Given
        val initialState = DocumentsUiState(
            isLoading = true,
            searchQuery = "test",
            selectedSortOption = DocumentSortOption.NameAZ
        )

        // When
        val updatedState = initialState.copy(isLoading = false)

        // Then
        assertFalse("isLoading should be updated", updatedState.isLoading)
        assertEquals("searchQuery should be preserved", "test", updatedState.searchQuery)
        assertEquals("sortOption should be preserved", DocumentSortOption.NameAZ, updatedState.selectedSortOption)
    }

    @Test
    fun `sort documents by name ascending`() {
        // Given
        val documents = listOf(
            DocumentItem(id = "1", name = "Zebra", description = "", file_link = "", createdAt = 0L),
            DocumentItem(id = "2", name = "Apple", description = "", file_link = "", createdAt = 0L),
            DocumentItem(id = "3", name = "Mango", description = "", file_link = "", createdAt = 0L)
        )

        // When
        val sorted = documents.sortedBy { it.name }

        // Then
        assertEquals("Apple", sorted[0].name)
        assertEquals("Mango", sorted[1].name)
        assertEquals("Zebra", sorted[2].name)
    }

    @Test
    fun `sort documents by name descending`() {
        // Given
        val documents = listOf(
            DocumentItem(id = "1", name = "Zebra", description = "", file_link = "", createdAt = 0L),
            DocumentItem(id = "2", name = "Apple", description = "", file_link = "", createdAt = 0L),
            DocumentItem(id = "3", name = "Mango", description = "", file_link = "", createdAt = 0L)
        )

        // When
        val sorted = documents.sortedByDescending { it.name }

        // Then
        assertEquals("Zebra", sorted[0].name)
        assertEquals("Mango", sorted[1].name)
        assertEquals("Apple", sorted[2].name)
    }

    @Test
    fun `sort documents by date newest first`() {
        // Given
        val documents = listOf(
            DocumentItem(id = "1", name = "Doc1", description = "", file_link = "", createdAt = 1000L),
            DocumentItem(id = "2", name = "Doc2", description = "", file_link = "", createdAt = 3000L),
            DocumentItem(id = "3", name = "Doc3", description = "", file_link = "", createdAt = 2000L)
        )

        // When
        val sorted = documents.sortedByDescending { it.createdAt }

        // Then
        assertEquals("Doc2", sorted[0].name)
        assertEquals("Doc3", sorted[1].name)
        assertEquals("Doc1", sorted[2].name)
    }

    @Test
    fun `sort documents by date oldest first`() {
        // Given
        val documents = listOf(
            DocumentItem(id = "1", name = "Doc1", description = "", file_link = "", createdAt = 1000L),
            DocumentItem(id = "2", name = "Doc2", description = "", file_link = "", createdAt = 3000L),
            DocumentItem(id = "3", name = "Doc3", description = "", file_link = "", createdAt = 2000L)
        )

        // When
        val sorted = documents.sortedBy { it.createdAt }

        // Then
        assertEquals("Doc1", sorted[0].name)
        assertEquals("Doc3", sorted[1].name)
        assertEquals("Doc2", sorted[2].name)
    }

    @Test
    fun `filter documents by name contains query`() {
        // Given
        val documents = listOf(
            DocumentItem(id = "1", name = "STEM Research", description = "", file_link = "", createdAt = 0L),
            DocumentItem(id = "2", name = "ABM Project", description = "", file_link = "", createdAt = 0L),
            DocumentItem(id = "3", name = "Research Paper", description = "", file_link = "", createdAt = 0L)
        )
        val query = "research"

        // When
        val filtered = documents.filter { it.name.contains(query, ignoreCase = true) }

        // Then
        assertEquals(2, filtered.size)
        assertTrue(filtered.any { it.name == "STEM Research" })
        assertTrue(filtered.any { it.name == "Research Paper" })
    }

    @Test
    fun `filter documents by description contains query`() {
        // Given
        val documents = listOf(
            DocumentItem(id = "1", name = "Doc1", description = "About STEM topics", file_link = "", createdAt = 0L),
            DocumentItem(id = "2", name = "Doc2", description = "ABM related content", file_link = "", createdAt = 0L),
            DocumentItem(id = "3", name = "Doc3", description = "General information", file_link = "", createdAt = 0L)
        )
        val query = "stem"

        // When
        val filtered = documents.filter { it.description.contains(query, ignoreCase = true) }

        // Then
        assertEquals(1, filtered.size)
        assertEquals("Doc1", filtered[0].name)
    }

    @Test
    fun `empty query returns all documents`() {
        // Given
        val documents = listOf(
            DocumentItem(id = "1", name = "Doc1", description = "", file_link = "", createdAt = 0L),
            DocumentItem(id = "2", name = "Doc2", description = "", file_link = "", createdAt = 0L)
        )
        val query = ""

        // When
        val filtered = if (query.isBlank()) documents else documents.filter { it.name.contains(query, ignoreCase = true) }

        // Then
        assertEquals(2, filtered.size)
    }

    @Test
    fun `blank query returns all documents`() {
        // Given
        val documents = listOf(
            DocumentItem(id = "1", name = "Doc1", description = "", file_link = "", createdAt = 0L)
        )
        val query = "   "

        // When
        val filtered = if (query.isBlank()) documents else documents.filter { it.name.contains(query, ignoreCase = true) }

        // Then
        assertEquals(1, filtered.size)
    }
}
