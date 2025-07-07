package com.bdbshs.crest.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bdbshs.crest.ui.viewmodels.ResearchType
import com.bdbshs.crest.ui.viewmodels.Strand
import com.bdbshs.crest.ui.viewmodels.UploadResearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadResearchScreen(
    onNavigateBack: () -> Unit,
    viewModel: UploadResearchViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // --- State and Launchers ---
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = getFileNameFromUri(uri, context)
            viewModel.onFileSelected(uri, fileName)
        }
    }

    // Show error snackbar
    uiState.error?.let {
        LaunchedEffect(it) {
            snackbarHostState.showSnackbar(it, withDismissAction = true)
            viewModel.clearError()
        }
    }

    // Navigate back on successful upload
    if (uiState.isSuccess) {
        LaunchedEffect(Unit) {
            snackbarHostState.showSnackbar("Upload Successful!")
            // A small delay can feel better than an instant navigation
            // kotlinx.coroutines.delay(500)
            onNavigateBack()
        }
    }

    // --- Main UI ---
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Upload New Research") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(24.dp), // Increased spacing for sections
            contentPadding = PaddingValues(16.dp)
        ) {
            // --- Research Details Section ---
            item {
                FormSection(title = "Research Details") {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = uiState.title,
                            onValueChange = viewModel::onTitleChange,
                            label = { Text("Research Title") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Title, contentDescription = null) }
                        )
                        StrandDropdown(
                            selectedStrand = uiState.selectedStrand,
                            onStrandSelected = viewModel::onStrandSelected
                        )
                        ResearchTypeDropdown(
                            selectedType = uiState.selectedType,
                            onTypeSelected = viewModel::onTypeSelected
                        )
                    }
                }
            }

            // --- Authors Section ---
            item {
                FormSection(title = "Authors") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        uiState.members.forEachIndexed { index, memberName ->
                            MemberInputField(
                                name = memberName,
                                onNameChange = { newName -> viewModel.onMemberChange(index, newName) },
                                onRemove = { viewModel.removeMemberField(index) },
                                canBeRemoved = uiState.members.size > 1
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = viewModel::addMemberField,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Member")
                            Spacer(Modifier.width(8.dp))
                            Text("Add Another Member")
                        }
                    }
                }
            }

            // --- Document Section ---
            item {
                FormSection(title = "Document") {
                    FilePicker(
                        fileName = uiState.selectedFileName,
                        onPickFileClick = { filePickerLauncher.launch("application/pdf") },
                        onClearFileClick = viewModel::onFileCleared,
                        enabled = !uiState.isLoading,
                    )
                }
            }

            // --- Upload Button ---
            item {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = viewModel::uploadResearch,
                    enabled = !uiState.isLoading,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 3.dp
                        )
                    } else {
                        Icon(Icons.Default.Upload, contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Text("Upload Research", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}


// --- Modern Helper Composables ---

@Composable
private fun FormSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 12.dp),
            color = MaterialTheme.colorScheme.primary
        )
        Column(content = content)
    }
}

@Composable
private fun MemberInputField(
    name: String,
    onNameChange: (String) -> Unit,
    onRemove: () -> Unit,
    canBeRemoved: Boolean
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Full Name") },
            modifier = Modifier.weight(1f),
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
        )
        if (canBeRemoved) {
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, contentDescription = "Remove Member")
            }
        } else {
            // Spacer to keep alignment consistent when remove button is not present
            Spacer(modifier = Modifier.width(48.dp))
        }
    }
}

@Composable
fun FilePicker(
    fileName: String?,
    onPickFileClick: () -> Unit,
    onClearFileClick: () -> Unit,
    enabled: Boolean
) {
    val shape = RoundedCornerShape(12.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(shape)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = shape
            )
            .clickable(enabled = fileName == null, onClick = onPickFileClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (fileName == null) {
            Icon(
                Icons.Default.AttachFile,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Text("Select PDF File", modifier = Modifier.weight(1f))
            // Add a spacer to align with the clear button in the other state
            Spacer(Modifier.width(48.dp))
        } else {
            Icon(
                // A more specific icon for PDFs
                Icons.Default.PictureAsPdf,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = fileName,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            IconButton(onClick = onClearFileClick) {
                Icon(Icons.Default.Clear, contentDescription = "Clear selected file")
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StrandDropdown(
    selectedStrand: Strand?,
    onStrandSelected: (Strand) -> Unit
) {
    val strands = listOf(
        Strand("STEM"), Strand("HUMSS"), Strand("ABM"), Strand("TVL-ICT"), Strand("GAS")
    )
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            // FIX: Use empty string for no selection; label provides context.
            value = selectedStrand?.name ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("Strand") },
            leadingIcon = { Icon(Icons.Default.School, contentDescription = null) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            // FIX: Correct way to set text color for read-only fields for better visibility.
            colors = TextFieldDefaults.colors(
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedTextColor = MaterialTheme.colorScheme.onSurface
            )
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            strands.forEach { strand ->
                DropdownMenuItem(
                    text = { Text(strand.name) },
                    onClick = {
                        onStrandSelected(strand)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResearchTypeDropdown(
    selectedType: ResearchType?,
    onTypeSelected: (ResearchType) -> Unit
) {
    // FIX: .entries returns a List, no need for .toTypedArray()
    val types = ResearchType.entries
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            // FIX: Use empty string for no selection.
            value = selectedType?.name?.replaceFirstChar { it.uppercase() } ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("Research Type") },
            leadingIcon = { Icon(Icons.Default.Book, contentDescription = null) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            // FIX: Correct way to set text color.
            colors = TextFieldDefaults.colors(
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedTextColor = MaterialTheme.colorScheme.onSurface
            )
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            types.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.name.replaceFirstChar { it.uppercase() }) },
                    onClick = {
                        onTypeSelected(type)
                        expanded = false
                    }
                )
            }
        }
    }
}

// FIX: This incorrect function has been removed.

// Utility function (unchanged)
fun getFileNameFromUri(uri: Uri, context: Context): String? {
    var fileName: String? = null
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1) {
                fileName = cursor.getString(nameIndex)
            }
        }
    }
    return fileName
}