package com.bdbshs.crest.ui.screens

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bdbshs.crest.data.FileCache
import com.bdbshs.crest.ui.screens.common.EmptyState
import com.bdbshs.crest.ui.theme.*
import com.bdbshs.crest.ui.viewmodels.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageManagementScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StorageManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show snackbar when message is available
    LaunchedEffect(uiState.message) {
        uiState.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    // Confirmation Dialog
    if (uiState.showConfirmDialog) {
        ConfirmationDialog(
            dialogType = uiState.confirmDialogType,
            selectedCount = uiState.selectedCount,
            selectedSize = uiState.selectedSize,
            onConfirm = viewModel::confirmAction,
            onDismiss = viewModel::dismissDialog
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Storage", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.cachedResearches.isNotEmpty()) {
                        IconButton(onClick = viewModel::toggleSelectAll) {
                            val allSelected = uiState.cachedResearches.all { it.isSelected }
                            Icon(
                                if (allSelected) Icons.Default.Deselect else Icons.Default.SelectAll,
                                contentDescription = if (allSelected) "Deselect All" else "Select All"
                            )
                        }
                    }
                    IconButton(onClick = viewModel::loadStorageData) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        bottomBar = {
            // Show action bar when items are selected
            if (uiState.selectedCount > 0) {
                SelectionActionBar(
                    selectedCount = uiState.selectedCount,
                    selectedSize = uiState.selectedSize,
                    isDeleting = uiState.isDeleting,
                    onDeleteClick = viewModel::showDeleteSelectedDialog
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        StorageContent(
            state = uiState,
            onRefresh = viewModel::loadStorageData,
            onClearPdf = viewModel::showClearPdfCacheDialog,
            onClearAll = viewModel::showClearAllCacheDialog,
            onToggleSelection = viewModel::toggleSelection,
            modifier = Modifier.padding(padding)
        )
    }
}

@Composable
internal fun StorageContent(
    state: StorageUiState,
    onRefresh: () -> Unit,
    onClearPdf: () -> Unit,
    onClearAll: () -> Unit,
    onToggleSelection: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        item {
            com.bdbshs.crest.ui.components.ContentHeader(
                "Saved paper storage",
                "Manage the downloaded research and temporary files on this device."
            )
        }
        state.error?.let { message ->
            item { com.bdbshs.crest.ui.components.FeedbackPanel("Storage could not load", message, "Try again", onRefresh) }
        }
        if (state.isLoading) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    Text("Checking saved files…", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else if (state.error == null) {
            item { StorageSummaryCard(state.pdfCacheSize, state.totalAppCacheSize, onClearPdf, onClearAll) }
            item { Text("Saved research PDFs · ${state.cachedResearches.size} ${if (state.cachedResearches.size == 1) "file" else "files"}", style = MaterialTheme.typography.titleMedium) }
            if (state.cachedResearches.isEmpty()) {
                item {
                    com.bdbshs.crest.ui.components.EmptyCollectionPanel(
                        "No papers saved on this device",
                        "Open a research PDF while online to save a copy for offline reading. This page lists the files stored locally, not every paper in the school library.",
                        "Refresh saved files", onRefresh,
                        listOf("Clearing storage" to "Removing a local copy does not delete the school research paper. It will need to be downloaded again before offline reading.")
                    )
                }
            }
        }
        items(state.cachedResearches, key = { it.fileId }) { item ->
            CachedResearchCard(item, { onToggleSelection(item.fileId) })
        }
    }
}

@Composable
private fun StorageSummaryCard(
    pdfCacheSize: Long,
    totalAppCacheSize: Long,
    onClearPdfCache: () -> Unit,
    onClearAllCache: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Storage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Column {
                    Text(
                        text = "Storage Usage",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Manage your cached files",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider()

            // Storage Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StorageStatItem(
                    icon = Icons.Outlined.PictureAsPdf,
                    label = "PDF Cache",
                    value = FileCache.formatSize(pdfCacheSize),
                    color = MaterialTheme.colorScheme.primary
                )
                StorageStatItem(
                    icon = Icons.Outlined.Folder,
                    label = "Total Cache",
                    value = FileCache.formatSize(totalAppCacheSize),
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            // Clear Cache Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onClearPdfCache,
                    modifier = Modifier.weight(1f),
                    enabled = pdfCacheSize > 0
                ) {
                    Icon(Icons.Outlined.DeleteSweep, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Clear PDFs")
                }
                Button(
                    onClick = onClearAllCache,
                    modifier = Modifier.weight(1f),
                    enabled = totalAppCacheSize > 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Clear All")
                }
            }
        }
    }
}

@Composable
private fun StorageStatItem(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(32.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CachedResearchCard(
    item: CachedResearchItem,
    onToggleSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleSelect),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
                Text(item.formattedSize, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Checkbox(checked = item.isSelected, onCheckedChange = { onToggleSelect() })
            }
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = listOfNotNull(item.strand.takeIf { it.isNotBlank() }, formatDate(item.lastModified)).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun SelectionActionBar(
    selectedCount: Int,
    selectedSize: Long,
    isDeleting: Boolean,
    onDeleteClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "$selectedCount selected",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = FileCache.formatSize(selectedSize),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }

            Button(
                onClick = onDeleteClick,
                enabled = !isDeleting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onError,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Delete")
                }
            }
        }
    }
}

@Composable
private fun ConfirmationDialog(
    dialogType: ConfirmDialogType,
    selectedCount: Int,
    selectedSize: Long,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val (title, message, icon) = when (dialogType) {
        ConfirmDialogType.DELETE_SELECTED -> Triple(
            "Delete Selected Files?",
            "This will delete $selectedCount cached file(s) (${FileCache.formatSize(selectedSize)}). You can re-download them later.",
            Icons.Default.Delete
        )
        ConfirmDialogType.CLEAR_PDF_CACHE -> Triple(
            "Clear PDF Cache?",
            "This will delete all cached research PDFs. You can re-download them later.",
            Icons.Default.DeleteSweep
        )
        ConfirmDialogType.CLEAR_ALL_CACHE -> Triple(
            "Clear All Cache?",
            "This will delete all app cache including PDFs and temporary files. This action cannot be undone.",
            Icons.Default.DeleteForever
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp)
            )
        },
        title = { Text(title) },
        text = { CrestDialogSystemBars(); Text(message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatDate(timestamp: Long): String {
    return try {
        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        sdf.format(Date(timestamp))
    } catch (e: Exception) {
        "Unknown date"
    }
}
