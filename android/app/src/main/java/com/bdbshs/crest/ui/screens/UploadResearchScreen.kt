package com.bdbshs.crest.ui.screens

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bdbshs.crest.ui.components.FilePicker
import com.bdbshs.crest.ui.theme.CrestShapeTokens
import com.bdbshs.crest.ui.viewmodels.ResearchType
import com.bdbshs.crest.ui.viewmodels.Strand
import com.bdbshs.crest.ui.viewmodels.UploadResearchViewModel
import com.bdbshs.crest.utils.getFileNameFromUri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadResearchScreen(
    onNavigateBack: () -> Unit,
    viewModel: UploadResearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = getFileNameFromUri(uri, context)
            viewModel.onFileSelected(uri, fileName)
        }
    }

    if (uiState.isSuccess) {
        LaunchedEffect(Unit) {
            snackbarHostState.showSnackbar("Research paper added to the library.")
            onNavigateBack()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "Add research paper", 
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold 
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        TeacherUploadContent(
            uiState, viewModel::onTitleChange, viewModel::onStrandSelected, viewModel::onTypeSelected,
            viewModel::onMemberChange, viewModel::addMemberField, viewModel::removeMemberField,
            { filePickerLauncher.launch("application/pdf") }, viewModel::onFileCleared, viewModel::uploadResearch,
            Modifier.padding(paddingValues)
        )
    }
}

@Composable
internal fun TeacherUploadContent(
    state: com.bdbshs.crest.ui.viewmodels.UploadUiState,
    onTitle: (String) -> Unit,
    onStrand: (Strand) -> Unit,
    onType: (ResearchType) -> Unit,
    onMember: (Int, String) -> Unit,
    onAddMember: () -> Unit,
    onRemoveMember: (Int) -> Unit,
    onPickFile: () -> Unit,
    onClearFile: () -> Unit,
    onUpload: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ready = state.title.isNotBlank() && state.selectedStrand != null && state.selectedType != null &&
        state.members.isNotEmpty() && state.members.all { it.isNotBlank() } && !state.selectedFileName.isNullOrBlank()
    Column(
        modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        com.bdbshs.crest.ui.components.ContentHeader("Add research paper", "Share a PDF with complete author and research details in the school library.")
        Text("Research details", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            state.title, onTitle, modifier = Modifier.fillMaxWidth(), label = { Text("Research title · required") },
            supportingText = { Text("Use the complete title printed on the paper.") }, enabled = !state.isLoading
        )
        StrandDropdown(state.selectedStrand, onStrand)
        ResearchTypeDropdown(state.selectedType, onType)
        Text("Contributors", style = MaterialTheme.typography.titleMedium)
        Text("List the authors in the same order as the paper.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        state.members.forEachIndexed { index, member ->
            MemberInputField(member, { onMember(index, it) }, { onRemoveMember(index) }, state.members.size > 1)
        }
        TextButton(onClick = onAddMember) { Text("Add contributor") }
        Text("Research PDF", style = MaterialTheme.typography.titleMedium)
        FilePicker(state.selectedFileName, onPickFile, onClearFile, isLoading = state.isLoading)
        if (state.error != null) {
            com.bdbshs.crest.ui.components.FeedbackPanel("Paper could not be added", state.error, "Try upload again", onUpload)
        }
        if (state.isLoading) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text("Uploading the PDF and saving the research details…", style = MaterialTheme.typography.bodyMedium)
        } else {
            Text(if (ready) "Ready to add to the library." else "Complete the research details, contributor names, and PDF before uploading.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Button(onClick = onUpload, enabled = ready && !state.isLoading,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = MaterialTheme.shapes.small) {
            Text(if (state.isLoading) "Uploading…" else "Add research paper")
        }
    }
}

@Composable
private fun ModernFormSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            border = borderStroke()
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun borderStroke() = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)

@Composable
private fun ModernTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium) },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            leadingIcon = { 
                Icon(
                    leadingIcon, 
                    contentDescription = null, 
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                ) 
            },
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            singleLine = true
        )
    }
}

@Composable
private fun MemberInputField(
    name: String,
    onNameChange: (String) -> Unit,
    onRemove: () -> Unit,
    canBeRemoved: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically, 
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            placeholder = { Text("Author's full name") },
            modifier = Modifier.weight(1f),
            shape = MaterialTheme.shapes.small,
            leadingIcon = { 
                Icon(
                    Icons.Default.Person, 
                    contentDescription = null, 
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                ) 
            },
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                focusedBorderColor = MaterialTheme.colorScheme.primary
            ),
            singleLine = true
        )
        if (canBeRemoved) {
            FilledIconButton(
                onClick = onRemove,
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Close, contentDescription = "Remove Author", modifier = Modifier.size(20.dp))
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
        Strand("STEM"), Strand("HUMSS"), Strand("ABM"), Strand("TVL"), Strand("GAS")
    )
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Academic Strand",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )
        ExposedDropdownMenuBox(
            expanded = expanded, 
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedStrand?.name ?: "Select Strand",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                leadingIcon = { 
                    Icon(
                        Icons.Default.School, 
                        contentDescription = null, 
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    ) 
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                shape = MaterialTheme.shapes.small,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedTextColor = if (selectedStrand == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
            )
            ExposedDropdownMenu(
                expanded = expanded, 
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
            ) {
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResearchTypeDropdown(
    selectedType: ResearchType?,
    onTypeSelected: (ResearchType) -> Unit
) {
    val types = ResearchType.entries
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Research Category",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )
        ExposedDropdownMenuBox(
            expanded = expanded, 
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedType?.name?.replaceFirstChar { it.uppercase() } ?: "Select Category",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                leadingIcon = { 
                    Icon(
                        Icons.Default.Book, 
                        contentDescription = null, 
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    ) 
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                shape = MaterialTheme.shapes.small,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedTextColor = if (selectedType == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
            )
            ExposedDropdownMenu(
                expanded = expanded, 
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
            ) {
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
}
