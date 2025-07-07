package com.bdbshs.crest.ui.screens

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bdbshs.crest.ui.viewmodels.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.coroutines.resume

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
    val context = LocalContext.current
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var showJoinGroupDialog by remember { mutableStateOf(false) }
    var showUnsubmitDialog by remember { mutableStateOf(false) }

    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()

    val compositionContext = rememberCompositionContext()

    fun handleCertificateDownload() {
        val group = uiState.groupDetails
        val members = uiState.memberNames

        if (group == null || members.isEmpty()) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Cannot generate certificate. Group data is missing.")
            }
            return
        }

        viewModel.triggerCertificateDownload()

        coroutineScope.launch {
            // Delay slightly to allow the UI to update to the "Downloading..." state
            delay(100)

            // --- FIX: Pass the composition context to the capture function ---
            val bitmap = captureCertificateBitmap(view.context, group, members, compositionContext)

            if (bitmap != null) {
                val success = saveBitmapToDownloads(context, bitmap, group.group_name)
                if (success) {
                    Toast.makeText(context, "Certificate saved to Downloads/CREST", Toast.LENGTH_LONG).show()
                    // Now that it's saved, tell the ViewModel to leave the group
                    viewModel.leaveGroup(isTriggeredByCertificate = true)
                } else {
                    Toast.makeText(context, "Failed to save the certificate.", Toast.LENGTH_SHORT).show()
                    viewModel.onCertificateDownloadFailed() // Reset state on save failure
                }
            } else {
                Toast.makeText(context, "Failed to generate certificate image.", Toast.LENGTH_SHORT).show()
                // --- FIX: Call a dedicated function to reset state on failure ---
                // Do NOT leave the group if the certificate generation fails.
                viewModel.onCertificateDownloadFailed()
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
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(paddingValues),
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
                        onLeaveGroup = { viewModel.leaveGroup() },
                        onCreateGroupClick = { showCreateGroupDialog = true },
                        onJoinGroupClick = { showJoinGroupDialog = true },
                        onUploadClick = onNavigateToUpload,
                        onUnsubmitRequest = { showUnsubmitDialog = true },
                        onDownloadAndLeave = { handleCertificateDownload() }
                    )
                }
            }
            item {
                Text("Recent Researches", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 16.dp))
            }
            if (uiState.isLoading && uiState.recentResearches.isEmpty()) {
                // Shimmer can be added here
            } else if (uiState.recentResearches.isEmpty()) {
                item { Text("No recent researches found.", modifier = Modifier.padding(vertical = 16.dp), textAlign = TextAlign.Center) }
            } else {
                items(uiState.recentResearches, key = { it.id }) { research ->
                    ResearchListItem(research = research, onClick = { onNavigateToResearchDetails(research.id) })
                }
            }
        }
    }

}


