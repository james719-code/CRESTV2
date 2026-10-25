package com.bdbshs.crest.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bdbshs.crest.ui.theme.crestColors
import java.util.Calendar

private val ColorScheme.isDark: Boolean get() = background.luminance() < 0.5f

/** Light: solid CREST blue. Dark: the deeper blue container, so the hero does not glare on a dark page. */
@Composable
fun heroContainer(): Color = MaterialTheme.colorScheme.let { if (it.isDark) it.primaryContainer else it.primary }

@Composable
fun heroContent(): Color = MaterialTheme.colorScheme.let { if (it.isDark) it.onPrimaryContainer else it.onPrimary }

/** Original book-stack artwork for hero cards and empty states. Not a replacement for the app icon. */
@Composable
fun CrestIllustration(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val bookA = heroContent().copy(alpha = .55f)
    val bookB = if (scheme.isDark) scheme.secondary else scheme.secondaryContainer
    val page = heroContent()
    val ink = heroContainer().copy(alpha = .55f)
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val r = CornerRadius(w * .03f)
        drawRoundRect(bookA, Offset(w * .04f, h * .30f), Size(w * .22f, h * .70f), r)
        drawRoundRect(bookB, Offset(w * .30f, h * .08f), Size(w * .19f, h * .92f), r)
        rotate(10f, Offset(w * .78f, h)) {
            drawRoundRect(page, Offset(w * .56f, h * .22f), Size(w * .30f, h * .78f), r)
            repeat(3) {
                drawLine(
                    ink,
                    Offset(w * .61f, h * (.42f + it * .10f)), Offset(w * .81f, h * (.42f + it * .10f)),
                    2.dp.toPx()
                )
            }
        }
        drawLine(page.copy(alpha = .8f), Offset(0f, h), Offset(w, h), 3.dp.toPx())
        drawLine(ink, Offset(w * .09f, h * .42f), Offset(w * .21f, h * .42f), 2.dp.toPx())
        drawLine(ink, Offset(w * .09f, h * .50f), Offset(w * .21f, h * .50f), 2.dp.toPx())
    }
}

/** Solid brand surface. Colors come from the theme so light and dark both keep text contrast. */
@Composable
fun BrandHero(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = heroContainer(),
        contentColor = heroContent()
    ) {
        Column(Modifier.fillMaxWidth().padding(contentPadding), verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
    }
}

