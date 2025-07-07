package com.bdbshs.crest.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bdbshs.crest.ui.screens.common.EmptyState
import com.bdbshs.crest.ui.screens.common.ShimmerResearchCardPlaceholder
import com.bdbshs.crest.ui.theme.*
import com.bdbshs.crest.ui.viewmodels.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ResearchesScreen(
    userRole: UserType?,
    isOnline: Boolean,
    // Correct, idiomatic function type for navigation
    onNavigateToDetails: (researchId: String) -> Unit,
    viewModel: ResearchesViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val researchesToShow by viewModel.filteredAndSortedResearches.collectAsState()
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()

    LaunchedEffect(userRole) {
        viewModel.setUserRole(userRole)
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState.isRefreshing,
        onRefresh = viewModel::onRefresh
    )

    // --- Dialogs and Sheets ---

    // Action Dialog (Edit/Delete) - This is well-implemented and can remain an AlertDialog.
    if (uiState.isActionDialogVisible && uiState.selectedResearchForAction != null) {
        ActionDialog(
            researchItem = uiState.selectedResearchForAction!!,
            isDeleting = uiState.isDeleting,
            onDismiss = viewModel::dismissActionDialog,
            onEditClick = { /* TODO: Navigate to Edit Screen */ },
            onDeleteClick = viewModel::onDeleteConfirmed
        )
    }

    // Filter Bottom Sheet - Replaced AlertDialog for a much better UX.
    if (uiState.isFilterDialogVisible) {
        FilterBottomSheet(
            uiState = uiState,
            sheetState = sheetState,
            onDismiss = viewModel::dismissFilterDialog,
            onResearchTypeSelected = viewModel::onResearchTypeSelected,
            onStrandCheckedChange = viewModel::onStrandCheckedChange,
            onSortOptionSelected = viewModel::onSortOptionSelected,
            onApplyClick = {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    viewModel.applyFilters()
                }
            },
            onResetClick = viewModel::resetFilters
        )
    }

    Scaffold(
        topBar = {
            // ✨ IMPROVEMENT: A sleek, compact TopAppBar houses the search field.
            ResearchesTopBar(
                query = uiState.searchQuery,
                onQueryChange = viewModel::onSearchQueryChanged,
                onFilterClick = viewModel::showFilterDialog
            )
        },
        modifier = modifier
    )  { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .pullRefresh(pullRefreshState)
        ) {
            when {
                uiState.isLoading -> {
                    // Shimmer Loading UI
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Adaptive(minSize = 160.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalItemSpacing = 12.dp,
                    ) {
                        items(10) { ShimmerResearchCardPlaceholder() }
                    }
                }
                researchesToShow.isEmpty() -> {
                    // Illustrated Empty State
                    EmptyState(message = "No researches found.\nTry adjusting your search or filters.")
                }
                else -> {
                    // The actual content
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Adaptive(minSize = 160.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalItemSpacing = 12.dp
                    ) {
                        items(researchesToShow, key = { it.id }) { research ->
                            ResearchCard(
                                research = research,
                                onClick = { onNavigateToDetails(research.id) },
                                onLongClick = { viewModel.onResearchLongPressed(research) }
                            )
                        }
                    }
                }
            }

            PullRefreshIndicator(
                refreshing = uiState.isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                backgroundColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.primary
            )
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResearchesTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onFilterClick: () -> Unit
) {
    // This TopAppBar is designed to be compact and integrate the search field seamlessly.
    TopAppBar(
        title = {
            // Using TextField for a sleeker, more integrated look than OutlinedTextField here.
            TextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search title or author...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                singleLine = true,
                shape = CircleShape,
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    // Blend the background with the TopAppBar
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                ),
            )
        },
        actions = {
            IconButton(onClick = onFilterClick) {
                Icon(Icons.Default.FilterList, contentDescription = "Filter and Sort")
            }
        },
        // Standard TopAppBar colors for a consistent look.
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ResearchCard(
    research: ResearchItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val strandColor = when (research.strand.uppercase()) {
        "STEM" -> STEMColor
        "HUMSS" -> HUMSSColor
        "ABM" -> ABMColor
        "TVL-ICT" -> TVLColor
        "GAS" -> GASColor
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(
            Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
        ) {
            // Colored side-border for a subtle, elegant accent
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(strandColor)
            )

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = research.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "by ${research.members.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = research.strand,
                        color = strandColor,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )

                    if (research.unfinished) {
                        Icon(
                            imageVector = Icons.Default.Construction,
                            contentDescription = "Unfinished Research",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "Views",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = research.downloads.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier.navigationBarsPadding() // Handles padding for gesture nav
        ) {
            // --- Sheet Header ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Filter & Sort", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onResetClick) { Text("Reset") }
            }
            HorizontalDivider()

            // --- Scrollable Filter Options ---
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // ... (The content of the filter dialog remains the same) ...
                // You can copy the items from your original FilterDialog here
                item {
                    Text("Research Type", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Column {
                        ResearchType.entries.forEach { type ->
                            Row(
                                modifier = Modifier.fillMaxWidth().selectable(selected = (uiState.selectedResearchType == type), onClick = { onResearchTypeSelected(type) }, role = Role.RadioButton).padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = (uiState.selectedResearchType == type), onClick = null)
                                Text(text = type.name.lowercase().replaceFirstChar { it.titlecase() }, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }
                item {
                    Text("Strand", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Column {
                        uiState.strands.forEach { strand ->
                            Row(
                                modifier = Modifier.fillMaxWidth().selectable(selected = strand.isSelected, onClick = { onStrandCheckedChange(strand.name, !strand.isSelected) }, role = Role.Checkbox).padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = strand.isSelected, onCheckedChange = null)
                                Text(text = strand.name, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }
                item {
                    Text("Sort By", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    SortByDropdown(selectedOption = uiState.selectedSortOption, onOptionSelected = onSortOptionSelected)
                }
            }

            // --- Action Buttons ---
            HorizontalDivider()
            Button(
                onClick = onApplyClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Apply Filters")
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
            modifier = Modifier.menuAnchor().fillMaxWidth(),
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