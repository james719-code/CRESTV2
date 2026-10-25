package com.bdbshs.crest.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.HowToReg
import androidx.compose.material.icons.outlined.NoteAdd
import com.bdbshs.crest.ui.components.ActionTile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bdbshs.crest.ui.components.AdaptivePair
import com.bdbshs.crest.ui.components.HomeSummary
import com.bdbshs.crest.ui.components.HomeSectionHeading
import com.bdbshs.crest.ui.components.HomeResearchRow
import com.bdbshs.crest.ui.components.HomeEmptyResearch
import com.bdbshs.crest.ui.viewmodels.DashboardCardItem
import com.bdbshs.crest.ui.viewmodels.SimpleResearch
import com.bdbshs.crest.ui.viewmodels.TeacherHomeUiState
import com.bdbshs.crest.ui.viewmodels.TeacherHomeViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherHomeScreen(
    modifier: Modifier = Modifier,
    onNavigateToUploadResearch: () -> Unit,
    onNavigateToResearchDetails: (String) -> Unit,
    onNavigateToAccounts: () -> Unit,
    onNavigateToGroups: () -> Unit,
    viewModel: TeacherHomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Error feedback
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it, withDismissAction = true)
            viewModel.clearError()
        }
    }

    // --- Document Upload Dialog ---
    if (uiState.isUploadDialogVisible) {
        UploadDocumentDialog(
            uiState = uiState,
            onDocumentNameChange = viewModel::onUploadDocumentNameChange,
            onDocumentDescriptionChange = viewModel::onUploadDocumentDescriptionChange,
            onFileSelected = viewModel::onUploadFileSelected,
            onFileCleared = viewModel::onUploadFileCleared,
            onDismiss = viewModel::dismissUploadDialog,
            onUpload = viewModel::uploadDocument
        )
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        TeacherHomeContent(
            uiState = uiState,
            onNavigateToUploadResearch = onNavigateToUploadResearch,
            onNavigateToResearchDetails = onNavigateToResearchDetails,
            onNavigateToAccounts = onNavigateToAccounts,
            onNavigateToGroups = onNavigateToGroups,
            onUploadDocument = viewModel::showUploadDialog,
            onRetry = viewModel::onRefresh,
            modifier = Modifier.padding(padding)
        )
    }
}