/** Tinted figure for use on [BrandHero]. */
@Composable
fun HeroStat(icon: ImageVector, value: String, label: String, modifier: Modifier = Modifier) {
    val on = heroContent()
    Row(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(on.copy(alpha = .14f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, Modifier.size(22.dp), tint = on)
        Column {
            Text(value, style = MaterialTheme.typography.titleLarge, color = on)
            Text(label, style = MaterialTheme.typography.labelMedium, color = on.copy(alpha = .85f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun HeroText(text: String, modifier: Modifier = Modifier, strong: Boolean = false) {
    val on = heroContent()
    Text(
        text,
        modifier,
        style = if (strong) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.bodyMedium,
        color = if (strong) on else on.copy(alpha = .85f)
    )
}

/** A time-aware greeting; avoids a generic "Welcome". */
fun timeGreeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 5..11 -> "Good morning"
    in 12..17 -> "Good afternoon"
    else -> "Good evening"
}

@Composable
fun IconBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    container: Color = MaterialTheme.colorScheme.primaryContainer,
    content: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    Box(
        modifier.size(size).clip(RoundedCornerShape(size * .3f)).background(container),
        contentAlignment = Alignment.Center
    ) { Icon(icon, null, Modifier.size(size * .5f), tint = content) }
}

data class StrandStyle(val container: Color, val content: Color, val icon: ImageVector)

/** Strand identity only; never used to signal approval or failure. */
@Composable
fun strandStyle(strand: String): StrandStyle {
    val c = MaterialTheme.crestColors
    return when (strand.trim().uppercase()) {
        "STEM" -> StrandStyle(c.strandStem, c.onStrandStem, Icons.Outlined.Science)
        "HUMSS" -> StrandStyle(c.strandHumss, c.onStrandHumss, Icons.Outlined.Psychology)
        "ABM" -> StrandStyle(c.strandAbm, c.onStrandAbm, Icons.Outlined.TrendingUp)
        "TVL" -> StrandStyle(c.strandTvl, c.onStrandTvl, Icons.Outlined.Build)
        "GAS" -> StrandStyle(c.strandGas, c.onStrandGas, Icons.Outlined.School)
        else -> StrandStyle(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, Icons.Outlined.MenuBook)
    }
}

/** Book-cover style tile that gives each paper a recognisable identity by strand. */
@Composable
fun PaperCover(strand: String, modifier: Modifier = Modifier, size: Dp = 64.dp) {
    val style = strandStyle(strand)
    Box(
        modifier.size(size).clip(RoundedCornerShape(size * .22f)).background(style.container),
        contentAlignment = Alignment.Center
    ) {
        Icon(style.icon, null, Modifier.size(size * .46f), tint = style.content)
        Box(
            Modifier.align(Alignment.CenterStart).width(size * .07f).fillMaxHeight().background(style.content.copy(alpha = .25f))
        )
    }
}

@Composable
fun InitialsAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    container: Color = MaterialTheme.colorScheme.primaryContainer,
    content: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    val initials = remember(name) {
        name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }.ifBlank { "?" }
    }
    Box(modifier.size(size).clip(CircleShape).background(container), contentAlignment = Alignment.Center) {
        Text(initials, style = MaterialTheme.typography.labelLarge, color = content, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun AvatarStack(names: List<String>, modifier: Modifier = Modifier, max: Int = 4, size: Dp = 32.dp) {
    if (names.isEmpty()) return
    val shown = names.take(max)
    val palette = listOf(
        MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer,
        MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
    )
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        shown.forEachIndexed { i, name ->
            val (bg, fg) = palette[i % palette.size]
            InitialsAvatar(
                name, Modifier.offset(x = -(size * .25f) * i).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface).padding(2.dp),
                size = size, container = bg, content = fg
            )
        }
        if (names.size > max) {
            Text("+${names.size - max}", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.offset(x = -(size * .25f) * shown.size + 8.dp))
        }
    }
}

/** Horizontal stepper. Steps before [current] are done, [current] is in progress, later ones are upcoming. */
@Composable
fun StepProgress(labels: List<String>, current: Int, modifier: Modifier = Modifier) {
    val done = MaterialTheme.colorScheme.primary
    val todo = MaterialTheme.colorScheme.outlineVariant
    Row(modifier.fillMaxWidth()) {
        labels.forEachIndexed { index, label ->
            val state = when { index < current -> 2; index == current -> 1; else -> 0 }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f).height(2.dp).background(if (index == 0) Color.Transparent else if (index <= current) done else todo))
                    Box(
                        Modifier.size(28.dp).clip(CircleShape)
                            .background(if (state == 2) done else if (state == 1) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        when (state) {
                            2 -> Icon(Icons.Filled.Check, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimary)
                            1 -> Box(Modifier.size(10.dp).clip(CircleShape).background(done))
                            else -> Text("${index + 1}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Box(Modifier.weight(1f).height(2.dp).background(if (index == labels.lastIndex) Color.Transparent else if (index < current) done else todo))
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    label, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center,
                    color = if (state == 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (state == 1) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

/** Large tappable tile with an icon; used for the main tasks on Home. */
@Composable
fun ActionTile(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    badge: String? = null,
    container: Color = MaterialTheme.colorScheme.surface,
    iconContainer: Color = MaterialTheme.colorScheme.primaryContainer,
    iconContent: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick, modifier = modifier, color = container, shape = MaterialTheme.shapes.large,
        border = if (container == MaterialTheme.colorScheme.surface) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(icon, container = iconContainer, content = iconContent)
                Spacer(Modifier.weight(1f))
                badge?.let {
                    Text(it, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                supporting?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}
