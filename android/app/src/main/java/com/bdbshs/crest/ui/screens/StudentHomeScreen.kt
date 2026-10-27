package com.bdbshs.crest.ui.screens

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bdbshs.crest.ui.components.AvatarStack
import com.bdbshs.crest.ui.components.IconBadge
import com.bdbshs.crest.ui.components.StepProgress
import com.bdbshs.crest.ui.components.HomeSummary
import com.bdbshs.crest.ui.components.HomeSectionHeading
import com.bdbshs.crest.ui.components.HomeResearchRow
import com.bdbshs.crest.ui.components.HomeEmptyResearch
import com.bdbshs.crest.ui.certificates.ResearchCertificateData
import com.bdbshs.crest.ui.certificates.exportResearchCertificate
import com.bdbshs.crest.ui.viewmodels.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentHomeScreen(
    modifier: Modifier = Modifier,
    viewModel: StudentHomeViewModel = hiltViewModel(),
    onNavigateToUpload: () -> Unit,
    onNavigateToResearchDetails: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var showJoinGroupDialog by remember { mutableStateOf(false) }
    var showUnsubmitDialog by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    fun handleCertificateDownload() {
        val snapshot = viewModel.uiState.value
        val group = snapshot.groupDetails ?: return
        if (!viewModel.triggerCertificateDownload()) return
        val members = snapshot.memberNames.filter { it.isNotBlank() }

        coroutineScope.launch {
            var saved = false
            try {
                val certificate = ResearchCertificateData(
                    groupName = group.group_name,
                    strand = group.strand,
                    researchTitle = group.research_title,
                    memberNames = members,
                    groupReference = snapshot.studentDetails?.groupId.orEmpty()
                )
                saved = exportResearchCertificate(context, certificate)
                if (saved) {
                    Toast.makeText(context, "Certificate saved to Downloads/CREST", Toast.LENGTH_LONG).show()
                    viewModel.leaveGroup(isTriggeredByCertificate = true)
                } else {
                    snackbarHostState.showSnackbar("Could not save your certificate. Please try again.")
                }
            } finally {
                if (!saved) viewModel.onCertificateDownloadFailed()
            }
        }
    }

    

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
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        StudentHomeContent(
            uiState = uiState,
            onLeaveGroup = { viewModel.leaveGroup() },
            onCreateGroup = { showCreateGroupDialog = true },
            onJoinGroup = { showJoinGroupDialog = true },
            onUpload = onNavigateToUpload,
            onUnsubmit = { showUnsubmitDialog = true },
            onDownloadCertificate = { handleCertificateDownload() },
            onResearchClick = onNavigateToResearchDetails,
            onRetry = viewModel::onRefresh,
            modifier = Modifier.padding(padding)
        )
    }
}

