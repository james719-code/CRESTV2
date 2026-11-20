package com.bdbshs.crest.ui.theme

import com.bdbshs.crest.branding.CrestLightColorScheme
import com.bdbshs.crest.branding.CrestDarkColorScheme
import com.bdbshs.crest.branding.CrestLightExtendedColors
import com.bdbshs.crest.branding.CrestDarkExtendedColors

// Compatibility aliases only. Product composables use MaterialTheme.colorScheme
// and MaterialTheme.crestColors so values follow the active light/dark theme.

// ==================== PRIMARY BRAND COLORS ====================
// Clean blue palette for a professional, normal app feel

val PrimaryBlue = CrestLightColorScheme.primary
val PrimaryLight = CrestDarkColorScheme.primary
val PrimaryDark = CrestLightColorScheme.onPrimaryContainer
val PrimaryContainer = CrestLightColorScheme.primaryContainer

// Secondary blues for additional hierarchy
val SecondaryBlue = CrestLightColorScheme.secondary
val SecondaryLight = CrestDarkColorScheme.secondary
val SecondaryDark = CrestLightColorScheme.onSecondaryContainer

// Legacy compatibility - map to new colors
val PrimaryIndigo = PrimaryBlue
val PrimaryPurple = SecondaryBlue
val AccentCyan = SecondaryLight
val AccentTeal = SecondaryBlue
val AccentLight = PrimaryLight
val PrimaryLightBlue = PrimaryLight
val PrimaryHighlight = SecondaryLight

// ==================== STRAND COLOR CODING ====================
// Keep functional colors for student strands

val TVLColor = CrestLightExtendedColors.onStrandTvl
val STEMColor = CrestLightExtendedColors.onStrandStem
val HUMSSColor = CrestLightExtendedColors.onStrandHumss
val ABMColor = CrestLightExtendedColors.onStrandAbm
val GASColor = CrestLightExtendedColors.onStrandGas

// Strand colors with transparency for backgrounds
val TVLColorLight = CrestLightExtendedColors.strandTvl
val STEMColorLight = CrestLightExtendedColors.strandStem
val HUMSSColorLight = CrestLightExtendedColors.strandHumss
val ABMColorLight = CrestLightExtendedColors.strandAbm
val GASColorLight = CrestLightExtendedColors.strandGas

// ==================== LIGHT THEME NEUTRALS ====================

val White = CrestLightColorScheme.onPrimary
val Black = CrestLightColorScheme.scrim

// Refined gray scale for light theme
val Gray50 = CrestLightColorScheme.surfaceContainerLow
val Gray100 = CrestLightColorScheme.surfaceContainer
val Gray200 = CrestLightColorScheme.surfaceContainerHigh
val Gray300 = CrestLightColorScheme.outlineVariant
val Gray400 = CrestLightColorScheme.outline
val Gray500 = CrestLightColorScheme.onSurfaceVariant
val Gray600 = CrestLightColorScheme.onSurfaceVariant
val Gray700 = CrestLightColorScheme.onSurface
val Gray800 = CrestLightColorScheme.onSurface
val Gray900 = CrestLightColorScheme.onSurface

// ==================== LIGHT THEME SURFACES ====================

val BackgroundLight = CrestLightColorScheme.background
val SurfaceLight = CrestLightColorScheme.surface
val SurfaceVariantLight = CrestLightColorScheme.surfaceVariant
val SurfaceContainerLight = CrestLightColorScheme.surfaceContainer
val SurfaceContainerHighLight = CrestLightColorScheme.surfaceContainerHigh

// ==================== DARK THEME SURFACES ====================

val BackgroundDark = CrestDarkColorScheme.background
val SurfaceDark = CrestDarkColorScheme.surface
val SurfaceVariantDark = CrestDarkColorScheme.surfaceVariant
val SurfaceContainerDark = CrestDarkColorScheme.surfaceContainer
val SurfaceContainerHighDark = CrestDarkColorScheme.surfaceContainerHigh

// ==================== TEXT COLORS ====================

// Light theme text
val TextPrimaryLight = CrestLightColorScheme.onSurface
val TextSecondaryLight = CrestLightColorScheme.onSurfaceVariant
val TextTertiaryLight = CrestLightColorScheme.onSurfaceVariant

// Dark theme text
val TextPrimaryDark = CrestDarkColorScheme.onSurface
val TextSecondaryDark = CrestDarkColorScheme.onSurfaceVariant
val TextTertiaryDark = CrestDarkColorScheme.onSurfaceVariant

// Legacy compatibility
val TextPrimary = TextPrimaryLight
val TextSecondary = TextSecondaryLight
val TextInverse = White

// ==================== ACTION & STATE COLORS ====================

// Success
val SuccessGreen = CrestLightExtendedColors.onSuccess
val SuccessGreenLight = CrestLightExtendedColors.success
val SuccessGreenDark = CrestDarkExtendedColors.onSuccess

// Warning
val WarningAmber = CrestLightExtendedColors.onWarning
val WarningAmberLight = CrestLightExtendedColors.warning
val WarningAmberDark = CrestDarkExtendedColors.onWarning

// Error
val ErrorRed = CrestLightColorScheme.error
val ErrorRedLight = CrestLightColorScheme.errorContainer
val ErrorRedDark = CrestDarkColorScheme.error

// Info
val InfoBlue = CrestLightColorScheme.primary
val InfoBlueLight = CrestLightExtendedColors.info
val InfoBlueDark = CrestDarkColorScheme.primary

// Legacy compatibility
val WarningOrange = WarningAmber

// ==================== UI STATE COLORS ====================

val DisabledLight = CrestLightColorScheme.outlineVariant
val DisabledDark = CrestDarkColorScheme.outlineVariant
val DividerLight = CrestLightColorScheme.outlineVariant
val DividerDark = CrestDarkColorScheme.outlineVariant

// Legacy compatibility
val DisabledGray = DisabledLight
val DividerColor = DividerLight

// ==================== GRADIENT DEFINITIONS ====================
// Note: Actual Brush gradients are defined in Theme.kt
// These are the base colors for gradients

val GradientStart = PrimaryBlue
val GradientEnd = SecondaryBlue
val GradientAccentStart = PrimaryLight
val GradientAccentEnd = SecondaryLight

// Card gradient colors (subtle)
val CardGradientStart = CrestLightColorScheme.surface
val CardGradientEnd = CrestLightColorScheme.surfaceContainer

// Dark card gradient
val CardGradientStartDark = CrestDarkColorScheme.surface
val CardGradientEndDark = CrestDarkColorScheme.surfaceContainer
