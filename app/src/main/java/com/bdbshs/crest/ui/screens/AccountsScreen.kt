package com.bdbshs.crest.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bdbshs.crest.ui.screens.common.EmptyState
import com.bdbshs.crest.ui.screens.common.ShimmerListItemPlaceholder
import com.bdbshs.crest.ui.viewmodels.*

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    viewModel: AccountsViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val accountsToShow by viewModel.filteredAndSortedAccounts.collectAsState()
    val pullRefreshState = rememberPullRefreshState(uiState.isRefreshing, viewModel::onRefresh)

    // --- DIALOGS ---
    if (uiState.isFilterDialogVisible) {
        AccountsFilterDialog(
            uiState = uiState,
            onDismiss = viewModel::dismissFilterDialog,
            onAccountTypeSelected = viewModel::onAccountTypeSelected,
            onStatusSelected = viewModel::onStatusSelected,
            onSortOptionSelected = viewModel::onSortOptionSelected,
            onApplyClick = viewModel::applyFilters
        )
    }

    uiState.selectedAccountForAction?.let { account ->
        if (uiState.isActionDialogVisible) {
            AccountActionDialog(
                account = account,
                isUpdating = uiState.isUpdatingAccount,
                onDismiss = viewModel::dismissActionDialog,
                onApprove = viewModel::approveSelectedAccount,
                onDeny = viewModel::denySelectedAccount
            )
        }
    }

    // --- MAIN UI ---
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // --- Standalone Search Bar ---
        SearchBarWithFilter(
            query = uiState.searchQuery,
            onQueryChange = viewModel::onSearchQueryChanged,
            onFilterClick = viewModel::showFilterDialog
        )

        // --- Content Area with List ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pullRefresh(pullRefreshState)
        ) {
            if (uiState.isLoading) {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    userScrollEnabled = false
                ) {
                    items(10) { ShimmerListItemPlaceholder() }
                }
            } else if (accountsToShow.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.SearchOff,
                    message = "No accounts found.\nTry a different search or filter."
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(accountsToShow, key = { it.uid }) { account ->
                        AccountCard(
                            account = account,
                            onClick = { viewModel.onAccountClicked(account) }
                        )
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
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp), // Adjusted padding
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Search name or email...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            singleLine = true,
            shape = RoundedCornerShape(50)
        )
        IconButton(onClick = onFilterClick) {
            Icon(Icons.Default.FilterList, contentDescription = "Filter and Sort")
        }
    }
}


@Composable
private fun AccountCard(account: AccountItem, onClick: () -> Unit) {
    val isApproved = (account is AccountItem.Student && account.accepted) || (account is AccountItem.Teacher && account.access)

    // Using OutlinedCard for better visual separation on list backgrounds
    OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // A more distinct icon treatment
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                val userTypeIcon = when (account) {
                    is AccountItem.Student -> Icons.Default.School
                    is AccountItem.Teacher -> Icons.Default.Work
                }
                Icon(
                    imageVector = userTypeIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(Modifier.width(16.dp))

            // Name and details column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                val details = when (account) {
                    is AccountItem.Student -> "Student • ${account.strand}"
                    is AccountItem.Teacher -> account.email
                }
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(8.dp))
            StatusIndicator(isApproved = isApproved)
        }
    }
}

@Composable
private fun StatusIndicator(isApproved: Boolean) {
    val icon = if (isApproved) Icons.Default.CheckCircle else Icons.Default.Pending
    val text = if (isApproved) "Accepted" else "Pending"
    // Using a more vibrant color for accepted status
    val color = if (isApproved) Color(0xFF1E88E5) else MaterialTheme.colorScheme.onSurfaceVariant

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = text, tint = color)
        Text(text, color = color, style = MaterialTheme.typography.labelMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountsFilterDialog(
    uiState: AccountsUiState,
    onDismiss: () -> Unit,
    onAccountTypeSelected: (AccountType) -> Unit,
    onStatusSelected: (AccountStatus) -> Unit,
    onSortOptionSelected: (AccountSortOption) -> Unit,
    onApplyClick: () -> Unit
) {
    var isSortExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter & Sort Accounts") },
        text = {
            Column {
                // Status Section
                Text("Status", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AccountStatus.entries.forEach { status ->
                        FilterChip(
                            selected = uiState.selectedStatus == status,
                            onClick = { onStatusSelected(status) },
                            label = { Text(status.displayName) }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))

                // Account Type Section
                Text("Account Type", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AccountType.entries.forEach { type ->
                        FilterChip(
                            selected = uiState.selectedAccountType == type,
                            onClick = { onAccountTypeSelected(type) },
                            label = { Text(type.name.replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))

                // Sort By Section
                ExposedDropdownMenuBox(
                    expanded = isSortExpanded,
                    onExpandedChange = { isSortExpanded = !isSortExpanded }
                ) {
                    OutlinedTextField(
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        readOnly = true,
                        value = uiState.selectedSortOption.displayName,
                        onValueChange = {},
                        label = { Text("Sort By") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isSortExpanded) },
                    )
                    ExposedDropdownMenu(expanded = isSortExpanded, onDismissRequest = { isSortExpanded = false }) {
                        AccountSortOption.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.displayName) },
                                onClick = { onSortOptionSelected(option); isSortExpanded = false }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onApplyClick) { Text("Apply") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun AccountActionDialog(
    account: AccountItem,
    isUpdating: Boolean,
    onDismiss: () -> Unit,
    onApprove: () -> Unit,
    onDeny: () -> Unit
) {
    val isPending = (account is AccountItem.Student && !account.accepted) || (account is AccountItem.Teacher && !account.access)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            val icon = when (account) {
                is AccountItem.Student -> Icons.Default.School
                is AccountItem.Teacher -> Icons.Default.Work
            }
            Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp))
        },
        title = { Text(account.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val details = when (account) {
                    is AccountItem.Student -> "Student • ${account.strand}"
                    is AccountItem.Teacher -> account.email
                }
                Text("Role: $details")
                StatusIndicator(isApproved = !isPending)
            }
        },
        confirmButton = {
            if (isPending) {
                Button(
                    onClick = onApprove,
                    enabled = !isUpdating,
                ) {
                    if (isUpdating) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = LocalContentColor.current, strokeWidth = 2.dp)
                    } else {
                        Text("Approve")
                    }
                }
            } else {
                TextButton(onClick = onDismiss) { Text("OK") }
            }
        },
        dismissButton = {
            if (isPending) {
                TextButton(onClick = onDeny, enabled = !isUpdating) {
                    Text("Deny", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    )
}