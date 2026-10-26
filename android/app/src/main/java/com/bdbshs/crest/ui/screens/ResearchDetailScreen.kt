package com.bdbshs.crest.ui.screens

import androidx.lifecycle.compose.collectAsStateWithLifecycle

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
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bdbshs.crest.ui.viewmodels.ResearchDetailViewModel
import com.bdbshs.crest.ui.viewmodels.ResearchItem
import com.bdbshs.crest.data.UserPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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



@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ResearchDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: ResearchDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val researchItem = uiState.researchItem
    val isOnline by rememberNetworkState()
    
    val scaffoldState = rememberBottomSheetScaffoldState()
    val coroutineScope = rememberCoroutineScope()
    
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isDetailsLoading -> {
                Column(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Loading research details…", style = MaterialTheme.typography.titleMedium)
                    CircularProgressIndicator()
                    Text("Finding the title, authors, and PDF information.", style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = onNavigateBack) { Text("Back to library") }
                }
            }
            uiState.error != null && researchItem == null -> ErrorState(message = uiState.error!!, onBack = onNavigateBack)
            researchItem != null -> {
                val context = LocalContext.current
                var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }
                var pageCount by remember { mutableIntStateOf(0) }
                var pdfLoading by remember { mutableStateOf(false) }
                var pdfOpenError by remember { mutableStateOf<String?>(null) }
                val pdfMutex = remember { Mutex() }

                LaunchedEffect(uiState.pdfFilePath) {
                    val filePath = uiState.pdfFilePath
                    if (filePath != null) {
                        pdfLoading = true
                        pdfOpenError = null
                        withContext(Dispatchers.IO) {
                            try {
                                val sourceFile = File(filePath)
                                val descriptor = ParcelFileDescriptor.open(sourceFile, ParcelFileDescriptor.MODE_READ_ONLY)
                                val renderer = PdfRenderer(descriptor)
                                withContext(Dispatchers.Main) {
                                    pdfRenderer?.close()
                                    pdfRenderer = renderer
                                    pageCount = renderer.pageCount
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    pdfOpenError = "The PDF could not be opened. Try again or ask a teacher to check the file."
                                }
                            }
                        }
                        pdfLoading = false
                    } else {
                        pdfRenderer?.close()
                        pdfRenderer = null
                        pageCount = 0
                    }
                }

                DisposableEffect(Unit) {
                    onDispose {
                        try {
                            pdfRenderer?.close()
                        } catch (e: Exception) {
                        }
                    }
                }

                if (uiState.pdfFilePath != null && pdfRenderer != null) {
                    // Read saved page directly from SharedPreferences (synchronous, reliable)
                    val savedPage = remember(researchItem.id) {
                        UserPrefs.getLastPageSync(context, researchItem.id).coerceIn(0, maxOf(pageCount - 1, 0))
                    }
                    
                    val pagerState = rememberPagerState(initialPage = savedPage) { pageCount }
                    
                    // Scroll to saved page once pageCount is ready (in case it was 0 when pager was created)
                    LaunchedEffect(pageCount) {
                        if (pageCount > 0 && savedPage > 0 && pagerState.currentPage != savedPage) {
                            pagerState.scrollToPage(savedPage)
                        }
                    }
                    
                    // Update persistent page index in ViewModel when user swipes
                    LaunchedEffect(pagerState.currentPage) {
                        if (pageCount > 0) {
                            viewModel.updateCurrentPage(pagerState.currentPage)
                        }
                    }

                    BottomSheetScaffold(
                        scaffoldState = scaffoldState,
                        sheetPeekHeight = 100.dp,
                        sheetShadowElevation = 8.dp,
                        sheetDragHandle = { BottomSheetDefaults.DragHandle() },
                        sheetContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 32.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                ) {
                                    ResearchMetadata(researchItem = researchItem)

                                    if (pageCount > 1) {
                                        HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))
                                        Text(
                                            "Navigate to Page",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            Text(
                                                text = "${pagerState.currentPage + 1}",
                                                style = MaterialTheme.typography.labelLarge,
                                                modifier = Modifier.width(32.dp),
                                                textAlign = TextAlign.Center
                                            )
                                            Slider(
                                                value = pagerState.currentPage.toFloat(),
                                                onValueChange = {
                                                    coroutineScope.launch {
                                                        pagerState.scrollToPage(it.toInt())
                                                    }
                                                    viewModel.updateCurrentPage(it.toInt())
                                                },
                                                valueRange = 0f..(if (pageCount > 0) pageCount - 1 else 0).toFloat(),
                                                modifier = Modifier.weight(1f),
                                                steps = if (pageCount > 1) pageCount - 2 else 0
                                            )
                                            Text(
                                                text = "$pageCount",
                                                style = MaterialTheme.typography.labelLarge,
                                                modifier = Modifier.width(32.dp),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        topBar = {
                            TopAppBar(
                                title = {
                                    Column {
                                        Text(
                                            researchItem.title,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            "Page ${pagerState.currentPage + 1} of $pageCount",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                                navigationIcon = {
                                    IconButton(onClick = onNavigateBack) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                                    }
                                },
                                actions = {
                                    IconButton(onClick = {
                                        coroutineScope.launch {
                                            scaffoldState.bottomSheetState.expand()
                                        }
                                    }) {
                                        Icon(Icons.Default.Info, "Details")
                                    }
                                }
                            )
                        }
                    ) { paddingValues ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (pdfLoading) {
                                CircularProgressIndicator()
                            } else {
                                HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier.fillMaxSize(),
                                    pageSpacing = 16.dp,
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) { pageIndex ->
                                    PdfPageItem(
                                        renderer = pdfRenderer!!,
                                        pageIndex = pageIndex,
                                        mutex = pdfMutex,
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth()
                                            .clip(MaterialTheme.shapes.medium)
                                            .background(Color.White)
                                            .padding(4.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text("Research Details") },
                                navigationIcon = {
                                    IconButton(onClick = onNavigateBack) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                                    }
                                }
                            )
                        }
                    ) { paddingValues ->
                        ResearchOverview(
                            researchItem = researchItem,
                            isLoading = uiState.isPdfLoading || pdfLoading,
                            onRead = {
                                viewModel.clearPdfBytes()
                                viewModel.loadPdf(isOnline)
                            },
                            readError = pdfOpenError ?: uiState.error,
                            modifier = Modifier.padding(paddingValues)
                        )
                    }
                }
            }
            else -> ErrorState("This research paper is not available.", onNavigateBack)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResearchMetadata(researchItem: ResearchItem, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (researchItem.strand.isNotBlank()) com.bdbshs.crest.ui.components.MetadataBadge(researchItem.strand)
            com.bdbshs.crest.ui.components.MetadataBadge(researchItem.type.name.lowercase().replaceFirstChar { it.titlecase(Locale.ROOT) })
            if (researchItem.unfinished) com.bdbshs.crest.ui.components.MetadataBadge("In progress")
        }
        Text(researchItem.title, style = MaterialTheme.typography.headlineMedium)
        Text("Authors", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
        Text(researchItem.members.joinToString(", ").ifBlank { "Author names not provided" }, style = MaterialTheme.typography.bodyLarge)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        if (researchItem.createdAt > 0) {
            com.bdbshs.crest.ui.components.InformationLine(
                Icons.Default.CalendarToday,
                "Added " + remember(researchItem.createdAt) {
                    java.text.SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(java.util.Date(researchItem.createdAt))
                }
            )
        }
        com.bdbshs.crest.ui.components.InformationLine(Icons.Default.Download, "${researchItem.downloads} downloads")
    }
}

