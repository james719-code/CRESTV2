package com.bdbshs.crest.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bdbshs.crest.ui.components.FilePicker
import com.bdbshs.crest.ui.viewmodels.*
import com.bdbshs.crest.utils.getFileNameFromUri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupUploadScreen(
    onNavigateBack: () -> Unit,
    viewModel: GroupUploadViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
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

    // Navigate back on successful upload
    if (uiState.isSuccess) {
        LaunchedEffect(Unit) {
            snackbarHostState.showSnackbar("Research submitted for review.")
            onNavigateBack()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Submit research") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        GroupUploadContent(
            uiState,
            viewModel::onTitleChange,
            viewModel::onTypeSelected,
            { filePickerLauncher.launch("application/pdf") },
            viewModel::onFileCleared,
            viewModel::submitResearchForReview,
            Modifier.padding(paddingValues)
        )
    }
}

@Composable
internal fun GroupUploadContent(
    state: GroupUploadUiState,
    onTitle: (String) -> Unit,
    onType: (ResearchType) -> Unit,
    onPickFile: () -> Unit,
    onClearFile: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ready = state.title.isNotBlank() && state.researchType != null && !state.selectedFileName.isNullOrBlank()
    Column(
        modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        com.bdbshs.crest.ui.components.ContentHeader("Submit your group's research", "Your teacher will review the paper and share feedback with your group.")
        Text("1. Research details", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = state.title, onValueChange = onTitle,
            label = { Text("Research title · required") },
            supportingText = { Text("Use the complete title printed on your paper.") },
            modifier = Modifier.fillMaxWidth(), enabled = !state.isLoading,
            isError = state.error != null && state.title.isBlank()
        )
        ResearchTypeDropdown(state.researchType, onType)
        Text("2. Research file", style = MaterialTheme.typography.titleMedium)
        Text("Select one PDF containing your complete research paper.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FilePicker(state.selectedFileName, onPickFile, onClearFile, enabled = !state.isLoading)
        if (state.error != null) {
            com.bdbshs.crest.ui.components.FeedbackPanel("Submission needs attention", state.error, "Try submission again", onSubmit)
        }
        if (state.isLoading) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text("Uploading your PDF and sending it for review…", style = MaterialTheme.typography.bodyMedium)
        } else {
            Text(if (ready) "Ready to submit. Check the title and PDF before continuing." else "Add a title, select a research type, and choose a PDF to continue.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Button(
            onClick = onSubmit, enabled = ready && !state.isLoading,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = MaterialTheme.shapes.small
        ) { Text(if (state.isLoading) "Submitting…" else "Submit for review") }
    }
}
