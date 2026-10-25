package com.bdbshs.crest.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.bdbshs.crest.branding.CrestDarkColorScheme
import com.bdbshs.crest.branding.CrestDarkExtendedColors
import com.bdbshs.crest.branding.CrestExtendedColors
import com.bdbshs.crest.branding.CrestLightColorScheme
import com.bdbshs.crest.branding.CrestLightExtendedColors

private val LocalBrandColors = staticCompositionLocalOf { CrestLightExtendedColors }

val MaterialTheme.crestColors: CrestExtendedColors
    @Composable @ReadOnlyComposable get() = LocalBrandColors.current

@Composable
fun CRESTTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            (view.context as? Activity)?.window?.let { window ->
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }
    CompositionLocalProvider(
        LocalBrandColors provides if (darkTheme) CrestDarkExtendedColors else CrestLightExtendedColors,
        LocalSpacing provides Spacing(),
        LocalContentPadding provides ContentPadding(),
        LocalIconSizes provides IconSizes(),
        LocalTouchTargets provides TouchTargets(),
        LocalDimensions provides Dimensions()
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) CrestDarkColorScheme else CrestLightColorScheme,
            typography = Typography,
            shapes = CrestShapes,
            content = content
        )
    }
}