@Composable
internal fun StudentHomeContent(
    uiState: StudentHomeUiState,
    onLeaveGroup: () -> Unit,
    onCreateGroup: () -> Unit,
    onJoinGroup: () -> Unit,
    onUpload: () -> Unit,
    onUnsubmit: () -> Unit,
    onDownloadCertificate: () -> Unit,
    onResearchClick: (String) -> Unit,
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
                    uiState.studentDetails?.name?.takeIf { it.isNotBlank() } ?: "Student workspace",
                    "Your research workspace",
                    if (uiState.isLoading || uiState.loadError != null) emptyList() else listOf(
                        uiState.totalResearchCount.toString() to "Library papers",
                        uiState.strandResearchCount.toString() to "In your strand"
                    )
                )
            }
            if (uiState.loadError != null) {
                item { com.bdbshs.crest.ui.components.FeedbackPanel("Workspace could not load", uiState.loadError, "Try again", onRetry) }
            }
            if (uiState.isLoading) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                        Text("Loading your group and recent papers…", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            if (uiState.loadError == null || uiState.groupDetails != null) {
                item {
                    GroupInfoCard(
                        uiState, onLeaveGroup, onCreateGroup, onJoinGroup, onUpload, onUnsubmit, onDownloadCertificate
                    )
                }
            }
            item { HomeSectionHeading("Recent research papers", uiState.recentResearches.size.takeIf { it > 0 }) }
            if (uiState.isLoading && uiState.recentResearches.isEmpty()) {
                items(2) { com.bdbshs.crest.ui.screens.common.ShimmerResearchCardPlaceholder() }
            } else if (uiState.recentResearches.isEmpty() && uiState.loadError == null) {
                item {
                    HomeEmptyResearch(
                        "No recent papers available",
                        "Papers appear here when added to the school library. You can prepare your group's submission now.",
                        "Refresh library", onRetry
                    )
                }
            }
            items(uiState.recentResearches.chunked(paperColumns), key = { it.first().id }) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    row.forEach { research ->
                        HomeResearchRow(research, { onResearchClick(research.id) }, Modifier.weight(1f))
                    }
                    if (row.size < paperColumns) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun GroupInfoCard(
    uiState: StudentHomeUiState,
    onLeaveGroup: () -> Unit,
    onCreateGroupClick: () -> Unit,
    onJoinGroupClick: () -> Unit,
    onUploadClick: () -> Unit,
    onUnsubmitRequest: () -> Unit,
    onDownloadAndLeave: () -> Unit
) {
    key(uiState.studentDetails?.groupId) {
        val groupDetails = uiState.groupDetails
        if (groupDetails != null && uiState.studentDetails != null) {
            GroupDetailsCard(
                groupId = uiState.studentDetails.groupId,
                groupDetails = groupDetails,
                isUpdating = uiState.isLoading,
                isDownloadingCertificate = uiState.isDownloadingCertificate,
                onLeaveGroup = onLeaveGroup,
                onUploadClick = onUploadClick,
                onUnsubmitRequest = onUnsubmitRequest,
                memberNames = uiState.memberNames,
                onDownloadAndLeave = onDownloadAndLeave
            )
        } else {
            if (!uiState.isLoading) {
                GroupEmptyStateCard(onCreateGroupClick, onJoinGroupClick)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GroupDetailsCard(
    groupId: String,
    groupDetails: GroupDetails,
    isUpdating: Boolean,
    isDownloadingCertificate: Boolean,
    onLeaveGroup: () -> Unit,
    onUploadClick: () -> Unit,
    onUnsubmitRequest: () -> Unit,
    onDownloadAndLeave: () -> Unit,
    memberNames: List<String> = emptyList()
) {
    val context = LocalContext.current
    var detailsExpanded by rememberSaveable(groupId) { mutableStateOf(false) }
    Surface(
        shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface, shadowElevation = 3.dp
    ) {
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconBadge(Icons.Default.Groups, size = 48.dp, container = MaterialTheme.colorScheme.secondaryContainer, content = MaterialTheme.colorScheme.onSecondaryContainer)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Your research group", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                    Text(groupDetails.group_name, style = MaterialTheme.typography.titleLarge)
                }
            }
            StepProgress(
                listOf("Group", "Submit", "Review"),
                current = when {
                    groupDetails.accepted_research -> 3
                    groupDetails.uploaded -> 2
                    else -> 1
                },
                modifier = Modifier.padding(vertical = 8.dp)
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (groupDetails.strand.isNotBlank()) com.bdbshs.crest.ui.components.MetadataBadge(groupDetails.strand)
                when {
                    groupDetails.accepted_research -> com.bdbshs.crest.ui.components.SubmissionStatusBadge("approved")
                    groupDetails.uploaded -> com.bdbshs.crest.ui.components.SubmissionStatusBadge("pending")
                    else -> com.bdbshs.crest.ui.components.MetadataBadge("Not submitted")
                }
                if (groupDetails.research_type.isNotBlank()) com.bdbshs.crest.ui.components.MetadataBadge(
                    groupDetails.research_type.lowercase().replaceFirstChar { it.titlecase() }
                )
            }
            if (groupDetails.research_title.isNotBlank()) Text(groupDetails.research_title, style = MaterialTheme.typography.titleSmall)
            else Text(if (groupDetails.uploaded) "Research submitted for teacher review" else "Add your research title and PDF when your group is ready.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (groupDetails.comments.isNotEmpty()) {
                Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Teacher feedback", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        groupDetails.comments.forEach { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer) }
                    }
                }
            }
            val busy = isUpdating || isDownloadingCertificate
            when {
                groupDetails.accepted_research -> Button(
                    onClick = onDownloadAndLeave, enabled = !isDownloadingCertificate,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                ) { Text(if (isDownloadingCertificate) "Downloading…" else "Download certificate & leave") }
                groupDetails.uploaded -> Row(
                    Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "With your teacher for review", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(onClick = onUnsubmitRequest, enabled = !busy) { Text("Unsubmit") }
                }
                else -> Button(
                    onClick = onUploadClick, enabled = !busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                ) { Text("Submit research") }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = { detailsExpanded = !detailsExpanded },
                    contentPadding = PaddingValues(horizontal = 0.dp)
                ) {
                    Icon(if (detailsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (detailsExpanded) "Hide group details" else "Group details · ${groupDetails.group_member.size} members")
                }
                Spacer(Modifier.weight(1f))
                if (memberNames.isNotEmpty()) AvatarStack(memberNames, size = 30.dp)
            }
            if (detailsExpanded) {
                Text(
                    if (memberNames.isNotEmpty()) "Members: " + memberNames.joinToString(", ") else "${groupDetails.group_member.size} group members",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Group ID: $groupId", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    IconButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Group ID", groupId))
                        Toast.makeText(context, "Group ID copied", Toast.LENGTH_SHORT).show()
                    }) { Icon(Icons.Outlined.ContentCopy, "Copy Group ID") }
                }
            }
            if (!groupDetails.accepted_research) {
                TextButton(onClick = onLeaveGroup, enabled = !busy, contentPadding = PaddingValues(horizontal = 0.dp)) {
                    Text("Leave group", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun UnsubmitConfirmationDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { com.bdbshs.crest.ui.theme.CrestDialogSystemBars(); Icon(Icons.Default.Warning, contentDescription = null) },
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
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
            Column {
                Text(count.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun GroupEmptyStateCard(onCreateGroupClick: () -> Unit, onJoinGroupClick: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Start your research group", style = MaterialTheme.typography.titleMedium)
            Text("Create a group or join your classmates with their group ID.", style = MaterialTheme.typography.bodyMedium)
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onCreateGroupClick, modifier = Modifier.weight(1f).fillMaxHeight().heightIn(min = 48.dp), shape = MaterialTheme.shapes.small) { Text("Create a group") }
                OutlinedButton(onClick = onJoinGroupClick, modifier = Modifier.weight(1f).fillMaxHeight().heightIn(min = 48.dp), shape = MaterialTheme.shapes.small) { Text("Join a group") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateGroupDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var groupName by remember { mutableStateOf("") }
    val isNameValid = groupName.isNotBlank()
    AlertDialog(onDismissRequest = onDismiss, icon = { com.bdbshs.crest.ui.theme.CrestDialogSystemBars(); Icon(Icons.Default.GroupAdd, null) }, title = { Text("Create a New Group") },
        text = { OutlinedTextField(value = groupName, onValueChange = { groupName = it }, label = { Text("Group Name") }, singleLine = true, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)) },
        confirmButton = { Button(onClick = { onCreate(groupName.trim()) }, enabled = isNameValid) { Text("Create") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JoinGroupDialog(onDismiss: () -> Unit, onJoin: (String) -> Unit) {
    var groupId by remember { mutableStateOf("") }
    val isIdValid = groupId.trim().length > 5
    AlertDialog(onDismissRequest = onDismiss, icon = { com.bdbshs.crest.ui.theme.CrestDialogSystemBars(); Icon(Icons.AutoMirrored.Filled.Login, "Join Group") }, title = { Text("Join an Existing Group") },
        text = { OutlinedTextField(value = groupId, onValueChange = { groupId = it }, label = { Text("Group ID") }, placeholder = { Text("Enter the ID from your group leader") }, singleLine = true, keyboardOptions = KeyboardOptions.Default, modifier = Modifier.fillMaxWidth()) },
        confirmButton = { Button(onClick = { onJoin(groupId.trim()) }, enabled = isIdValid) { Text("Join") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
