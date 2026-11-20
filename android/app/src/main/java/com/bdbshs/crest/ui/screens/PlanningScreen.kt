package com.bdbshs.crest.ui.screens

import com.bdbshs.crest.ui.components.CrestFilterChip as FilterChip

import com.bdbshs.crest.ui.components.CrestButton as Button
import com.bdbshs.crest.ui.components.CrestOutlinedButton as OutlinedButton
import com.bdbshs.crest.ui.components.CrestTextButton as TextButton
import com.bdbshs.crest.ui.components.CrestOutlinedCard as OutlinedCard
import com.bdbshs.crest.ui.components.CrestTextField as OutlinedTextField
import com.bdbshs.crest.ui.components.CrestTonalButton as TonalButton
import com.bdbshs.crest.ui.components.IconBadge
import com.bdbshs.crest.ui.components.MetadataBadge
import com.bdbshs.crest.ui.components.PageHeading
import com.bdbshs.crest.ui.components.CrestPageList
import com.bdbshs.crest.ui.theme.crestColors

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
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
    val counts = remember(all, today) { all.groupingBy { it.status(today) }.eachCount() }
    val hasFilters = status != "all" || stage != "all" || group != "all"
    val clearFilters = { status = "all"; stage = "all"; group = "all" }
    CrestPageList(modifier) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PageHeading(
                    "Deadlines & milestones",
                    if (state.teacher) "Keep every research group on track." else "Stay on track with your group’s research deadlines."
                )
                if (state.teacher) Button(
                    onClick = { editing = null; onDismissError(); showEditor = true },
                    enabled = !state.busy && groups.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.Add, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Add milestone")
                }
            }
        }
        if (!state.records.loading) item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PlanningSectionHeading("Milestone overview", Modifier.weight(1f))
                    Text("Tap to filter", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                MilestoneOverview(
                    counts, status,
                    onSelect = { status = if (status == it.name) "all" else it.name }
                )
            }
        }
        item {
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Tune, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Filter milestones", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                        if (hasFilters) TextButton(onClick = clearFilters, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("Clear") }
                    }
                    BoxWithConstraints {
                        if (state.teacher && maxWidth >= 320.dp && LocalDensity.current.fontScale <= 1.3f) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                PlanningChoice("Research group", group, listOf("all" to "All groups") + groups.values.map { it.id to it.name }, modifier = Modifier.weight(1f)) { group = it }
                                PlanningChoice("Stage", stage, listOf("all" to "All stages") + MilestoneStage.entries.map { it.value to it.label }, modifier = Modifier.weight(1f)) { stage = it }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (state.teacher) PlanningChoice("Research group", group, listOf("all" to "All groups") + groups.values.map { it.id to it.name }) { group = it }
                                PlanningChoice("Stage", stage, listOf("all" to "All stages") + MilestoneStage.entries.map { it.value to it.label }) { stage = it }
                            }
                        }
                    }
                }
            }
        }
        if (state.error != null && !showEditor && deleting == null) item { PlanningError(state.error, onRetry) }
        if (state.records.loading) item { PlanningLoading("Loading milestones") }
        else if (state.error == null || all.isNotEmpty()) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PlanningSectionHeading(if (hasFilters) "Matching milestones" else "All milestones", Modifier.weight(1f))
                    MetadataBadge(visible.size.toString())
                }
            }
            if (visible.isEmpty()) item {
                PlanningEmpty(
                    if (all.isEmpty()) "No milestones yet" else "No matching milestones",
                    if (all.isNotEmpty()) "Try a different filter to see more deadlines."
                    else if (state.teacher) "Add your first group deadline to get started."
                    else "Your teacher’s group deadlines will appear here.",
                    Icons.Outlined.EventNote,
                    action = if (hasFilters) clearFilters else null
                )
            }
        }
        items(visible, key = { it.id }) { item ->
            MilestoneCard(
                item, groups[item.groupId]?.name.orEmpty(), item.status(today), today,
                teacher = state.teacher, enabled = !state.busy,
                onToggle = { onChange(item, if (item.completedAt > 0) "reopen" else "complete", {}) },
                onEdit = { editing = item; onDismissError(); showEditor = true },
                onDelete = { onDismissError(); deleting = item }
            )
        }
        item {
            PlanningNote("Deadlines are inclusive through the end of the selected day in Manila time. Teachers record completion.")
        }
    }
    if (showEditor) MilestoneEditor(state, editing, onDismiss = { if (!state.busy) showEditor = false }, onSave = { input -> onSave(input, editing) { showEditor = false } })
    deleting?.let { item -> AlertDialog(onDismissRequest = { if (!state.busy) deleting = null }, title = { Text("Delete this milestone?") },
        text = { Column {
            com.bdbshs.crest.ui.theme.CrestDialogSystemBars()
            Text("${item.title} will be removed from the group’s plan.")
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        } },
        confirmButton = { TextButton(enabled = !state.busy, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error), onClick = { onChange(item, "delete") { deleting = null } }) { Text("Delete milestone") } },
        dismissButton = { TextButton(enabled = !state.busy, onClick = { deleting = null }) { Text("Cancel") } }) }
}

