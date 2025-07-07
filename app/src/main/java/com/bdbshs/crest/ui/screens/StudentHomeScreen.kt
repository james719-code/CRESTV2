package com.bdbshs.crest.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bdbshs.crest.ui.viewmodels.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentHomeScreen(
    modifier: Modifier = Modifier,
    viewModel: StudentHomeViewModel = viewModel(),
    onNavigateToUpload: () -> Unit,
    onNavigateToResearchDetails: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var showJoinGroupDialog by remember { mutableStateOf(false) }
    var showUnsubmitDialog by remember { mutableStateOf(false) }

    // --- Side Effects & Dialogs ---
    uiState.error?.let {
        LaunchedEffect(it) {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    if (showCreateGroupDialog) {
        CreateGroupDialog(
            onDismiss = { showCreateGroupDialog = false },
            onCreate = { groupName -> viewModel.createGroup(groupName); showCreateGroupDialog = false }
        )
    }

    if (showJoinGroupDialog) {
        JoinGroupDialog(
            onDismiss = { showJoinGroupDialog = false },
            onJoin = { groupId -> viewModel.joinGroup(groupId); showJoinGroupDialog = false }
        )
    }

    if (showUnsubmitDialog) {
        UnsubmitConfirmationDialog(
            onDismiss = { showUnsubmitDialog = false },
            onConfirm = { viewModel.unsubmitResearch(); showUnsubmitDialog = false }
        )
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { GreetingCard(name = uiState.studentDetails?.name) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatCard(Modifier.weight(1f), "Total Researches", uiState.totalResearchCount, Icons.Default.Public)
                    StatCard(Modifier.weight(1f), "Your Strand's Researches", uiState.strandResearchCount, Icons.Default.School)
                }
            }
            item {
                Box(modifier = Modifier.animateContentSize()) {
                    GroupInfoCard(
                        uiState = uiState,
                        onLeaveGroup = viewModel::leaveGroup,
                        onCreateGroupClick = { showCreateGroupDialog = true },
                        onJoinGroupClick = { showJoinGroupDialog = true },
                        onUploadClick = onNavigateToUpload,
                        onUnsubmitRequest = { showUnsubmitDialog = true }
                    )
                }
            }
            item {
                Text("Recent Researches", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 16.dp))
            }
            if (uiState.isLoading) { /* Placeholder */ }
            else if (uiState.recentResearches.isEmpty()) {
                item { Text("No recent researches have been added yet.", modifier = Modifier.padding(vertical = 16.dp), textAlign = TextAlign.Center) }
            } else {
                items(uiState.recentResearches, key = { it.id }) { research ->
                    ResearchListItem(research = research, onClick = { onNavigateToResearchDetails(research.id) })
                }
            }
        }
    }
}

// --- HELPER COMPOSABLES ---

@Composable
private fun GroupInfoCard(
    uiState: StudentHomeUiState,
    onLeaveGroup: () -> Unit,
    onCreateGroupClick: () -> Unit,
    onJoinGroupClick: () -> Unit,
    onUploadClick: () -> Unit,
    onUnsubmitRequest: () -> Unit
) {
    AnimatedContent(targetState = uiState.groupDetails, label = "GroupCardAnimation") { groupDetails ->
        if (groupDetails != null && uiState.studentDetails != null) {
            GroupDetailsCard(
                groupId = uiState.studentDetails.groupId,
                groupDetails = groupDetails,
                isUpdating = uiState.isLoading,
                onLeaveGroup = onLeaveGroup,
                onUploadClick = onUploadClick,
                onUnsubmitRequest = onUnsubmitRequest
            )
        } else {
            if (!uiState.isLoading) {
                GroupEmptyStateCard(onCreateGroupClick, onJoinGroupClick)
            }
        }
    }
}

