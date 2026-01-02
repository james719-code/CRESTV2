package com.bdbshs.crest.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bdbshs.crest.ui.theme.*
import com.bdbshs.crest.ui.viewmodels.ResearchItem
import com.bdbshs.crest.ui.viewmodels.ResearchType
import java.text.SimpleDateFormat
import java.util.*

/**
 * Modernized research card with improved visual hierarchy,
 * strand color coding, and better information display
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ModernResearchCard(
    research: ResearchItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    showActions: Boolean = false,
    onDownloadClick: (() -> Unit)? = null,
    onShareClick: (() -> Unit)? = null,
    onFavoriteClick: (() -> Unit)? = null,
    isFavorite: Boolean = false,
    modifier: Modifier = Modifier
) {
    val hapticFeedback = LocalHapticFeedback.current
    val strandInfo = getStrandInfo(research.strand)
    
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        label = "cardScale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongClick()
                    }
                )
        ) {
            // Colored header bar with strand info
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(strandInfo.color)
            )
            
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Top row: Strand badge and status indicators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Strand chip with icon
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = strandInfo.color.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = strandInfo.icon,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = strandInfo.color
                            )
                            Text(
                                text = research.strand,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = strandInfo.color
                            )
                        }
                    }
                    
                    // Status indicators row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Research type badge
                        Surface(
                            shape = CircleShape,
                            color = if (research.type == ResearchType.QUALITATIVE)
                                InfoBlue.copy(alpha = 0.15f)
                            else
                                AccentTeal.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (research.type == ResearchType.QUALITATIVE) "QL" else "QN",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (research.type == ResearchType.QUALITATIVE) InfoBlue else AccentTeal,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        
                        // Unfinished indicator
                        if (research.unfinished) {
                            Surface(
                                shape = CircleShape,
                                color = WarningAmber.copy(alpha = 0.15f)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Construction,
                                    contentDescription = "In Progress",
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .size(16.dp),
                                    tint = WarningAmber
                                )
                            }
                        }
                    }
                }
                
                // Title
                Text(
                    text = research.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // Authors with avatar placeholder
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Author avatars (stacked)
                    Box {
                        val maxAvatars = minOf(3, research.members.size)
                        repeat(maxAvatars) { index ->
                            Box(
                                modifier = Modifier
                                    .offset(x = (index * 16).dp)
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(
                                        getAvatarColor(index)
                                    )
                                    .then(
                                        if (index > 0) Modifier.background(
                                            MaterialTheme.colorScheme.surface,
                                            CircleShape
                                        ) else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = research.members.getOrNull(index)?.firstOrNull()?.uppercase() ?: "?",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.width((research.members.size.coerceAtMost(3) * 16).dp))
                    
                    Text(
                        text = when {
                            research.members.isEmpty() -> "Unknown authors"
                            research.members.size == 1 -> research.members.first()
                            research.members.size == 2 -> research.members.joinToString(" & ")
                            else -> "${research.members.first()} +${research.members.size - 1}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // Bottom row: Stats and date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Stats row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Downloads
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Download,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatCount(research.downloads),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // Date
                    if (research.createdAt > 0) {
                        Text(
                            text = formatDate(research.createdAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Action buttons (optional)
                if (showActions) {
                    HorizontalDivider(
                        modifier = Modifier.padding(top = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        onDownloadClick?.let {
                            IconButton(onClick = it, modifier = Modifier.size(40.dp)) {
                                Icon(
                                    imageVector = Icons.Outlined.Download,
                                    contentDescription = "Download",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        onShareClick?.let {
                            IconButton(onClick = it, modifier = Modifier.size(40.dp)) {
                                Icon(
                                    imageVector = Icons.Outlined.Share,
                                    contentDescription = "Share",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        onFavoriteClick?.let {
                            IconButton(onClick = it, modifier = Modifier.size(40.dp)) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = "Favorite",
                                    tint = if (isFavorite) ErrorRed else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact version of research card for lists
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CompactResearchCard(
    research: ResearchItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val hapticFeedback = LocalHapticFeedback.current
    val strandInfo = getStrandInfo(research.strand)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongClick()
                    }
                )
                .height(IntrinsicSize.Min)
        ) {
            // Colored side bar
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(strandInfo.color)
            )
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Title
                Text(
                    text = research.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Authors
                Text(
                    text = "by ${research.members.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Bottom row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Strand badge
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = strandInfo.color.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = research.strand,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = strandInfo.color,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        
                        if (research.unfinished) {
                            Icon(
                                imageVector = Icons.Filled.Construction,
                                contentDescription = "In Progress",
                                modifier = Modifier.size(14.dp),
                                tint = WarningAmber
                            )
                        }
                    }
                    
                    // Downloads count
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Download,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatCount(research.downloads),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// --- Helper functions and data ---

data class StrandInfo(
    val color: Color,
    val icon: ImageVector
)

fun getStrandInfo(strand: String): StrandInfo {
    return when (strand.uppercase()) {
        "STEM" -> StrandInfo(STEMColor, Icons.Outlined.Science)
        "HUMSS" -> StrandInfo(HUMSSColor, Icons.Outlined.Psychology)
        "ABM" -> StrandInfo(ABMColor, Icons.Outlined.BusinessCenter)
        "TVL" -> StrandInfo(TVLColor, Icons.Outlined.Construction)
        "GAS" -> StrandInfo(GASColor, Icons.Outlined.School)
        else -> StrandInfo(Gray500, Icons.Outlined.Article)
    }
}

fun getAvatarColor(index: Int): Color {
    val colors = listOf(
        PrimaryIndigo,
        AccentTeal,
        PrimaryPurple,
        AccentCyan,
        SuccessGreen
    )
    return colors[index % colors.size]
}

private fun formatCount(count: Int): String {
    return when {
        count >= 1000000 -> "${count / 1000000}M"
        count >= 1000 -> "${count / 1000}K"
        else -> count.toString()
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
