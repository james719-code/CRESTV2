package com.bdbshs.crest.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.bdbshs.crest.navigation.AppDestination
import com.bdbshs.crest.ui.components.*
import com.bdbshs.crest.ui.screens.*
import com.bdbshs.crest.ui.theme.CRESTTheme
import com.bdbshs.crest.ui.viewmodels.*
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Exercises the shared UI with local fixtures, without a school account or network writes. */
class BrandUiTest {
    @get:Rule val compose = createComposeRule()

    private val paper = ResearchItem(
        id = "sample-1",
        title = "Student perspectives on sustainable waste management in senior high school",
        members = listOf("Andrea Santos", "Miguel Reyes", "Sofia Cruz"),
        strand = "STEM",
        downloads = 24,
        type = ResearchType.QUALITATIVE,
        createdAt = 1767225600000
    )
    private val papers get() = listOf(paper, paper.copy(id = "sample-2", title = "Digital study habits and academic performance", strand = "HUMSS", type = ResearchType.QUANTITATIVE))

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun Frame(role: UserType = UserType.STUDENT, route: String = AppDestination.Researches.route, content: @Composable (PaddingValues) -> Unit) {
        Scaffold(
            topBar = { CrestTopAppBar(if (route == AppDestination.Home.route) "Home" else "Research", role, userName = "Andrea Santos") },
            bottomBar = { CrestBottomNavBar(getBottomNavItems(role), route, {}) },
            content = content
        )
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val folder = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "ui-review")
        folder.mkdirs()
        File(folder, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun navigationKeepsEveryLabelAndSelection() {
        var clicked = ""
        compose.setContent {
            CRESTTheme {
                var current by remember { mutableStateOf(AppDestination.Home.route) }
                CrestBottomNavBar(getBottomNavItems(UserType.TEACHER), current, {
                    current = it.route
                    clicked = it.route
                })
            }
        }
        listOf("Home", "Research", "Documents", "Groups").forEach { compose.onNodeWithText(it).assertIsDisplayed() }
        compose.onNode(hasText("Groups") and hasClickAction()).performClick().assertIsSelected()
        assertEquals(AppDestination.Groups.route, clicked)
        compose.onNodeWithText("Home").assertIsNotSelected()
    }

    @Test fun paperActionsRemainIndependent() {
        var opened = 0
        var saved = 0
        var managed = 0
        compose.setContent {
            CRESTTheme {
                ModernResearchCard(paper, { opened++ }, onFavoriteClick = { saved++ }, onManageClick = { managed++ })
            }
        }
        compose.onNodeWithContentDescription("Add to favorites").performClick()
        compose.onNodeWithContentDescription("Manage research").performClick()
        assertEquals(0, opened)
        assertEquals(1, saved)
        assertEquals(1, managed)
        compose.onNodeWithText(paper.title).performClick()
        assertEquals(1, opened)
        compose.onNodeWithText("Qualitative").assertExists()
        compose.onNodeWithText("24 downloads").assertExists()
    }

    @Composable
    private fun Library(dark: Boolean = false, largeText: Boolean = false, empty: Boolean = false) {
        CRESTTheme(darkTheme = dark) {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, if (largeText) 2f else 1f)) {
                var query by remember { mutableStateOf(if (empty) "unmatched" else "") }
                Frame { padding ->
                    ResearchLibraryContent(
                        uiState = ResearchesUiState(isLoading = false, searchQuery = query),
                        papers = if (empty && query.isNotBlank()) emptyList() else papers,
                        favoritesOnly = false, isOnline = true,
                        onQueryChange = { query = it }, onFilterClick = {}, onFavoritesClick = {},
                        onClearFilters = { query = "" }, onRefresh = {},
                        onPaperClick = {}, onFavoriteClick = {}, onManageClick = null,
                        modifier = Modifier.padding(padding)
                    )
                }
            }
        }
    }

    @Test fun researchLightAndSearchRecovery() {
        compose.setContent { Library(empty = true) }
        compose.onNodeWithText("No papers match your search").assertIsDisplayed()
        compose.onAllNodesWithText("Clear filters").onFirst().performClick()
        compose.onNodeWithText(paper.title).assertIsDisplayed()
        capture("research-light")
        compose.onNodeWithContentDescription("Search title or author").performTextInput("climate")
        compose.onNodeWithContentDescription("Clear").performClick()
        compose.onNodeWithText("Search title or author").assertExists()
    }

    @Test fun researchDark() {
        compose.setContent { Library(dark = true) }
        compose.onNodeWithText(paper.title).assertIsDisplayed()
        capture("research-dark")
    }

    @Test fun researchLargeText() {
        compose.setContent { Library(largeText = true) }
        compose.onNodeWithText(paper.title).performScrollTo().assertIsDisplayed()
        capture("research-large-text")
    }

    @Test fun teacherHomeReviewAction() {
        var reviews = 0
        compose.setContent {
            CRESTTheme {
                Frame(UserType.TEACHER, AppDestination.Home.route) { padding ->
                    TeacherHomeContent(
                        TeacherHomeUiState(
                            isLoading = false, teacherName = "Ms. Reyes",
                            dashboardItems = listOf(DashboardCardItem("Library papers", "128", Icons.Default.Description)),
                            recentResearches = listOf(SimpleResearch("sample-1", paper.title))
                        ),
                        {}, {}, {}, { reviews++ }, {}, Modifier.padding(padding)
                    )
                }
            }
        }
        compose.onNodeWithText("Review group submissions").performClick()
        assertEquals(1, reviews)
        capture("teacher-home")
    }

    @Test fun studentHomeShowsSubmissionStateFirst() {
        compose.setContent {
            CRESTTheme {
                Frame(route = AppDestination.Home.route) { padding ->
                    StudentHomeContent(
                        StudentHomeUiState(
                            isLoading = false,
                            studentDetails = StudentDetails("Andrea Santos", "STEM", "GROUP-024"),
                            groupDetails = GroupDetails(group_name = "Sustainable futures", strand = "STEM", uploaded = true, group_member = listOf("a", "b", "c")),
                            totalResearchCount = 128, strandResearchCount = 42, recentResearches = papers
                        ),
                        {}, {}, {}, {}, {}, {}, {}, Modifier.padding(padding)
                    )
                }
            }
        }
        compose.onNodeWithText("Pending review").assertIsDisplayed()
        compose.onNodeWithText("Unsubmit").assertIsDisplayed()
        capture("student-home")
    }

    @Test fun paperDetailsBeforeReading() {
        var reads = 0
        compose.setContent {
            CRESTTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    ResearchOverview(paper, false, { reads++ })
                }
            }
        }
        compose.onNodeWithText(paper.title).assertIsDisplayed()
        compose.onNodeWithText("Authors").assertExists()
        capture("paper-details")
        compose.onNodeWithText("Read paper").performScrollTo().performClick()
        assertEquals(1, reads)
    }

    @Test fun signInScreenshot() {
        compose.setContent {
            CRESTTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        LoginScreenContent(false, {})
                    }
                }
            }
        }
        compose.onNodeWithText("Continue with Google").assertIsDisplayed()
        capture("sign-in")
    }

    @Test fun homeReviewOpensGroupsInTheCurrentPager() {
        compose.setContent {
            CRESTTheme {
                MainScreenWithPagerContent(
                    navigationActions = com.bdbshs.crest.navigation.NavigationActions(androidx.navigation.compose.rememberNavController()),
                    isOnline = true, initialPage = 0,
                    uiState = MainUiState(userRole = UserType.TEACHER, isLoading = false),
                    onThemeChanged = {}, onSignOutClick = {},
                    homeContent = { padding, _, openGroups ->
                        Box(Modifier.fillMaxSize().padding(padding)) {
                            Button(onClick = openGroups) { Text("Review group submissions") }
                        }
                    },
                    researchesContent = { _, _ -> Text("Research destination") },
                    documentsContent = { _, _ -> Text("Documents destination") },
                    groupsContent = { padding, _ -> Box(Modifier.fillMaxSize().padding(padding)) { Text("Group destination") } }
                )
            }
        }
        compose.onNodeWithText("Review group submissions").performClick()
        compose.onNodeWithText("Group destination").assertIsDisplayed()
        compose.onNode(hasText("Groups") and hasClickAction()).assertIsSelected()
    }

    @Test fun groupsRouteWaitsForTeacherRole() {
        val role = mutableStateOf<UserType?>(null)
        compose.setContent {
            CRESTTheme {
                MainScreenWithPagerContent(
                    navigationActions = com.bdbshs.crest.navigation.NavigationActions(androidx.navigation.compose.rememberNavController()),
                    isOnline = true, initialPage = 3,
                    uiState = MainUiState(userRole = role.value, isLoading = role.value == null),
                    onThemeChanged = {}, onSignOutClick = {},
                    homeContent = { _, _, _ -> Text("Home destination") },
                    researchesContent = { _, _ -> Text("Research destination") },
                    documentsContent = { _, _ -> Text("Documents destination") },
                    groupsContent = { padding, _ -> Box(Modifier.fillMaxSize().padding(padding)) { Text("Group destination") } }
                )
            }
        }
        compose.runOnIdle { role.value = UserType.TEACHER }
        compose.onNodeWithText("Group destination").assertIsDisplayed()
        compose.onNode(hasText("Groups") and hasClickAction()).assertIsSelected()
    }
}
