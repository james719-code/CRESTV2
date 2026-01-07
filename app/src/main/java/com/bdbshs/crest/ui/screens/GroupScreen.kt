package com.bdbshs.crest.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bdbshs.crest.ui.screens.common.EmptyState
import com.bdbshs.crest.ui.screens.common.ShimmerListItemPlaceholder
import com.bdbshs.crest.ui.components.ModernSearchBar
import com.bdbshs.crest.ui.components.ModernFilterChip
import com.bdbshs.crest.ui.components.FilterSectionTitle
import com.bdbshs.crest.ui.viewmodels.GroupItem
import com.bdbshs.crest.ui.viewmodels.GroupSortOption
import com.bdbshs.crest.ui.viewmodels.GroupsUiState
import com.bdbshs.crest.ui.viewmodels.GroupsViewModel

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(
    modifier: Modifier = Modifier,
    viewModel: GroupsViewModel = viewModel(),
    onNavigateToDetails: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val groupsToShow by viewModel.filteredAndSortedGroups.collectAsState()
    val pullRefreshState = rememberPullRefreshState(uiState.isRefreshing, viewModel::onRefresh)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isSheetOpen by remember { mutableStateOf(false) }

    if (uiState.isActionDialogVisible && uiState.selectedGroup != null) {
        GroupActionDialog(
            group = uiState.selectedGroup!!,
            isUpdating = uiState.isUpdating,
            comment = uiState.denialComment,
            onCommentChange = viewModel::onDenialCommentChange,
            onDismiss = viewModel::dismissActionDialog,
            onApprove = viewModel::approveSubmission,
            onDeny = viewModel::denySubmission
        )
    }

    if (isSheetOpen) {
        FilterSortBottomSheet(
            sheetState = sheetState,
            uiState = uiState,
            onDismiss = { isSheetOpen = false },
            onSortOptionSelected = viewModel::onSortOptionSelected,
            onShowPendingOnlyToggled = viewModel::onShowPendingOnlyToggled,
            onShowAcceptedOnlyToggled = viewModel::onShowAcceptedOnlyToggled
        )
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        ModernSearchBar(
            query = uiState.searchQuery,
            onQueryChange = viewModel::onSearchQueryChanged,
            placeholder = "Search by group name...",
            onFilterClick = { isSheetOpen = true },
            activeFilterCount = (if (uiState.showPendingOnly) 1 else 0) + 
                               (if (uiState.showAcceptedOnly) 1 else 0) + 
                               (if (uiState.selectedSortOption != GroupSortOption.NameAZ) 1 else 0),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pullRefresh(pullRefreshState)
        ) {
            when {
                uiState.isLoading -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        userScrollEnabled = false
                    ) {
                        items(10) { ShimmerListItemPlaceholder() }
                    }
                }
                groupsToShow.isEmpty() -> {
                    EmptyState(icon = Icons.Default.SearchOff, message = "No groups match your search or filters.")
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(groupsToShow, key = { it.id }) { group ->
                            GroupCard(
                                group = group,
                                onClick = { onNavigateToDetails(group.id) }
                            )
                        }
                    }
                }
            }
            PullRefreshIndicator(uiState.isRefreshing, pullRefreshState, Modifier.align(Alignment.TopCenter))
        }
    }
}




@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FilterSortBottomSheet(
    sheetState: SheetState,
    uiState: GroupsUiState,
    onDismiss: () -> Unit,
    onSortOptionSelected: (GroupSortOption) -> Unit,
    onShowPendingOnlyToggled: (Boolean) -> Unit,
    onShowAcceptedOnlyToggled: (Boolean) -> Unit
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
            // Header
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
                TextButton(onClick = {
                    onShowPendingOnlyToggled(false)
                    onShowAcceptedOnlyToggled(false)
                    onSortOptionSelected(GroupSortOption.NameAZ)
                }) {
                    Text("Reset All", fontWeight = FontWeight.SemiBold)
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            LazyColumn(
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                // Status Filter
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilterSectionTitle(icon = Icons.Default.ListAlt, title = "Filter by Status")
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ModernFilterChip(
                                label = "Pending Only",
                                isSelected = uiState.showPendingOnly,
                                onClick = { onShowPendingOnlyToggled(!uiState.showPendingOnly) }
                            )
                            ModernFilterChip(
                                label = "Accepted Only",
                                isSelected = uiState.showAcceptedOnly,
                                onClick = { onShowAcceptedOnlyToggled(!uiState.showAcceptedOnly) }
                            )
                        }
                    }
                }

                // Sort Section
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilterSectionTitle(icon = Icons.Default.Sort, title = "Sort By")
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GroupSortOption.entries.forEach { option ->
                                val isSelected = uiState.selectedSortOption == option
                                ModernFilterChip(
                                    label = option.displayName,
                                    isSelected = isSelected,
                                    onClick = { onSortOptionSelected(option) }
                                )
                            }
                        }
                    }
                }
            }

            // Action Button
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 2.dp,
                shadowElevation = 8.dp
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.FilterList, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Apply Changes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun GroupCard(group: GroupItem, onClick: () -> Unit) {
    OutlinedCard(onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = group.group_name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${group.strand} • ${group.group_member.size} members",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatusChip(
                isAccepted = group.accepted_research,
                isUploaded = group.uploaded
            )
        }
    }
}

@Composable
private fun StatusChip(isAccepted: Boolean, isUploaded: Boolean) {
    when {
        isAccepted -> {
            AssistChip(
                onClick = {},
                label = { Text("Accepted") },
                leadingIcon = { Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary) }
            )
        }
        isUploaded -> {
            AssistChip(
                onClick = {},
                label = { Text("Pending Review") },
                leadingIcon = { Icon(Icons.Default.Pending, null, tint = MaterialTheme.colorScheme.tertiary) }
            )
        }
        else -> {
            AssistChip(
                onClick = {},
                label = { Text("Not Submitted") }
            )
        }
    }
}

@Composable
private fun GroupActionDialog(
    group: GroupItem,
    isUpdating: Boolean,
    comment: String,
    onCommentChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onApprove: () -> Unit,
    onDeny: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.FolderShared, null) },
        title = { Text(group.group_name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Reviewing Submission:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = group.research_title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                HorizontalDivider()
                OutlinedTextField(
                    value = comment,
                    onValueChange = onCommentChange,
                    label = { Text("Reason for Denial (if any)") },
                    placeholder = { Text("e.g., Incorrect format...") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (isUpdating) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onApprove, enabled = !isUpdating) {
                Text("Approve")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDeny,
                enabled = !isUpdating && comment.isNotBlank()
            ) {
                Text("Deny", color = MaterialTheme.colorScheme.error)
            }
        }
    )
}