@Composable
private fun PlanningSectionHeading(title: String, modifier: Modifier = Modifier) {
    Text(title, modifier.semantics { heading() }, style = MaterialTheme.typography.titleMedium)
}

private data class MilestoneAppearance(val background: Color, val foreground: Color, val icon: ImageVector)

@Composable
private fun milestoneAppearance(status: MilestoneStatus): MilestoneAppearance {
    val scheme = MaterialTheme.colorScheme
    val extended = MaterialTheme.crestColors
    return when (status) {
        MilestoneStatus.OVERDUE -> MilestoneAppearance(scheme.errorContainer, scheme.onErrorContainer, Icons.Outlined.ErrorOutline)
        MilestoneStatus.TODAY -> MilestoneAppearance(extended.warning, extended.onWarning, Icons.Outlined.Schedule)
        MilestoneStatus.UPCOMING -> MilestoneAppearance(extended.info, extended.onInfo, Icons.Outlined.CalendarMonth)
        MilestoneStatus.COMPLETED -> MilestoneAppearance(extended.success, extended.onSuccess, Icons.Outlined.CheckCircleOutline)
    }
}

@Composable
private fun MilestoneOverview(counts: Map<MilestoneStatus, Int>, selected: String, onSelect: (MilestoneStatus) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val fontScale = LocalDensity.current.fontScale
        val columns = if (maxWidth >= 640.dp && fontScale <= 1.3f) 4
            else if (maxWidth >= (if (fontScale > 1.5f) 480.dp else 280.dp)) 2 else 1
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MilestoneStatus.entries.chunked(columns).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { status ->
                        val appearance = milestoneAppearance(status)
                        val active = selected == status.name
                        Surface(
                            modifier = Modifier.weight(1f).clip(MaterialTheme.shapes.medium).selectable(active, onClick = { onSelect(status) }, role = Role.Button),
                            shape = MaterialTheme.shapes.medium,
                            color = if (active) appearance.background else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(if (active) 2.dp else 1.dp, if (active) appearance.foreground else MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                Modifier.heightIn(min = 64.dp).padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconBadge(appearance.icon, size = 36.dp, container = if (active) MaterialTheme.colorScheme.surface else appearance.background, content = appearance.foreground)
                                Column(Modifier.weight(1f)) {
                                    Text((counts[status] ?: 0).toString(), style = MaterialTheme.typography.titleLarge, color = if (active) appearance.foreground else MaterialTheme.colorScheme.onSurface)
                                    Text(status.label, style = MaterialTheme.typography.bodyMedium, color = if (active) appearance.foreground else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (active) Icon(Icons.Outlined.Check, "Filtering by ${status.label}", Modifier.size(20.dp), tint = appearance.foreground)
                            }
                        }
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/** Status is always shown as text plus icon; color only reinforces it. */
@Composable
private fun MilestoneStatusBadge(status: MilestoneStatus) {
    val appearance = milestoneAppearance(status)
    Surface(shape = RoundedCornerShape(50), color = appearance.background, contentColor = appearance.foreground) {
        Row(
            Modifier.padding(start = 8.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(appearance.icon, null, Modifier.size(16.dp))
            Text(status.label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** Plain-language distance to the deadline, e.g. "in 7 days" or "2 days late". */
private fun relativeDue(dueDate: String, today: LocalDate, completed: Boolean): String? {
    if (completed) return null
    val days = runCatching { java.time.temporal.ChronoUnit.DAYS.between(today, LocalDate.parse(dueDate)) }.getOrNull() ?: return null
    return when {
        days == 0L -> "today"
        days == 1L -> "tomorrow"
        days > 1L -> "in $days days"
        days == -1L -> "1 day late"
        else -> "${-days} days late"
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MilestoneCard(
    item: ResearchMilestone, groupName: String, status: MilestoneStatus, today: LocalDate, teacher: Boolean, enabled: Boolean,
    onToggle: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit
) {
    val completed = item.completedAt > 0
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                if (groupName.isNotBlank()) Text(groupName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MilestoneStatusBadge(status)
                MetadataBadge(item.stage.label)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.CalendarMonth, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                val relative = relativeDue(item.dueDate, today, completed)
                Text(
                    buildString {
                        append("Due ${displayPlanningDate(item.dueDate)}")
                        relative?.let { append(" · $it") }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (teacher) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedButton(onClick = onToggle, enabled = enabled, modifier = Modifier.weight(1f).semantics {
                        contentDescription = "${if (completed) "Reopen" else "Complete"} ${item.title}"
                    }, contentPadding = PaddingValues(horizontal = 12.dp)) {
                        Icon(if (completed) Icons.Outlined.Refresh else Icons.Outlined.CheckCircleOutline, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (completed) "Reopen" else "Mark complete", textAlign = TextAlign.Center)
                    }
                    IconButton(onClick = onEdit, enabled = enabled) {
                        Icon(Icons.Outlined.Edit, "Edit ${item.title}", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onDelete, enabled = enabled) {
                        Icon(Icons.Outlined.DeleteOutline, "Delete ${item.title}", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
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
            com.bdbshs.crest.ui.theme.CrestDialogSystemBars()
            Column(Modifier.heightIn(max = 450.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PlanningChoice("Research group", group, state.records.groups.map { it.id to it.name }, enabled = !state.busy && existing == null) { group = it }
                OutlinedTextField(title, { title = it.take(200) }, Modifier.fillMaxWidth(), label = { Text("Milestone title") }, enabled = !state.busy)
                PlanningChoice("Stage", stage, MilestoneStage.entries.map { it.value to it.label }, enabled = !state.busy) { stage = it }
                OutlinedTextField(dueDate, { dueDate = it }, Modifier.fillMaxWidth(), label = { Text("Deadline (Manila time)") }, placeholder = { Text("YYYY-MM-DD") },
                    enabled = !state.busy, singleLine = true, trailingIcon = { IconButton(onClick = { datePicker = true }, enabled = !state.busy) { Icon(Icons.Outlined.CalendarMonth, "Choose deadline") } })
                Text("The group has until the end of this day. Milestones are completed manually by a teacher.", style = MaterialTheme.typography.bodySmall)
                (error ?: state.error)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }, confirmButton = { Button(enabled = !state.busy, onClick = {
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
    CrestPageList(modifier) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PageHeading("Notification inbox", "Stay up to date with school and research activity.")
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            IconBadge(Icons.Outlined.Notifications, size = 48.dp, container = MaterialTheme.colorScheme.surface, content = MaterialTheme.colorScheme.primary)
                            Column(Modifier.weight(1f)) {
                                Text(if (state.records.loading) "Checking updates…" else "${unread.size} unread", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text("Read status is private to your account.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                        TonalButton(
                            onClick = { onRead(unread.map { it.id }, true, {}) },
                            enabled = !state.busy && unread.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Outlined.DoneAll, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Mark all read")
                        }
                    }
                }
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(!unreadOnly, { unreadOnly = false }, label = { Text("All") })
                            FilterChip(unreadOnly, { unreadOnly = true }, label = { Text("Unread") })
                        }
                        PlanningChoice("Update type", kind, listOf("all" to "All updates") + kinds) { kind = it }
                    }
                }
            }
        }
        state.error?.let { item { PlanningError(it, onRetry) } }
        state.inboxError?.let { item { PlanningError(it, onRetry) } }
        if (state.records.loading) item { PlanningLoading("Loading inbox") }
        else if (state.error == null && state.inboxError == null || all.isNotEmpty()) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    PlanningSectionHeading(if (unreadOnly) "Unread updates" else "Recent updates", Modifier.weight(1f))
                    MetadataBadge(visible.size.toString())
                }
            }
            if (visible.isEmpty()) item { PlanningEmpty(if (unreadOnly) "You’re all caught up" else "No updates to show", "Research submissions, approvals, and revision feedback appear here as school records sync.", Icons.Outlined.NotificationsNone) }
        }
        items(visible, key = { it.id }) { update ->
            val available = update.groupId.isBlank() || state.records.groups.any { it.id == update.groupId }
            OutlinedCard(
                Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, if (update.read) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.primary)
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IconBadge(when (update.kind) {
                            "submission" -> Icons.Outlined.UploadFile
                            "approval" -> Icons.Outlined.CheckCircleOutline
                            "revision" -> Icons.Outlined.RateReview
                            "account" -> Icons.Outlined.PersonAdd
                            else -> Icons.Outlined.Notifications
                        }, size = 40.dp)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(kinds.firstOrNull { it.first == update.kind }?.second ?: "School update", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Text(update.title, style = MaterialTheme.typography.titleMedium)
                        }
                        if (!update.read) MetadataBadge("New", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Text(update.message, style = MaterialTheme.typography.bodyMedium)
                    Text("Added to inbox ${if (update.observedAt > 0) DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.ENGLISH).format(Instant.ofEpochMilli(update.observedAt).atZone(PlanningDates.zone)) else "just now"} · Manila time", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (!available) Text("This group is no longer available.", style = MaterialTheme.typography.bodySmall)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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
        item { PlanningNote("Updates appear when school records sync. Times show when an update was added to your inbox, not when it happened.") }
    }
}

@Composable
private fun PlanningChoice(label: String, value: String, choices: List<Pair<String, String>>, modifier: Modifier = Modifier, enabled: Boolean = true, selected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box {
            OutlinedButton(onClick = { expanded = true }, enabled = enabled, modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                border = BorderStroke(1.dp, if (expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                contentPadding = PaddingValues(start = 12.dp, end = 8.dp)) {
                Text(choices.firstOrNull { it.first == value }?.second ?: "Choose ${label.lowercase()}",
                    Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Start)
                Icon(Icons.Outlined.KeyboardArrowDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            DropdownMenu(expanded, { expanded = false }) { choices.forEach { (id, text) ->
                DropdownMenuItem(text = { Text(text) }, onClick = { selected(id); expanded = false })
            } }
        }
    }
}
@Composable private fun PlanningEmpty(title: String, description: String, icon: ImageVector, action: (() -> Unit)? = null) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            IconBadge(icon, size = 56.dp)
            Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            if (action != null) TextButton(onClick = action) { Text("Clear filters") }
        }
    }
}
@Composable private fun PlanningError(message: String, retry: () -> Unit) {
    com.bdbshs.crest.ui.components.FeedbackPanel("Could not update this page", message, "Retry", retry)
}
@Composable private fun PlanningLoading(label: String) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(24.dp).semantics { contentDescription = label }, strokeWidth = 2.dp)
            Text("$label…", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
@Composable private fun PlanningNote(message: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
        Icon(Icons.Outlined.Info, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
private fun displayPlanningDate(date: String) = LocalDate.parse(date).format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH))
