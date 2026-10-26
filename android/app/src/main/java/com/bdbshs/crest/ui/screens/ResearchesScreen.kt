package com.bdbshs.crest.ui.screens

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.pullrefresh.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bdbshs.crest.ui.components.*
import com.bdbshs.crest.ui.screens.common.ShimmerResearchCardPlaceholder
import com.bdbshs.crest.ui.theme.*
import com.bdbshs.crest.ui.viewmodels.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResearchesScreen(
    userRole: UserType?,
    isOnline: Boolean,
    onNavigateToDetails: (researchId: String) -> Unit,
    favoritesOnly: Boolean = false,
    viewModel: ResearchesViewModel = hiltViewModel(),
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val papers by viewModel.filteredAndSortedResearches.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val listState = rememberLazyListState()

    LaunchedEffect(userRole) { viewModel.setUserRole(userRole) }
    LaunchedEffect(favoritesOnly) { viewModel.setFavoritesOnly(favoritesOnly) }
    LaunchedEffect(isOnline) { viewModel.setOnlineStatus(isOnline) }
    LaunchedEffect(listState, papers.size, isOnline) {
        snapshotFlow {
            val layout = listState.layoutInfo
            val lastIndex = layout.visibleItemsInfo.lastOrNull()?.index ?: -1
            layout.totalItemsCount > 0 && lastIndex >= layout.totalItemsCount - 3
        }.collect { nearEnd ->
            if (nearEnd && isOnline && papers.isNotEmpty()) {
                viewModel.loadMore()
            }
        }
    }

    uiState.selectedResearchForAction?.takeIf { uiState.isActionDialogVisible }?.let { selected ->
        ActionDialog(
            researchItem = selected,
            isDeleting = uiState.isDeleting,
            onDismiss = viewModel::dismissActionDialog,
            onEditClick = {
                viewModel.dismissActionDialog()
                onNavigateToDetails(selected.id)
            },
            onDeleteClick = viewModel::onDeleteConfirmed
        )
    }
    if (uiState.isFilterDialogVisible) {
        FilterBottomSheet(
            uiState = uiState, sheetState = sheetState,
            onDismiss = viewModel::dismissFilterDialog,
            onResearchTypeSelected = viewModel::onResearchTypeSelected,
            onStrandCheckedChange = viewModel::onStrandCheckedChange,
            onSortOptionSelected = viewModel::onSortOptionSelected,
            onApplyClick = {
                scope.launch { sheetState.hide() }.invokeOnCompletion { viewModel.applyFilters() }
            },
            onResetClick = {
                viewModel.resetFilters()
                if (favoritesOnly) viewModel.setFavoritesOnly(true)
            }
        )
    }
    ResearchLibraryContent(
        uiState = uiState,
        papers = papers,
        favoritesOnly = favoritesOnly,
        isOnline = isOnline,
        listState = listState,
        onQueryChange = viewModel::onSearchQueryChanged,
        onFilterClick = viewModel::showFilterDialog,
        onFavoritesClick = { viewModel.setFavoritesOnly(!uiState.showFavoritesOnly) },
        onClearFilters = {
            viewModel.resetFilters()
            if (favoritesOnly) viewModel.setFavoritesOnly(true)
        },
        onRefresh = viewModel::onRefresh,
        onPaperClick = { onNavigateToDetails(it.id) },
        onFavoriteClick = viewModel::toggleResearchFavorite,
        onManageClick = if (userRole == UserType.TEACHER && isOnline) viewModel::onResearchLongPressed else null,
        onStrandToggle = viewModel::onStrandCheckedChange,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun ResearchLibraryContent(
    uiState: ResearchesUiState,
    papers: List<ResearchItem>,
    favoritesOnly: Boolean,
    isOnline: Boolean,
    onQueryChange: (String) -> Unit,
    onFilterClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onClearFilters: () -> Unit,
    onRefresh: () -> Unit,
    onPaperClick: (ResearchItem) -> Unit,
    onFavoriteClick: (ResearchItem) -> Unit,
    onManageClick: ((ResearchItem) -> Unit)?,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    onStrandToggle: ((String, Boolean) -> Unit)? = null
) {
    val refreshState = rememberPullRefreshState(uiState.isRefreshing, onRefresh)
    val activeCount = calculateActiveFilterCount(uiState)
    val hasSearchFilters = uiState.searchQuery.isNotBlank() || activeCount > 0
    Box(
        modifier = modifier.fillMaxSize().pullRefresh(refreshState),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.widthIn(max = 840.dp).fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                ModernSearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = onQueryChange,
                    placeholder = "Search title or author",
                    onFilterClick = onFilterClick,
                    activeFilterCount = activeCount
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!favoritesOnly && isOnline) {
                        FilterChip(
                            selected = uiState.showFavoritesOnly,
                            onClick = onFavoritesClick,
                            label = { Text("Favorites") },
                            leadingIcon = { Icon(Icons.Outlined.FavoriteBorder, null, Modifier.size(18.dp)) }
                        )
                    }
                    if (onStrandToggle != null) {
                        uiState.strands.forEach { strand ->
                            FilterChip(
                                selected = strand.isSelected,
                                onClick = { onStrandToggle(strand.name, !strand.isSelected) },
                                label = { Text(strand.name) }
                            )
                        }
                    }
                    val typeAndSort = listOfNotNull(
                        uiState.selectedResearchType?.name?.lowercase()?.replaceFirstChar { it.titlecase() },
                        uiState.selectedSortOption.takeIf { it != SortOption.DateNewest }?.displayName
                    )
                    if (typeAndSort.isNotEmpty() || (onStrandToggle == null && uiState.strands.any { it.isSelected })) {
                        InputChip(
                            selected = true, onClick = onFilterClick,
                            label = { Text((typeAndSort + listOfNotNull(
                                uiState.strands.filter { it.isSelected }.joinToString(", ")
                                    .takeIf { it.isNotBlank() && onStrandToggle == null }
                            )).joinToString(" · ")) }
                        )
                    }
                    if (hasSearchFilters) {
                        TextButton(onClick = onClearFilters) { Text("Clear filters") }
                    }
                }
            }
            item {
                Text(
                    if (uiState.isLoading) "Loading papers…" else if (uiState.error != null && papers.isEmpty()) "Paper count unavailable" else "${papers.size} ${if (papers.size == 1) "paper" else "papers"} · ${uiState.selectedSortOption.displayName}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (uiState.error != null) {
                item { FeedbackPanel("Could not update the library", uiState.error!!, "Try again", onRefresh) }
            }
            when {
                uiState.isLoading -> items(4) { ShimmerResearchCardPlaceholder() }
                papers.isEmpty() && uiState.error == null -> item {
                    EmptyCollectionPanel(
                        title = if (hasSearchFilters) "No papers match your search" else if (uiState.showFavoritesOnly) "No favorite papers yet" else "No research papers yet",
                        message = if (hasSearchFilters) "Try another title or clear the filters." else if (uiState.showFavoritesOnly) "Tap the heart on a paper in Research to save it here." else "Teacher papers and approved student research appear here. Groups can prepare a submission from Home.",
                        actionLabel = if (hasSearchFilters) "Clear filters" else "Refresh papers",
                        onAction = if (hasSearchFilters) onClearFilters else onRefresh,
                        guidance = if (!hasSearchFilters && !uiState.showFavoritesOnly) listOf(
                            "For students" to "Create or join a group from Home, then submit your research PDF for teacher review.",
                            "For teachers" to "Add research from Home or approve a submitted group paper."
                        ) else emptyList()
                    )
                }
                else -> itemsIndexed(papers, key = { _, paper -> paper.id }) { index, paper ->
                    ModernResearchCard(
                        research = paper,
                        onClick = { onPaperClick(paper) },
                        onLongClick = { onManageClick?.invoke(paper) },
                        onFavoriteClick = if (isOnline) ({ onFavoriteClick(paper) }) else null,
                        isFavorite = paper.id in uiState.favoriteResearchIds,
                        isFavoriteUpdating = paper.id in uiState.favoriteUpdateInProgressIds,
                        onManageClick = onManageClick?.let { manage -> { manage(paper) } }
                    )
                }
            }
            if (uiState.isLoadingMore) {
                item {
                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(Modifier.size(24.dp))
                    }
                }
            }
        }
        PullRefreshIndicator(
            refreshing = uiState.isRefreshing, state = refreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            backgroundColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        )
    }
}

