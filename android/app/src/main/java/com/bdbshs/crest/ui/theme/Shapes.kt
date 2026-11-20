package com.bdbshs.crest.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import com.bdbshs.crest.branding.CrestRadii
import com.bdbshs.crest.branding.CrestShapes as SharedCrestShapes
import androidx.compose.ui.unit.dp

// ==================== CUSTOM SHAPE SYSTEM ====================
// Consistent corner radiuses throughout the app

val CrestShapes = SharedCrestShapes

// ==================== CUSTOM SHAPE EXTENSIONS ====================
// Additional shapes for specific use cases

object CrestShapeTokens {
    val Pill = RoundedCornerShape(50)
    val TopSheet = RoundedCornerShape(
        topStart = 0.dp,
        topEnd = 0.dp,
        bottomStart = CrestRadii.sheet,
        bottomEnd = CrestRadii.sheet
    )
    val BottomSheet = RoundedCornerShape(
        topStart = CrestRadii.sheet,
        topEnd = CrestRadii.sheet,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    )
    val CardSmall = RoundedCornerShape(CrestRadii.control)
    val CardMedium = RoundedCornerShape(CrestRadii.card)
    val CardLarge = RoundedCornerShape(CrestRadii.card)
    val Button = RoundedCornerShape(CrestRadii.control)
    val ButtonPill = RoundedCornerShape(50)
    val Chip = RoundedCornerShape(50)
    val Dialog = RoundedCornerShape(CrestRadii.sheet)
    val FAB = RoundedCornerShape(CrestRadii.card)
}
