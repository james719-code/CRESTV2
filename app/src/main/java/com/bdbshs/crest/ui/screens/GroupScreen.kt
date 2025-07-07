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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bdbshs.crest.ui.screens.common.EmptyState
import com.bdbshs.crest.ui.screens.common.ShimmerListItemPlaceholder
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
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isSheetOpen by remember { mutableStateOf(false) }

    // --- DIALOGS ---
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

    // --- BOTTOM SHEET FOR FILTERS ---
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

    // --- MAIN UI ---
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // --- Standalone Search Bar ---
        SearchBarWithFilter(
            query = uiState.searchQuery,
            onQueryChange = viewModel::onSearchQueryChanged,
            onFilterClick = { isSheetOpen = true }
        )

        // --- CONTENT AREA ---
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
                        userScrollEnabled = false // Disable scroll on shimmer
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
                                // THIS IS THE CORRECT IMPLEMENTATION:
                                // The onClick lambda now directly calls the navigation function
                                // that was passed into the screen.
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


// A consistent, reusable Search Bar composable
@Composable
private fun SearchBarWithFilter(
    query: String,
    onQueryChange: (String) -> Unit,
    onFilterClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Search by group name...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            singleLine = true,
            shape = RoundedCornerShape(50)
        )
        IconButton(onClick = onFilterClick) {
            Icon(Icons.Default.FilterList, contentDescription = "Filter and Sort")
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
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
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .padding(bottom = 32.dp), // Extra padding for nav bar
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Title
            Text("Filter & Sort", style = MaterialTheme.typography.titleLarge)

            // Filter Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Filter by Status", style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = uiState.showPendingOnly, onCheckedChange = onShowPendingOnlyToggled)
                    Text("Show Pending Only")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = uiState.showAcceptedOnly, onCheckedChange = onShowAcceptedOnlyToggled)
                    Text("Show Accepted Only")
                }
            }

            // Sort Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Sort By", style = MaterialTheme.typography.titleMedium)
                GroupSortOption.entries.forEach { option ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (uiState.selectedSortOption == option),
                                onClick = { onSortOptionSelected(option) },
                                role = androidx.compose.ui.semantics.Role.RadioButton
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (uiState.selectedSortOption == option),
                            onClick = null // Recommended for accessibility
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(option.displayName)
                    }
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
                leadingIcon = { Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF1E88E5)) }
            )
        }
        isUploaded -> {
            AssistChip(
                onClick = {},
                label = { Text("Pending Review") },
                leadingIcon = { Icon(Icons.Default.Pending, null, tint = Color(0xFFFB8C00)) }
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