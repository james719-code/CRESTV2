package com.bdbshs.crest.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bdbshs.crest.ui.screens.common.ActionBottomSheet
import com.bdbshs.crest.ui.screens.common.SheetAction
import com.bdbshs.crest.ui.viewmodels.DashboardCardItem
import com.bdbshs.crest.ui.viewmodels.SimpleResearch // Import the correct data type
import com.bdbshs.crest.ui.viewmodels.TeacherHomeUiState
import com.bdbshs.crest.ui.viewmodels.TeacherHomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherHomeScreen(
    modifier: Modifier = Modifier,
    onNavigateToUploadResearch: () -> Unit,
    onNavigateToResearchDetails: (String) -> Unit,
    viewModel: TeacherHomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Error feedback
    uiState.error?.let {
        LaunchedEffect(it) {
            snackbarHostState.showSnackbar(it, withDismissAction = true)
            viewModel.clearError()
        }
    }

    // --- Bottom Sheet & Dialogs ---
    var showActionBottomSheet by remember { mutableStateOf(false) }

    val teacherActions = listOf(
        SheetAction("Upload a Research", Icons.Outlined.Description) {
            onNavigateToUploadResearch()
            showActionBottomSheet = false
        },
        SheetAction("Upload a Document", Icons.Outlined.UploadFile) {
            viewModel.showUploadDialog() // Trigger upload dialog
            showActionBottomSheet = false
        }
    )

    if (showActionBottomSheet) {
        ActionBottomSheet(
            onDismiss = { showActionBottomSheet = false },
            actions = teacherActions
        )
    }

    // --- NEW: Document Upload Dialog ---
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showActionBottomSheet = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { paddingValues ->
        // Main content area
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item { GreeterCard(teacherName = uiState.teacherName) }
                if (uiState.dashboardItems.isNotEmpty()) {
                    item { DashboardGrid(items = uiState.dashboardItems) }
                }
                if (uiState.recentResearches.isNotEmpty()) {
                    item {
                        Text(
                            text = "Recent Researches",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    items(uiState.recentResearches, key = { it.id }) { research ->
                        SimpleResearchListItem(
                            research = research,
                            onClick = { onNavigateToResearchDetails(research.id) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
        }
    }
}

// --- NEW: Upload Document Dialog Composable ---
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
            val fileName = getFileNameFromUri(uri, context) // Reusing helper from DocumentsScreen
            onFileSelected(uri, fileName)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Upload New Document") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = uiState.uploadDocumentName,
                    onValueChange = onDocumentNameChange,
                    label = { Text("Document Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !uiState.isUploadingDocument
                )
                OutlinedTextField(
                    value = uiState.uploadDocumentDescription,
                    onValueChange = onDocumentDescriptionChange,
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    enabled = !uiState.isUploadingDocument
                )
                FilePicker(
                    fileName = uiState.uploadSelectedFileName,
                    onPickFileClick = { filePickerLauncher.launch("*/*") }, // Allow any file type
                    onClearFileClick = onFileCleared,
                    enabled = !uiState.isUploadingDocument
                )

                if (uiState.isUploadingDocument) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                // Optional: Show success message
                if (uiState.uploadSuccess) {
                    Text("Document uploaded successfully!", color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onUpload,
                enabled = !uiState.isUploadingDocument &&
                        uiState.uploadDocumentName.isNotBlank() &&
                        uiState.uploadDocumentDescription.isNotBlank() &&
                        uiState.uploadSelectedFileUri != null
            ) {
                Text("Upload")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !uiState.isUploadingDocument) {
                Text("Cancel")
            }
        }
    )
}


// --- NEW COMPOSABLE: Specifically for SimpleResearch data ---
@Composable
private fun SimpleResearchListItem(research: SimpleResearch, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Article,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = research.title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "View Details",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


// GreeterCard now takes a name parameter to display the teacher's name
@Composable
private fun GreeterCard(teacherName: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Text("Welcome back,", style = MaterialTheme.typography.titleMedium)
            if (teacherName.isNotBlank()) {
                Text(teacherName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}


@Composable
private fun DashboardGrid(items: List<DashboardCardItem>) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            if (items.isNotEmpty()) {
                Box(modifier = Modifier.weight(1f)) { DashboardCard(item = items[0]) }
            }
            if (items.size >= 2) {
                Box(modifier = Modifier.weight(1f)) { DashboardCard(item = items[1]) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            if (items.size >= 3) {
                Box(modifier = Modifier.weight(1f)) { DashboardCard(item = items[2]) }
            }
            if (items.size >= 4) {
                Box(modifier = Modifier.weight(1f)) { DashboardCard(item = items[3]) }
            }
        }
    }
}

@Composable
private fun DashboardCard(item: DashboardCardItem) {
    Card(
        modifier = Modifier.aspectRatio(1f), // Keeps the card square
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column {
                Text(
                    text = item.value,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}