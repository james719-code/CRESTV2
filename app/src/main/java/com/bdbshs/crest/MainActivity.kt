package com.bdbshs.crest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.bdbshs.crest.data.ThemeMode
import com.bdbshs.crest.data.UserPrefs.userDataFlow
import com.bdbshs.crest.navigation.CrestApp
import com.bdbshs.crest.ui.screens.SplashScreen
import com.bdbshs.crest.ui.theme.CRESTTheme
import kotlinx.coroutines.delay

// Splash screen timing constants
private const val SPLASH_DISPLAY_DURATION_MS = 1200L
private const val SPLASH_FADE_DURATION_MS = 500

/**
 * Main entry point for the CREST application.
 *
 * This activity handles:
 * - Splash screen with smooth transitions
 * - Edge-to-edge display configuration
 * - Theme selection
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before super.onCreate()
        installSplashScreen()

        super.onCreate(savedInstanceState)

        // Enable edge-to-edge display
        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current
            val userData by context.userDataFlow.collectAsState(initial = null)
            val themeMode = userData?.theme ?: ThemeMode.SYSTEM
            
            val isDarkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            CRESTTheme(darkTheme = isDarkTheme) {
                CrestAppWithSplash()
            }
        }
    }
}

/**
 * Composable that wraps CrestApp with a splash screen animation.
 */
@Composable
private fun CrestAppWithSplash() {
    var showSplash by remember { mutableStateOf(true) }

    // Delay hiding splash screen for smooth transition
    LaunchedEffect(Unit) {
        delay(SPLASH_DISPLAY_DURATION_MS)
        showSplash = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Main app content (always present, but covered by splash initially)
        AnimatedVisibility(
            visible = !showSplash,
            enter = fadeIn(animationSpec = tween(SPLASH_FADE_DURATION_MS)),
            exit = fadeOut(animationSpec = tween(SPLASH_FADE_DURATION_MS))
        ) {
            CrestApp()
        }

        // Splash screen overlay
        AnimatedVisibility(
            visible = showSplash,
            enter = fadeIn(),
            exit = fadeOut(animationSpec = tween(SPLASH_FADE_DURATION_MS))
        ) {
            SplashContent()
        }
    }
}

/**
 * Splash screen content with app logo and branding.
 */
@Composable
private fun SplashContent() {
    SplashScreen()
}