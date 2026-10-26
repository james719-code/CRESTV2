package com.bdbshs.crest.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
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
    onLongClick: () -> Unit = {},
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
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            Modifier.combinedClickable(onClickLabel = "Read paper", onClick = onClick, onLongClick = onLongClick)
                .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = if (showActions) 8.dp else 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PaperCover(research.strand, size = 36.dp)
                Spacer(Modifier.width(12.dp))
                FlowRow(
                    Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (research.strand.isNotBlank()) MetadataBadge(research.strand, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
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
                onManageClick?.let {
                    IconButton(onClick = it, Modifier.size(48.dp)) { Icon(Icons.Outlined.MoreVert, "Manage research") }
                }
            }
            Text(
                research.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = if (large) 8 else 3,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                research.members.joinToString(", ").ifBlank { "Author names not provided" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (large) 4 else 2,
                overflow = TextOverflow.Ellipsis
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                date?.let { InformationLine(Icons.Outlined.CalendarToday, it) }
                InformationLine(Icons.Outlined.Download,
                    "${research.downloads} ${if (research.downloads == 1) "download" else "downloads"}")
            }
            if (showActions) {
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
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier
) { ModernResearchCard(research, onClick, onLongClick, modifier = modifier) }
