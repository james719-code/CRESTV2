package com.bdbshs.crest.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bdbshs.crest.ui.components.*
import com.bdbshs.crest.ui.theme.crestColors
import com.bdbshs.crest.ui.viewmodels.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun DocumentsContent(
    state: DocumentsUiState,
    documents: List<DocumentItem>,
    onQuery: (String) -> Unit,
    onFilter: () -> Unit,
    onRefresh: () -> Unit,
    onDownload: (DocumentItem) -> Unit,
    onManage: (DocumentItem) -> Unit,
    modifier: Modifier = Modifier,
    onGoHome: (() -> Unit)? = null
) {
    CollectionLayout(
        title = "School documents",
        subtitle = "Templates, reference files, and resources shared by teachers.",
        records = documents, isLoading = state.isLoading, isRefreshing = state.isRefreshing,
        error = state.error, onRefresh = onRefresh, recordKey = { it.id },
        emptyTitle = if (state.searchQuery.isNotBlank()) "No documents match this search" else "No school documents added yet",
        emptyMessage = if (state.searchQuery.isNotBlank()) "Try a different name or clear the search."
            else "Documents will appear here when a teacher shares a resource. Research papers are in the Research tab.",
        emptyActionLabel = if (state.searchQuery.isNotBlank()) "Clear search" else if (onGoHome != null) "Go to Home" else "Refresh documents",
        onEmptyAction = if (state.searchQuery.isNotBlank()) ({ onQuery("") }) else onGoHome ?: onRefresh,
        guidance = if (state.searchQuery.isBlank()) listOf(
            "For students" to "Download templates and supporting materials from this page.",
            "For teachers" to "Use Upload school document on Home to share a file with the school."
        ) else emptyList(),
        modifier = modifier,
        controls = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ModernSearchBar(state.searchQuery, onQuery, placeholder = "Search documents...", onFilterClick = onFilter,
                    activeFilterCount = if (state.selectedSortOption != DocumentSortOption.DateNewest) 1 else 0)
                Text(if (state.isLoading) "Loading documents…" else if (state.error != null && documents.isEmpty()) "Document count unavailable" else "${documents.size} documents · ${state.selectedSortOption.displayName}", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    ) { document -> DocumentRecordCard(document, state.userRole == UserType.TEACHER, { onDownload(document) }, { onManage(document) }) }
}

@Composable
internal fun DocumentRecordCard(document: DocumentItem, canManage: Boolean, onDownload: () -> Unit, onManage: () -> Unit) {
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                IconBadge(
                    Icons.Outlined.Description, size = 36.dp,
                    container = MaterialTheme.colorScheme.tertiaryContainer, content = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(document.name, style = MaterialTheme.typography.titleMedium)
                    if (document.createdAt > 0) Text("Added " + formattedDate(document.createdAt),
                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (canManage) IconButton(onClick = onManage) { Icon(Icons.Outlined.MoreVert, "Manage document") }
            }
            Text(document.description.ifBlank { "A school resource shared by a teacher." },
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 4, overflow = TextOverflow.Ellipsis)
            FilledTonalButton(onClick = onDownload, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Icon(Icons.Outlined.Download, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Download document")
            }
        }
    }
}

@Composable
internal fun GroupsContent(
    state: GroupsUiState,
    groups: List<GroupItem>,
    onQuery: (String) -> Unit,
    onFilter: () -> Unit,
    onPending: (Boolean) -> Unit,
    onAccepted: (Boolean) -> Unit,
    onClearFilters: () -> Unit,
    onRefresh: () -> Unit,
    onGroupClick: (GroupItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val filtered = state.searchQuery.isNotBlank() || state.showPendingOnly || state.showAcceptedOnly
    CollectionLayout(
        title = "Research groups",
        subtitle = "See each group's paper, submission status, and members.",
        records = groups, isLoading = state.isLoading, isRefreshing = state.isRefreshing, error = state.error,
        onRefresh = onRefresh, recordKey = { it.id },
        emptyTitle = if (state.showPendingOnly) "No submissions waiting for review" else if (filtered) "No groups match these filters" else "No research groups yet",
        emptyMessage = if (filtered) "Show all groups or try another group name." else "Groups appear here after students create them. Submitted papers can then be reviewed from each group.",
        emptyActionLabel = if (filtered) "Show all groups" else "Refresh groups",
        onEmptyAction = if (filtered) onClearFilters else onRefresh,
        guidance = if (!filtered) listOf("1. Form a group" to "Students create or join a group from Home.",
            "2. Submit a paper" to "The group adds its research title and PDF.",
            "3. Review the work" to "Open the group to read, approve, or return its submission with feedback.") else emptyList(),
        modifier = modifier,
        controls = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ModernSearchBar(state.searchQuery, onQuery, placeholder = "Search by group name...", onFilterClick = onFilter)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(!state.showPendingOnly && !state.showAcceptedOnly, onClearFilters, label = { Text("All (${state.allGroups.size})") })
                    FilterChip(state.showPendingOnly, { onPending(!state.showPendingOnly) }, label = { Text("Pending review (${state.allGroups.count { it.uploaded && !it.accepted_research }})") })
                    FilterChip(state.showAcceptedOnly, { onAccepted(!state.showAcceptedOnly) }, label = { Text("Approved (${state.allGroups.count { it.accepted_research }})") })
                }
            }
        }
    ) { group -> GroupRecordCard(group, { onGroupClick(group) }) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GroupRecordCard(group: GroupItem, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                PaperCover(group.strand, size = 56.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(group.group_name.ifBlank { "Unnamed group" }, style = MaterialTheme.typography.titleMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (group.strand.isNotBlank()) MetadataBadge(group.strand)
                        when {
                            group.accepted_research -> SubmissionStatusBadge("approved")
                            group.uploaded -> SubmissionStatusBadge("pending")
                            else -> MetadataBadge("Not submitted")
                        }
                    }
                }
            }
            if (group.research_title.isNotBlank()) {
                Text(group.research_title, style = MaterialTheme.typography.titleMedium)
            } else {
                Text(if (group.uploaded) "Submitted paper · title not provided" else "No research paper submitted",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                InformationLine(Icons.Outlined.People, "${group.group_member.size} members")
                if (group.research_type.isNotBlank()) InformationLine(Icons.Outlined.Article,
                    group.research_type.lowercase().replaceFirstChar { it.titlecase() })
            }
            if (group.uploaded && !group.accepted_research) {
                Button(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text("Review submission")
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Outlined.ChevronRight, null, Modifier.size(18.dp))
                }
            } else {
                OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text("View group details")
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Outlined.ChevronRight, null, Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
internal fun AccountsContent(
    state: AccountsUiState,
    accounts: List<AccountItem>,
    onQuery: (String) -> Unit,
    onFilter: () -> Unit,
    onStatus: (AccountStatus) -> Unit,
    onRefresh: () -> Unit,
    onAccount: (AccountItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val pending = state.allAccounts.count { !accountIsApproved(it) }
    val filtered = state.searchQuery.isNotBlank() || state.selectedAccountType != null
    CollectionLayout(
        title = "School accounts",
        subtitle = "Verify registrations and manage student and teacher access.",
        records = accounts, isLoading = state.isLoading, isRefreshing = state.isRefreshing, error = state.error,
        onRefresh = onRefresh, recordKey = { it.uid },
        emptyTitle = if (filtered) "No accounts match these filters" else if (state.selectedStatus == AccountStatus.Pending) "No accounts waiting for approval" else "No approved accounts to show",
        emptyMessage = if (filtered) "Clear the search or change the account type filter."
            else if (state.selectedStatus == AccountStatus.Pending) "You're viewing pending registrations. Approved accounts are available in the Approved tab."
            else "Accounts appear here after their registration is approved.",
        emptyActionLabel = if (state.selectedStatus == AccountStatus.Pending && !filtered) "View approved accounts" else "Refresh accounts",
        onEmptyAction = if (state.selectedStatus == AccountStatus.Pending && !filtered) ({ onStatus(AccountStatus.Accepted) }) else onRefresh,
        guidance = if (!filtered && state.selectedStatus == AccountStatus.Pending) listOf(
            "Review a registration" to "Check the person's role and school details before approving access.",
            "Manage existing users" to "Switch to Approved and open an account to see its access actions."
        ) else emptyList(),
        modifier = modifier,
        controls = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ModernSearchBar(state.searchQuery, onQuery, placeholder = "Search name or email", onFilterClick = onFilter)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(state.selectedStatus == AccountStatus.Pending, { onStatus(AccountStatus.Pending) }, label = { Text("Pending ($pending)") })
                    FilterChip(state.selectedStatus == AccountStatus.Accepted, { onStatus(AccountStatus.Accepted) }, label = { Text("Approved (${state.allAccounts.size - pending})") })
                }
            }
        }
    ) { account -> AccountRecordCard(account, { onAccount(account) }) }
}

