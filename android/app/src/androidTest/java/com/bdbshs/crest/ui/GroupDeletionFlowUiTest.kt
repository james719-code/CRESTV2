package com.bdbshs.crest.ui

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bdbshs.crest.data.repository.WorkspaceDeletionRepository
import com.bdbshs.crest.navigation.AppDestination
import com.bdbshs.crest.navigation.NavigationActions
import com.bdbshs.crest.ui.screens.GroupDetailScreen
import com.bdbshs.crest.ui.screens.GroupsContent
import com.bdbshs.crest.ui.theme.CRESTTheme
import com.bdbshs.crest.ui.viewmodels.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Uses fixture groups and a suspended fake deletion; no school records are changed. */
@RunWith(AndroidJUnit4::class)
class GroupDeletionFlowUiTest {
    @get:Rule val compose = createComposeRule()
    private val store = ViewModelStore()
    @After fun cleanup() { compose.runOnIdle { store.clear() } }

    @Test fun confirmationReturnsToGroupsWhileTheDeleteContinuesOnItsCard() {
        val groups = listOf(GroupItem("science", "Science Group"), GroupItem("other", "Other Group"))
        val completion = CompletableDeferred<Unit>()
        val repository = mockk<WorkspaceDeletionRepository>()
        coEvery { repository.deleteGroup("science") } coAnswers { completion.await() }
        val user = mockk<FirebaseUser> { every { uid } returns "fixture-teacher" }
        val auth = mockk<FirebaseAuth> {
            every { currentUser } returns user
            every { addAuthStateListener(any()) } just Runs
            every { removeAuthStateListener(any()) } just Runs
        }
        val deletion = GroupDeletionViewModel(repository, auth)
        store.put("deletion", deletion)
        compose.setContent {
            CRESTTheme {
                val nav = rememberNavController()
                val actions = remember(nav) { NavigationActions(nav) }
                val state by deletion.uiState.collectAsStateWithLifecycle()
                NavHost(nav, startDestination = AppDestination.Groups.route) {
                    composable(AppDestination.Groups.route) {
                        val visible = state.visibleGroups(groups)
                        GroupsContent(
                            GroupsUiState(isLoading = false, allGroups = visible), visible,
                            {}, {}, {}, {}, {}, {}, { actions.navigateToGroupDetails(it.id) },
                            deletions = state.operations, onRetryDelete = deletion::retry
                        )
                    }
                    composable(AppDestination.GroupDetail.routeWithArg) { entry ->
                        val id = entry.arguments!!.getString(AppDestination.GroupDetail.groupIdArg)
                        val detail = remember(id) {
                            mockk<GroupDetailViewModel> {
                                every { uiState } returns MutableStateFlow(GroupDetailUiState(
                                    isLoading = false, group = groups.first { it.id == id }
                                ))
                            }
                        }
                        GroupDetailScreen(
                            onNavigateBack = { actions.navigateBack() },
                            onDeleteGroup = { group -> deletion.deleteGroup(group); actions.navigateTo(AppDestination.Groups) },
                            viewModel = detail
                        )
                    }
                }
            }
        }
        compose.onNode(hasText("Science Group") and hasClickAction()).performClick()
        compose.onNodeWithContentDescription("Delete group").performClick()
        compose.onNodeWithText("Delete group?").assertIsDisplayed()
        compose.onNodeWithText("Delete group", substring = false).performClick()
        compose.onNodeWithText("Research groups").assertIsDisplayed()
        compose.onNodeWithText("Deleting group…").assertExists()
        compose.onNodeWithText("Group Information").assertDoesNotExist()
        compose.onNodeWithText("View group details").performScrollTo().assertIsEnabled()
        compose.runOnIdle { completion.complete(Unit) }
        compose.waitUntil(5_000) { deletion.uiState.value.deletedIds.contains("science") }
        compose.onNodeWithText("Science Group").assertDoesNotExist()
        compose.onNodeWithText("Deleting group…").assertDoesNotExist()
        compose.onNodeWithText("Other Group").assertExists()
    }
}
