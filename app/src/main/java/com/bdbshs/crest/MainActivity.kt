package com.bdbshs.crest

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.bdbshs.crest.data.AppwriteClient
import com.bdbshs.crest.navigation.CrestApp
import com.bdbshs.crest.ui.components.AppLogo
import com.bdbshs.crest.ui.theme.CRESTTheme
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Splash screen timing constants
private const val SPLASH_DISPLAY_DURATION_MS = 800L
private const val SPLASH_FADE_DURATION_MS = 400

/**
 * Main entry point for the CREST application.
 *
 * This activity handles:
 * - Splash screen with smooth transitions
 * - Firebase Firestore initialization with offline persistence
 * - Appwrite client initialization
 * - Edge-to-edge display configuration
 * - Global exception handling for crash reporting
 */
class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"

        // Firebase Firestore configuration
        private const val FIRESTORE_CACHE_SIZE_MB = 100L
        private const val FIRESTORE_CACHE_SIZE_BYTES = FIRESTORE_CACHE_SIZE_MB * 1024 * 1024
    }

    // Track initialization state for splash screen
    private var isInitialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before super.onCreate()
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !isInitialized }

        super.onCreate(savedInstanceState)

        // Set up global exception handler for crash reporting
        setupGlobalExceptionHandler()

        // Initialize services asynchronously
        lifecycleScope.launch {
            initializeServices()
            isInitialized = true
        }

        // Enable edge-to-edge display
        enableEdgeToEdge()

        setContent {
            CRESTTheme {
                CrestAppWithSplash(isInitialized)
            }
        }
    }

    /**
     * Initialize all required services for the app.
     * This includes Firebase Firestore and Appwrite client.
     */
    private suspend fun initializeServices() {
        try {
            // Initialize Appwrite client
            AppwriteClient.initialize(applicationContext)
            Log.d(TAG, "Appwrite client initialized successfully")

            // Configure Firebase Firestore with offline persistence
            configureFirestore()
            Log.d(TAG, "Firestore configured successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Error during service initialization", e)
            // Continue anyway - the app should handle errors gracefully
        }
    }

    /**
     * Configure Firebase Firestore with persistent cache for offline support.
     */
    private fun configureFirestore() {
        val firestore = Firebase.firestore

        val cacheSettings = PersistentCacheSettings.newBuilder()
            .setSizeBytes(FIRESTORE_CACHE_SIZE_BYTES)
            .build()

        val settings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(cacheSettings)
            .build()

        firestore.firestoreSettings = settings
    }

    /**
     * Set up global exception handler for uncaught exceptions.
     * This helps with debugging and can be extended for crash reporting.
     */
    private fun setupGlobalExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "Uncaught exception in thread ${thread.name}", throwable)

            // Log additional context for debugging
            Log.e(TAG, buildString {
                appendLine("=== CRASH REPORT ===")
                appendLine("Thread: ${thread.name}")
                appendLine("Exception: ${throwable.javaClass.simpleName}")
                appendLine("Message: ${throwable.message}")
                appendLine("Stack trace:")
                appendLine(throwable.stackTraceToString())
            })

            // Call the default handler to ensure proper crash behavior
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}

/**
 * Composable that wraps CrestApp with a splash screen animation.
 */
@Composable
private fun CrestAppWithSplash(isInitialized: Boolean) {
    var showSplash by remember { mutableStateOf(true) }

    // Delay hiding splash screen for smooth transition
    LaunchedEffect(isInitialized) {
        if (isInitialized) {
            delay(SPLASH_DISPLAY_DURATION_MS)
            showSplash = false
        }
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        AppLogo()
    }
}