package com.bdbshs.crest.ui.screens

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bdbshs.crest.ui.components.*
import com.bdbshs.crest.ui.screens.common.EmptyState
import com.bdbshs.crest.ui.screens.common.ShimmerListItemPlaceholder
import com.bdbshs.crest.ui.viewmodels.*
import com.bdbshs.crest.utils.getFileNameFromUri
import kotlinx.coroutines.launch
import androidx.core.net.toUri

@SuppressLint("QueryPermissionsNeeded")
@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DocumentsScreen(
    userRole: UserType?,
    modifier: Modifier = Modifier,
    viewModel: DocumentsViewModel = hiltViewModel(),
    onGoHome: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val documentsToShow by viewModel.filteredAndSortedDocuments.collectAsStateWithLifecycle()
    val pullRefreshState = rememberPullRefreshState(uiState.isRefreshing, viewModel::onRefresh)
    val context = LocalContext.current // Context captured at Composable scope
    val downloadScope = rememberCoroutineScope()

    LaunchedEffect(userRole) {
        viewModel.setUserRole(userRole)
    }

    if (uiState.isEditDialogVisible && uiState.selectedDocumentForEdit != null) {
        DocumentEditDialog(
            document = uiState.selectedDocumentForEdit!!,
            editedName = uiState.editedDocumentName,
            onEditedNameChange = viewModel::onEditedNameChange,
            editedDescription = uiState.editedDocumentDescription,
            onEditedDescriptionChange = viewModel::onEditedDescriptionChange,
            newSelectedFileName = uiState.newSelectedFileName,
            onNewFileSelected = viewModel::onNewFileSelected,
            onNewFileCleared = viewModel::onNewFileCleared,
            isUpdating = uiState.isUpdatingDocument,
            onDismiss = viewModel::dismissEditDialog,
            onUpdate = viewModel::updateDocument,
            onDelete = viewModel::deleteDocument
        )
    }

    var isFilterSheetOpen by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    if (isFilterSheetOpen) {
        DocumentFilterSortBottomSheet(
            sheetState = sheetState,
            uiState = uiState,
            onDismiss = { isFilterSheetOpen = false },
            onSortOptionSelected = viewModel::onSortOptionSelected
        )
    }

    DocumentsContent(
        state = uiState,
        documents = documentsToShow,
        onQuery = viewModel::onSearchQueryChanged,
        onFilter = { isFilterSheetOpen = true },
        onRefresh = viewModel::onRefresh,
        onDownload = { document ->
          downloadScope.launch {
            try {
                val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val request = DownloadManager.Request(viewModel.downloadUrl(document.file_link).toUri())
                    .setTitle(document.name)
                    .setDescription("Downloading ${document.name}")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, document.name)
                    .setAllowedOverMetered(true)
                    .setAllowedOverRoaming(true)
                downloadManager.enqueue(request)
                Toast.makeText(context, "Downloading ${document.name}…", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                Toast.makeText(context, "Could not start the download. Try again.", Toast.LENGTH_LONG).show()
            }
          }
        },
        onManage = viewModel::onDocumentLongPress,
        onGoHome = onGoHome,
        modifier = modifier
    )
}

// --- Filter/Sort Bottom Sheet ---

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun DocumentFilterSortBottomSheet(
    sheetState: SheetState,
    uiState: DocumentsUiState,
    onDismiss: () -> Unit,
    onSortOptionSelected: (DocumentSortOption) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        com.bdbshs.crest.ui.theme.CrestDialogSystemBars()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Sort Documents",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = {
                    onSortOptionSelected(DocumentSortOption.DateNewest)
                }) {
                    Text("Reset", fontWeight = FontWeight.SemiBold)
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            LazyColumn(
                modifier = Modifier.weight(1f, fill = false),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                // Sort Section
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilterSectionTitle(icon = Icons.AutoMirrored.Filled.Sort, title = "Sort By")
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DocumentSortOption.entries.forEach { option ->
                                val isSelected = uiState.selectedSortOption == option
                                ModernFilterChip(
                                    label = option.displayName,
                                    isSelected = isSelected,
                                    onClick = { onSortOptionSelected(option) }
                                )
                            }
                        }
                    }
                }
            }

            // Action Button
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 2.dp,
                shadowElevation = 8.dp
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.FilterList, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Apply Changes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// --- Teacher Edit Dialog ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DocumentEditDialog(
    document: DocumentItem,
    editedName: String,
    onEditedNameChange: (String) -> Unit,
    editedDescription: String,
    onEditedDescriptionChange: (String) -> Unit,
    newSelectedFileName: String?,
    onNewFileSelected: (Uri?, String?) -> Unit,
    onNewFileCleared: () -> Unit,
    isUpdating: Boolean,
    onDismiss: () -> Unit,
    onUpdate: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = getFileNameFromUri(uri, context)
            onNewFileSelected(uri, fileName)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Document") },
        text = {
            com.bdbshs.crest.ui.theme.CrestDialogSystemBars()
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = editedName,
                    onValueChange = onEditedNameChange,
                    label = { Text("Document Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isUpdating
                )
                OutlinedTextField(
                    value = editedDescription,
                    onValueChange = onEditedDescriptionChange,
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    enabled = !isUpdating
                )
                FilePicker(
                    fileName = newSelectedFileName ?: "Current: ${document.name} (${document.file_link.take(8)}...)",
                    onPickFileClick = { filePickerLauncher.launch("*/*") },
                    onClearFileClick = onNewFileCleared,
                    enabled = !isUpdating
                )

                if (isUpdating) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            Button(onClick = onUpdate, enabled = !isUpdating) {
                Text("Update")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDelete, enabled = !isUpdating) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = onDismiss, enabled = !isUpdating) {
                    Text("Cancel")
                }
            }
        }
    )
}
