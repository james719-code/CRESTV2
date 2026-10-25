package com.bdbshs.crest.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Standardized spacing system for consistent UI layout
 * Based on 4dp grid system for visual rhythm
 */
data class Spacing(
    /** 4dp - Minimal spacing, used for tight layouts */
    val extraSmall: Dp = 4.dp,
    
    /** 8dp - Small spacing, used between closely related elements */
    val small: Dp = 8.dp,
    
    /** 12dp - Medium-small, used for compact card padding */
    val mediumSmall: Dp = 12.dp,
    
    /** 16dp - Standard spacing, primary padding value */
    val medium: Dp = 16.dp,
    
    /** 20dp - Medium-large, used for card internal padding */
    val mediumLarge: Dp = 20.dp,
    
    /** 24dp - Large spacing, used for section separation */
    val large: Dp = 24.dp,
    
    /** 32dp - Extra large spacing, used for major sections */
    val extraLarge: Dp = 32.dp,
    
    /** 48dp - Huge spacing, used for page-level spacing */
    val huge: Dp = 48.dp,
    
    /** 64dp - Massive spacing, used for hero sections */
    val massive: Dp = 64.dp
)

/**
 * Content padding values for different screen contexts
 */
data class ContentPadding(
    /** Horizontal padding for screen content */
    val horizontal: Dp = 16.dp,
    
    /** Vertical padding for screen content */
    val vertical: Dp = 16.dp,
    
    /** Padding inside cards */
    val card: Dp = 16.dp,
    
    /** Padding inside larger cards with more content */
    val cardLarge: Dp = 20.dp,
    
    /** Padding for list items */
    val listItem: Dp = 16.dp,
    
    /** Padding for dialogs */
    val dialog: Dp = 24.dp,
    
    /** Padding for bottom sheets */
    val bottomSheet: Dp = 24.dp
)

/**
 * Icon sizes for consistent iconography
 */
data class IconSizes(
    /** 16dp - Small inline icons */
    val small: Dp = 16.dp,
    
    /** 20dp - Compact icons */
    val compact: Dp = 20.dp,
    
    /** 24dp - Standard icon size (Material default) */
    val medium: Dp = 24.dp,
    
    /** 32dp - Large icons for emphasis */
    val large: Dp = 32.dp,
    
    /** 40dp - Extra large icons */
    val extraLarge: Dp = 40.dp,
    
    /** 48dp - Touch target size icons */
    val huge: Dp = 48.dp,
    
    /** 80dp - Hero/empty state icons */
    val hero: Dp = 80.dp,
    
    /** 120dp - Illustration icons */
    val illustration: Dp = 120.dp
)

/**
 * Touch target sizes for accessibility
 */
data class TouchTargets(
    /** 44dp - Minimum recommended touch target */
    val minimum: Dp = 44.dp,
    
    /** 48dp - Standard touch target (Material recommendation) */
    val standard: Dp = 48.dp,
    
    /** 56dp - Large touch target for primary actions */
    val large: Dp = 56.dp
)

/**
 * Component dimensions for consistency
 */
data class Dimensions(
    /** Bottom navigation bar height */
    val bottomNavHeight: Dp = 80.dp,
    
    /** Top app bar height */
    val topAppBarHeight: Dp = 64.dp,
    
    /** Standard button height */
    val buttonHeight: Dp = 48.dp,
    
    /** Large button height */
    val buttonHeightLarge: Dp = 56.dp,
    
    /** Small button height */
    val buttonHeightSmall: Dp = 36.dp,
    
    /** Standard card minimum height */
    val cardMinHeight: Dp = 80.dp,
    
    /** Avatar size small */
    val avatarSmall: Dp = 32.dp,
    
    /** Avatar size medium */
    val avatarMedium: Dp = 40.dp,
    
    /** Avatar size large */
    val avatarLarge: Dp = 56.dp,
    
    /** Avatar size extra large */
    val avatarExtraLarge: Dp = 80.dp,
    
    /** Divider thickness */
    val dividerThickness: Dp = 1.dp,
    
    /** Progress indicator size */
    val progressIndicatorSize: Dp = 48.dp,
    
    /** FAB size */
    val fabSize: Dp = 56.dp,
    
    /** Small FAB size */
    val fabSizeSmall: Dp = 40.dp,
    
    /** Search bar height */
    val searchBarHeight: Dp = 56.dp,
    
    /** Chip height */
    val chipHeight: Dp = 32.dp
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }
val LocalContentPadding = staticCompositionLocalOf { ContentPadding() }
val LocalIconSizes = staticCompositionLocalOf { IconSizes() }
val LocalTouchTargets = staticCompositionLocalOf { TouchTargets() }
val LocalDimensions = staticCompositionLocalOf { Dimensions() }

/**
 * Access spacing values in composables
 */
object CrestSpacing {
    val spacing: Spacing
        @Composable
        @ReadOnlyComposable
        get() = LocalSpacing.current
    
    val contentPadding: ContentPadding
        @Composable
        @ReadOnlyComposable
        get() = LocalContentPadding.current
    
    val iconSizes: IconSizes
        @Composable
        @ReadOnlyComposable
        get() = LocalIconSizes.current
    
    val touchTargets: TouchTargets
        @Composable
        @ReadOnlyComposable
        get() = LocalTouchTargets.current
    
    val dimensions: Dimensions
        @Composable
        @ReadOnlyComposable
        get() = LocalDimensions.current
}
