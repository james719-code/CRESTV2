package com.bdbshs.crest

import android.app.Application
import android.util.Log
import com.bdbshs.crest.data.AppwriteClient
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

/**
 * Main application class for CREST.
 * Handles global initialization of services like Firebase and Appwrite.
 */
class CrestApplication : Application() {

    companion object {
        private const val TAG = "CrestApplication"

        // Firebase Firestore configuration
        private const val FIRESTORE_CACHE_SIZE_MB = 100L
        private const val FIRESTORE_CACHE_SIZE_BYTES = FIRESTORE_CACHE_SIZE_MB * 1024 * 1024
    }

    override fun onCreate() {
        super.onCreate()

        // Set up global exception handler for crash reporting
        setupGlobalExceptionHandler()

        // Initialize services
        initializeServices()
    }

    /**
     * Initialize all required services for the app.
     * This includes Firebase Firestore and Appwrite client.
     */
    private fun initializeServices() {
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
        try {
            val firestore = Firebase.firestore

            val cacheSettings = PersistentCacheSettings.newBuilder()
                .setSizeBytes(FIRESTORE_CACHE_SIZE_BYTES)
                .build()

            val settings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(cacheSettings)
                .build()

            firestore.firestoreSettings = settings
        } catch (e: Exception) {
            Log.e(TAG, "Error configuring Firestore", e)
        }
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
