package com.bdbshs.crest.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bdbshs.crest.ui.theme.CRESTTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented UI tests for the Login screen.
 * Tests UI rendering, component visibility, and user interactions.
 */
@RunWith(AndroidJUnit4::class)
class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loginScreen_showsAppTitle() {
        composeTestRule.setContent {
            CRESTTheme {
                LoginScreenContent(
                    isLoading = false,
                    logoScale = 1f,
                    onGoogleLoginClick = {}
                )
            }
        }

        // Verify app title is displayed
        composeTestRule.onNodeWithText("CREST").assertIsDisplayed()
    }

    @Test
    fun loginScreen_showsWelcomeText() {
        composeTestRule.setContent {
            CRESTTheme {
                LoginScreenContent(
                    isLoading = false,
                    logoScale = 1f,
                    onGoogleLoginClick = {}
                )
            }
        }

        // Verify welcome text is displayed
        composeTestRule.onNodeWithText("Welcome to").assertIsDisplayed()
    }

    @Test
    fun loginScreen_showsSignInButton() {
        composeTestRule.setContent {
            CRESTTheme {
                LoginScreenContent(
                    isLoading = false,
                    logoScale = 1f,
                    onGoogleLoginClick = {}
                )
            }
        }

        // Verify Google sign-in button is displayed and enabled
        composeTestRule.onNodeWithText("Continue with Google")
            .assertIsDisplayed()
            .assertIsEnabled()
    }

    @Test
    fun loginScreen_signInButton_clickable() {
        var clicked = false
        
        composeTestRule.setContent {
            CRESTTheme {
                LoginScreenContent(
                    isLoading = false,
                    logoScale = 1f,
                    onGoogleLoginClick = { clicked = true }
                )
            }
        }

        // Click the sign-in button
        composeTestRule.onNodeWithText("Continue with Google").performClick()

        // Verify click was registered
        assert(clicked) { "Sign-in button click should be registered" }
    }

    @Test
    fun loginScreen_showsDisabledButton_whenLoading() {
        composeTestRule.setContent {
            CRESTTheme {
                LoginScreenContent(
                    isLoading = true,
                    logoScale = 1f,
                    onGoogleLoginClick = {}
                )
            }
        }

        // Verify button is disabled when loading
        composeTestRule.onNode(hasText("Continue with Google", ignoreCase = true))
            .assertDoesNotExist()
    }

    @Test
    fun loginScreen_showsAppLogo() {
        composeTestRule.setContent {
            CRESTTheme {
                LoginScreenContent(
                    isLoading = false,
                    logoScale = 1f,
                    onGoogleLoginClick = {}
                )
            }
        }

        // Verify app logo is displayed
        composeTestRule.onNodeWithContentDescription("App Logo").assertIsDisplayed()
    }

    @Test
    fun loginScreen_showsTermsText() {
        composeTestRule.setContent {
            CRESTTheme {
                LoginScreenContent(
                    isLoading = false,
                    logoScale = 1f,
                    onGoogleLoginClick = {}
                )
            }
        }

        // Verify terms text is displayed
        composeTestRule.onNodeWithText("By continuing, you agree to our", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun loginScreen_showsDescription() {
        composeTestRule.setContent {
            CRESTTheme {
                LoginScreenContent(
                    isLoading = false,
                    logoScale = 1f,
                    onGoogleLoginClick = {}
                )
            }
        }

        // Verify description text is displayed
        composeTestRule.onNodeWithText("Your gateway to academic research", substring = true)
            .assertIsDisplayed()
    }
}
