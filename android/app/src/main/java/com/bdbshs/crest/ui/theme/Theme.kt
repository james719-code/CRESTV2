package com.bdbshs.crest.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.ViewTreeObserver
import android.view.Window
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.window.DialogWindowProvider
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
        val activity = remember(view) { view.context.findActivity() }
        SideEffect {
            activity?.applySystemBarAppearance(darkTheme)
        }
        DisposableEffect(activity, darkTheme) {
            val observer = activity?.window?.decorView?.viewTreeObserver
            val listener = ViewTreeObserver.OnWindowFocusChangeListener { focused ->
                if (focused) activity?.applySystemBarAppearance(darkTheme)
            }
            observer?.addOnWindowFocusChangeListener(listener)
            onDispose {
                if (observer?.isAlive == true) observer.removeOnWindowFocusChangeListener(listener)
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

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun Activity.applySystemBarAppearance(darkTheme: Boolean) {
    window.applySystemBarAppearance(darkTheme)
}

private fun Window.applySystemBarAppearance(darkTheme: Boolean) {
    WindowCompat.getInsetsController(this, decorView).apply {
        isAppearanceLightStatusBars = !darkTheme
        isAppearanceLightNavigationBars = !darkTheme
    }
}

/** Material dialogs own a separate window and otherwise follow the device theme. */
@Composable
fun CrestDialogSystemBars() {
    val view = LocalView.current
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < .5f
    SideEffect {
        (view.parent as? DialogWindowProvider)?.window?.applySystemBarAppearance(darkTheme)
    }
}
