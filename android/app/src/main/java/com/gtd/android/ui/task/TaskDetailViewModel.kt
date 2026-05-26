package com.gtd.android.ui.task

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gtd.android.data.remote.dto.CategoryDto
import com.gtd.android.data.remote.dto.ReminderDto
import com.gtd.android.data.remote.dto.TaskDto
import com.gtd.android.data.remote.dto.UpdateTaskRequest
import com.gtd.android.data.repository.ApiResult
import com.gtd.android.data.repository.GtdRepository
import com.gtd.android.data.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SubtaskUiItem(
    val id: String,
    val title: String,
    val isCompleted: Boolean,
    val nestingLevel: Int,
    val subtasks: List<SubtaskUiItem> = emptyList(),
)

data class ReminderUiItem(
    val id: String,
    val remindAt: String,
    val offsetType: String?,
    val offsetValue: Int?,
)

data class TaskDetailUiState(
    val taskId: String = "",
    val contextId: String = "",
    val contextTheme: String = "MINIMALIST",
    val title: String = "",
    val notes: String = "",
    val dueDate: String = "",
    val gtdList: String = "INBOX",
    val categoryId: String? = null,
    val nestingLevel: Int = 1,
    val isCompleted: Boolean = false,
    val progress: Int? = null,
    val recurrenceRule: String? = null,
    val subtasks: List<SubtaskUiItem> = emptyList(),
    val reminders: List<ReminderUiItem> = emptyList(),
    val categories: List<CategoryDto> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val isDeleted: Boolean = false,
    val showAddSubtask: Boolean = false,
    val showMoveDialog: Boolean = false,
    val showDatePicker: Boolean = false,
    val showAddReminder: Boolean = false,
    val showRecurrencePicker: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
)

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: GtdRepository,
    private val syncManager: SyncManager,
) : ViewModel() {

    val taskId: String = savedStateHandle["taskId"] ?: ""
    val contextId: String = savedStateHandle["contextId"] ?: ""

    private val _uiState = MutableStateFlow(TaskDetailUiState(taskId = taskId, contextId = contextId))
    val uiState: StateFlow<TaskDetailUiState> = _uiState.asStateFlow()

    private var originalTitle = ""
    private var originalNotes = ""
    private var originalDueDate = ""
    private var originalCategoryId: String? = null

    init {
        loadTask()
    }

    fun loadTask() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val taskResult = repository.getTask(taskId)
            val categoriesResult = repository.getCategories(contextId)
            val remindersResult = repository.getReminders(taskId)
            val contextResult = repository.getContexts()

            val contextTheme = if (contextResult is ApiResult.Success) {
                contextResult.data.find { it.id == contextId }?.theme ?: "MINIMALIST"
            } else "MINIMALIST"

            when (taskResult) {
                is ApiResult.Success -> {
                    val task = taskResult.data
                    originalTitle = task.title
                    originalNotes = task.notes ?: ""
                    originalDueDate = task.dueDate ?: ""
                    originalCategoryId = task.categoryId

                    val reminderItems = if (remindersResult is ApiResult.Success) {
                        remindersResult.data.map { it.toUiItem() }
                    } else emptyList()

                    _uiState.value = _uiState.value.copy(
                        contextTheme = contextTheme,
                        title = task.title,
                        notes = task.notes ?: "",
                        dueDate = task.dueDate ?: "",
                        gtdList = task.gtdList,
                        categoryId = task.categoryId,
                        nestingLevel = task.nestingLevel,
                        isCompleted = task.isCompleted,
                        progress = task.progress,
                        recurrenceRule = task.recurrenceRule,
                        subtasks = task.subtasks?.map { it.toSubtaskUi() } ?: emptyList(),
                        reminders = reminderItems,
                        categories = if (categoriesResult is ApiResult.Success) categoriesResult.data else emptyList(),
                        isLoading = false,
                        hasUnsavedChanges = false,
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        contextTheme = contextTheme,
                        error = taskResult.message,
                        isLoading = false,
                    )
                }
            }
        }
    }

    fun updateTitle(value: String) {
        _uiState.value = _uiState.value.copy(title = value)
        checkUnsavedChanges()
    }

    fun updateNotes(value: String) {
        _uiState.value = _uiState.value.copy(notes = value)
        checkUnsavedChanges()
    }

    fun updateDueDate(value: String) {
        _uiState.value = _uiState.value.copy(dueDate = value, showDatePicker = false)
        checkUnsavedChanges()
    }

    fun clearDueDate() {
        _uiState.value = _uiState.value.copy(dueDate = "", showDatePicker = false)
        checkUnsavedChanges()
    }

    fun updateCategory(categoryId: String?) {
        _uiState.value = _uiState.value.copy(categoryId = categoryId)
        checkUnsavedChanges()
    }

    fun showDatePicker() {
        _uiState.value = _uiState.value.copy(showDatePicker = true)
    }

    fun dismissDatePicker() {
        _uiState.value = _uiState.value.copy(showDatePicker = false)
    }

    private fun checkUnsavedChanges() {
        val s = _uiState.value
        val changed = s.title != originalTitle ||
            s.notes != originalNotes ||
            s.dueDate != originalDueDate ||
            s.categoryId != originalCategoryId
        _uiState.value = s.copy(hasUnsavedChanges = changed)
    }

    fun saveTask() {
        val s = _uiState.value
        if (!s.hasUnsavedChanges) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            val request = UpdateTaskRequest(
                title = s.title.ifBlank { null },
                notes = s.notes.ifBlank { null },
                dueDate = s.dueDate.ifBlank { null },
                categoryId = s.categoryId,
            )
            when (val result = repository.updateTask(taskId, request)) {
                is ApiResult.Success -> {
                    originalTitle = result.data.title
                    originalNotes = result.data.notes ?: ""
                    originalDueDate = result.data.dueDate ?: ""
                    originalCategoryId = result.data.categoryId
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        hasUnsavedChanges = false,
                    )
                    syncManager.requestSync()
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = result.message,
                    )
                }
            }
        }
    }

    fun deleteTask() {
        viewModelScope.launch {
            when (repository.deleteTask(taskId)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(isDeleted = true)
                    syncManager.requestSync()
                }
                is ApiResult.Error -> { /* stay on screen */ }
            }
        }
    }

    fun completeTask() {
        viewModelScope.launch {
            when (repository.completeTask(taskId)) {
                is ApiResult.Success -> {
                    syncManager.requestSync()
                    loadTask()
                }
                is ApiResult.Error -> { /* silently fail */ }
            }
        }
    }

    fun completeSubtask(subtaskId: String) {
        viewModelScope.launch {
            when (repository.completeTask(subtaskId)) {
                is ApiResult.Success -> {
                    syncManager.requestSync()
                    loadTask()
                }
                is ApiResult.Error -> { /* silently fail */ }
            }
        }
    }

    fun showMoveDialog() {
        _uiState.value = _uiState.value.copy(showMoveDialog = true)
    }

    fun dismissMoveDialog() {
        _uiState.value = _uiState.value.copy(showMoveDialog = false)
    }

    fun moveToList(gtdList: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(showMoveDialog = false)
            when (repository.moveTask(taskId, gtdList)) {
                is ApiResult.Success -> {
                    syncManager.requestSync()
                    loadTask()
                }
                is ApiResult.Error -> { /* silently fail */ }
            }
        }
    }

    fun showAddSubtask() {
        _uiState.value = _uiState.value.copy(showAddSubtask = true)
    }

    fun dismissAddSubtask() {
        _uiState.value = _uiState.value.copy(showAddSubtask = false)
    }

    fun createSubtask(title: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(showAddSubtask = false)
            when (repository.createSubtask(taskId, title)) {
                is ApiResult.Success -> {
                    syncManager.requestSync()
                    loadTask()
                }
                is ApiResult.Error -> { /* silently fail */ }
            }
        }
    }

    // ── Reminders ──

    fun showAddReminder() {
        _uiState.value = _uiState.value.copy(showAddReminder = true)
    }

    fun dismissAddReminder() {
        _uiState.value = _uiState.value.copy(showAddReminder = false)
    }

    fun createReminder(remindAt: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(showAddReminder = false)
            when (repository.createReminder(taskId, remindAt, null, null)) {
                is ApiResult.Success -> {
                    syncManager.requestSync()
                    loadTask()
                }
                is ApiResult.Error -> { /* silently fail */ }
            }
        }
    }

    fun deleteReminder(reminderId: String) {
        viewModelScope.launch {
            when (repository.deleteReminder(reminderId)) {
                is ApiResult.Success -> {
                    syncManager.requestSync()
                    loadTask()
                }
                is ApiResult.Error -> { /* silently fail */ }
            }
        }
    }

    // ── Recurrence ──

    fun showRecurrencePicker() {
        _uiState.value = _uiState.value.copy(showRecurrencePicker = true)
    }

    fun dismissRecurrencePicker() {
        _uiState.value = _uiState.value.copy(showRecurrencePicker = false)
    }

    fun setRecurrence(rule: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(showRecurrencePicker = false)
            val request = UpdateTaskRequest(recurrenceRule = rule)
            when (repository.updateTask(taskId, request)) {
                is ApiResult.Success -> {
                    syncManager.requestSync()
                    loadTask()
                }
                is ApiResult.Error -> { /* silently fail */ }
            }
        }
    }

    private fun ReminderDto.toUiItem() = ReminderUiItem(
        id = id,
        remindAt = remindAt,
        offsetType = offsetType,
        offsetValue = offsetValue,
    )

    private fun TaskDto.toSubtaskUi(): SubtaskUiItem = SubtaskUiItem(
        id = id,
        title = title,
        isCompleted = isCompleted,
        nestingLevel = nestingLevel,
        subtasks = subtasks?.map { it.toSubtaskUi() } ?: emptyList(),
    )
}
