package com.bdbshs.crest

import android.app.Application
import android.util.Log
import com.bdbshs.crest.data.AppwriteClient
import dagger.hilt.android.HiltAndroidApp

/**
 * Main application class for CREST.
 * Handles global initialization of services like Firebase and Appwrite.
 */
@HiltAndroidApp
class CrestApplication : Application() {

    companion object {
        private const val TAG = "CrestApplication"
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
     * This includes Appwrite client.
     */
    private fun initializeServices() {
        try {
            // Initialize Appwrite client
            AppwriteClient.initialize(applicationContext)
            Log.d(TAG, "Appwrite client initialized successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Error during service initialization", e)
            // Continue anyway - the app should handle errors gracefully
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
