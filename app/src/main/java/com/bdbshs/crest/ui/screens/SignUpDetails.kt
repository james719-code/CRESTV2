package com.bdbshs.crest.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bdbshs.crest.ui.viewmodels.SignUpDetailsViewModel
import com.bdbshs.crest.ui.viewmodels.UserDetails
import com.bdbshs.crest.ui.viewmodels.UserType
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@ExperimentalMaterial3Api
@Composable
fun SignUpDetails(
    onNavigateToHome: () -> Unit,
    viewModel: SignUpDetailsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // State for all possible input fields
    var fullName by rememberSaveable { mutableStateOf("") }
    var lrn by rememberSaveable { mutableStateOf("") }
    var strand by rememberSaveable { mutableStateOf("") }
    var gender by rememberSaveable { mutableStateOf("") }
    val email = Firebase.auth.currentUser?.email ?: ""

    // Pre-fill name from Google Account
    LaunchedEffect(Unit) {
        fullName = Firebase.auth.currentUser?.displayName ?: ""
    }

    // Listen for navigation events
    LaunchedEffect(Unit) {
        viewModel.navigateToHome.collect { onNavigateToHome() }
    }

    // Show error snackbar
    uiState.error?.let { errorMessage ->
        LaunchedEffect(errorMessage) {
            snackbarHostState.showSnackbar(errorMessage)
            viewModel.clearError()
        }
    }

    // REMOVED the `if (uiState.isRoleSelectionDialogVisible)` block completely

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { TopAppBar(title = { Text("Complete Your Profile") }) }
    ) { paddingValues ->
        // Use LazyColumn for better form handling on small screens
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(Modifier.height(32.dp))
                // Call the NEW RoleSelector
                RoleSelector(
                    selectedRole = uiState.selectedRole,
                    onRoleSelected = { viewModel.onRoleSelected(it) }
                )
                Spacer(Modifier.height(24.dp))
            }

            item {
                // This 'when' block remains the same
                when (uiState.selectedRole) {
                    UserType.STUDENT -> StudentInputFields(
                        name = fullName, onNameChange = { fullName = it },
                        lrn = lrn, onLrnChange = { lrn = it },
                        strand = strand, onStrandChange = { strand = it },
                        gender = gender, onGenderChange = { gender = it }
                    )
                    UserType.TEACHER -> TeacherInputFields(
                        name = fullName, onNameChange = { fullName = it },
                        email = email
                    )
                    null -> {
                        Text("Please select your account type to continue.")
                    }
                }
            }

            item {
                Spacer(Modifier.height(32.dp))
                // This save button logic remains the same
                val isFormValid = when(uiState.selectedRole) {
                    UserType.STUDENT -> fullName.isNotBlank() && lrn.isNotBlank() && strand.isNotBlank() && gender.isNotBlank()
                    UserType.TEACHER -> fullName.isNotBlank()
                    null -> false
                }
                Button(
                    onClick = {
                        // ... on click logic is the same ...
                        when (uiState.selectedRole) {
                            UserType.STUDENT -> viewModel.saveUserDetails(
                                UserDetails.Student(name = fullName, lrn = lrn.toLongOrNull() ?: 0, strand = strand, gender = gender)
                            )
                            UserType.TEACHER -> viewModel.saveUserDetails(
                                UserDetails.Teacher(name = fullName, email = email)
                            )
                            null -> {}
                        }
                    },
                    enabled = isFormValid && !uiState.isLoading,
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Save and Continue")
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

// THIS IS THE NEW, SIMPLIFIED SPINNER/DROPDOWN COMPONENT
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoleSelector(
    selectedRole: UserType?,
    onRoleSelected: (UserType) -> Unit
) {
    val options = UserType.entries.toTypedArray()
    var isExpanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = isExpanded,
        onExpandedChange = { isExpanded = !isExpanded }
    ) {
        OutlinedTextField(
            // The `menuAnchor` modifier is required.
            // It connects the TextField to the dropdown menu.
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            readOnly = true,
            value = selectedRole?.name?.replaceFirstChar { it.titlecase() } ?: "Select Account Type...",
            onValueChange = {},
            label = { Text("Account Type") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
        )
        ExposedDropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false }
        ) {
            options.forEach { selectionOption ->
                DropdownMenuItem(
                    text = { Text(selectionOption.name.replaceFirstChar { it.titlecase() }) },
                    onClick = {
                        onRoleSelected(selectionOption)
                        isExpanded = false
                    }
                )
            }
        }
    }
}


// REMOVED the old RoleSelectionDialog composable comple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudentInputFields(
    name: String, onNameChange: (String) -> Unit,
    lrn: String, onLrnChange: (String) -> Unit,
    strand: String, onStrandChange: (String) -> Unit,
    gender: String, onGenderChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(value = name, onValueChange = onNameChange, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = lrn, onValueChange = onLrnChange, label = { Text("LRN (Learner Reference Number)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = strand, onValueChange = onStrandChange, label = { Text("Strand (e.g., STEM, HUMSS)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = gender, onValueChange = onGenderChange, label = { Text("Gender") }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun TeacherInputFields(name: String, onNameChange: (String) -> Unit, email: String) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(value = name, onValueChange = onNameChange, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = email, onValueChange = {}, readOnly = true, label = { Text("Email Address") }, modifier = Modifier.fillMaxWidth())
    }
}