package com.bdbshs.crest.ui.screens.drawer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * A generic placeholder screen that displays a title.
 * It correctly accepts and applies a modifier from its parent.
 */
@Composable
fun PlaceholderScreen(
    screenName: String,
    modifier: Modifier = Modifier
) {
    Box(
        // The incoming modifier (with padding) is applied first, then our own.
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$screenName Screen",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}