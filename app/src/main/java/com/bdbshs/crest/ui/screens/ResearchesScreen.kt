package com.bdbshs.crest.ui.screens

import android.annotation.SuppressLint
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
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.bdbshs.crest.ui.components.ModernSearchBar
import com.bdbshs.crest.ui.components.FilterChipsRow
import com.bdbshs.crest.ui.components.FilterChipData
import com.bdbshs.crest.ui.components.ModernFilterChip
import com.bdbshs.crest.ui.components.FilterSectionTitle
import com.bdbshs.crest.ui.components.ModernResearchCard
import com.bdbshs.crest.ui.components.ResearchEmptyState
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
    onNavigateToDetails: (researchId: String) -> Unit,
    viewModel: ResearchesViewModel = hiltViewModel(),
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
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
            onEditClick = {
                // Edit functionality - navigate to upload screen with research ID for editing
                viewModel.dismissActionDialog()
                scope.launch {
                    // For now, navigate to details where editing can be done in future
                    onNavigateToDetails(uiState.selectedResearchForAction!!.id)
                }
            },
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

    // The entire screen is now a Column
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Modern Search Bar with filter button
        ModernSearchBar(
            query = uiState.searchQuery,
            onQueryChange = viewModel::onSearchQueryChanged,
            placeholder = "Search by title or author...",
            onFilterClick = viewModel::showFilterDialog,
            activeFilterCount = calculateActiveFilterCount(uiState),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        // Quick filter chips for strands
        FilterChipsRow(
            chips = uiState.strands.map { strand ->
                FilterChipData(
                    id = strand.name,
                    label = strand.name,
                    isSelected = strand.isSelected,
                    icon = when (strand.name.uppercase()) {
                        "STEM" -> Icons.Outlined.Science
                        "HUMSS" -> Icons.Outlined.Psychology
                        "ABM" -> Icons.Outlined.BusinessCenter
                        "TVL" -> Icons.Outlined.Construction
                        "GAS" -> Icons.Outlined.School
                        else -> null
                    }
                )
            },
            onChipClick = { chip ->
                viewModel.onStrandCheckedChange(chip.id, !chip.isSelected)
            },
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        // The content area with pull-to-refresh
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pullRefresh(pullRefreshState)
        ) {
            when {
                uiState.isLoading -> {
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
                    ResearchEmptyState(
                        isSearchResult = uiState.searchQuery.isNotEmpty(),
                        searchQuery = uiState.searchQuery,
                        onClearSearch = { viewModel.onSearchQueryChanged("") }
                    )
                }
                else -> {
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Adaptive(minSize = 160.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalItemSpacing = 12.dp
                    ) {
                        items(researchesToShow, key = { it.id }) { research ->
                            ModernResearchCard(
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

// Helper function to calculate active filter count
private fun calculateActiveFilterCount(uiState: ResearchesUiState): Int {
    var count = 0
    if (uiState.selectedResearchType != null) count++
    count += uiState.strands.count { it.isSelected }
    if (uiState.selectedSortOption != SortOption.DateNewest) count++
    return count
}

// --- REMOVED: Old SearchBarWithFilter (replaced with ModernSearchBar) ---

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
        "TVL" -> TVLColor
        "GAS" -> GASColor
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        // Use intrinsic height to ensure the Box fills the Row's height
        Row(
            Modifier
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .height(IntrinsicSize.Min)
        ) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight() // This now works correctly with IntrinsicSize.Min
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
                tonalElevation = 2.dp,
                shadowElevation = 8.dp
            ) {
                Button(
                    onClick = onApplyClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
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