package com.bdbshs.crest.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bdbshs.crest.ui.components.FilePicker
import com.bdbshs.crest.ui.viewmodels.*
import com.bdbshs.crest.utils.getFileNameFromUri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupUploadScreen(
    onNavigateBack: () -> Unit,
    viewModel: GroupUploadViewModel = viewModel()
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
            snackbarHostState.showSnackbar("Research Submitted for Review!")
            onNavigateBack()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Submit Group Research") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = uiState.title,
                onValueChange = viewModel::onTitleChange,
                label = { Text("Research Title") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Title, null) }
            )

            ResearchTypeDropdown( // Reuse from your main Upload screen
                selectedType = uiState.researchType,
                onTypeSelected = viewModel::onTypeSelected
            )

            FilePicker(
                fileName = uiState.selectedFileName,
                onPickFileClick = { filePickerLauncher.launch("application/pdf") },
                onClearFileClick = viewModel::onFileCleared,
                enabled = !uiState.isLoading,
            )

            Spacer(Modifier.weight(1f))

            Button(
                onClick = viewModel::submitResearchForReview,
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Submit for Review")
                }
            }
        }
    }
}
