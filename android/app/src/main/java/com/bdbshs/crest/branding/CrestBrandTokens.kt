// Generated from brand.tokens.json v1.0.0. Do not edit directly.
// Uses the bundled inter_* font resources in the Android app.
package com.bdbshs.crest.branding

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bdbshs.crest.R

const val CrestBrandVersion = "1.0.0"

val CrestInter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

val CrestLightColorScheme = lightColorScheme(
    primary = Color(0xFF2457D6),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE8EEFF),
    onPrimaryContainer = Color(0xFF16377F),
    secondary = Color(0xFF0E7075),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDDF3F2),
    onSecondaryContainer = Color(0xFF064F54),
    tertiary = Color(0xFF505E7B),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE8EDF7),
    onTertiaryContainer = Color(0xFF29364F),
    background = Color(0xFFF5F7FB),
    onBackground = Color(0xFF14233B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF14233B),
    surfaceVariant = Color(0xFFE9EEF5),
    onSurfaceVariant = Color(0xFF526078),
    surfaceTint = Color(0xFF2457D6),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF8FAFD),
    surfaceContainer = Color(0xFFEFF3F9),
    surfaceContainerHigh = Color(0xFFE9EEF5),
    surfaceContainerHighest = Color(0xFFE1E7F0),
    error = Color(0xFFB42336),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFDE9ED),
    onErrorContainer = Color(0xFF82172A),
    outline = Color(0xFF768398),
    outlineVariant = Color(0xFFD6DEEA),
    inverseSurface = Color(0xFF1F3048),
    inverseOnSurface = Color(0xFFF5F7FB),
    inversePrimary = Color(0xFFB6CAFF),
    scrim = Color(0xFF000000),
)

val CrestDarkColorScheme = darkColorScheme(
    primary = Color(0xFFB6CAFF),
    onPrimary = Color(0xFF102B68),
    primaryContainer = Color(0xFF203D7B),
    onPrimaryContainer = Color(0xFFE2EAFF),
    secondary = Color(0xFF88D6D3),
    onSecondary = Color(0xFF003C40),
    secondaryContainer = Color(0xFF084F54),
    onSecondaryContainer = Color(0xFFC3F1EE),
    tertiary = Color(0xFFC1CCE2),
    onTertiary = Color(0xFF27334B),
    tertiaryContainer = Color(0xFF3B4963),
    onTertiaryContainer = Color(0xFFE3EAF8),
    background = Color(0xFF0E1726),
    onBackground = Color(0xFFE8EEF8),
    surface = Color(0xFF152236),
    onSurface = Color(0xFFE8EEF8),
    surfaceVariant = Color(0xFF2A3A50),
    onSurfaceVariant = Color(0xFFB2BFD3),
    surfaceTint = Color(0xFFB6CAFF),
    surfaceContainerLowest = Color(0xFF0A1220),
    surfaceContainerLow = Color(0xFF111D2F),
    surfaceContainer = Color(0xFF19283E),
    surfaceContainerHigh = Color(0xFF22324A),
    surfaceContainerHighest = Color(0xFF2A3A50),
    error = Color(0xFFFFB2BE),
    onError = Color(0xFF660E23),
    errorContainer = Color(0xFF762037),
    onErrorContainer = Color(0xFFFFD9E0),
    outline = Color(0xFF8291A8),
    outlineVariant = Color(0xFF3B4D66),
    inverseSurface = Color(0xFFE8EEF8),
    inverseOnSurface = Color(0xFF14233B),
    inversePrimary = Color(0xFF2457D6),
    scrim = Color(0xFF000000),
)

@Immutable
data class CrestExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val warning: Color,
    val onWarning: Color,
    val info: Color,
    val onInfo: Color,
    val strandStem: Color,
    val onStrandStem: Color,
    val strandTvl: Color,
    val onStrandTvl: Color,
    val strandHumss: Color,
    val onStrandHumss: Color,
    val strandAbm: Color,
    val onStrandAbm: Color,
    val strandGas: Color,
    val onStrandGas: Color,
)

