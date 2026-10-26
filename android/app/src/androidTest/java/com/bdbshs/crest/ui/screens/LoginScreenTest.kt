package com.bdbshs.crest.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.bdbshs.crest.ui.theme.CRESTTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LoginScreenTest {
    @get:Rule val composeTestRule = createComposeRule()

    private fun show(loading: Boolean = false, onClick: () -> Unit = {}) {
        composeTestRule.setContent {
            CRESTTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        LoginScreenContent(loading, onClick)
                    }
                }
            }
        }
    }

    @Test fun showsBrandAndSchool() {
        show()
        composeTestRule.onNodeWithText("CREST").assertExists()
        composeTestRule.onNodeWithText("School research, shared.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bonifacio D. Borebor Sr. High School").assertExists()
    }

    @Test fun signInCallsAction() {
        var clicked = false
        show(onClick = { clicked = true })
        composeTestRule.onNodeWithText("Continue with Google").performScrollTo().assertIsEnabled().performClick()
        assertTrue(clicked)
    }

    @Test fun signInIsVisibleWithoutScrolling() {
        show()
        composeTestRule.onNodeWithText("Continue with Google").assertIsDisplayed()
    }

    @Test fun signInIsDisabledWhileLoading() {
        show(loading = true)
        composeTestRule.onNodeWithText("Signing in…").performScrollTo().assertIsNotEnabled()
    }
}
