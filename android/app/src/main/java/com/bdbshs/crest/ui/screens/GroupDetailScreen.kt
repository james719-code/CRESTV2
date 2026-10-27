package com.bdbshs.crest.ui.screens

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.ConnectivityManager
import android.net.Network
import android.os.ParcelFileDescriptor
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bdbshs.crest.ui.viewmodels.GroupDetailViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
private fun rememberNetworkState(): State<Boolean> {
    val context = LocalContext.current
    return produceState(initialValue = context.isCurrentlyConnected()) {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { value = true }
            override fun onLost(network: Network) { value = false }
        }
        manager.registerDefaultNetworkCallback(callback)
        awaitDispose { manager.unregisterNetworkCallback(callback) }
    }
}

private fun Context.isCurrentlyConnected(): Boolean {
    val manager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    return manager.activeNetwork != null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    onNavigateBack: () -> Unit,
    onDeleteGroup: (com.bdbshs.crest.ui.viewmodels.GroupItem) -> Unit,
    viewModel: GroupDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isOnline by rememberNetworkState()
    var confirmDelete by remember { mutableStateOf(false) }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { if (!uiState.isUpdating) confirmDelete = false },
            title = { Text("Delete group?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Permanently delete ${uiState.group?.group_name ?: "this group"}, its submission record, and feedback? Members can create or join another group. Published research and PDFs remain in the library. This cannot be undone.")
                    uiState.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    uiState.group?.let { group ->
                        confirmDelete = false
                        onDeleteGroup(group)
                    }
                }, enabled = !uiState.isUpdating && uiState.group != null,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text("Delete group")
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }, enabled = !uiState.isUpdating) { Text("Cancel") } }
        )
    }
    val pdfPath = uiState.pdfFilePath
    if (uiState.isShowingPdf && pdfPath != null) {
        PdfViewerScreen(pdfPath, viewModel::hidePdfViewer)
        return
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.group?.group_name ?: "Group details", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = onNavigateBack, enabled = !uiState.isUpdating) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    if (uiState.group != null) IconButton(onClick = { confirmDelete = true }, enabled = !uiState.isUpdating) {
                        Icon(Icons.Default.Delete, "Delete group", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        GroupDetailsContent(
            uiState, { viewModel.loadPdf(isOnline) }, viewModel::approveSubmission, viewModel::denySubmission,
            viewModel::onDenialCommentChange, viewModel::onRefresh, Modifier.padding(padding)
        )
    }
}

@Composable
internal fun GroupDetailsContent(
    state: com.bdbshs.crest.ui.viewmodels.GroupDetailUiState,
    onReadPdf: () -> Unit,
    onApprove: () -> Unit,
    onDeny: () -> Unit,
    onComment: (String) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        item { com.bdbshs.crest.ui.components.ContentHeader(state.group?.group_name ?: "Group details", "Submission, members, and teacher review.") }
        if (state.isLoading) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    Text("Loading group and submission details…", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        state.error?.let { message ->
            item { com.bdbshs.crest.ui.components.FeedbackPanel("Group needs attention", message, "Refresh group", onRefresh) }
        }
        state.group?.let { group ->
            item { GroupInfoSection(group) }
            if (group.research_title.isNotBlank()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Research paper", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                        Text(group.research_title, style = MaterialTheme.typography.titleLarge)
                        if (group.research_type.isNotBlank()) Text(group.research_type.lowercase().replaceFirstChar { it.titlecase() }, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            item { GroupMembersSection(state.memberNames) }
            if (group.uploaded && !group.accepted_research) {
                item { ReviewSection(group.research_title, state.denialComment, state.isPdfLoading, state.isUpdating,
                    onComment, onReadPdf, onApprove, onDeny) }
            } else if (group.accepted_research) {
                item {
                    com.bdbshs.crest.ui.components.FeedbackPanel(
                        "Research approved", "This group's submission has been approved and added to the research collection.",
                        "Refresh group", onRefresh, isError = false
                    )
                }
            } else {
                item {
                    com.bdbshs.crest.ui.components.EmptyCollectionPanel(
                        "No research paper submitted",
                        "Members can add a research title and PDF from their student workspace. Review controls will appear after submission.",
                        "Refresh group", onRefresh
                    )
                }
            }
        }
        if (!state.isLoading && state.group == null && state.error == null) {
            item { com.bdbshs.crest.ui.components.EmptyCollectionPanel(
                "Group details are unavailable", "This group may have been removed, or its details have not loaded.", "Try again", onRefresh
            ) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PdfViewerScreen(pdfFilePath: String, onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Document Viewer") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                }
            )
        }
    ) { padding ->
        NativePdfViewer(pdfFilePath = pdfFilePath, modifier = Modifier.padding(padding))
    }
}

@Composable
private fun GroupInfoSection(group: com.bdbshs.crest.ui.viewmodels.GroupItem) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("Group Information", style = MaterialTheme.typography.titleLarge)
            HorizontalDivider()
            InfoRow(icon = Icons.Default.Info, label = "Strand", value = group.strand)
            InfoRow(
                icon = Icons.Default.Bookmark,
                label = "Research Type",
                value = group.research_type.ifBlank { "Not specified" }
            )
            InfoRow(
                icon = Icons.Default.CheckCircle,
                label = "Submission Status",
                value = when {
                    group.accepted_research -> "Accepted"
                    group.uploaded -> "Pending Review"
                    else -> "Not Submitted"
                }
            )
        }
    }
}

