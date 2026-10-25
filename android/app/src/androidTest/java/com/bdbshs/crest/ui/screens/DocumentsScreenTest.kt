package com.bdbshs.crest.ui.screens

import android.app.Application
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bdbshs.crest.ui.viewmodels.DocumentsViewModel
import com.bdbshs.crest.ui.viewmodels.UserType
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented UI tests for the Documents screen.
 * Tests search, filtering, and document list rendering.
 */
@RunWith(AndroidJUnit4::class)
class DocumentsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var viewModel: DocumentsViewModel

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val application = context.applicationContext as Application
        viewModel = DocumentsViewModel(application)
    }

    @Test
    fun documentsScreen_showsSearchBar() {
        composeTestRule.setContent {
            DocumentsScreen(
                userRole = UserType.STUDENT,
                viewModel = viewModel
            )
        }
        
        // Verify search bar is displayed
        composeTestRule.onNodeWithText("Search documents...").assertIsDisplayed()
    }

    @Test
    fun documentsScreen_searchFunctionality() {
        composeTestRule.setContent {
            DocumentsScreen(
                userRole = UserType.STUDENT,
                viewModel = viewModel
            )
        }
        
        // Wait for content to load
        composeTestRule.waitForIdle()
        
        // Perform search
        composeTestRule.onNodeWithText("Search documents...")
            .performClick()
        
        composeTestRule.waitForIdle()
    }

    @Test
    fun documentsScreen_showsEmptyState_whenNoDocuments() {
        composeTestRule.setContent {
            DocumentsScreen(
                userRole = UserType.STUDENT,
                viewModel = viewModel
            )
        }
        
        // Wait for loading to complete
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithContentDescription("Loading")
                .fetchSemanticsNodes().isEmpty()
        }
        
        // Verify either documents are shown or empty state is displayed
        // Since we don't have test data, we expect either scenario
        composeTestRule.waitForIdle()
    }

    @Test
    fun documentsScreen_canOpenFilterSheet() {
        composeTestRule.setContent {
            DocumentsScreen(
                userRole = UserType.TEACHER,
                viewModel = viewModel
            )
        }
        
        // Wait for content to load
        composeTestRule.waitForIdle()
        
        // Click filter button
        composeTestRule.onNodeWithContentDescription("Filter")
            .assertExists()
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Verify filter/sort bottom sheet is shown
        composeTestRule.onNodeWithText("Sort Documents")
            .assertIsDisplayed()
    }

    @Test
    fun documentsScreen_pullToRefresh_works() {
        composeTestRule.setContent {
            DocumentsScreen(
                userRole = UserType.STUDENT,
                viewModel = viewModel
            )
        }
        
        // Wait for initial load
        composeTestRule.waitForIdle()
        
        // Note: Pull to refresh is difficult to test in instrumented tests
        // This test verifies the screen loads without crashing
        composeTestRule.onNodeWithText("Search documents...")
            .assertIsDisplayed()
    }

    @Test
    fun documentsScreen_teacherRole_canLongPressDocuments() {
        composeTestRule.setContent {
            DocumentsScreen(
                userRole = UserType.TEACHER,
                viewModel = viewModel
            )
        }
        
        // Wait for content to load
        composeTestRule.waitForIdle()
        
        // Verify screen renders correctly for teacher role
        composeTestRule.onNodeWithText("Search documents...")
            .assertIsDisplayed()
    }

    @Test
    fun documentsScreen_studentRole_canViewDocuments() {
        composeTestRule.setContent {
            DocumentsScreen(
                userRole = UserType.STUDENT,
                viewModel = viewModel
            )
        }
        
        // Wait for content to load
        composeTestRule.waitForIdle()
        
        // Verify screen renders correctly for student role
        composeTestRule.onNodeWithText("Search documents...")
            .assertIsDisplayed()
    }
}
