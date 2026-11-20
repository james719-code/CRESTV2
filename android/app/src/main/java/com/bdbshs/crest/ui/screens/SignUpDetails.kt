package com.bdbshs.crest.ui.screens

import com.bdbshs.crest.ui.theme.crestScreenGutter

import com.bdbshs.crest.ui.components.CrestButton as Button
import com.bdbshs.crest.ui.components.CrestCard as Card
import com.bdbshs.crest.ui.components.CrestTextButton as TextButton
import com.bdbshs.crest.ui.components.CrestTextField as OutlinedTextField

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bdbshs.crest.branding.CrestBrandName
import com.bdbshs.crest.branding.CrestWordmarkTrackingEm
import com.bdbshs.crest.ui.components.CrestMark
import com.bdbshs.crest.ui.components.IconBadge
import com.bdbshs.crest.ui.theme.CRESTTheme
import com.bdbshs.crest.ui.viewmodels.LoginResult
import com.bdbshs.crest.ui.viewmodels.SignUpDetailsUiState
import com.bdbshs.crest.ui.viewmodels.SignUpDetailsViewModel
import com.bdbshs.crest.ui.viewmodels.UserDetails
import com.bdbshs.crest.ui.viewmodels.UserType
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun SignUpDetails(
    onNavigate: (LoginResult) -> Unit,
    onSignOut: () -> Unit = {},
    viewModel: SignUpDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    // The Google account name is only a starting point; the person can correct it.
    var fullName by rememberSaveable { mutableStateOf(uiState.accountName) }
    var lrn by rememberSaveable { mutableStateOf("") }

    var nameError by remember { mutableStateOf(false) }
    var lrnError by remember { mutableStateOf(false) }
    var strandError by remember { mutableStateOf(false) }
    var genderError by remember { mutableStateOf(false) }

    val strandOptions = remember { listOf("STEM", "GAS", "HUMSS", "TVL", "ABM") }
    val genderOptions = remember { listOf("Male", "Female", "Other") }

    LaunchedEffect(viewModel) {
        viewModel.signUpNavigationEvent.collectLatest { result -> onNavigate(result) }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(message = it, actionLabel = "Dismiss", duration = SnackbarDuration.Short)
            viewModel.clearError()
        }
    }

    val validationMessage = "Check the highlighted fields and try again."
    Scaffold(
        modifier = Modifier.imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(Modifier.fillMaxSize().padding(paddingValues)) {
            SignUpFrame(
                uiState = uiState,
                onBack = if (uiState.selectedRole != null) viewModel::resetRoleSelection else null,
                onSignOut = onSignOut
            ) {
                when (uiState.selectedRole) {
                    null -> RoleSelectionScreen(
                        onStudentSelected = { viewModel.onRoleSelected(UserType.STUDENT) },
                        onTeacherSelected = { viewModel.onRoleSelected(UserType.TEACHER) }
                    )

                    UserType.STUDENT -> StudentSignUpForm(
                        uiState = uiState,
                        studentName = fullName,
                        onStudentNameChange = { fullName = it; nameError = false },
                        lrn = lrn,
                        onLrnChange = {
                            if (it.all { char -> char.isDigit() } && it.length <= 15) {
                                lrn = it
                                lrnError = false
                            }
                        },
                        strandOptions = strandOptions,
                        onStrandSelected = { strandError = false; viewModel.onStrandSelected(it) },
                        onStrandDropdownExpandedChange = viewModel::onStrandDropdownExpandedChange,
                        genderOptions = genderOptions,
                        onGenderSelected = { genderError = false; viewModel.onGenderSelected(it) },
                        onGenderDropdownExpandedChange = viewModel::onGenderDropdownExpandedChange,
                        studentNameError = nameError,
                        lrnError = lrnError,
                        strandError = strandError,
                        genderError = genderError,
                        onSaveDetails = {
                            nameError = fullName.isBlank()
                            lrnError = lrn.isBlank() || !(lrn.length == 12 || lrn.length == 15)
                            strandError = uiState.selectedStrand.isBlank()
                            genderError = uiState.selectedGender.isBlank()
                            if (!nameError && !lrnError && !strandError && !genderError) {
                                focusManager.clearFocus()
                                viewModel.saveUserDetails(
                                    UserDetails.Student(fullName.trim(), lrn.toLong(), uiState.selectedStrand, uiState.selectedGender)
                                )
                            } else {
                                coroutineScope.launch { snackbarHostState.showSnackbar(validationMessage) }
                            }
                        }
                    )

                    UserType.TEACHER -> TeacherSignUpForm(
                        uiState = uiState,
                        teacherName = fullName,
                        onTeacherNameChange = { fullName = it; nameError = false },
                        teacherNameError = nameError,
                        onSaveDetails = {
                            nameError = fullName.isBlank()
                            if (!nameError) {
                                focusManager.clearFocus()
                                viewModel.saveUserDetails(UserDetails.Teacher(fullName.trim()))
                            } else {
                                coroutineScope.launch { snackbarHostState.showSnackbar(validationMessage) }
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Shared layout for every registration step: CREST header, step label, the Google account that is
 * being registered, then the step content. Showing the account here is what tells people their
 * email is already known, so no form asks for it again.
 */
@Composable
internal fun SignUpFrame(
    uiState: SignUpDetailsUiState,
    onBack: (() -> Unit)?,
    onSignOut: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            Modifier.widthIn(max = 480.dp).fillMaxWidth().padding(horizontal = crestScreenGutter()).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SignUpHeader(onBack)
            SignedInAccountCard(uiState.accountName, uiState.accountEmail, onSignOut, enabled = !uiState.isLoading)
            content()
        }
    }
}

@Composable
private fun SignUpHeader(onBack: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back to role selection") }
        }
        CrestMark(36.dp, Modifier.padding(start = if (onBack == null) 0.dp else 4.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            CrestBrandName,
            style = MaterialTheme.typography.titleLarge.copy(letterSpacing = CrestWordmarkTrackingEm.em),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SignedInAccountCard(name: String, email: String, onSignOut: () -> Unit, enabled: Boolean) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBadge(Icons.Outlined.AccountCircle, size = 40.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    name.ifBlank { "Google account" },
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (email.isNotBlank()) {
                    Text(
                        email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            TextButton(
                onClick = onSignOut,
                enabled = enabled,
                modifier = Modifier.semantics { contentDescription = "Use a different Google account" }
            ) { Text("Switch", style = MaterialTheme.typography.labelLarge) }
        }
    }
}

@Composable
private fun StepHeading(step: String, title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(step, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(
            title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() }
        )
        Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun RoleSelectionScreen(
    onStudentSelected: () -> Unit,
    onTeacherSelected: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StepHeading(
            "Step 1 of 2",
            "Choose your role",
            "A teacher reviews every new registration before it opens."
        )
        RoleSelectionCard(
            icon = Icons.Outlined.School,
            title = "I'm a student",
            abilities = listOf(
                "Browse and read research papers",
                "Join or create a research group",
                "Submit your PDF for teacher review"
            ),
            onClick = onStudentSelected
        )
        RoleSelectionCard(
            icon = Icons.Outlined.Badge,
            title = "I'm a teacher",
            abilities = listOf(
                "Review and approve submissions",
                "Approve student and teacher accounts",
                "Upload school documents and resources"
            ),
            onClick = onTeacherSelected
        )
        NextStepsCard()
    }
}

@Composable
private fun RoleSelectionCard(icon: ImageVector, title: String, abilities: List<String>, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconBadge(icon, size = 48.dp)
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                abilities.forEach { ability ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                        Icon(
                            Icons.Outlined.CheckCircle, null,
                            Modifier.size(20.dp).padding(top = 2.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Text(ability, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

/** Sets expectations on every step so people know what comes after they submit. */
@Composable
private fun NextStepsCard() {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("What happens next", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            listOf(
                "Pick your role and add your details.",
                "A teacher reviews and approves your registration.",
                "Reopen CREST to open the school library."
            ).forEachIndexed { index, text ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(28.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "${index + 1}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SubmitButton(isLoading: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth(), enabled = !isLoading) {
        if (isLoading) {
            CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
        } else {
            Text("Submit for approval", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StudentSignUpForm(
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
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StepHeading("Step 2 of 2", "Your student details", "Your teacher uses these to match you to your class.")

        OutlinedTextField(
            value = studentName,
            onValueChange = onStudentNameChange,
            label = { Text("Full name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !uiState.isLoading,
            isError = studentNameError,
            supportingText = if (studentNameError) ({ Text("Enter your full name.") }) else null
        )
        OutlinedTextField(
            value = lrn,
            onValueChange = onLrnChange,
            label = { Text("LRN") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            enabled = !uiState.isLoading,
            isError = lrnError,
            supportingText = {
                Text(
                    when {
                        lrnError && lrn.isBlank() -> "Enter your LRN."
                        lrnError -> "An LRN has 12 or 15 digits."
                        else -> "Learner Reference Number, 12 or 15 digits."
                    }
                )
            }
        )

        ExposedDropdownMenuBox(
            expanded = uiState.isStrandDropdownExpanded,
            onExpandedChange = onStrandDropdownExpandedChange,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = uiState.selectedStrand,
                onValueChange = {},
                readOnly = true,
                label = { Text("Strand") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = uiState.isStrandDropdownExpanded) },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                enabled = !uiState.isLoading,
                isError = strandError,
                supportingText = if (strandError) ({ Text("Choose your strand.") }) else null
            )
            ExposedDropdownMenu(
                expanded = uiState.isStrandDropdownExpanded,
                onDismissRequest = { onStrandDropdownExpandedChange(false) }
            ) {
                strandOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = { onStrandSelected(option); focusManager.clearFocus() }
                    )
                }
            }
        }

        ExposedDropdownMenuBox(
            expanded = uiState.isGenderDropdownExpanded,
            onExpandedChange = onGenderDropdownExpandedChange,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = uiState.selectedGender,
                onValueChange = {},
                readOnly = true,
                label = { Text("Gender") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = uiState.isGenderDropdownExpanded) },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                enabled = !uiState.isLoading,
                isError = genderError,
                supportingText = if (genderError) ({ Text("Choose an option.") }) else null
            )
            ExposedDropdownMenu(
                expanded = uiState.isGenderDropdownExpanded,
                onDismissRequest = { onGenderDropdownExpandedChange(false) }
            ) {
                genderOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = { onGenderSelected(option); focusManager.clearFocus() }
                    )
                }
            }
        }

        SubmitButton(uiState.isLoading, onSaveDetails)
        NextStepsCard()
    }
}

@Composable
internal fun TeacherSignUpForm(
    uiState: SignUpDetailsUiState,
    teacherName: String,
    onTeacherNameChange: (String) -> Unit,
    teacherNameError: Boolean,
    onSaveDetails: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StepHeading(
            "Step 2 of 2",
            "Your teacher details",
            "An approved teacher will review your request."
        )

        OutlinedTextField(
            value = teacherName,
            onValueChange = onTeacherNameChange,
            label = { Text("Full name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !uiState.isLoading,
            isError = teacherNameError,
            supportingText = if (teacherNameError) ({ Text("Enter your full name.") }) else null
        )
        SubmitButton(uiState.isLoading, onSaveDetails)
        NextStepsCard()
    }
}

private val previewAccount = SignUpDetailsUiState(accountName = "Jane Santos", accountEmail = "jane.santos@school.edu.ph")

@Preview(name = "Role selection", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun PreviewRoleSelectionScreen() {
    CRESTTheme { SignUpFrame(previewAccount, null, {}) { RoleSelectionScreen({}, {}) } }
}

@Preview(name = "Teacher · Dark", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun PreviewTeacherSignUpForm() {
    CRESTTheme(darkTheme = true) {
        SignUpFrame(previewAccount.copy(selectedRole = UserType.TEACHER), {}, {}) {
            TeacherSignUpForm(previewAccount, "Jane Santos", {}, false, {})
        }
    }
}

@Preview(name = "Student · Large text", showBackground = true, widthDp = 360, heightDp = 900, fontScale = 1.6f)
@Composable
private fun PreviewStudentSignUpForm() {
    CRESTTheme {
        SignUpFrame(previewAccount.copy(selectedRole = UserType.STUDENT), {}, {}) {
            StudentSignUpForm(
                previewAccount, "Jane Santos", {}, "123456789012", {}, listOf("STEM", "GAS", "HUMSS", "TVL", "ABM"),
                {}, {}, listOf("Male", "Female", "Other"), {}, {}, false, false, false, false, {}
            )
        }
    }
}
