package com.gtd.android.ui.task

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gtd.android.domain.model.GtdList
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    taskId: String,
    contextId: String,
    onNavigateBack: () -> Unit,
    onNavigateToSubtask: (String) -> Unit,
    viewModel: TaskDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(state.isDeleted) {
        if (state.isDeleted) onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Task", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (state.hasUnsavedChanges) viewModel.saveTask()
                        onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.hasUnsavedChanges) {
                        IconButton(onClick = { viewModel.saveTask() }) {
                            if (state.isSaving) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Check, contentDescription = "Save")
                            }
                        }
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Move to...") },
                                onClick = {
                                    showMenu = false
                                    viewModel.showMoveDialog()
                                },
                            )
                            if (!state.isCompleted) {
                                DropdownMenuItem(
                                    text = { Text("Complete") },
                                    onClick = {
                                        showMenu = false
                                        viewModel.completeTask()
                                    },
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    viewModel.deleteTask()
                                },
                            )
                        }
                    }
                },
            )
        },
    ) { paddingValues ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            state.error != null && state.title.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Failed to load task", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(state.error!!, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Tap to retry", color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { viewModel.loadTask() })
                    }
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                ) {
                    TaskTitleSection(state, viewModel)
                    Spacer(modifier = Modifier.height(16.dp))
                    TaskMetadataSection(state, viewModel)
                    Spacer(modifier = Modifier.height(16.dp))
                    TaskNotesSection(state, viewModel)

                    if (state.progress != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        ProgressSection(state.progress!!)
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    SubtasksSection(
                        subtasks = state.subtasks,
                        nestingLevel = state.nestingLevel,
                        onComplete = { viewModel.completeSubtask(it) },
                        onTap = onNavigateToSubtask,
                        onAddSubtask = { viewModel.showAddSubtask() },
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    RemindersSection(
                        reminders = state.reminders,
                        isCompleted = state.isCompleted,
                        onAdd = { viewModel.showAddReminder() },
                        onDelete = { viewModel.deleteReminder(it) },
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    RecurrenceSection(
                        recurrenceRule = state.recurrenceRule,
                        isCompleted = state.isCompleted,
                        onConfigure = { viewModel.showRecurrencePicker() },
                        onClear = { viewModel.setRecurrence(null) },
                    )
                }
            }
        }
    }

    if (state.showMoveDialog) {
        MoveTaskDialog(
            currentList = state.gtdList,
            onDismiss = { viewModel.dismissMoveDialog() },
            onMove = { viewModel.moveToList(it) },
        )
    }

    if (state.showAddSubtask) {
        AddSubtaskDialog(
            onDismiss = { viewModel.dismissAddSubtask() },
            onCreate = { viewModel.createSubtask(it) },
        )
    }

    if (state.showDatePicker) {
        TaskDatePickerDialog(
            currentDate = state.dueDate,
            onDismiss = { viewModel.dismissDatePicker() },
            onConfirm = { viewModel.updateDueDate(it) },
            onClear = { viewModel.clearDueDate() },
        )
    }

    if (state.showAddReminder) {
        AddReminderDialog(
            onDismiss = { viewModel.dismissAddReminder() },
            onCreate = { viewModel.createReminder(it) },
        )
    }

    if (state.showRecurrencePicker) {
        RecurrencePickerDialog(
            currentRule = state.recurrenceRule,
            onDismiss = { viewModel.dismissRecurrencePicker() },
            onSelect = { viewModel.setRecurrence(it) },
        )
    }
}

@Composable
private fun TaskTitleSection(state: TaskDetailUiState, viewModel: TaskDetailViewModel) {
    OutlinedTextField(
        value = state.title,
        onValueChange = { viewModel.updateTitle(it) },
        label = { Text("Title") },
        modifier = Modifier.fillMaxWidth(),
        textStyle = MaterialTheme.typography.titleLarge,
        singleLine = false,
        maxLines = 3,
        enabled = !state.isCompleted,
    )
}

