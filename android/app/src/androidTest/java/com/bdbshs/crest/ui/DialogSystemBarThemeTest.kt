package com.bdbshs.crest.ui

import android.view.Window
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import com.bdbshs.crest.ui.theme.CRESTTheme
import com.bdbshs.crest.ui.theme.CrestDialogSystemBars
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class DialogSystemBarThemeTest(private val dark: Boolean) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "dark={0}")
        fun themes() = listOf(arrayOf(false), arrayOf(true))
    }
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @OptIn(ExperimentalMaterial3Api::class)
    @Test fun sheetSystemIconsFollowTheAppTheme() {
        var dialogWindow: Window? = null
        compose.activity.runOnUiThread { compose.activity.enableEdgeToEdge() }
        compose.setContent {
            CRESTTheme(darkTheme = dark) {
                ModalBottomSheet(onDismissRequest = {}, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
                    CrestDialogSystemBars()
                    val view = LocalView.current
                    SideEffect { dialogWindow = (view.parent as? DialogWindowProvider)?.window }
                    Text("Appearance follows the selected app theme")
                }
            }
        }
        compose.waitForIdle()
        compose.runOnIdle {
            assertNotNull(dialogWindow)
            val window = dialogWindow!!
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            assertEquals(!dark, controller.isAppearanceLightStatusBars)
            assertEquals(!dark, controller.isAppearanceLightNavigationBars)
        }
    }
}
