package com.bdbshs.crest.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bdbshs.crest.ui.theme.crestColors
import com.bdbshs.crest.ui.viewmodels.ResearchItem
import com.bdbshs.crest.ui.viewmodels.ResearchType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun ModernResearchCard(
    research: ResearchItem,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    showActions: Boolean = false,
    onDownloadClick: (() -> Unit)? = null,
    onShareClick: (() -> Unit)? = null,
    onFavoriteClick: (() -> Unit)? = null,
    isFavorite: Boolean = false,
    isFavoriteUpdating: Boolean = false,
    onManageClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val large = LocalDensity.current.fontScale > 1.3f
    val date = remember(research.createdAt) {
        if (research.createdAt > 0) SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(research.createdAt)) else null
    }
    Surface(
        modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            Modifier.combinedClickable(
                onClickLabel = "View paper", onClick = onClick,
                onLongClickLabel = if (onManageClick != null || onLongClick != null) "Manage research" else null,
                onLongClick = onManageClick ?: onLongClick
            ).padding(start = 20.dp, top = 12.dp, end = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FlowRow(
                    Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (research.strand.isNotBlank()) {
                        val strandColors = strandStyle(research.strand)
                        MetadataBadge(research.strand, strandColors.container, strandColors.content)
                    }
                    MetadataBadge(if (research.type == ResearchType.QUALITATIVE) "Qualitative" else "Quantitative")
                    if (research.unfinished) MetadataBadge("In progress", MaterialTheme.crestColors.warning, MaterialTheme.crestColors.onWarning)
                }
                onFavoriteClick?.let {
                    IconButton(onClick = it, enabled = !isFavoriteUpdating, modifier = Modifier.size(48.dp)) {
                        if (isFavoriteUpdating) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Icon(
                            if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            if (isFavorite) "Remove from favorites" else "Add to favorites",
                            tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Text(
                research.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = if (large) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                research.members.joinToString(", ").ifBlank { "Author names not provided" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (large) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 8.dp)
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                date?.let { InformationLine(Icons.Outlined.CalendarToday, it) }
                InformationLine(Icons.Outlined.Download,
                    "${research.downloads} ${if (research.downloads == 1) "download" else "downloads"}")
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(end = 8.dp))
            Row(Modifier.fillMaxWidth().heightIn(min = 36.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("View paper", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                }
                onManageClick?.let {
                    IconButton(onClick = it, Modifier.size(48.dp)) { Icon(Icons.Outlined.MoreVert, "Manage research") }
                }
            }
            if (showActions && (onDownloadClick != null || onShareClick != null)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    onDownloadClick?.let { TextButton(onClick = it) { Text("Download") } }
                    onShareClick?.let { TextButton(onClick = it) { Text("Share") } }
                }
            }
        }
    }
}

@Composable
fun CompactResearchCard(
    research: ResearchItem,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) { ModernResearchCard(research, onClick, onLongClick, modifier = modifier) }
