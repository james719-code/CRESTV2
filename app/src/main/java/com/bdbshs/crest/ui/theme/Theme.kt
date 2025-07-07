package com.bdbshs.crest.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = White, // Text/icons on primary color
    secondary = PrimaryLightBlue,
    onSecondary = White,
    tertiary = PrimaryHighlight,
    onTertiary = Black,

    background = BackgroundLight,
    onBackground = TextPrimary,

    surface = SurfaceLight,
    onSurface = TextPrimary,

    error = ErrorRed,
    onError = White,

    outline = Gray300,
    surfaceVariant = Gray100
)

@Composable
fun CRESTTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