val CrestLightExtendedColors = CrestExtendedColors(
    success = Color(0xFFDDF3E8),
    onSuccess = Color(0xFF14623E),
    warning = Color(0xFFFFF1CE),
    onWarning = Color(0xFF785000),
    info = Color(0xFFE8EEFF),
    onInfo = Color(0xFF16377F),
    strandStem = Color(0xFFDDF3F2),
    onStrandStem = Color(0xFF064F54),
    strandTvl = Color(0xFFEFE9FB),
    onStrandTvl = Color(0xFF593292),
    strandHumss = Color(0xFFFFF1CE),
    onStrandHumss = Color(0xFF785000),
    strandAbm = Color(0xFFFDE9ED),
    onStrandAbm = Color(0xFF9C2947),
    strandGas = Color(0xFFE8EEFF),
    onStrandGas = Color(0xFF16377F),
)

val CrestDarkExtendedColors = CrestExtendedColors(
    success = Color(0xFF153D30),
    onSuccess = Color(0xFFA2E4BC),
    warning = Color(0xFF473717),
    onWarning = Color(0xFFF4D58A),
    info = Color(0xFF203D7B),
    onInfo = Color(0xFFE2EAFF),
    strandStem = Color(0xFF084F54),
    onStrandStem = Color(0xFFC3F1EE),
    strandTvl = Color(0xFF3A2858),
    onStrandTvl = Color(0xFFDCC5FF),
    strandHumss = Color(0xFF473717),
    onStrandHumss = Color(0xFFF4D58A),
    strandAbm = Color(0xFF512538),
    onStrandAbm = Color(0xFFFFBDCE),
    strandGas = Color(0xFF203D7B),
    onStrandGas = Color(0xFFE2EAFF),
)

val CrestTypography = Typography(
    displayLarge = TextStyle(fontFamily = CrestInter, fontWeight = FontWeight(700), fontSize = 48.sp, lineHeight = 56.sp, letterSpacing = -0.5.sp),
    displayMedium = TextStyle(fontFamily = CrestInter, fontWeight = FontWeight(700), fontSize = 36.sp, lineHeight = 44.sp, letterSpacing = -0.5.sp),
    displaySmall = TextStyle(fontFamily = CrestInter, fontWeight = FontWeight(700), fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = -0.25.sp),
    headlineLarge = TextStyle(fontFamily = CrestInter, fontWeight = FontWeight(700), fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = -0.25.sp),
    headlineMedium = TextStyle(fontFamily = CrestInter, fontWeight = FontWeight(600), fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = 0.sp),
    headlineSmall = TextStyle(fontFamily = CrestInter, fontWeight = FontWeight(600), fontSize = 22.sp, lineHeight = 30.sp, letterSpacing = 0.sp),
    titleLarge = TextStyle(fontFamily = CrestInter, fontWeight = FontWeight(600), fontSize = 20.sp, lineHeight = 28.sp, letterSpacing = 0.sp),
    titleMedium = TextStyle(fontFamily = CrestInter, fontWeight = FontWeight(600), fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.sp),
    titleSmall = TextStyle(fontFamily = CrestInter, fontWeight = FontWeight(600), fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.sp),
    bodyLarge = TextStyle(fontFamily = CrestInter, fontWeight = FontWeight(400), fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.sp),
    bodyMedium = TextStyle(fontFamily = CrestInter, fontWeight = FontWeight(400), fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.sp),
    bodySmall = TextStyle(fontFamily = CrestInter, fontWeight = FontWeight(400), fontSize = 12.sp, lineHeight = 18.sp, letterSpacing = 0.sp),
    labelLarge = TextStyle(fontFamily = CrestInter, fontWeight = FontWeight(600), fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.sp),
    labelMedium = TextStyle(fontFamily = CrestInter, fontWeight = FontWeight(500), fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.sp),
    labelSmall = TextStyle(fontFamily = CrestInter, fontWeight = FontWeight(500), fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.sp),
)

object CrestSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val compact = 12.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
    val hero = 64.dp
}

object CrestRadii {
    val small = 8.dp
    val control = 12.dp
    val card = 16.dp
    val sheet = 24.dp
    val pill = 999.dp
}

object CrestSizes {
    val touchMinimum = 48.dp
    val buttonMinimum = 48.dp
    val icon = 24.dp
    val screenGutter = 16.dp
    val wideGutter = 32.dp
    val readingMaxWidth = 720.dp
}

val CrestShapes = Shapes(
    extraSmall = RoundedCornerShape(CrestRadii.small),
    small = RoundedCornerShape(CrestRadii.control),
    medium = RoundedCornerShape(CrestRadii.card),
    large = RoundedCornerShape(CrestRadii.sheet),
    extraLarge = RoundedCornerShape(CrestRadii.sheet),
)

object CrestMotion {
    const val fast = 120
    const val standard = 200
    const val slow = 300
}
