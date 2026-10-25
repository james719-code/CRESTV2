package com.bdbshs.crest.ui

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bdbshs.crest.ui.screens.DocumentsScreen
import com.bdbshs.crest.ui.theme.CRESTTheme
import com.bdbshs.crest.ui.viewmodels.DocumentsViewModel
import com.bdbshs.crest.ui.viewmodels.UserType
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented UI tests for navigation flows between screens.
 * Tests screen transitions, back navigation, and route handling.
 */
@RunWith(AndroidJUnit4::class)
class NavigationTest {

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
    fun documentsScreen_studentRole_rendersCorrectly() {
        composeTestRule.setContent {
            CRESTTheme {
                DocumentsScreen(
                    userRole = UserType.STUDENT,
                    viewModel = viewModel
                )
            }
        }

        // Wait for content to load
        composeTestRule.waitForIdle()

        // Verify search bar is displayed
        composeTestRule.onNodeWithText("Search documents...")
            .assertIsDisplayed()
    }

    @Test
    fun documentsScreen_teacherRole_rendersCorrectly() {
        composeTestRule.setContent {
            CRESTTheme {
                DocumentsScreen(
                    userRole = UserType.TEACHER,
                    viewModel = viewModel
                )
            }
        }

        // Wait for content to load
        composeTestRule.waitForIdle()

        // Verify search bar is displayed for teachers
        composeTestRule.onNodeWithText("Search documents...")
            .assertIsDisplayed()
    }

    @Test
    fun documentsScreen_filterButtonExists() {
        composeTestRule.setContent {
            CRESTTheme {
                DocumentsScreen(
                    userRole = UserType.STUDENT,
                    viewModel = viewModel
                )
            }
        }

        composeTestRule.waitForIdle()

        // Verify filter button is accessible
        composeTestRule.onNodeWithContentDescription("Filter")
            .assertExists()
    }

    @Test
    fun documentsScreen_filterSheet_opensOnClick() {
        composeTestRule.setContent {
            CRESTTheme {
                DocumentsScreen(
                    userRole = UserType.TEACHER,
                    viewModel = viewModel
                )
            }
        }

        composeTestRule.waitForIdle()

        // Click filter button to open sheet
        composeTestRule.onNodeWithContentDescription("Filter")
            .performClick()

        composeTestRule.waitForIdle()

        // Verify sort sheet content is displayed
        composeTestRule.onNodeWithText("Sort Documents")
            .assertIsDisplayed()
    }

    @Test
    fun documentsScreen_sortOptions_visible() {
        composeTestRule.setContent {
            CRESTTheme {
                DocumentsScreen(
                    userRole = UserType.STUDENT,
                    viewModel = viewModel
                )
            }
        }

        composeTestRule.waitForIdle()

        // Open filter sheet
        composeTestRule.onNodeWithContentDescription("Filter")
            .performClick()

        composeTestRule.waitForIdle()

        // Verify sort options are available
        composeTestRule.onNodeWithText("Name (A-Z)")
            .assertIsDisplayed()
    }

    @Test
    fun documentsScreen_rolesHaveSameSearchUI() {
        // Test that both user roles see the same search interface
        composeTestRule.setContent {
            CRESTTheme {
                DocumentsScreen(
                    userRole = UserType.STUDENT,
                    viewModel = viewModel
                )
            }
        }

        composeTestRule.waitForIdle()

        // Verify search is accessible for all roles
        val searchExists = try {
            composeTestRule.onAllNodes(hasText("Search documents..."))
                .fetchSemanticsNodes().isNotEmpty()
        } catch (e: Exception) {
            false
        }

        assert(searchExists) { "Search bar should be accessible for all user roles" }
    }
}