@Composable
internal fun TeacherHomeContent(
    uiState: TeacherHomeUiState,
    onNavigateToUploadResearch: () -> Unit,
    onNavigateToResearchDetails: (String) -> Unit,
    onNavigateToAccounts: () -> Unit,
    onNavigateToGroups: () -> Unit,
    onUploadDocument: () -> Unit,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit = {}
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val paperColumns = if (maxWidth >= 760.dp) 2 else 1
        LazyColumn(
            Modifier.widthIn(max = 840.dp).fillMaxSize().align(Alignment.TopCenter),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                HomeSummary(
                    uiState.teacherName.ifBlank { "Teacher workspace" },
                    "Your teaching workspace",
                    if (uiState.isLoading || uiState.loadError != null) emptyList() else
                        uiState.dashboardItems.mapNotNull {
                            when (it.title) {
                                "Total Researches" -> it.value to "Library papers"
                                "Total Accounts" -> it.value to "School accounts"
                                else -> null
                            }
                        }
                )
            }
            if (uiState.loadError != null) {
                item { com.bdbshs.crest.ui.components.FeedbackPanel("Workspace could not load", uiState.loadError, "Try again", onRetry) }
            }
            if (uiState.isLoading) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                        Text("Loading review counts and recent papers…", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else if (uiState.loadError == null) {
                item {
                    HomeSectionHeading("Review queue")
                }
                item {
                    val pendingPapers = uiState.dashboardItems.firstOrNull { it.title == "Pending Researches" }?.value
                    val pendingAccounts = uiState.dashboardItems.firstOrNull { it.title == "Pending Accounts" }?.value
                    AdaptivePair(
                        first = { tile ->
                            ActionTile(
                                Icons.Outlined.RateReview, "Review group submissions", tile,
                                supporting = pendingPapers?.let { "$it pending papers" } ?: "Open queue",
                                badge = pendingPapers, onClick = onNavigateToGroups,
                                container = if ((pendingPapers?.toIntOrNull() ?: 0) > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                iconContainer = MaterialTheme.colorScheme.surface,
                                iconContent = MaterialTheme.colorScheme.primary
                            )
                        },
                        second = { tile ->
                            ActionTile(
                                Icons.Outlined.HowToReg, "Review school accounts", tile,
                                supporting = pendingAccounts?.let { "$it pending accounts" } ?: "Open queue",
                                badge = pendingAccounts, onClick = onNavigateToAccounts,
                                container = if ((pendingAccounts?.toIntOrNull() ?: 0) > 0) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                                iconContainer = MaterialTheme.colorScheme.surface,
                                iconContent = MaterialTheme.colorScheme.secondary
                            )
                        }
                    )
                }
            }
            item {
                AdaptivePair(
                    first = { button ->
                        FilledTonalButton(
                            onClick = onNavigateToUploadResearch, modifier = button.heightIn(min = 56.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Icon(Icons.Outlined.NoteAdd, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Add research paper", maxLines = 2, textAlign = TextAlign.Center)
                        }
                    },
                    second = { button ->
                        FilledTonalButton(
                            onClick = onUploadDocument, modifier = button.heightIn(min = 56.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Icon(Icons.Outlined.UploadFile, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Upload document", maxLines = 2, textAlign = TextAlign.Center)
                        }
                    }
                )
            }
            item {
                HomeSectionHeading("Recent research papers", uiState.recentResearches.size.takeIf { it > 0 })
            }
            if (uiState.isLoading && uiState.recentResearches.isEmpty()) {
                items(2) { com.bdbshs.crest.ui.screens.common.ShimmerResearchCardPlaceholder() }
            } else if (uiState.recentResearches.isEmpty() && uiState.loadError == null) {
                item {
                    HomeEmptyResearch(
                        "No research papers added yet",
                        "Add a paper or review a group submission to grow the library.",
                        "Add research paper", onNavigateToUploadResearch
                    )
                }
            }
            items(uiState.recentResearches.chunked(paperColumns), key = { it.first().id }) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    row.forEach { research ->
                        HomeResearchRow(
                            research = com.bdbshs.crest.ui.viewmodels.ResearchItem(
                                id = research.id, title = research.title, members = research.members, strand = research.strand,
                                type = research.type, downloads = research.downloads, unfinished = research.unfinished, createdAt = research.date
                            ),
                            onClick = { onNavigateToResearchDetails(research.id) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (row.size < paperColumns) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun WorkspaceTaskCard(title: String, count: String?, description: String, onClick: () -> Unit, modifier: Modifier, secondary: Boolean = false) {
    Surface(
        onClick = onClick, modifier = modifier,
        color = if (secondary) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (secondary) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.small
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                count?.let { Text(it, style = MaterialTheme.typography.titleLarge) }
                Text(if (count == null) "Open queue" else description, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, Modifier.size(18.dp))
            }
            Text(title, style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
private fun TeacherDeskAction(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(onClick = onClick, color = Color.Transparent) {
        Column {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.secondary)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.primary)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun RecentResearchCard(research: SimpleResearch, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(research.title, style = MaterialTheme.typography.titleLarge)
        if (research.date > 0) {
            Text(
                remember(research.date) { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(research.date)) },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UploadDocumentDialog(
    uiState: TeacherHomeUiState,
    onDocumentNameChange: (String) -> Unit,
    onDocumentDescriptionChange: (String) -> Unit,
    onFileSelected: (Uri?, String?) -> Unit,
    onFileCleared: () -> Unit,
    onDismiss: () -> Unit,
    onUpload: () -> Unit
) {
    val context = LocalContext.current
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = getLocalFileNameFromUri(uri, context) 
            onFileSelected(uri, fileName)
        }
    }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.padding(16.dp),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(20.dp)
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
                            imageVector = Icons.Outlined.UploadFile,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column {
                        Text(
                            text = "New Document",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Upload a file to your collection",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Input Fields
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = uiState.uploadDocumentName,
                        onValueChange = onDocumentNameChange,
                        label = { Text("Document Name") },
                        placeholder = { Text("e.g. Research Proposal Template") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !uiState.isUploadingDocument,
                        shape = MaterialTheme.shapes.medium
                    )
                    OutlinedTextField(
                        value = uiState.uploadDocumentDescription,
                        onValueChange = onDocumentDescriptionChange,
                        label = { Text("Description") },
                        placeholder = { Text("Describe what this document is for...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        enabled = !uiState.isUploadingDocument,
                        shape = MaterialTheme.shapes.medium
                    )
                    
                    // Improved File Picker Area
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .background(
                                if (uiState.uploadSelectedFileName != null) 
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                else 
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                            .clickable(enabled = !uiState.isUploadingDocument) { 
                                filePickerLauncher.launch("*/*") 
                            }
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(MaterialTheme.shapes.small)
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (uiState.uploadSelectedFileName != null) 
                                        Icons.Default.UploadFile 
                                    else 
                                        Icons.Default.AttachFile,
                                    contentDescription = null,
                                    tint = if (uiState.uploadSelectedFileName != null) 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = uiState.uploadSelectedFileName ?: "Select a file",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (uiState.uploadSelectedFileName != null) FontWeight.Bold else FontWeight.Normal,
                                    color = if (uiState.uploadSelectedFileName != null) 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (uiState.uploadSelectedFileName != null) 
                                        "Tap to change file" 
                                    else 
                                        "Available formats: .pdf, .docx, .png, etc.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            if (uiState.uploadSelectedFileName != null && !uiState.isUploadingDocument) {
                                IconButton(
                                    onClick = onFileCleared,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, "Clear file", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }

                if (uiState.isUploadingDocument) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                        Text(
                            text = "Uploading...",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
                
                if (uiState.uploadSuccess) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "Document uploaded successfully!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        modifier = Modifier.weight(1f),
                        onClick = onDismiss,
                        enabled = !uiState.isUploadingDocument,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = onUpload,
                        enabled = !uiState.isUploadingDocument &&
                                uiState.uploadDocumentName.isNotBlank() &&
                                uiState.uploadDocumentDescription.isNotBlank() &&
                                uiState.uploadSelectedFileUri != null,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Default.CloudUpload, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Upload")
                    }
                }
            }
        }
    }
}

// Utility to get filename (copied from DocumentsScreen usage, renamed to avoid collision)
private fun getLocalFileNameFromUri(uri: Uri, context: Context): String? {
    var fileName: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index != -1) fileName = it.getString(index)
            }
        }
    }
    if (fileName == null) {
        fileName = uri.path
        val cut = fileName?.lastIndexOf('/')
        if (cut != null && cut != -1) {
            fileName = fileName?.substring(cut + 1)
        }
    }
    return fileName
}
