package com.bdbshs.crest.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bdbshs.crest.data.*
import com.bdbshs.crest.ui.viewmodels.PlanningUiState
import com.bdbshs.crest.ui.viewmodels.PlanningViewModel
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun PlanningScreen(viewModel: PlanningViewModel, inbox: Boolean, modifier: Modifier = Modifier,
                   onOpenGroup: (String) -> Unit, onOpenAccounts: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    if (inbox) NotificationInboxContent(state, modifier, viewModel::retry,
        { ids, read, done -> viewModel.markRead(ids, read, done) }, onOpenGroup, onOpenAccounts)
    else MilestonesContent(state, modifier, viewModel::retry, viewModel::save, viewModel::change, viewModel::dismissError)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MilestonesContent(
    state: PlanningUiState, modifier: Modifier = Modifier, onRetry: () -> Unit = {},
    onSave: (MilestoneInput, ResearchMilestone?, () -> Unit) -> Unit = { _, _, _ -> },
    onChange: (ResearchMilestone, String, () -> Unit) -> Unit = { _, _, _ -> },
    onDismissError: () -> Unit = {}
) {
    var today by remember { mutableStateOf(PlanningDates.today()) }
    LaunchedEffect(Unit) { while (true) { today = PlanningDates.today(); delay(30_000) } }
    var status by rememberSaveable { mutableStateOf("all") }
    var stage by rememberSaveable { mutableStateOf("all") }
    var group by rememberSaveable { mutableStateOf("all") }
    var editing by remember { mutableStateOf<ResearchMilestone?>(null) }
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<ResearchMilestone?>(null) }
    val groups = remember(state.records.groups) { state.records.groups.associateBy { it.id } }
    val all = remember(state.records.milestones, groups) { state.records.milestones.filter { groups.containsKey(it.groupId) } }
    val visible = remember(all, today, status, stage, group) {
        all.filter { (status == "all" || it.status(today).name == status) && (stage == "all" || it.stage.value == stage) && (group == "all" || it.groupId == group) }
            .sortedWith(compareBy<ResearchMilestone> { it.completedAt > 0 }.thenBy { it.dueDate }.thenBy { it.id })
    }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Deadlines & milestones", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(if (state.teacher) "Plan proposals, chapters, defenses, and final manuscripts." else "Follow the deadlines and milestones for your research group.", style = MaterialTheme.typography.bodyMedium)
                Text("Deadlines include the entire selected day in Manila time. Completion is recorded by a teacher.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (state.teacher) Button(onClick = { editing = null; onDismissError(); showEditor = true }, enabled = !state.busy && groups.isNotEmpty()) { Text("Add milestone") }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MilestoneStatus.entries.forEach { choice ->
                        FilterChip(selected = status == choice.name, onClick = { status = if (status == choice.name) "all" else choice.name },
                            label = { Text("${all.count { it.status(today) == choice }} ${choice.label}") })
                    }
                }
                if (state.teacher) PlanningChoice("Research group", group, listOf("all" to "All groups") + groups.values.map { it.id to it.name }) { group = it }
                PlanningChoice("Stage", stage, listOf("all" to "All stages") + MilestoneStage.entries.map { it.value to it.label }) { stage = it }
                PlanningChoice("Status", status, listOf("all" to "All statuses") + MilestoneStatus.entries.map { it.name to it.label }) { status = it }
            }
        }
        if (state.error != null && !showEditor && deleting == null) item { PlanningError(state.error, onRetry) }
        if (state.records.loading) item { CircularProgressIndicator(Modifier.semantics { contentDescription = "Loading milestones" }) }
        else if (visible.isEmpty()) item { PlanningEmpty(if (all.isEmpty()) "No milestones yet" else "No matching milestones",
            if (state.teacher) "Add a group deadline, or choose another filter." else "Your teacher’s group deadlines will appear here.") }
        items(visible, key = { it.id }) { item ->
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(item.stage.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text(item.title, style = MaterialTheme.typography.titleMedium)
                    Text(groups[item.groupId]?.name.orEmpty(), style = MaterialTheme.typography.bodyMedium)
                    val currentStatus = item.status(today)
                    Text("${currentStatus.label} · ${displayPlanningDate(item.dueDate)}", color = if (currentStatus == MilestoneStatus.OVERDUE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelLarge)
                    if (state.teacher) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onChange(item, if (item.completedAt > 0) "reopen" else "complete", {}) }, enabled = !state.busy,
                            modifier = Modifier.semantics { contentDescription = "${if (item.completedAt > 0) "Reopen" else "Complete"} ${item.title}" }) { Text(if (item.completedAt > 0) "Reopen" else "Mark complete") }
                        TextButton(onClick = { editing = item; onDismissError(); showEditor = true }, enabled = !state.busy,
                            modifier = Modifier.semantics { contentDescription = "Edit ${item.title}" }) { Text("Edit") }
                        TextButton(onClick = { onDismissError(); deleting = item }, enabled = !state.busy,
                            modifier = Modifier.semantics { contentDescription = "Delete ${item.title}" }) { Text("Delete") }
                    }
                }
            }
        }
    }
    if (showEditor) MilestoneEditor(state, editing, onDismiss = { if (!state.busy) showEditor = false }, onSave = { input -> onSave(input, editing) { showEditor = false } })
    deleting?.let { item -> AlertDialog(onDismissRequest = { if (!state.busy) deleting = null }, title = { Text("Delete this milestone?") },
        text = { Column { Text("${item.title} will be removed from the group’s plan."); state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) } } },
        confirmButton = { TextButton(enabled = !state.busy, onClick = { onChange(item, "delete") { deleting = null } }) { Text("Delete milestone") } },
        dismissButton = { TextButton(enabled = !state.busy, onClick = { deleting = null }) { Text("Cancel") } }) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MilestoneEditor(state: PlanningUiState, existing: ResearchMilestone?, onDismiss: () -> Unit, onSave: (MilestoneInput) -> Unit) {
    var group by rememberSaveable { mutableStateOf(existing?.groupId ?: "") }
    var title by rememberSaveable { mutableStateOf(existing?.title.orEmpty()) }
    var stage by rememberSaveable { mutableStateOf(existing?.stage?.value ?: "proposal") }
    var dueDate by rememberSaveable { mutableStateOf(existing?.dueDate ?: PlanningDates.today().toString()) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var datePicker by rememberSaveable { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (existing == null) "Add a milestone" else "Edit milestone") },
        text = {
            Column(Modifier.heightIn(max = 450.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PlanningChoice("Research group", group, state.records.groups.map { it.id to it.name }, enabled = !state.busy && existing == null) { group = it }
                OutlinedTextField(title, { title = it.take(200) }, Modifier.fillMaxWidth(), label = { Text("Milestone title") }, enabled = !state.busy)
                PlanningChoice("Stage", stage, MilestoneStage.entries.map { it.value to it.label }, enabled = !state.busy) { stage = it }
                OutlinedTextField(dueDate, { dueDate = it }, Modifier.fillMaxWidth(), label = { Text("Deadline (Manila time)") }, placeholder = { Text("YYYY-MM-DD") },
                    enabled = !state.busy, singleLine = true, trailingIcon = { IconButton(onClick = { datePicker = true }, enabled = !state.busy) { Icon(Icons.Outlined.CalendarMonth, "Choose deadline") } })
                Text("The group has until the end of this day. Milestones are completed manually by a teacher.", style = MaterialTheme.typography.bodySmall)
                (error ?: state.error)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }, confirmButton = { TextButton(enabled = !state.busy, onClick = {
            val input = runCatching { PlanningDates.validate(MilestoneInput(group, title, MilestoneStage.entries.first { it.value == stage }, dueDate)) }
            input.onSuccess { error = null; onSave(it) }.onFailure { error = it.message }
        }) { Text(if (state.busy) "Saving…" else "Save milestone") } },
        dismissButton = { TextButton(enabled = !state.busy, onClick = onDismiss) { Text("Cancel") } })
    if (datePicker) {
        val selected = runCatching { LocalDate.parse(dueDate) }.getOrDefault(PlanningDates.today())
        val picker = rememberDatePickerState(initialSelectedDateMillis = selected.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(), yearRange = 2000..2100)
        DatePickerDialog(onDismissRequest = { datePicker = false }, confirmButton = { TextButton(onClick = {
            picker.selectedDateMillis?.let { dueDate = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString() }; datePicker = false
        }) { Text("Choose date") } }, dismissButton = { TextButton(onClick = { datePicker = false }) { Text("Cancel") } }) { DatePicker(picker) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NotificationInboxContent(
    state: PlanningUiState, modifier: Modifier = Modifier, onRetry: () -> Unit = {},
    onRead: (List<String>, Boolean, () -> Unit) -> Unit = { _, _, _ -> },
    onOpenGroup: (String) -> Unit = {}, onOpenAccounts: () -> Unit = {}
) {
    var unreadOnly by rememberSaveable { mutableStateOf(false) }
    var kind by rememberSaveable { mutableStateOf("all") }
    val kinds = listOf("submission" to "Submissions", "approval" to "Approvals", "revision" to "Revision updates", "account" to "Account registrations")
    val all = state.records.notifications
    val unread = remember(all) { all.filterNot { it.read } }
    val visible = remember(all, unreadOnly, kind) { all.filter { (!unreadOnly || !it.read) && (kind == "all" || it.kind == kind) }.sortedWith(compareByDescending<SchoolUpdate> { it.observedAt }.thenBy { it.id }) }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Notification inbox", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("${unread.size} unread · Read status is private to your account.")
                Text("Updates are saved when the app connects. Browser or phone push permission is not required.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = { onRead(unread.map { it.id }, true, {}) }, enabled = !state.busy && unread.isNotEmpty()) { Text("Mark all as read") }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(!unreadOnly, { unreadOnly = false }, label = { Text("All notifications") })
                    FilterChip(unreadOnly, { unreadOnly = true }, label = { Text("Unread only") })
                }
                PlanningChoice("Update type", kind, listOf("all" to "All updates") + kinds) { kind = it }
            }
        }
        state.error?.let { item { PlanningError(it, onRetry) } }
        state.inboxError?.let { item { PlanningError(it, onRetry) } }
        if (state.records.loading) item { CircularProgressIndicator(Modifier.semantics { contentDescription = "Loading inbox" }) }
        else if (visible.isEmpty()) item { PlanningEmpty(if (unreadOnly) "You’re all caught up" else "No updates to show", "Research submissions, approvals, and revision feedback appear here as school records sync.") }
        items(visible, key = { it.id }) { update ->
            val available = update.groupId.isBlank() || state.records.groups.any { it.id == update.groupId }
            Card(colors = CardDefaults.cardColors(containerColor = if (update.read) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Notifications, null, Modifier.size(18.dp))
                        Text(kinds.firstOrNull { it.first == update.kind }?.second.orEmpty(), style = MaterialTheme.typography.labelMedium)
                        if (!update.read) Text("Unread", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(update.title, style = MaterialTheme.typography.titleMedium)
                    Text(update.message, style = MaterialTheme.typography.bodyMedium)
                    Text("Added to inbox ${if (update.observedAt > 0) DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.ENGLISH).format(Instant.ofEpochMilli(update.observedAt).atZone(PlanningDates.zone)) else "just now"} · Manila time", style = MaterialTheme.typography.bodySmall)
                    if (!available) Text("This group is no longer available.", style = MaterialTheme.typography.bodySmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(enabled = !state.busy && available, onClick = {
                            val open = { if (update.groupId.isBlank()) onOpenAccounts() else onOpenGroup(update.groupId) }
                            if (update.read) open() else onRead(listOf(update.id), true, open)
                        }) { Text(if (update.groupId.isBlank()) "View accounts" else if (state.teacher) "View group" else "View research progress") }
                        TextButton(enabled = !state.busy, onClick = { onRead(listOf(update.id), !update.read, {}) },
                            modifier = Modifier.semantics { contentDescription = "Mark ${update.title} as ${if (update.read) "unread" else "read"}" }) { Text(if (update.read) "Mark unread" else "Mark read") }
                    }
                }
            }
        }
        item { Text("Times show when updates were added to your inbox. Older records may not include their original submission or review time.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun PlanningChoice(label: String, value: String, choices: List<Pair<String, String>>, enabled: Boolean = true, selected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Box {
            OutlinedButton(onClick = { expanded = true }, enabled = enabled, modifier = Modifier.fillMaxWidth().semantics { contentDescription = label }) {
                Text(choices.firstOrNull { it.first == value }?.second ?: "Choose ${label.lowercase()}")
            }
            DropdownMenu(expanded, { expanded = false }) { choices.forEach { (id, text) ->
                DropdownMenuItem(text = { Text(text) }, onClick = { selected(id); expanded = false })
            } }
        }
    }
}
@Composable private fun PlanningEmpty(title: String, description: String) {
    OutlinedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(title, style = MaterialTheme.typography.titleMedium); Text(description) } }
}
@Composable private fun PlanningError(message: String, retry: () -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text(message, color = MaterialTheme.colorScheme.error); TextButton(onClick = retry) { Text("Retry") } } }
}
private fun displayPlanningDate(date: String) = LocalDate.parse(date).format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH))