@Composable
private fun GroupMembersSection(memberNames: List<String>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("Members (${memberNames.size})", style = MaterialTheme.typography.titleLarge)
            HorizontalDivider()
            if (memberNames.isEmpty()) {
                InfoRow(icon = Icons.Default.Person, value = "Member names are not available.")
            } else {
                memberNames.forEach { name ->
                    InfoRow(icon = Icons.Default.Person, value = name)
                }
            }
        }
    }
}

@Composable
private fun ReviewSection(
    researchTitle: String,
    comment: String,
    isPdfLoading: Boolean,
    isUpdating: Boolean,
    onCommentChange: (String) -> Unit,
    onViewPdfClick: () -> Unit,
    onApprove: () -> Unit,
    onDeny: () -> Unit
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Review Submission", style = MaterialTheme.typography.titleLarge)
            InfoRow(icon = Icons.Default.Title, label = "Research Title", value = researchTitle)

            if (isPdfLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else {
                Button(onClick = onViewPdfClick, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.PictureAsPdf, null, Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("View Submitted PDF")
                }
            }
            HorizontalDivider()

            OutlinedTextField(
                value = comment,
                onValueChange = onCommentChange,
                label = { Text("Reason for Denial (Required)") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isUpdating,
                minLines = 3
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isUpdating && !isPdfLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    TextButton(onClick = onDeny, enabled = comment.isNotBlank() && !isUpdating,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                        Text("Deny")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = onApprove, enabled = !isUpdating) {
                        Text("Approve")
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, value: String, label: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary)
        Column {
            if (label != null) {
                Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ErrorState(message: String) {
    Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.ErrorOutline, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(16.dp))
        Text("An Error Occurred", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
    }
}


@Composable
private fun NativePdfViewer(pdfFilePath: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var bitmaps by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    DisposableEffect(pdfFilePath) {
        val sourceFile = File(pdfFilePath)
        val job = scope.launch(Dispatchers.IO) {
            try {
                val renderer = PdfRenderer(ParcelFileDescriptor.open(sourceFile, ParcelFileDescriptor.MODE_READ_ONLY))
                val pageBitmaps = mutableListOf<Bitmap>()
                for (i in 0 until renderer.pageCount) {
                    renderer.openPage(i).use { page ->
                        val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        pageBitmaps.add(bitmap)
                    }
                }
                renderer.close()
                withContext(Dispatchers.Main) {
                    bitmaps = pageBitmaps
                    isLoading = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { isLoading = false }
            }
        }
        onDispose {
            job.cancel()
            bitmaps.forEach { it.recycle() }
        }
    }

    if (isLoading) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                CircularProgressIndicator()
                Text("Processing PDF...")
            }
        }
    } else if (bitmaps.isEmpty()){
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            ErrorState(message = "Could not render the PDF file. It may be corrupted.")
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize().background(Color.Gray.copy(alpha = 0.2f)),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            itemsIndexed(bitmaps, key = { index, _ -> index }) { index, bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "PDF Page ${index + 1}",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(MaterialTheme.shapes.small)
                )
            }
        }
    }
}