@Composable
private fun AuthorChip(name: String) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun InfoChip(
    icon: ImageVector, 
    text: String,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    contentColor: Color = MaterialTheme.colorScheme.primary
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = containerColor,
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = contentColor
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun PdfPageItem(
    renderer: PdfRenderer,
    pageIndex: Int,
    mutex: Mutex,
    modifier: Modifier = Modifier
) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    
    // Reset zoom when page Index changes (though it shouldn't for a single item)
    // and provide a way to reset it externally if needed
    LaunchedEffect(pageIndex) {
        scale = 1f
        offset = Offset.Zero
    }

    val state = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        if (scale > 1f) {
            offset += offsetChange
        } else {
            offset = Offset.Zero
        }
    }
    
    LaunchedEffect(renderer, pageIndex) {
        mutex.withLock {
            withContext(Dispatchers.IO) {
                try {
                    renderer.openPage(pageIndex).use { page ->
                        val w = page.width
                        val h = page.height
                        val scaleRes = 2.5f 
                        val rw = (w * scaleRes).toInt()
                        val rh = (h * scaleRes).toInt()
                        val bm = createBitmap(rw, rh)
                        bm.eraseColor(android.graphics.Color.WHITE)
                        page.render(bm, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmap = bm
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        // 1. The Image (Base Layer - always visible and scaled)
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "PDF Page ${pageIndex + 1}",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
            )
        } else {
            CircularProgressIndicator(modifier = Modifier.size(32.dp))
        }

        // 2. Gesture Area (Top Layer - centered, leaves gutters for paging)
        // We handle taps on the whole screen for double-tap zoom
        // But we only handle transformations in the middle area to allow paging at the edges
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            scale = if (scale > 1f) 1f else 3f
                            offset = androidx.compose.ui.geometry.Offset.Zero
                        }
                    )
                }
        ) {
            // This inner Box handles the pan and zoom but is restricted to 
            // the center area, letting swipes at the edges fall through to the Pager.
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.85f) // 15% edges reserved for paging
                    .align(Alignment.Center)
                    .transformable(state = state)
            )
        }
        
        // 3. Page indicator overlay (only visible when not zoomed much)
        if (scale == 1f) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                shape = CircleShape
            ) {
                Text(
                    text = "${pageIndex + 1}",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}


@Composable
private fun ErrorState(message: String, onBack: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().padding(32.dp)
    ) {
        Icon(
            Icons.Default.ErrorOutline, 
            null, 
            modifier = Modifier.size(80.dp), 
            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
        )
        Spacer(Modifier.height(24.dp))
        Text(text = "Opening Failed", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(
            text = message, 
            style = MaterialTheme.typography.bodyLarge, 
            textAlign = TextAlign.Center, 
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(32.dp))
        Button(onClick = onBack) {
            Text("Back to library")
        }
    }
}

@Composable
internal fun ResearchOverview(
    researchItem: ResearchItem,
    isLoading: Boolean,
    onRead: () -> Unit,
    modifier: Modifier = Modifier,
    readError: String? = null
) {
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        readError?.let { message ->
            item { com.bdbshs.crest.ui.components.FeedbackPanel("Paper could not open", message, "Try again", onRead) }
        }
        item {
            val strand = com.bdbshs.crest.ui.components.strandStyle(researchItem.strand)
            Surface(color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                Column {
                    Box(
                        Modifier.fillMaxWidth().height(112.dp).background(strand.container),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(strand.icon, null, Modifier.size(56.dp), tint = strand.content)
                    }
                    ResearchMetadata(researchItem, Modifier.padding(24.dp))
                }
            }
        }
        item {
            Button(
                onClick = onRead,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                } else {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, null)
                    Spacer(Modifier.width(12.dp))
                }
                Text(if (isLoading) "Opening paper…" else "Read paper")
            }
        }
    }
}
