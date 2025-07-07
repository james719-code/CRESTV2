package com.bdbshs.crest.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.ConnectivityManager
import android.net.Network
import android.os.ParcelFileDescriptor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bdbshs.crest.ui.viewmodels.ResearchDetailViewModel
import com.bdbshs.crest.ui.viewmodels.ResearchItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*
import androidx.core.graphics.createBitmap

// --- HELPER FOR NETWORK STATE ---
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
fun ResearchDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: ResearchDetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val researchItem = uiState.researchItem
    // Get the real-time network status
    val isOnline by rememberNetworkState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        researchItem?.title ?: "Details",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            when {
                uiState.isDetailsLoading -> CircularProgressIndicator()
                uiState.error != null -> ErrorState(message = uiState.error!!)
                researchItem != null -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        item {
                            ResearchMetadata(
                                researchItem = researchItem,
                                modifier = Modifier.padding(16.dp)
                            )
                            HorizontalDivider(Modifier.padding(bottom = 16.dp))
                        }

                        if (uiState.pdfBytes != null) {
                            item { NativePdfViewer(pdfBytes = uiState.pdfBytes!!) }
                        } else {
                            item {
                                LoadPdfButton(
                                    isLoading = uiState.isPdfLoading,
                                    // --- THIS IS THE FIX ---
                                    // Pass the network status to the ViewModel function
                                    onClick = { viewModel.loadPdf(isOnline) },
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- METADATA COMPOSABLES ---

@Composable
private fun ResearchMetadata(researchItem: ResearchItem, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = researchItem.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Authors", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            researchItem.members.forEach { member ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = member,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InfoChip(icon = Icons.Default.School, text = researchItem.strand)
            InfoChip(icon = Icons.Default.Book, text = researchItem.type.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() })
            InfoChip(icon = Icons.Default.Visibility, text = "${researchItem.downloads} views")
        }
    }
}

@Composable
private fun InfoChip(icon: ImageVector, text: String) {
    AssistChip(
        onClick = { /* No action needed */ },
        label = { Text(text) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        },
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
    )
}

// --- PDF-RELATED COMPOSABLES ---

@Composable
private fun NativePdfViewer(pdfBytes: ByteArray) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var bitmaps by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    DisposableEffect(pdfBytes) {
        val tempFile = File(context.cacheDir, "temp_detail_view.pdf")
        tempFile.writeBytes(pdfBytes)
        val job = scope.launch {
            isLoading = true
            bitmaps = withContext(Dispatchers.IO) {
                try {
                    val renderer = PdfRenderer(ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY))
                    val pageBitmaps = mutableListOf<Bitmap>()
                    for (i in 0 until renderer.pageCount) {
                        renderer.openPage(i).use { page ->
                            val bitmap = createBitmap(page.width, page.height)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            pageBitmaps.add(bitmap)
                        }
                    }
                    renderer.close()
                    pageBitmaps
                } catch (e: Exception) {
                    emptyList()
                }
            }
            isLoading = false
        }
        onDispose {
            job.cancel()
            bitmaps.forEach { it.recycle() }
            tempFile.delete()
        }
    }

    AnimatedVisibility(
        visible = isLoading,
        enter = fadeIn(),
        exit = fadeOut(animationSpec = tween(durationMillis = 200))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text("Processing PDF...")
        }
    }

    AnimatedVisibility(
        visible = !isLoading && bitmaps.isNotEmpty(),
        enter = fadeIn(animationSpec = tween(delayMillis = 200)),
        exit = fadeOut()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            bitmaps.forEachIndexed { index, bitmap ->
                PdfPage(bitmap = bitmap, pageNumber = index + 1)
            }
        }
    }

    if (!isLoading && bitmaps.isEmpty()) {
        ErrorState(message = "Could not render the PDF file. It may be corrupted or in an unsupported format.")
    }
}

@Composable
private fun PdfPage(bitmap: Bitmap, pageNumber: Int) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "PDF Page $pageNumber",
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Page $pageNumber",
            modifier = Modifier
                .align(Alignment.End)
                .padding(8.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LoadPdfButton(isLoading: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator()
            Text("Downloading PDF...")
        } else {
            Icon(
                Icons.Default.PictureAsPdf,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            )
            Text(
                "PDF document available",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Button(onClick = onClick, modifier = Modifier.height(56.dp)) {
                Icon(Icons.Default.Download, null)
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("View Document")
            }
        }
    }
}

// --- UTILITY COMPOSABLES ---

@Composable
private fun ErrorState(message: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(24.dp)
    ) {
        Icon(Icons.Default.CloudOff, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Text(text = "An Error Occurred", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(text = message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
    }
}