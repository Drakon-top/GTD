package com.gtd.android.ui.workspace

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gtd.android.data.remote.dto.TaskDto
import com.gtd.android.data.repository.ApiResult
import com.gtd.android.data.repository.GtdRepository
import com.gtd.android.data.sync.SyncManager
import com.gtd.android.data.sync.SyncStatus
import com.gtd.android.domain.model.GtdList
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class TaskUiItem(
    val id: String,
    val title: String,
    val isCompleted: Boolean,
    val dueDate: String?,
    val hasSubtasks: Boolean,
    val progress: Int?,
    val gtdList: String,
)

data class CategoryUiItem(
    val id: String,
    val name: String,
    val icon: String?,
    val color: String?,
    val taskCount: Int,
)

data class SidebarSection(
    val key: String,
    val label: String,
    val count: Int,
    val isGtdList: Boolean,
)

data class WorkspaceUiState(
    val contextName: String = "",
    val contextTheme: String = "MINIMALIST",
    val contextIcon: String = "briefcase",
    val sidebarSections: List<SidebarSection> = emptyList(),
    val selectedSection: String = "INBOX",
    val tasks: List<TaskUiItem> = emptyList(),
    val categories: List<CategoryUiItem> = emptyList(),
    val isLoadingTasks: Boolean = true,
    val isLoadingContext: Boolean = true,
    val error: String? = null,
    val showCreateTask: Boolean = false,
    val isCreatingTask: Boolean = false,
    val showMoveTask: Boolean = false,
    val moveTaskId: String? = null,
    val moveTaskCurrentList: String = "INBOX",
    val syncStatus: SyncStatus = SyncStatus.IDLE,
    val showCategoryManager: Boolean = false,
    val showThemePicker: Boolean = false,
    val showExport: Boolean = false,
    val isExporting: Boolean = false,
    val exportError: String? = null,
)

