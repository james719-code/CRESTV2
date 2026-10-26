package com.bdbshs.crest.ui

import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.bdbshs.crest.ui.screens.SelectionActionBar
import com.bdbshs.crest.ui.theme.CRESTTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class StorageSelectionInsetsTest(private val dark: Boolean) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "dark={0}")
        fun themes() = listOf(arrayOf(false), arrayOf(true))
    }
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun deleteStaysAboveSystemNavigationAndReceivesTheTap() {
        var requests = 0
        var bottomInset = 0
        compose.activity.runOnUiThread { compose.activity.enableEdgeToEdge() }
        compose.setContent {
            CRESTTheme(darkTheme = dark) {
                bottomInset = WindowInsets.navigationBars.getBottom(LocalDensity.current)
                Scaffold(bottomBar = { SelectionActionBar(1, 1048576, false) { requests++ } }) { padding ->
                    Box(Modifier.fillMaxSize().padding(padding))
                }
            }
        }
        val root = compose.onRoot().fetchSemanticsNode().boundsInRoot
        val delete = compose.onNodeWithText("Delete").assertIsDisplayed()
        assertTrue("Delete overlaps the system navigation area",
            delete.fetchSemanticsNode().boundsInRoot.bottom <= root.bottom - bottomInset + 1f)
        delete.performClick()
        assertEquals(1, requests)
    }
}
