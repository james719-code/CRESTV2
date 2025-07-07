package com.bdbshs.crest.ui.screens

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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bdbshs.crest.data.AppwriteClient
import com.bdbshs.crest.ui.screens.common.EmptyState
import com.bdbshs.crest.ui.screens.common.ShimmerListItemPlaceholder
import com.bdbshs.crest.ui.viewmodels.*
import kotlinx.coroutines.launch
import androidx.core.net.toUri

@SuppressLint("QueryPermissionsNeeded")
@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DocumentsScreen(
    userRole: UserType?,
    modifier: Modifier = Modifier,
    viewModel: DocumentsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val documentsToShow by viewModel.filteredAndSortedDocuments.collectAsState()
    val pullRefreshState = rememberPullRefreshState(uiState.isRefreshing, viewModel::onRefresh)
    val context = LocalContext.current // Context captured at Composable scope

    LaunchedEffect(userRole) {
        viewModel.setUserRole(userRole)
    }

    uiState.error?.let {
        LaunchedEffect(it) {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
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

    Column(modifier = modifier.fillMaxSize()) {
        SearchBarWithFilter(
            query = uiState.searchQuery,
            onQueryChange = viewModel::onSearchQueryChanged,
            onFilterClick = { isFilterSheetOpen = true }
        )
        HorizontalDivider()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pullRefresh(pullRefreshState)
        ) {
            when {
                uiState.isLoading -> {
                    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(10) { ShimmerListItemPlaceholder() }
                    }
                }
                documentsToShow.isEmpty() -> {
                    EmptyState(icon = Icons.Default.Description, message = "No documents found.\nTry a different search or sort.")
                }
                else -> {
                    LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(documentsToShow, key = { it.id }) { document ->
                            DocumentCard(
                                document = document,
                                userRole = uiState.userRole,
                                onClick = {
                                    // --- THIS IS THE FIX ---
                                    // Re-added the download to browser logic
                                    // --- THIS IS THE FIX ---
                                    // Use DownloadManager for more robust downloading
                                    val fileUri = AppwriteClient.getDownloadUrl(
                                        bucketId = viewModel.DOCUMENTS_BUCKET_ID,
                                        fileId = document.file_link
                                    )
                                    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                    val request = DownloadManager.Request(fileUri.toUri())
                                        .setTitle(document.name) // Title of the download notification
                                        .setDescription("Downloading ${document.name}") // Description of the download notification
                                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED) // Show notification
                                        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, document.name) // Save to Downloads folder
                                        .setAllowedOverMetered(true) // Allow download over mobile data
                                        .setAllowedOverRoaming(true) // Allow download over roaming

                                    try {
                                        downloadManager.enqueue(request)
                                        Toast.makeText(context, "Downloading ${document.name}...", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
                                        e.printStackTrace()
                                    }
                                },
                                onLongClick = { viewModel.onDocumentLongPress(document) }
                            )
                        }
                    }
                }
            }
            PullRefreshIndicator(uiState.isRefreshing, pullRefreshState, Modifier.align(Alignment.TopCenter))
        }
    }
}

// --- Card & Components ---

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DocumentCard(
    document: DocumentItem,
    userRole: UserType?,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val isTeacher = userRole == UserType.TEACHER
    Card(
        modifier = Modifier.fillMaxWidth().then(
            if (isTeacher) Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
            else Modifier.clickable(onClick = onClick)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = document.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = document.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = "Download",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// --- Filter/Sort Bottom Sheet ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DocumentFilterSortBottomSheet(
    sheetState: SheetState,
    uiState: DocumentsUiState,
    onDismiss: () -> Unit,
    onSortOptionSelected: (DocumentSortOption) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text("Sort Documents", style = MaterialTheme.typography.titleLarge)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Sort By", style = MaterialTheme.typography.titleMedium)
                DocumentSortOption.entries.forEach { option ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (uiState.selectedSortOption == option),
                                onClick = { onSortOptionSelected(option) },
                                role = androidx.compose.ui.semantics.Role.RadioButton
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = (uiState.selectedSortOption == option), onClick = null)
                        Spacer(Modifier.width(8.dp))
                        Text(option.displayName)
                    }
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


// --- Reusable Search Bar (from ResearchesScreen) ---
@Composable
private fun SearchBarWithFilter(
    query: String,
    onQueryChange: (String) -> Unit,
    onFilterClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Search document name or description...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            singleLine = true,
            shape = RoundedCornerShape(50)
        )
        IconButton(onClick = onFilterClick) {
            Icon(Icons.Default.FilterList, contentDescription = "Filter and Sort")
        }
    }
}