// --- FIX: Completely redesigned Certificate Composable for a modern look ---
@Composable
private fun CertificateContent(
    group: GroupDetails,
    memberNames: List<String>,
    modifier: Modifier = Modifier
) {
    val goldColor = Color(0xFFD4AF37) // A nice gold color for accents

    Card(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .border(2.dp, goldColor, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Background seal/logo for a professional touch
            Icon(
                imageVector = Icons.Filled.WorkspacePremium,
                contentDescription = null,
                modifier = Modifier
                    .size(250.dp)
                    .alpha(0.08f),
                tint = goldColor
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 48.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Certificate of Completion",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 44.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "AWARDED FOR OUTSTANDING ACHIEVEMENT IN RESEARCH",
                        style = MaterialTheme.typography.labelMedium,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Main Content
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "This certificate is proudly presented to",
                        style = MaterialTheme.typography.bodyLarge,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        memberNames.forEach { name ->
                            Text(
                                text = name,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        buildAnnotatedString {
                            append("of group ")
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(group.group_name)
                            }
                            append(" for their successful completion and submission of the research manuscript.")
                        },
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 24.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Awarded On",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Text(
                            "Signature",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Divider(
                            modifier = Modifier.width(180.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Research Coordinator",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}


private suspend fun captureCertificateBitmap(
    context: Context,
    group: GroupDetails,
    memberNames: List<String>,
    parentCompositionContext: CompositionContext
): Bitmap? = withContext(Dispatchers.Main) { // Must run on the Main thread
    // Find the activity to get the root view of the window
    val activity = context as? Activity ?: run {
        Log.e("CaptureBitmap", "Context is not an Activity, cannot get window decor view.")
        return@withContext null
    }
    val rootView = activity.window.decorView.rootView as ViewGroup

    // Use suspendCancellableCoroutine to bridge the callback-style View.post with coroutines
    suspendCancellableCoroutine { continuation ->
        val composeView = ComposeView(context).apply {
            // Provide the Recomposer from the main UI tree
            setParentCompositionContext(parentCompositionContext)

            // Set the content to be rendered
            setContent {
                MaterialTheme {
                    CertificateContent(group, memberNames, Modifier.size(800.dp, 600.dp))
                }
            }

            // Make the view invisible to the user
            alpha = 0f
        }

        // This is the capture logic that will run after the view is drawn
        val capture: () -> Unit = {
            try {
                val bitmap = Bitmap.createBitmap(composeView.width, composeView.height, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bitmap)
                composeView.draw(canvas)
                // Successfully captured, resume the coroutine with the bitmap
                continuation.resume(bitmap)
            } catch (e: Exception) {
                Log.e("CaptureBitmap", "Error capturing bitmap", e)
                continuation.resume(null) // Resume with null on failure
            } finally {
                // IMPORTANT: Always remove the view to avoid memory leaks
                rootView.removeView(composeView)
            }
        }

        // Add the invisible view to the window
        rootView.addView(
            composeView,
            ViewGroup.LayoutParams(
                800.dp.dpToPx(context),
                600.dp.dpToPx(context)
            )
        )

        // Post the capture action to the view's message queue.
        // It will be executed after the view has been measured, laid out, and drawn.
        composeView.post(capture)

        // If the coroutine is cancelled, clean up the view
        continuation.invokeOnCancellation {
            rootView.removeView(composeView)
        }
    }
}

private fun Dp.dpToPx(context: Context): Int = (this.value * context.resources.displayMetrics.density).toInt()

// --- FIX: Modified to return a boolean for success/failure ---
private fun saveBitmapToDownloads(context: Context, bitmap: Bitmap, groupName: String): Boolean {
    val fileName = "CREST_Certificate_${groupName.replace(" ", "_")}_${System.currentTimeMillis()}.png"
    val mimeType = "image/png"
    var success = false
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + File.separator + "CREST")
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let {
                resolver.openOutputStream(it)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    success = true
                }
            }
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val crestDir = File(downloadsDir, "CREST")
            if (!crestDir.exists()) crestDir.mkdirs()
            val file = File(crestDir, fileName)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                success = true
            }
            val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            mediaScanIntent.data = Uri.fromFile(file)
            context.sendBroadcast(mediaScanIntent)
        }
    } catch (e: IOException) {
        Log.e("SaveBitmap", "Failed to save bitmap", e)
        success = false
    }
    return success
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
    AnimatedContent(targetState = uiState.groupDetails, label = "GroupCardAnimation") { groupDetails ->
        if (groupDetails != null && uiState.studentDetails != null) {
            GroupDetailsCard(
                groupId = uiState.studentDetails.groupId,
                groupDetails = groupDetails,
                isUpdating = uiState.isLoading,
                isDownloadingCertificate = uiState.isDownloadingCertificate,
                onLeaveGroup = onLeaveGroup,
                onUploadClick = onUploadClick,
                onUnsubmitRequest = onUnsubmitRequest,
                onDownloadAndLeave = onDownloadAndLeave
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
    isDownloadingCertificate: Boolean,
    onLeaveGroup: () -> Unit,
    onUploadClick: () -> Unit,
    onUnsubmitRequest: () -> Unit,
    onDownloadAndLeave: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
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
            if (groupDetails.comments.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                Text("Teacher Comments", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    groupDetails.comments.forEach { comment ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.Comment, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(comment, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            if (groupDetails.accepted_research) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Text("Congratulations! Your research has been accepted.", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
                    Button(onClick = onDownloadAndLeave, enabled = !isDownloadingCertificate) {
                        if (isDownloadingCertificate) {
                            CircularProgressIndicator(modifier = Modifier.size(ButtonDefaults.IconSize), color = LocalContentColor.current)
                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                            Text("Downloading...")
                        } else {
                            Icon(Icons.Default.Download, null, Modifier.size(ButtonDefaults.IconSize))
                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                            Text("Download Certificate & Leave")
                        }
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (groupDetails.uploaded) {
                        OutlinedButton(onClick = onUnsubmitRequest, enabled = !isUpdating && !isDownloadingCertificate) {
                            Icon(Icons.Default.Cancel, "Unsubmit", Modifier.size(ButtonDefaults.IconSize))
                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                            Text("Unsubmit")
                        }
                    } else {
                        Button(onClick = onUploadClick, enabled = !isUpdating && !isDownloadingCertificate) {
                            Icon(Icons.Default.Upload, null, Modifier.size(ButtonDefaults.IconSize))
                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                            Text("Upload Research")
                        }
                    }
                    OutlinedButton(onClick = onLeaveGroup, enabled = !isUpdating && !isDownloadingCertificate) {
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