private fun calculateActiveFilterCount(uiState: ResearchesUiState): Int =
    (if (uiState.selectedResearchType != null) 1 else 0) +
        uiState.strands.count { it.isSelected } +
        (if (uiState.selectedSortOption != SortOption.DateNewest) 1 else 0)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FilterBottomSheet(
    uiState: ResearchesUiState,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onResearchTypeSelected: (ResearchType) -> Unit,
    onStrandCheckedChange: (String, Boolean) -> Unit,
    onSortOptionSelected: (SortOption) -> Unit,
    onApplyClick: () -> Unit,
    onResetClick: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        CrestDialogSystemBars()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // --- Sheet Header ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Filter & Sort",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = onResetClick,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Reset All", fontWeight = FontWeight.SemiBold)
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // --- Scrollable Filter Options ---
            LazyColumn(
                modifier = Modifier.weight(1f, fill = false),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                // Research Type Section
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilterSectionTitle(icon = Icons.Default.Category, title = "Research Type")
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ResearchType.entries.forEach { type ->
                                val isSelected = uiState.selectedResearchType == type
                                ModernFilterChip(
                                    label = type.name.lowercase().replaceFirstChar { it.titlecase() },
                                    isSelected = isSelected,
                                    onClick = { onResearchTypeSelected(type) }
                                )
                            }
                        }
                    }
                }

                // Strand Section
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilterSectionTitle(icon = Icons.Default.School, title = "Strands")
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            uiState.strands.forEach { strand ->
                                ModernFilterChip(
                                    label = strand.name,
                                    isSelected = strand.isSelected,
                                    onClick = { onStrandCheckedChange(strand.name, !strand.isSelected) }
                                )
                            }
                        }
                    }
                }

                // Sort By Section
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilterSectionTitle(icon = Icons.AutoMirrored.Filled.Sort, title = "Sort By")
                        SortByDropdown(
                            selectedOption = uiState.selectedSortOption,
                            onOptionSelected = onSortOptionSelected
                        )
                    }
                }
            }

            // --- Action Button ---
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Button(
                    onClick = onApplyClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .heightIn(min = 48.dp),
                    shape = MaterialTheme.shapes.small
                ) {
                    Icon(Icons.Default.FilterList, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Apply Filters", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}



// --- Unchanged Composables ---
// The following composables were already well-designed and did not require significant changes.
// - SortByDropdown
// - ActionDialog
// - ActionRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortByDropdown(
    selectedOption: SortOption,
    onOptionSelected: (SortOption) -> Unit
) {
    // This implementation is already correct and follows M3 guidelines.
    // No changes needed.
    val options = SortOption.entries.toTypedArray()
    var isExpanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = isExpanded,
        onExpandedChange = { isExpanded = !isExpanded },
        modifier = Modifier.padding(top = 8.dp)
    ) {
        OutlinedTextField(
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            readOnly = true,
            value = selectedOption.displayName,
            onValueChange = {},
            label = { Text("Sort By") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
        )
        ExposedDropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false }
        ) {
            options.forEach { selectionOption ->
                DropdownMenuItem(
                    text = { Text(selectionOption.displayName) },
                    onClick = {
                        onOptionSelected(selectionOption)
                        isExpanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ActionDialog(
    researchItem: ResearchItem,
    isDeleting: Boolean,
    onDismiss: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    // This implementation is excellent for its purpose.
    // No changes needed.
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Research") },
        text = {
            com.bdbshs.crest.ui.theme.CrestDialogSystemBars()
            Column {
                Text(
                    text = researchItem.title,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                HorizontalDivider()
                ActionRow(
                    text = "Edit",
                    icon = Icons.Default.Edit,
                    onClick = onEditClick
                )
                ActionRow(
                    text = "Delete",
                    icon = Icons.Default.Delete,
                    color = MaterialTheme.colorScheme.error,
                    isLoading = isDeleting,
                    onClick = onDeleteClick
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        dismissButton = {}
    )
}

@Composable
private fun ActionRow(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    isLoading: Boolean = false,
    color: Color = LocalContentColor.current
) {
    // This helper is well-designed.
    // No changes needed.
    ListItem(
        modifier = Modifier.clickable(enabled = !isLoading, onClick = onClick),
        headlineContent = {
            Text(text, fontWeight = FontWeight.Medium, color = color)
        },
        leadingContent = {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = color,
                    strokeWidth = 2.5.dp
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}