@Composable
internal fun AccountRecordCard(account: AccountItem, onClick: () -> Unit) {
    val approved = accountIsApproved(account)
    Surface(onClick = onClick, shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            val isStudent = account is AccountItem.Student
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                InitialsAvatar(
                    account.name, size = 48.dp,
                    container = if (isStudent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer,
                    content = if (isStudent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onTertiaryContainer
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(account.name.ifBlank { "Name not provided" }, style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetadataBadge(if (isStudent) "Student" else "Teacher")
                        if (approved) MetadataBadge("Approved", MaterialTheme.crestColors.success, MaterialTheme.crestColors.onSuccess)
                        else MetadataBadge("Pending approval", MaterialTheme.crestColors.warning, MaterialTheme.crestColors.onWarning)
                    }
                }
            }
            when (account) {
                is AccountItem.Student -> {
                    if (account.strand.isNotBlank()) InformationLine(Icons.Outlined.School, account.strand)
                    if (account.lrn > 0) InformationLine(Icons.Outlined.Badge, "LRN: ${account.lrn}")
                    if (account.research_accepted) Text("Research requirement completed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                }
                is AccountItem.Teacher -> {
                    if (account.email.isNotBlank()) InformationLine(Icons.Outlined.Email, account.email)
                    InformationLine(Icons.Outlined.UploadFile, "${account.upload_count} research uploads")
                }
            }
            TextButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 0.dp)) { Text(if (approved) "Manage access" else "Review account") }
        }
    }
}

private fun accountIsApproved(account: AccountItem) =
    when (account) { is AccountItem.Student -> account.accepted; is AccountItem.Teacher -> account.access }

@Composable
private fun formattedDate(timestamp: Long) = remember(timestamp) {
    SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
}