@HiltViewModel
class WorkspaceViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: GtdRepository,
    private val syncManager: SyncManager,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    val contextId: String = savedStateHandle["contextId"] ?: ""

    private val _uiState = MutableStateFlow(WorkspaceUiState())
    val uiState: StateFlow<WorkspaceUiState> = _uiState.asStateFlow()

    init {
        loadAll()
        observeSyncStatus()
    }

    private fun observeSyncStatus() {
        viewModelScope.launch {
            syncManager.syncStatus.collect { status ->
                _uiState.value = _uiState.value.copy(syncStatus = status)
                if (status == SyncStatus.IDLE) {
                    loadAll()
                }
            }
        }
    }

    fun loadAll() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingContext = true, error = null)

            val contextResult = repository.getContexts()
            val countsResult = repository.getTaskCounts(contextId)
            val categoriesResult = repository.getCategories(contextId)

            if (contextResult is ApiResult.Success) {
                val ctx = contextResult.data.find { it.id == contextId }
                if (ctx != null) {
                    _uiState.value = _uiState.value.copy(
                        contextName = ctx.name,
                        contextTheme = ctx.theme,
                        contextIcon = ctx.icon,
                    )
                }
            }

            val gtdCounts = if (countsResult is ApiResult.Success) countsResult.data.byGtdList else emptyMap()
            val catCounts = if (countsResult is ApiResult.Success) countsResult.data.byCategory else emptyMap()

            val gtdSections = GtdList.entries
                .filter { it != GtdList.DONE }
                .map { list ->
                    SidebarSection(
                        key = list.name,
                        label = GTD_LABELS[list.name] ?: list.name,
                        count = gtdCounts[list.name] ?: 0,
                        isGtdList = true,
                    )
                }

            val doneSections = listOf(
                SidebarSection(
                    key = GtdList.DONE.name,
                    label = "Done",
                    count = gtdCounts[GtdList.DONE.name] ?: 0,
                    isGtdList = true,
                )
            )

            val categoryItems = if (categoriesResult is ApiResult.Success) {
                categoriesResult.data.map { cat ->
                    CategoryUiItem(
                        id = cat.id,
                        name = cat.name,
                        icon = cat.icon,
                        color = cat.color,
                        taskCount = catCounts[cat.id] ?: cat.taskCount,
                    )
                }
            } else emptyList()

            val categorySections = categoryItems.map { cat ->
                SidebarSection(
                    key = "cat_${cat.id}",
                    label = cat.name,
                    count = cat.taskCount,
                    isGtdList = false,
                )
            }

            _uiState.value = _uiState.value.copy(
                sidebarSections = gtdSections + doneSections + categorySections,
                categories = categoryItems,
                isLoadingContext = false,
            )

            loadTasks()
        }
    }

    fun selectSection(key: String) {
        _uiState.value = _uiState.value.copy(selectedSection = key)
        loadTasks()
    }

    private fun loadTasks() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingTasks = true)
            val section = _uiState.value.selectedSection

            val result = if (section.startsWith("cat_")) {
                repository.getTasks(contextId)
            } else {
                repository.getTasks(contextId, section)
            }

            when (result) {
                is ApiResult.Success -> {
                    val filtered = if (section.startsWith("cat_")) {
                        val catId = section.removePrefix("cat_")
                        result.data.filter { it.categoryId == catId }
                    } else {
                        result.data
                    }
                    _uiState.value = _uiState.value.copy(
                        tasks = filtered.map { it.toUiItem() },
                        isLoadingTasks = false,
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        error = result.message,
                        isLoadingTasks = false,
                    )
                }
            }
        }
    }

    fun showCreateTask() {
        _uiState.value = _uiState.value.copy(showCreateTask = true)
    }

    fun dismissCreateTask() {
        _uiState.value = _uiState.value.copy(showCreateTask = false)
    }

    fun createTask(title: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreatingTask = true)
            when (repository.createTask(contextId, title)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isCreatingTask = false,
                        showCreateTask = false,
                    )
                    syncManager.requestSync()
                    loadAll()
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(isCreatingTask = false)
                }
            }
        }
    }

    fun completeTask(taskId: String) {
        viewModelScope.launch {
            when (repository.completeTask(taskId)) {
                is ApiResult.Success -> {
                    syncManager.requestSync()
                    loadAll()
                }
                is ApiResult.Error -> { /* silently fail */ }
            }
        }
    }

    fun showMoveTask(taskId: String) {
        val task = _uiState.value.tasks.find { it.id == taskId }
        _uiState.value = _uiState.value.copy(
            showMoveTask = true,
            moveTaskId = taskId,
            moveTaskCurrentList = task?.gtdList ?: "INBOX",
        )
    }

    fun dismissMoveTask() {
        _uiState.value = _uiState.value.copy(showMoveTask = false, moveTaskId = null)
    }

    fun moveTask(gtdList: String) {
        val taskId = _uiState.value.moveTaskId ?: return
        _uiState.value = _uiState.value.copy(showMoveTask = false, moveTaskId = null)
        viewModelScope.launch {
            when (repository.moveTask(taskId, gtdList)) {
                is ApiResult.Success -> {
                    syncManager.requestSync()
                    loadAll()
                }
                is ApiResult.Error -> { /* silently fail */ }
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            when (repository.deleteTask(taskId)) {
                is ApiResult.Success -> {
                    syncManager.requestSync()
                    loadAll()
                }
                is ApiResult.Error -> { /* silently fail */ }
            }
        }
    }

    // ── Category management ──

    fun showCategoryManager() {
        _uiState.value = _uiState.value.copy(showCategoryManager = true)
    }

    fun dismissCategoryManager() {
        _uiState.value = _uiState.value.copy(showCategoryManager = false)
    }

    fun createCategory(name: String, icon: String?, color: String?) {
        viewModelScope.launch {
            when (repository.createCategory(contextId, name, icon, color)) {
                is ApiResult.Success -> {
                    syncManager.requestSync()
                    loadAll()
                }
                is ApiResult.Error -> { /* silently fail */ }
            }
        }
    }

    fun updateCategory(categoryId: String, name: String, icon: String?, color: String?) {
        viewModelScope.launch {
            when (repository.updateCategory(categoryId, name, icon, color)) {
                is ApiResult.Success -> {
                    syncManager.requestSync()
                    loadAll()
                }
                is ApiResult.Error -> { /* silently fail */ }
            }
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            when (repository.deleteCategory(categoryId)) {
                is ApiResult.Success -> {
                    syncManager.requestSync()
                    loadAll()
                }
                is ApiResult.Error -> { /* silently fail */ }
            }
        }
    }

    // ── Theme picker ──

    fun showThemePicker() {
        _uiState.value = _uiState.value.copy(showThemePicker = true)
    }

    fun dismissThemePicker() {
        _uiState.value = _uiState.value.copy(showThemePicker = false)
    }

    fun changeTheme(newTheme: String) {
        viewModelScope.launch {
            val s = _uiState.value
            when (repository.updateContext(contextId, s.contextName, newTheme, s.contextIcon)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(contextTheme = newTheme, showThemePicker = false)
                    syncManager.requestSync()
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(showThemePicker = false)
                }
            }
        }
    }

    // ── Export ──

    fun showExport() {
        _uiState.value = _uiState.value.copy(showExport = true, exportError = null)
    }

    fun dismissExport() {
        _uiState.value = _uiState.value.copy(showExport = false, exportError = null)
    }

    fun exportContext() {
        export(contextId)
    }

    fun exportAll() {
        export(null)
    }

    private fun export(ctxId: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true, exportError = null)
            when (val result = repository.exportData(ctxId)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(isExporting = false, showExport = false)
                    shareJsonFile(result.data, if (ctxId != null) _uiState.value.contextName else "all_contexts")
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(isExporting = false, exportError = result.message)
                }
            }
        }
    }

    private fun shareJsonFile(json: String, name: String) {
        try {
            val dir = File(appContext.cacheDir, "exports")
            dir.mkdirs()
            val file = File(dir, "${name.replace(" ", "_")}_export.json")
            file.writeText(json)
            val uri: Uri = FileProvider.getUriForFile(appContext, "${appContext.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            appContext.startActivity(Intent.createChooser(intent, "Export GTD Data").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (_: Exception) {
            _uiState.value = _uiState.value.copy(exportError = "Failed to share file")
        }
    }

    private fun TaskDto.toUiItem() = TaskUiItem(
        id = id,
        title = title,
        isCompleted = isCompleted,
        dueDate = dueDate,
        hasSubtasks = !subtasks.isNullOrEmpty(),
        progress = progress,
        gtdList = gtdList,
    )

    companion object {
        val GTD_LABELS = mapOf(
            "INBOX" to "Inbox",
            "NEXT_ACTIONS" to "Next Actions",
            "PROJECTS" to "Projects",
            "WAITING_FOR" to "Waiting For",
            "SOMEDAY_MAYBE" to "Someday / Maybe",
            "REFERENCE" to "Reference",
            "CALENDAR" to "Calendar",
            "DONE" to "Done",
        )
    }
}
