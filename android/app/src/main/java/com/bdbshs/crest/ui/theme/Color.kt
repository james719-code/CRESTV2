package com.bdbshs.crest.ui.theme

import androidx.compose.ui.graphics.Color

// ==================== PRIMARY BRAND COLORS ====================
// Clean blue palette for a professional, normal app feel

val PrimaryBlue = Color(0xFF2563EB)             // Main brand blue
val PrimaryLight = Color(0xFF60A5FA)            // Light blue for accents
val PrimaryDark = Color(0xFF1E40AF)             // Deep blue for emphasis
val PrimaryContainer = Color(0xFFDBEAFE)        // Very light blue for containers

// Secondary blues for additional hierarchy
val SecondaryBlue = Color(0xFF3B82F6)           // Medium blue
val SecondaryLight = Color(0xFF93C5FD)          // Pale blue
val SecondaryDark = Color(0xFF1D4ED8)           // Darker blue

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

val TVLColor = Color(0xFF8B5CF6)      // Purple
val STEMColor = Color(0xFF10B981)     // Emerald
val HUMSSColor = Color(0xFFF59E0B)    // Amber
val ABMColor = Color(0xFFEF4444)      // Red
val GASColor = Color(0xFF22C55E)      // Green

// Strand colors with transparency for backgrounds
val TVLColorLight = Color(0x1A8B5CF6)
val STEMColorLight = Color(0x1A10B981)
val HUMSSColorLight = Color(0x1AF59E0B)
val ABMColorLight = Color(0x1AEF4444)
val GASColorLight = Color(0x1A22C55E)

// ==================== LIGHT THEME NEUTRALS ====================

val White = Color(0xFFFFFFFF)
val Black = Color(0xFF000000)

// Refined gray scale for light theme
val Gray50 = Color(0xFFFAFAFA)
val Gray100 = Color(0xFFF4F4F5)
val Gray200 = Color(0xFFE4E4E7)
val Gray300 = Color(0xFFD4D4D8)
val Gray400 = Color(0xFFA1A1AA)
val Gray500 = Color(0xFF71717A)
val Gray600 = Color(0xFF52525B)
val Gray700 = Color(0xFF3F3F46)
val Gray800 = Color(0xFF27272A)
val Gray900 = Color(0xFF18181B)

// ==================== LIGHT THEME SURFACES ====================

val BackgroundLight = Color(0xFFFAFAFC)        // Subtle neutral tint
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceVariantLight = Color(0xFFF4F4F5)
val SurfaceContainerLight = Color(0xFFF8F8FA)
val SurfaceContainerHighLight = Color(0xFFF0F0F4)

// ==================== DARK THEME SURFACES ====================

val BackgroundDark = Color(0xFF0F0F12)         // Deep background
val SurfaceDark = Color(0xFF18181B)            // Elevated surface
val SurfaceVariantDark = Color(0xFF27272A)
val SurfaceContainerDark = Color(0xFF1F1F23)
val SurfaceContainerHighDark = Color(0xFF2A2A2E)

// ==================== TEXT COLORS ====================

// Light theme text
val TextPrimaryLight = Color(0xFF18181B)
val TextSecondaryLight = Color(0xFF52525B)
val TextTertiaryLight = Color(0xFF71717A)

// Dark theme text
val TextPrimaryDark = Color(0xFFFAFAFA)
val TextSecondaryDark = Color(0xFFA1A1AA)
val TextTertiaryDark = Color(0xFF71717A)

// Legacy compatibility
val TextPrimary = TextPrimaryLight
val TextSecondary = TextSecondaryLight
val TextInverse = White

// ==================== ACTION & STATE COLORS ====================

// Success
val SuccessGreen = Color(0xFF22C55E)
val SuccessGreenLight = Color(0xFFBBF7D0)
val SuccessGreenDark = Color(0xFF16A34A)

// Warning
val WarningAmber = Color(0xFFF59E0B)
val WarningAmberLight = Color(0xFFFDE68A)
val WarningAmberDark = Color(0xFFD97706)

// Error
val ErrorRed = Color(0xFFEF4444)
val ErrorRedLight = Color(0xFFFECACA)
val ErrorRedDark = Color(0xFFDC2626)

// Info
val InfoBlue = Color(0xFF3B82F6)
val InfoBlueLight = Color(0xFFBFDBFE)
val InfoBlueDark = Color(0xFF2563EB)

// Legacy compatibility
val WarningOrange = WarningAmber

// ==================== UI STATE COLORS ====================

val DisabledLight = Color(0xFFD4D4D8)
val DisabledDark = Color(0xFF3F3F46)
val DividerLight = Color(0xFFE4E4E7)
val DividerDark = Color(0xFF27272A)

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
val CardGradientStart = Color(0xFFFAFAFC)
val CardGradientEnd = Color(0xFFF4F4F8)

// Dark card gradient
val CardGradientStartDark = Color(0xFF1F1F23)
val CardGradientEndDark = Color(0xFF27272A)
