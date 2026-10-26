package com.bdbshs.crest.ui

import android.view.ContextThemeWrapper
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.view.WindowCompat
import com.bdbshs.crest.ui.theme.CRESTTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class SystemBarThemeTest(private val dark: Boolean) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "dark={0}")
        fun themes() = listOf(arrayOf(false), arrayOf(true))
    }
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun wrappedActivityContextStillAppliesMatchingSystemIcons() {
        val activity = compose.activity
        activity.runOnUiThread { activity.enableEdgeToEdge() }
        val wrappedView = View(ContextThemeWrapper(activity, android.R.style.Theme_Material_Light_NoActionBar))
        compose.setContent {
            CompositionLocalProvider(LocalView provides wrappedView) {
                CRESTTheme(darkTheme = dark) { Text("System bars follow the chosen app theme") }
            }
        }
        compose.runOnIdle {
            val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
            assertEquals(!dark, controller.isAppearanceLightStatusBars)
            assertEquals(!dark, controller.isAppearanceLightNavigationBars)
        }
    }
}
