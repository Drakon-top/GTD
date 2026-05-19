package com.gtd.android.ui.workspace

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DismissibleDrawerSheet
import androidx.compose.material3.DismissibleNavigationDrawer
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gtd.android.domain.model.GtdList
import kotlinx.coroutines.launch

private val GTD_ICONS = mapOf(
    "INBOX" to "\uD83D\uDCE5",
    "NEXT_ACTIONS" to "⚡",
    "PROJECTS" to "\uD83D\uDCC2",
    "WAITING_FOR" to "\uD83D\uDD70\uFE0F",
    "SOMEDAY_MAYBE" to "\uD83D\uDCA1",
    "REFERENCE" to "\uD83D\uDCD6",
    "CALENDAR" to "\uD83D\uDCC5",
    "DONE" to "✅",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceScreen(
    contextId: String,
    onNavigateBack: () -> Unit,
    onTaskClick: (String) -> Unit = {},
    viewModel: WorkspaceViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    DismissibleNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DismissibleDrawerSheet {
                WorkspaceDrawerContent(
                    sections = state.sidebarSections,
                    selectedSection = state.selectedSection,
                    onSectionClick = { key ->
                        viewModel.selectSection(key)
                        scope.launch { drawerState.close() }
                    },
                )
            }
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = state.contextName,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = sectionLabel(state.selectedSection, state.sidebarSections),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    navigationIcon = {
                        Row {
                            IconButton(onClick = onNavigateBack) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                )
                            }
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        }
                    },
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { viewModel.showCreateTask() },
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add task")
                }
            },
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            ) {
                when {
                    state.isLoadingContext || state.isLoadingTasks -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                    state.error != null -> {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("Failed to load", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.error!!,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Tap to retry",
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { viewModel.loadAll() },
                            )
                        }
                    }
                    state.tasks.isEmpty() -> {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = "No tasks",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tap + to add your first task",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            items(state.tasks, key = { it.id }) { task ->
                                SwipeableTaskItem(
                                    task = task,
                                    onCheckedChange = { viewModel.completeTask(task.id) },
                                    onClick = { onTaskClick(task.id) },
                                    onSwipeComplete = { viewModel.completeTask(task.id) },
                                    onSwipeMove = { viewModel.showMoveTask(task.id) },
                                )
                            }
                        }
                    }
                }
            }

            if (state.showCreateTask) {
                CreateTaskDialog(
                    isCreating = state.isCreatingTask,
                    onDismiss = { viewModel.dismissCreateTask() },
                    onCreate = { title -> viewModel.createTask(title) },
                )
            }

            if (state.showMoveTask) {
                MoveTaskDialog(
                    currentList = state.moveTaskCurrentList,
                    onDismiss = { viewModel.dismissMoveTask() },
                    onMove = { gtdList -> viewModel.moveTask(gtdList) },
                )
            }
        }
    }
}

@Composable
private fun WorkspaceDrawerContent(
    sections: List<SidebarSection>,
    selectedSection: String,
    onSectionClick: (String) -> Unit,
) {
    val gtdSections = sections.filter { it.isGtdList && it.key != "DONE" }
    val doneSections = sections.filter { it.isGtdList && it.key == "DONE" }
    val categorySections = sections.filter { !it.isGtdList }

    Column(modifier = Modifier.padding(top = 16.dp)) {
        Text(
            text = "GTD Lists",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp),
        )

        gtdSections.forEach { section ->
            NavigationDrawerItem(
                icon = {
                    Text(GTD_ICONS[section.key] ?: "\uD83D\uDCCB", fontSize = 18.sp)
                },
                label = { Text(section.label) },
                badge = if (section.count > 0) {
                    { Text("${section.count}", fontSize = 12.sp) }
                } else null,
                selected = selectedSection == section.key,
                onClick = { onSectionClick(section.key) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
            )
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp))

        doneSections.forEach { section ->
            NavigationDrawerItem(
                icon = { Text("✅", fontSize = 18.sp) },
                label = { Text(section.label) },
                badge = if (section.count > 0) {
                    { Text("${section.count}", fontSize = 12.sp) }
                } else null,
                selected = selectedSection == section.key,
                onClick = { onSectionClick(section.key) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
            )
        }

        if (categorySections.isNotEmpty()) {
            HorizontalDivider(modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp))

            Text(
                text = "Categories",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp),
            )

            categorySections.forEach { section ->
                NavigationDrawerItem(
                    icon = { Text("\uD83C\uDFF7\uFE0F", fontSize = 18.sp) },
                    label = { Text(section.label) },
                    badge = if (section.count > 0) {
                        { Text("${section.count}", fontSize = 12.sp) }
                    } else null,
                    selected = selectedSection == section.key,
                    onClick = { onSectionClick(section.key) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableTaskItem(
    task: TaskUiItem,
    onCheckedChange: () -> Unit,
    onClick: () -> Unit,
    onSwipeComplete: () -> Unit,
    onSwipeMove: () -> Unit,
) {
    if (task.isCompleted) {
        TaskListItemContent(task = task, onCheckedChange = onCheckedChange, onClick = onClick)
        return
    }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onSwipeMove()
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onSwipeComplete()
                    false
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val bgColor by animateColorAsState(
                when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> Color(0xFF1565C0)
                    SwipeToDismissBoxValue.EndToStart -> Color(0xFF2E7D32)
                    else -> Color.Transparent
                },
                label = "swipeBg",
            )
            val icon = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> "📂"
                SwipeToDismissBoxValue.EndToStart -> "✅"
                else -> ""
            }
            val label = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> "Move"
                SwipeToDismissBoxValue.EndToStart -> "Complete"
                else -> ""
            }
            val alignment = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                else -> Alignment.CenterEnd
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgColor, RoundedCornerShape(8.dp))
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment,
            ) {
                if (icon.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(icon, fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(label, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
    ) {
        TaskListItemContent(task = task, onCheckedChange = onCheckedChange, onClick = onClick)
    }
}

@Composable
private fun TaskListItemContent(
    task: TaskUiItem,
    onCheckedChange: () -> Unit,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = task.isCompleted,
                    onCheckedChange = { if (!task.isCompleted) onCheckedChange() },
                )

                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                        color = if (task.isCompleted) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (task.dueDate != null) {
                            Text(
                                text = task.dueDate,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (task.hasSubtasks) {
                            if (task.dueDate != null) {
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = "▸ subtasks",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }

            if (task.progress != null && task.progress > 0) {
                LinearProgressIndicator(
                    progress = { task.progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .height(3.dp),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
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
                    val label = WorkspaceViewModel.GTD_LABELS[list.name] ?: list.name
                    val icon = GTD_ICONS[list.name] ?: "📋"
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
private fun CreateTaskDialog(
    isCreating: Boolean,
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
) {
    var title by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isCreating) onDismiss() },
        title = { Text("New Task") },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("What needs to be done?") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isCreating,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(title.trim()) },
                enabled = title.isNotBlank() && !isCreating,
            ) {
                if (isCreating) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Add")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isCreating,
            ) {
                Text("Cancel")
            }
        },
    )
}

private fun sectionLabel(key: String, sections: List<SidebarSection>): String {
    return sections.find { it.key == key }?.label ?: key
}