@Composable
private fun TaskMetadataSection(state: TaskDetailUiState, viewModel: TaskDetailViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Details", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)

        // GTD List chip
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("List: ", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            AssistChip(
                onClick = { viewModel.showMoveDialog() },
                label = {
                    val label = GTD_LIST_LABELS[state.gtdList] ?: state.gtdList
                    Text(label)
                },
            )
        }

        // Due date
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Due: ", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            AssistChip(
                onClick = { viewModel.showDatePicker() },
                label = {
                    Text(state.dueDate.ifBlank { "No due date" })
                },
                leadingIcon = {
                    Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                },
            )
            if (state.dueDate.isNotBlank()) {
                IconButton(onClick = { viewModel.clearDueDate() }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear date", modifier = Modifier.size(16.dp))
                }
            }
        }

        // Category
        if (state.categories.isNotEmpty()) {
            Text("Category", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.categoryId == null,
                    onClick = { viewModel.updateCategory(null) },
                    label = { Text("None") },
                )
                state.categories.forEach { cat ->
                    FilterChip(
                        selected = state.categoryId == cat.id,
                        onClick = { viewModel.updateCategory(cat.id) },
                        label = { Text(cat.name) },
                    )
                }
            }
        }

        if (state.isCompleted) {
            Text(
                "✅ Completed",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun TaskNotesSection(state: TaskDetailUiState, viewModel: TaskDetailViewModel) {
    Column {
        Text("Notes", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = state.notes,
            onValueChange = { viewModel.updateNotes(it) },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            placeholder = { Text("Add notes...") },
            enabled = !state.isCompleted,
        )
    }
}

@Composable
private fun ProgressSection(progress: Int) {
    Column {
        Text("Progress", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("$progress%", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SubtasksSection(
    subtasks: List<SubtaskUiItem>,
    nestingLevel: Int,
    onComplete: (String) -> Unit,
    onTap: (String) -> Unit,
    onAddSubtask: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Subtasks (${subtasks.size})",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            if (nestingLevel < 4) {
                IconButton(onClick = onAddSubtask) {
                    Icon(Icons.Default.Add, contentDescription = "Add subtask")
                }
            }
        }

        if (subtasks.isEmpty()) {
            Text(
                if (nestingLevel < 4) "No subtasks yet" else "Maximum nesting depth reached",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        } else {
            subtasks.forEach { subtask ->
                SubtaskItem(
                    subtask = subtask,
                    indent = 0,
                    onComplete = onComplete,
                    onTap = onTap,
                )
            }
        }
    }
}

@Composable
private fun SubtaskItem(
    subtask: SubtaskUiItem,
    indent: Int,
    onComplete: (String) -> Unit,
    onTap: (String) -> Unit,
) {
    Column(modifier = Modifier.padding(start = (indent * 24).dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onTap(subtask.id) }
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = subtask.isCompleted,
                onCheckedChange = { if (!subtask.isCompleted) onComplete(subtask.id) },
                modifier = Modifier.size(36.dp),
            )
            Text(
                text = subtask.title,
                style = MaterialTheme.typography.bodyMedium,
                textDecoration = if (subtask.isCompleted) TextDecoration.LineThrough else null,
                color = if (subtask.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (subtask.subtasks.isNotEmpty()) {
                Text(
                    "${subtask.subtasks.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 11.sp,
                )
            }
        }
        subtask.subtasks.forEach { child ->
            SubtaskItem(subtask = child, indent = indent + 1, onComplete = onComplete, onTap = onTap)
        }
    }
}

@Composable
private fun MoveTaskDialog(
    currentList: String,
    onDismiss: () -> Unit,
    onMove: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move to...") },
        text = {
            Column {
                GtdList.entries.forEach { list ->
                    val label = GTD_LIST_LABELS[list.name] ?: list.name
                    val icon = GTD_LIST_ICONS[list.name] ?: "📋"
                    val selected = list.name == currentList
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !selected) { onMove(list.name) }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(icon, fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            label,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    if (list != GtdList.entries.last()) {
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun AddSubtaskDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
) {
    var title by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Subtask") },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Subtask title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(title.trim()) },
                enabled = title.isNotBlank(),
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskDatePickerDialog(
    currentDate: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onClear: () -> Unit,
) {
    val initialMillis = if (currentDate.isNotBlank()) {
        try {
            LocalDate.parse(currentDate).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        } catch (_: Exception) { null }
    } else null

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val millis = datePickerState.selectedDateMillis
                if (millis != null) {
                    val date = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                    onConfirm(date.format(DateTimeFormatter.ISO_LOCAL_DATE))
                } else {
                    onDismiss()
                }
            }) { Text("OK") }
        },
        dismissButton = {
            Row {
                if (currentDate.isNotBlank()) {
                    TextButton(onClick = onClear) { Text("Clear") }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
private fun RemindersSection(
    reminders: List<ReminderUiItem>,
    isCompleted: Boolean,
    onAdd: () -> Unit,
    onDelete: (String) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Reminders (${reminders.size})",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (!isCompleted) {
                IconButton(onClick = onAdd) {
                    Icon(Icons.Default.Add, contentDescription = "Add reminder")
                }
            }
        }

        if (reminders.isEmpty()) {
            Text(
                "No reminders set",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        } else {
            reminders.forEach { reminder ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("🔔", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatReminderTime(reminder.remindAt),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    if (!isCompleted) {
                        IconButton(onClick = { onDelete(reminder.id) }, modifier = Modifier.size(28.dp)) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Delete",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecurrenceSection(
    recurrenceRule: String?,
    isCompleted: Boolean,
    onConfigure: () -> Unit,
    onClear: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Recurrence",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (recurrenceRule.isNullOrBlank()) {
            if (!isCompleted) {
                TextButton(onClick = onConfigure) {
                    Text("Set up recurring task")
                }
            } else {
                Text(
                    "Not recurring",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AssistChip(
                    onClick = { if (!isCompleted) onConfigure() },
                    label = { Text(formatRecurrenceLabel(recurrenceRule)) },
                )
                if (!isCompleted) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onClear, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Stop recurrence",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddReminderDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
) {
    val presets = listOf(
        "In 1 hour" to 1L,
        "In 3 hours" to 3L,
        "Tomorrow morning" to 24L,
        "In 2 days" to 48L,
        "In 1 week" to 168L,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Reminder") },
        text = {
            Column {
                presets.forEach { (label, hours) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val remindAt = java.time.Instant
                                    .now()
                                    .plusSeconds(hours * 3600)
                                    .toString()
                                onCreate(remindAt)
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("🔔", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                    if (label != presets.last().first) {
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun RecurrencePickerDialog(
    currentRule: String?,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    val options = listOf(
        "Daily" to """{"type":"daily"}""",
        "Weekly" to """{"type":"weekly"}""",
        "Every 2 days" to """{"type":"custom","intervalDays":2}""",
        "Every 3 days" to """{"type":"custom","intervalDays":3}""",
        "Monthly" to """{"type":"custom","intervalDays":30}""",
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Recurrence") },
        text = {
            Column {
                options.forEach { (label, rule) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(rule) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("🔄", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                        if (rule == currentRule) {
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    if (label != options.last().first) {
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

private fun formatReminderTime(isoString: String): String {
    return try {
        val instant = java.time.Instant.parse(isoString)
        val local = instant.atZone(java.time.ZoneId.systemDefault())
        val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm")
        local.format(formatter)
    } catch (_: Exception) {
        isoString
    }
}

private fun formatRecurrenceLabel(rule: String): String {
    return when {
        rule.contains("\"daily\"") -> "Daily"
        rule.contains("\"weekly\"") -> "Weekly"
        rule.contains("\"intervalDays\":2") -> "Every 2 days"
        rule.contains("\"intervalDays\":3") -> "Every 3 days"
        rule.contains("\"intervalDays\":30") -> "Monthly"
        else -> "Custom"
    }
}

private val GTD_LIST_LABELS = mapOf(
    "INBOX" to "Inbox",
    "NEXT_ACTIONS" to "Next Actions",
    "PROJECTS" to "Projects",
    "WAITING_FOR" to "Waiting For",
    "SOMEDAY_MAYBE" to "Someday / Maybe",
    "REFERENCE" to "Reference",
    "CALENDAR" to "Calendar",
    "DONE" to "Done",
)

private val GTD_LIST_ICONS = mapOf(
    "INBOX" to "\uD83D\uDCE5",
    "NEXT_ACTIONS" to "⚡",
    "PROJECTS" to "\uD83D\uDCC2",
    "WAITING_FOR" to "\uD83D\uDD70\uFE0F",
    "SOMEDAY_MAYBE" to "\uD83D\uDCA1",
    "REFERENCE" to "\uD83D\uDCD6",
    "CALENDAR" to "\uD83D\uDCC5",
    "DONE" to "✅",
)
