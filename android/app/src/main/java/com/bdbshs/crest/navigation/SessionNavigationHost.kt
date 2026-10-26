package com.bdbshs.crest.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.bdbshs.crest.ui.screens.SplashScreen

/** Keep navigation and its login collector alive during subsequent account loads. */
@Composable
internal fun SessionNavigationHost(
    isLoading: Boolean,
    initialDestination: () -> String,
    content: @Composable (String) -> Unit
) {
    var startDestination by remember { mutableStateOf<String?>(null) }
    if (startDestination == null) {
        if (isLoading) {
            SplashScreen()
            return
        }
        startDestination = initialDestination()
    }

    Box(Modifier.fillMaxSize()) {
        content(checkNotNull(startDestination))
        if (isLoading) {
            Surface(Modifier.fillMaxSize()) { SplashScreen() }
        }
    }
}