@Composable
private fun GroupDetailsCard(
    groupId: String,
    groupDetails: GroupDetails,
    isUpdating: Boolean,
    onLeaveGroup: () -> Unit,
    onUploadClick: () -> Unit,
    onUnsubmitRequest: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            // Group Details Section
            Text("Your Group", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(groupDetails.group_name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Members: ${groupDetails.group_member.size}", style = MaterialTheme.typography.bodyMedium)
            Text("Strand: ${groupDetails.strand}", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Group ID:", style = MaterialTheme.typography.bodyMedium)
                    Text(groupId, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary))
                }
                IconButton(onClick = { clipboardManager.setText(AnnotatedString(groupId)); Toast.makeText(context, "Group ID copied!", Toast.LENGTH_SHORT).show() }) {
                    Icon(Icons.Outlined.ContentCopy, "Copy Group ID")
                }
            }

            // Comments Section (always visible if comments exist)
            if (groupDetails.comments.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                Text("Teacher Comments", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    groupDetails.comments.forEach { comment ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = "Comment", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(comment, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            // --- REVISED: Conditional Action Buttons Section ---
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            if (groupDetails.accepted_research) {
                // --- STATE: RESEARCH ACCEPTED ---
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Congratulations! Your research has been accepted.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    Button(onClick = { /* TODO: Implement certificate download logic */ }) {
                        Icon(Icons.Default.Download, contentDescription = null, Modifier.size(ButtonDefaults.IconSize))
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Download Certificate")
                    }
                }
            } else {
                // --- STATE: NOT YET ACCEPTED ---
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (groupDetails.uploaded) {
                        OutlinedButton(
                            onClick = onUnsubmitRequest,
                            enabled = !isUpdating,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            if (isUpdating) {
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Cancel, "Unsubmit", Modifier.size(ButtonDefaults.IconSize))
                                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                Text("Unsubmit")
                            }
                        }
                    } else {
                        Button(onClick = onUploadClick, enabled = !isUpdating) {
                            Icon(Icons.Default.Upload, null, Modifier.size(ButtonDefaults.IconSize))
                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                            Text("Upload Research")
                        }
                    }
                    OutlinedButton(onClick = onLeaveGroup, enabled = !isUpdating) {
                        if (isUpdating) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Leave Group")
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun UnsubmitConfirmationDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Warning, contentDescription = null) },
        title = { Text("Unsubmit Research?") },
        text = { Text("This will withdraw your submission for review. You can upload it again later. Your teacher's comments will be kept.") },
        confirmButton = {
            Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                Text("Confirm Unsubmit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// Other composables (StatCard, ResearchListItem, GreetingCard, GroupEmptyStateCard, Dialogs) remain unchanged.
@Composable
private fun StatCard(modifier: Modifier = Modifier, label: String, count: Int, icon: ImageVector) {
    Card(modifier = modifier) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(icon, null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
            Column {
                Text(count.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ResearchListItem(research: ResearchItem, onClick: () -> Unit) {
    Card(onClick = onClick) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(research.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2)
            Text(research.strand, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun GreetingCard(name: String?) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Text("Welcome back,", style = MaterialTheme.typography.titleMedium)
            Text(name ?: "Student", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun GroupEmptyStateCard(onCreateGroupClick: () -> Unit, onJoinGroupClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(Icons.Default.Groups, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("You are not in a group", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Create a new group or join an existing one to upload your research.", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = onCreateGroupClick) {
                    Icon(Icons.Default.GroupAdd, null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Create Group")
                }
                TextButton(onClick = onJoinGroupClick) { Text("Join Group") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateGroupDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var groupName by remember { mutableStateOf("") }
    val isNameValid = groupName.isNotBlank()
    AlertDialog(onDismissRequest = onDismiss, icon = { Icon(Icons.Default.GroupAdd, null) }, title = { Text("Create a New Group") },
        text = { OutlinedTextField(value = groupName, onValueChange = { groupName = it }, label = { Text("Group Name") }, singleLine = true, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)) },
        confirmButton = { Button(onClick = { onCreate(groupName.trim()) }, enabled = isNameValid) { Text("Create") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JoinGroupDialog(onDismiss: () -> Unit, onJoin: (String) -> Unit) {
    var groupId by remember { mutableStateOf("") }
    val isIdValid = groupId.trim().length > 5
    AlertDialog(onDismissRequest = onDismiss, icon = { Icon(Icons.AutoMirrored.Filled.Login, "Join Group") }, title = { Text("Join an Existing Group") },
        text = { OutlinedTextField(value = groupId, onValueChange = { groupId = it }, label = { Text("Group ID") }, placeholder = { Text("Enter the ID from your group leader") }, singleLine = true, keyboardOptions = KeyboardOptions.Default, modifier = Modifier.fillMaxWidth()) },
        confirmButton = { Button(onClick = { onJoin(groupId.trim()) }, enabled = isIdValid) { Text("Join") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}