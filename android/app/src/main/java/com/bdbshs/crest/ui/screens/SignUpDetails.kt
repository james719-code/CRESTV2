package com.bdbshs.crest.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bdbshs.crest.ui.viewmodels.LoginResult // Import LoginResult
import com.bdbshs.crest.ui.viewmodels.SignUpDetailsViewModel
import com.bdbshs.crest.ui.viewmodels.SignUpDetailsUiState
import com.bdbshs.crest.ui.viewmodels.UserType
import com.bdbshs.crest.ui.viewmodels.UserDetails
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpDetails(
    // Changed: Accepts LoginResult for navigation
    onNavigate: (LoginResult) -> Unit,
    viewModel: SignUpDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    // Input states for Student details (kept local to the screen for validation)
    var studentName by remember { mutableStateOf("") }
    var lrn by remember { mutableStateOf("") }

    // Input states for Teacher details (kept local)
    var teacherName by remember { mutableStateOf("") }
    var teacherEmail by remember { mutableStateOf("") }

    // Validation states (kept local for immediate UI feedback)
    var studentNameError by remember { mutableStateOf(false) }
    var lrnError by remember { mutableStateOf(false) }
    var strandError by remember { mutableStateOf(false) }
    var genderError by remember { mutableStateOf(false) }
    var teacherNameError by remember { mutableStateOf(false) }
    var teacherEmailError by remember { mutableStateOf(false) }

    // Strand options for the dropdown
    val strandOptions = remember { listOf("STEM", "GAS", "HUMSS", "TVL", "ABM") }
    // Gender options for the dropdown
    val genderOptions = remember { listOf("Male", "Female", "Other") }


    LaunchedEffect(viewModel) {
        // Changed: Collect from signUpNavigationEvent and pass the LoginResult
        viewModel.signUpNavigationEvent.collectLatest { result ->
            onNavigate(result)
        }
    }

    // Show error snackbar from ViewModel
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(
                message = it,
                actionLabel = "Dismiss",
                duration = SnackbarDuration.Short
            )
            viewModel.clearError() // Clear error after showing
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            // Show top bar only if a role is selected, allowing user to go back
            if (uiState.selectedRole != null) {
                CenterAlignedTopAppBar(
                    title = { Text("Complete Your Profile") },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.resetRoleSelection() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back to role selection")
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        // Use AnimatedContent for smooth transitions between role selection and forms
        AnimatedContent(
            targetState = uiState.selectedRole,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            transitionSpec = {
                // Defines the animation for content changes
                fadeIn(animationSpec = tween(300)) togetherWith
                        fadeOut(animationSpec = tween(300))
            },
            label = "role_form_animation"
        ) { targetRole ->
            when (targetRole) {
                null -> {
                    // Display role selection screen if no role is selected
                    // This is where both cards are rendered.
                    RoleSelectionScreen(
                        onStudentSelected = { viewModel.onRoleSelected(UserType.STUDENT) },
                        onTeacherSelected = { viewModel.onRoleSelected(UserType.TEACHER) }
                    )
                }
                UserType.STUDENT -> {
                    // Display student registration form
                    StudentSignUpForm(
                        uiState = uiState,
                        studentName = studentName,
                        onStudentNameChange = {
                            studentName = it
                            studentNameError = false // Clear error on change
                        },
                        lrn = lrn,
                        onLrnChange = {
                            // Allow only digits and check for 12 or 15 length
                            if (it.all { char -> char.isDigit() } && it.length <= 15) { // Max length is 15
                                lrn = it
                                lrnError = false // Clear error on change
                            }
                        },
                        strandOptions = strandOptions,
                        onStrandSelected = viewModel::onStrandSelected,
                        onStrandDropdownExpandedChange = viewModel::onStrandDropdownExpandedChange,
                        genderOptions = genderOptions,
                        onGenderSelected = viewModel::onGenderSelected,
                        onGenderDropdownExpandedChange = viewModel::onGenderDropdownExpandedChange,
                        studentNameError = studentNameError,
                        lrnError = lrnError,
                        strandError = strandError,
                        genderError = genderError,
                        onSaveDetails = {
                            // Validate inputs before saving
                            studentNameError = studentName.isBlank()
                            // LRN must be 12 or 15 digits
                            lrnError = lrn.isBlank() || !(lrn.length == 12 || lrn.length == 15)
                            strandError = uiState.selectedStrand.isBlank()
                            genderError = uiState.selectedGender.isBlank()

                            if (!studentNameError && !lrnError && !strandError && !genderError) {
                                focusManager.clearFocus() // Dismiss keyboard
                                viewModel.saveUserDetails(
                                    UserDetails.Student(
                                        studentName,
                                        lrn.toLong(), // Convert LRN to Long for saving
                                        uiState.selectedStrand,
                                        uiState.selectedGender
                                    )
                                )
                            } else {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Please fill all required fields correctly.")
                                }
                            }
                        }
                    )
                }
                UserType.TEACHER -> {
                    // Display teacher registration form
                    TeacherSignUpForm(
                        uiState = uiState,
                        teacherName = teacherName,
                        onTeacherNameChange = {
                            teacherName = it
                            teacherNameError = false // Clear error on change
                        },
                        teacherEmail = teacherEmail,
                        onTeacherEmailChange = {
                            teacherEmail = it
                            teacherEmailError = false // Clear error on change
                        },
                        teacherNameError = teacherNameError,
                        teacherEmailError = teacherEmailError,
                        onSaveDetails = {
                            // Validate inputs before saving
                            teacherNameError = teacherName.isBlank()
                            // Basic email format validation
                            teacherEmailError = teacherEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(teacherEmail).matches()

                            if (!teacherNameError && !teacherEmailError) {
                                focusManager.clearFocus() // Dismiss keyboard
                                viewModel.saveUserDetails(
                                    UserDetails.Teacher(
                                        teacherName,
                                        teacherEmail
                                    )
                                )
                            } else {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Please fill all required fields correctly.")
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RoleSelectionScreen(
    onStudentSelected: () -> Unit,
    onTeacherSelected: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "I am a...",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp),
            textAlign = TextAlign.Center
        )

        // Both student and teacher cards are explicitly added here.
        RoleSelectionCard(
            title = "Student",
            description = "Access researches and documents, manage groups, and track your progress.",
            onClick = onStudentSelected,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp)) // Space between the two cards
        RoleSelectionCard(
            title = "Teacher",
            description = "Manage accounts, oversee research submissions, and access teaching resources.",
            onClick = onTeacherSelected,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun RoleSelectionCard(
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .heightIn(max = 200.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudentSignUpForm(
    uiState: SignUpDetailsUiState,
    studentName: String,
    onStudentNameChange: (String) -> Unit,
    lrn: String,
    onLrnChange: (String) -> Unit,
    strandOptions: List<String>,
    onStrandSelected: (String) -> Unit,
    onStrandDropdownExpandedChange: (Boolean) -> Unit,
    genderOptions: List<String>,
    onGenderSelected: (String) -> Unit,
    onGenderDropdownExpandedChange: (Boolean) -> Unit,
    studentNameError: Boolean,
    lrnError: Boolean,
    strandError: Boolean,
    genderError: Boolean,
    onSaveDetails: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Student Details",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = studentName,
            onValueChange = onStudentNameChange,
            label = { Text("Full Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !uiState.isLoading,
            isError = studentNameError,
            supportingText = { if (studentNameError) Text("Name cannot be empty") }
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = lrn,
            onValueChange = onLrnChange,
            label = { Text("LRN (12 or 15 digits)") }, // Updated label
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            enabled = !uiState.isLoading,
            isError = lrnError,
            supportingText = {
                if (lrnError) {
                    if (lrn.isBlank()) Text("LRN cannot be empty")
                    // Updated error message for LRN length
                    else Text("LRN must be 12 or 15 digits")
                }
            }
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Modern Dropdown for Strand Selection
        ExposedDropdownMenuBox(
            expanded = uiState.isStrandDropdownExpanded,
            onExpandedChange = onStrandDropdownExpandedChange,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = uiState.selectedStrand,
                onValueChange = {}, // Read-only
                readOnly = true,
                label = { Text("Select Strand") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = uiState.isStrandDropdownExpanded) },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
                enabled = !uiState.isLoading,
                isError = strandError,
                supportingText = { if (strandError) Text("Please select a strand") }
            )
            ExposedDropdownMenu(
                expanded = uiState.isStrandDropdownExpanded,
                onDismissRequest = { onStrandDropdownExpandedChange(false) }
            ) {
                strandOptions.forEach { selectionOption ->
                    DropdownMenuItem(
                        text = { Text(selectionOption) },
                        onClick = {
                            onStrandSelected(selectionOption)
                            focusManager.clearFocus() // Dismiss keyboard
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Modern Dropdown for Gender Selection
        ExposedDropdownMenuBox(
            expanded = uiState.isGenderDropdownExpanded,
            onExpandedChange = onGenderDropdownExpandedChange,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = uiState.selectedGender,
                onValueChange = {}, // Read-only
                readOnly = true,
                label = { Text("Select Gender") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = uiState.isGenderDropdownExpanded) },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
                enabled = !uiState.isLoading,
                isError = genderError,
                supportingText = { if (genderError) Text("Please select your gender") }
            )
            ExposedDropdownMenu(
                expanded = uiState.isGenderDropdownExpanded,
                onDismissRequest = { onGenderDropdownExpandedChange(false) }
            ) {
                genderOptions.forEach { selectionOption ->
                    DropdownMenuItem(
                        text = { Text(selectionOption) },
                        onClick = {
                            onGenderSelected(selectionOption)
                            focusManager.clearFocus() // Dismiss keyboard
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onSaveDetails,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Register as Student")
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TeacherSignUpForm(
    uiState: SignUpDetailsUiState,
    teacherName: String,
    onTeacherNameChange: (String) -> Unit,
    teacherEmail: String,
    onTeacherEmailChange: (String) -> Unit,
    teacherNameError: Boolean,
    teacherEmailError: Boolean,
    onSaveDetails: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Teacher Details",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = teacherName,
            onValueChange = onTeacherNameChange,
            label = { Text("Full Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !uiState.isLoading,
            isError = teacherNameError,
            supportingText = { if (teacherNameError) Text("Name cannot be empty") }
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = teacherEmail,
            onValueChange = onTeacherEmailChange,
            label = { Text("School Email") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            enabled = !uiState.isLoading,
            isError = teacherEmailError,
            supportingText = {
                if (teacherEmailError) {
                    if (teacherEmail.isBlank()) Text("Email cannot be empty")
                    else Text("Enter a valid school email")
                }
            }
        )
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onSaveDetails,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Register as Teacher")
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun PreviewRoleSelectionScreen() {
    // This preview specifically shows the RoleSelectionScreen (both cards)
    MaterialTheme {
        RoleSelectionScreen(onStudentSelected = {}, onTeacherSelected = {})
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun PreviewStudentSignUpForm() {
    val viewModel: SignUpDetailsViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // To preview a specific state, you might need to manually set it in the ViewModel for the preview
    // For example: viewModel.onRoleSelected(UserType.STUDENT)
    MaterialTheme {
        StudentSignUpForm(
            uiState = uiState,
            studentName = "John Doe",
            onStudentNameChange = {},
            lrn = "123456789012", // Example LRN for preview
            onLrnChange = {},
            strandOptions = listOf("STEM", "GAS", "HUMSS", "TVL", "ABM"),
            onStrandSelected = {},
            onStrandDropdownExpandedChange = {},
            genderOptions = listOf("Male", "Female", "Other"),
            onGenderSelected = {},
            onGenderDropdownExpandedChange = {},
            studentNameError = false,
            lrnError = false,
            strandError = false,
            genderError = false,
            onSaveDetails = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun PreviewTeacherSignUpForm() {
    val viewModel: SignUpDetailsViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MaterialTheme {
        TeacherSignUpForm(
            uiState = uiState,
            teacherName = "Jane Smith",
            onTeacherNameChange = {},
            teacherEmail = "jane.smith@school.com",
            onTeacherEmailChange = {},
            teacherNameError = false,
            teacherEmailError = false,
            onSaveDetails = {}
        )
    }
}