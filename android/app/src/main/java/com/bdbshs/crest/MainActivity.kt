package com.bdbshs.crest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bdbshs.crest.data.ThemeMode
import com.bdbshs.crest.data.UserPrefs.userDataFlow
import com.bdbshs.crest.navigation.CrestApp
import com.bdbshs.crest.notifications.NotificationOptIn
import com.bdbshs.crest.notifications.PushNotifications
import com.bdbshs.crest.ui.theme.CRESTTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main entry point for the CREST application.
 *
 * This activity handles:
 * - Native splash screen while the first frame is prepared
 * - Edge-to-edge display configuration
 * - Theme selection
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @javax.inject.Inject lateinit var pushNotifications: PushNotifications

    override fun onResume() {
        super.onResume()
        pushNotifications.refresh()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before super.onCreate()
        installSplashScreen().setOnExitAnimationListener { it.remove() }

        super.onCreate(savedInstanceState)

        // Enable edge-to-edge display
        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current
            val userData by context.userDataFlow.collectAsStateWithLifecycle(initialValue = null)
            val themeMode = userData?.theme ?: ThemeMode.SYSTEM
            
            val isDarkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            CRESTTheme(darkTheme = isDarkTheme) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    CrestApp()
                    NotificationOptIn(userData?.uid, pushNotifications)
                }
            }
        }
    }
}
