package com.bdbshs.crest.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ==================== LIGHT COLOR SCHEME ====================
private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = White,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = PrimaryDark,

    secondary = SecondaryBlue,
    onSecondary = White,
    secondaryContainer = SecondaryLight,
    onSecondaryContainer = SecondaryDark,

    tertiary = PrimaryLight,
    onTertiary = PrimaryDark,
    tertiaryContainer = PrimaryContainer,
    onTertiaryContainer = PrimaryDark,

    background = BackgroundLight,
    onBackground = TextPrimaryLight,

    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainerLowest = White,
    surfaceContainerLow = SurfaceContainerLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = Gray200,

    error = ErrorRed,
    onError = White,
    errorContainer = ErrorRedLight,
    onErrorContainer = ErrorRedDark,

    outline = Gray300,
    outlineVariant = Gray200,
    inverseSurface = Gray900,
    inverseOnSurface = Gray100,
    inversePrimary = PrimaryLight
)

// ==================== DARK COLOR SCHEME ====================
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryLight,
    onPrimary = PrimaryDark,
    primaryContainer = Color(0xFF1E3A8A), // Much darker blue (blue-900 equivalent)
    onPrimaryContainer = Color(0xFFDBEAFE), // Very light blue (blue-100 equivalent)

    secondary = SecondaryLight,
    onSecondary = SecondaryDark,
    secondaryContainer = Color(0xFF1E3A8A).copy(alpha = 0.5f), // Matching tone but distinctive
    onSecondaryContainer = Color(0xFFDBEAFE),

    tertiary = PrimaryLight,
    onTertiary = PrimaryDark,
    tertiaryContainer = SecondaryDark,
    onTertiaryContainer = PrimaryLight,

    background = BackgroundDark,
    onBackground = TextPrimaryDark,

    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerLowest = Color(0xFF0F0F12),
    surfaceContainerLow = SurfaceContainerDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = Gray700,

    error = Color(0xFFFCA5A5),
    onError = ErrorRedDark,
    errorContainer = ErrorRed,
    onErrorContainer = ErrorRedLight,

    outline = Gray600,
    outlineVariant = Gray700,
    inverseSurface = Gray100,
    inverseOnSurface = Gray900,
    inversePrimary = PrimaryBlue
)

// ==================== GRADIENT BRUSHES ====================
// These can be accessed from composables for gradient backgrounds

object CrestGradients {
    val Primary = Brush.linearGradient(
        colors = listOf(PrimaryBlue, PrimaryBlue.copy(alpha = 0.8f))
    )

    val PrimaryVertical = Brush.verticalGradient(
        colors = listOf(PrimaryBlue.copy(alpha = 0.05f), Color.Transparent)
    )

    val Accent = Brush.horizontalGradient(
        colors = listOf(PrimaryLight.copy(alpha = 0.5f), SecondaryLight.copy(alpha = 0.3f))
    )

    val SurfaceLight = Brush.verticalGradient(
        colors = listOf(CardGradientStart, CardGradientEnd)
    )

    val SurfaceDark = Brush.verticalGradient(
        colors = listOf(CardGradientStartDark, CardGradientEndDark)
    )

    val Success = Brush.horizontalGradient(
        colors = listOf(SuccessGreen, SuccessGreen.copy(alpha = 0.8f))
    )

    val Warm = Brush.horizontalGradient(
        colors = listOf(WarningAmber, WarningAmber.copy(alpha = 0.8f))
    )

    val CardOverlay = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.6f),
            Color.White.copy(alpha = 0.3f)
        )
    )
}

// ==================== THEME COMPOSABLE ====================
@Composable
fun CRESTTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set to true to use Material You
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Make status bar transparent for edge-to-edge
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    // Provide spacing and dimension tokens
    CompositionLocalProvider(
        LocalSpacing provides Spacing(),
        LocalContentPadding provides ContentPadding(),
        LocalIconSizes provides IconSizes(),
        LocalTouchTargets provides TouchTargets(),
        LocalDimensions provides Dimensions()
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = CrestShapes,
            content = content
        )
    }
}
