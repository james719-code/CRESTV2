package com.bdbshs.crest.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// ==================== CUSTOM SHAPE SYSTEM ====================
// Consistent corner radiuses throughout the app

val CrestShapes = Shapes(
    // Small components: chips, small buttons, text fields
    extraSmall = RoundedCornerShape(8.dp),

    // Compact cards, list items
    small = RoundedCornerShape(12.dp),

    // Standard cards, dialogs
    medium = RoundedCornerShape(16.dp),

    // Large cards, sheets
    large = RoundedCornerShape(24.dp),

    // Full-width sheets, expanded cards
    extraLarge = RoundedCornerShape(32.dp)
)

// ==================== CUSTOM SHAPE EXTENSIONS ====================
// Additional shapes for specific use cases

object CrestShapeTokens {
    val Pill = RoundedCornerShape(50)
    val TopSheet = RoundedCornerShape(
        topStart = 0.dp,
        topEnd = 0.dp,
        bottomStart = 24.dp,
        bottomEnd = 24.dp
    )
    val BottomSheet = RoundedCornerShape(
        topStart = 24.dp,
        topEnd = 24.dp,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    )
    val CardSmall = RoundedCornerShape(12.dp)
    val CardMedium = RoundedCornerShape(16.dp)
    val CardLarge = RoundedCornerShape(20.dp)
    val Button = RoundedCornerShape(12.dp)
    val ButtonPill = RoundedCornerShape(50)
    val Chip = RoundedCornerShape(8.dp)
    val Dialog = RoundedCornerShape(28.dp)
    val FAB = RoundedCornerShape(16.dp)
}
