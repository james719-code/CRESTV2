package com.bdbshs.crest

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTextInput

/**
 * Test helper utilities for Compose UI tests.
 * Provides common patterns for waiting, text input, and node queries.
 */
object TestHelpers {

    /**
     * Default timeout in milliseconds for waiting operations.
     */
    const val DEFAULT_TIMEOUT_MS = 5000L

    /**
     * Wait for a node with specific text to exist.
     * 
     * @param text The text to search for
     * @param useSubstring If true, matches partial text
     * @param timeoutMs Maximum time to wait in milliseconds
     * @return The SemanticsNodeInteraction if found
     * @throws AssertionError if node not found within timeout
     */
    fun ComposeContentTestRule.waitForNodeWithText(
        text: String,
        useSubstring: Boolean = false,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS
    ): SemanticsNodeInteraction {
        waitUntil(timeoutMs) {
            onAllNodes(hasText(text, substring = useSubstring))
                .fetchSemanticsNodes().isNotEmpty()
        }
        return onNode(hasText(text, substring = useSubstring))
    }

    /**
     * Wait for a node with specific content description to exist.
     * 
     * @param description The content description to search for
     * @param timeoutMs Maximum time to wait in milliseconds
     * @return The SemanticsNodeInteraction if found
     * @throws AssertionError if node not found within timeout
     */
    fun ComposeContentTestRule.waitForNodeWithContentDescription(
        description: String,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS
    ): SemanticsNodeInteraction {
        waitUntil(timeoutMs) {
            onAllNodes(hasContentDescription(description))
                .fetchSemanticsNodes().isNotEmpty()
        }
        return onNodeWithContentDescription(description)
    }

    /**
     * Wait for a node with specific test tag to exist.
     * 
     * @param tag The test tag to search for
     * @param timeoutMs Maximum time to wait in milliseconds
     * @return The SemanticsNodeInteraction if found
     */
    fun ComposeContentTestRule.waitForNodeWithTag(
        tag: String,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS
    ): SemanticsNodeInteraction {
        waitUntil(timeoutMs) {
            onAllNodes(hasTestTag(tag))
                .fetchSemanticsNodes().isNotEmpty()
        }
        return onNode(hasTestTag(tag))
    }

    /**
     * Wait for loading indicators to disappear.
     * 
     * @param loadingDescription Content description of loading indicator
     * @param timeoutMs Maximum time to wait in milliseconds
     */
    fun ComposeContentTestRule.waitForLoadingToComplete(
        loadingDescription: String = "Loading",
        timeoutMs: Long = DEFAULT_TIMEOUT_MS
    ) {
        waitUntil(timeoutMs) {
            onAllNodes(hasContentDescription(loadingDescription))
                .fetchSemanticsNodes().isEmpty()
        }
    }

    /**
     * Type text character by character with delays.
     * Useful for testing search input behavior.
     * 
     * @param node The node to type into
     * @param text The text to type
     * @param delayMs Delay between characters in milliseconds
     */
    suspend fun typeTextSlowly(
        node: SemanticsNodeInteraction,
        text: String,
        delayMs: Long = 100
    ) {
        text.forEach { char ->
            node.performTextInput(char.toString())
            kotlinx.coroutines.delay(delayMs)
        }
    }

    /**
     * Check if a node exists without throwing an assertion error.
     * 
     * @param text The text to search for
     * @return true if node exists, false otherwise
     */
    fun SemanticsNodeInteractionsProvider.nodeExistsWithText(text: String): Boolean {
        return try {
            onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
}
