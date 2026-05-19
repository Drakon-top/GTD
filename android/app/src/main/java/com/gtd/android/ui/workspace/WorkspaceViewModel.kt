package com.gtd.android.ui.workspace

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gtd.android.data.remote.dto.TaskDto
import com.gtd.android.data.repository.ApiResult
import com.gtd.android.data.repository.GtdRepository
import com.gtd.android.domain.model.GtdList
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
    val isLoadingTasks: Boolean = true,
    val isLoadingContext: Boolean = true,
    val error: String? = null,
    val showCreateTask: Boolean = false,
    val isCreatingTask: Boolean = false,
    val showMoveTask: Boolean = false,
    val moveTaskId: String? = null,
    val moveTaskCurrentList: String = "INBOX",
)

@HiltViewModel
class WorkspaceViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: GtdRepository,
) : ViewModel() {

    val contextId: String = savedStateHandle["contextId"] ?: ""

    private val _uiState = MutableStateFlow(WorkspaceUiState())
    val uiState: StateFlow<WorkspaceUiState> = _uiState.asStateFlow()

    init {
        loadAll()
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

            val categories = if (categoriesResult is ApiResult.Success) {
                categoriesResult.data.map { cat ->
                    SidebarSection(
                        key = "cat_${cat.id}",
                        label = cat.name,
                        count = catCounts[cat.id] ?: cat.taskCount,
                        isGtdList = false,
                    )
                }
            } else emptyList()

            _uiState.value = _uiState.value.copy(
                sidebarSections = gtdSections + doneSections + categories,
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
                is ApiResult.Success -> loadAll()
                is ApiResult.Error -> { /* silently fail for now */ }
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
                is ApiResult.Success -> loadAll()
                is ApiResult.Error -> { /* silently fail for now */ }
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            when (repository.deleteTask(taskId)) {
                is ApiResult.Success -> loadAll()
                is ApiResult.Error -> { /* silently fail for now */ }
            }
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
