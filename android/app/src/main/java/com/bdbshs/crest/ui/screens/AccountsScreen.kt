package com.bdbshs.crest.ui.screens

import androidx.lifecycle.compose.collectAsStateWithLifecycle

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
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bdbshs.crest.ui.screens.common.EmptyState
import com.bdbshs.crest.ui.screens.common.ShimmerListItemPlaceholder
import com.bdbshs.crest.ui.viewmodels.*

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    viewModel: AccountsViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val accountsToShow by viewModel.filteredAndSortedAccounts.collectAsStateWithLifecycle()
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
                onDeny = viewModel::denySelectedAccount,
                onDelete = viewModel::deleteSelectedAccount,
                error = uiState.error
            )
        }
    }

    AccountsContent(
        state = uiState,
        accounts = accountsToShow,
        onQuery = viewModel::onSearchQueryChanged,
        onFilter = viewModel::showFilterDialog,
        onStatus = viewModel::onStatusSelected,
        onRefresh = viewModel::onRefresh,
        onAccount = viewModel::onAccountClicked,
        modifier = modifier
    )
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
private fun StatusIndicator(isApproved: Boolean, isDeclined: Boolean = false) {
    val icon = if (isApproved) Icons.Default.CheckCircle else if (isDeclined) Icons.Default.Cancel else Icons.Default.Pending
    val text = if (isApproved) "Accepted" else if (isDeclined) "Declined" else "Pending"
    // Using theme primary color for accepted status
    val color = if (isApproved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

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
            com.bdbshs.crest.ui.theme.CrestDialogSystemBars()
            Column {
                // Status Section
                Text("Status", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
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
internal fun AccountActionDialog(
    account: AccountItem,
    isUpdating: Boolean,
    onDismiss: () -> Unit,
    onApprove: () -> Unit,
    onDeny: () -> Unit,
    onDelete: () -> Unit = {},
    error: String? = null
) {
    val isPending = (account is AccountItem.Student && !account.accepted) || (account is AccountItem.Teacher && !account.access)
    val isDeclined = account is AccountItem.Student && !account.accepted && account.declined
    var confirmation by remember(account.uid) { mutableStateOf<String?>(null) }

    if (confirmation != null) {
        AlertDialog(
            onDismissRequest = { if (!isUpdating) confirmation = null },
            title = { Text(if (confirmation == "delete") "Delete declined student?" else "Decline registration?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(if (confirmation == "delete")
                        "Permanently delete ${account.name}'s student profile and group memberships? Published research remains in the library. This cannot be undone."
                    else if (account is AccountItem.Student)
                        "Decline ${account.name}'s registration? Their profile will appear in Declined, where you can restore access or delete it."
                    else "Deny ${account.name}'s teacher registration? This removes the pending registration.")
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(onClick = { if (confirmation == "delete") onDelete() else onDeny() }, enabled = !isUpdating,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text(if (isUpdating) "Saving…" else if (confirmation == "delete") "Confirm delete" else "Confirm decline")
                }
            },
            dismissButton = { TextButton(onClick = { confirmation = null }, enabled = !isUpdating) { Text("Cancel") } }
        )
        return
    }

    AlertDialog(
        onDismissRequest = { if (!isUpdating) onDismiss() },
        icon = {
            val icon = when (account) {
                is AccountItem.Student -> Icons.Default.School
                is AccountItem.Teacher -> Icons.Default.Work
            }
            Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp))
        },
        title = { Text(account.name) },
        text = {
            com.bdbshs.crest.ui.theme.CrestDialogSystemBars()
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val details = when (account) {
                    is AccountItem.Student -> "Student • ${account.strand}"
                    is AccountItem.Teacher -> account.email
                }
                Text("Role: $details")
                StatusIndicator(isApproved = !isPending, isDeclined = isDeclined)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (isDeclined) {
                    TextButton(onClick = { confirmation = "delete" }, enabled = !isUpdating,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                        Text("Delete student")
                    }
                } else if (isPending) {
                    TextButton(onClick = { confirmation = "decline" }, enabled = !isUpdating,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                        Text("Deny registration")
                    }
                }
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
                        Text(if (isDeclined) "Restore access" else "Approve")
                    }
                }
            } else {
                TextButton(onClick = onDismiss) { Text("OK") }
            }
        },
        dismissButton = {
            if (isPending) {
                TextButton(onClick = onDismiss, enabled = !isUpdating) {
                    Text("Cancel")
                }
            }
        }
    )
}
