package com.bdbshs.crest.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bdbshs.crest.navigation.AppDestination
import com.bdbshs.crest.navigation.NavigationActions
import com.bdbshs.crest.ui.components.CrestBottomNavBar
import com.bdbshs.crest.ui.components.getBottomNavItems
import com.bdbshs.crest.ui.screens.MainScreenWithPagerContent
import com.bdbshs.crest.ui.theme.CRESTTheme
import com.bdbshs.crest.ui.viewmodels.MainUiState
import com.bdbshs.crest.ui.viewmodels.UserType
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class MainTabReturnTest(private val dark: Boolean) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "dark={0}")
        fun themes() = listOf(arrayOf(false), arrayOf(true))
    }
    @get:Rule val compose = createComposeRule()

    @Test fun homeFromAccountsDoesNotRestoreThePreviousGroupsTab() {
        compose.setContent {
            CRESTTheme(darkTheme = dark) {
                val controller = rememberNavController()
                val actions = remember(controller) { NavigationActions(controller) }
                NavHost(controller, AppDestination.Home.route,
                    enterTransition = { EnterTransition.None }, exitTransition = { ExitTransition.None }) {
                    composable(AppDestination.Home.route) {
                        MainScreenWithPagerContent(
                            actions, true, 0,
                            homeContent = { padding, _, _ -> Text("Home workspace", Modifier.padding(padding)) },
                            researchesContent = { padding, _ -> Text("Research workspace", Modifier.padding(padding)) },
                            documentsContent = { padding, _ -> Text("Document workspace", Modifier.padding(padding)) },
                            groupsContent = { padding, _ ->
                                Button(onClick = { actions.navigateTo(AppDestination.Accounts) }, Modifier.padding(padding)) {
                                    Text("Open account tools")
                                }
                            },
                            uiState = MainUiState(userRole = UserType.TEACHER, isLoading = false),
                            onThemeChanged = {}, onSignOutClick = {}
                        )
                    }
                    composable(AppDestination.Accounts.route) {
                        Scaffold(bottomBar = {
                            CrestBottomNavBar(getBottomNavItems(UserType.TEACHER), AppDestination.Accounts.route, actions::navigateTo)
                        }) { padding -> Text("Account tools", Modifier.padding(padding)) }
                    }
                }
            }
        }
        compose.onNode(hasText("Groups") and hasClickAction()).performClick()
        compose.onNodeWithText("Open account tools").performClick()
        compose.onNodeWithText("Account tools").assertIsDisplayed()
        compose.onNode(hasText("Home") and hasClickAction()).performClick()
        compose.onNodeWithText("Home workspace").assertIsDisplayed()
        compose.onNodeWithText("Open account tools").assertDoesNotExist()
    }
}
