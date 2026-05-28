package com.gtd.android.data.repository

import android.util.Log
import com.gtd.android.data.NetworkMonitor
import com.gtd.android.data.flattenWithSubtasks
import com.gtd.android.data.local.TokenStorage
import com.gtd.android.data.local.dao.CategoryDao
import com.gtd.android.data.local.dao.ContextDao
import com.gtd.android.data.local.dao.PendingChangeDao
import com.gtd.android.data.local.dao.ReminderDao
import com.gtd.android.data.local.dao.TaskDao
import com.gtd.android.data.local.dao.UserDao
import com.gtd.android.data.local.entity.CategoryEntity
import com.gtd.android.data.local.entity.ContextEntity
import com.gtd.android.data.local.entity.PendingChangeEntity
import com.gtd.android.data.local.entity.ReminderEntity
import com.gtd.android.data.local.entity.TaskEntity
import com.gtd.android.data.local.entity.UserEntity
import com.gtd.android.data.remote.api.GtdApi
import com.gtd.android.data.remote.dto.CategoryDto
import com.gtd.android.data.remote.dto.ContextDto
import com.gtd.android.data.remote.dto.CreateCategoryRequest
import com.gtd.android.data.remote.dto.CreateContextRequest
import com.gtd.android.data.remote.dto.CreateReminderRequest
import com.gtd.android.data.remote.dto.CreateTaskRequest
import com.gtd.android.data.remote.dto.MoveTaskRequest
import com.gtd.android.data.remote.dto.ReminderDto
import com.gtd.android.data.remote.dto.TaskCountsDto
import com.gtd.android.data.remote.dto.TaskDto
import com.gtd.android.data.remote.dto.UpdateTaskRequest
import com.gtd.android.data.toDto
import com.gtd.android.data.toEntity
import com.gtd.android.notification.ReminderScheduler
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: Int = 0) : ApiResult<Nothing>()
}

@Singleton
class GtdRepository @Inject constructor(
    private val api: GtdApi,
    private val json: Json,
    private val userDao: UserDao,
    private val contextDao: ContextDao,
    private val taskDao: TaskDao,
    private val categoryDao: CategoryDao,
    private val reminderDao: ReminderDao,
    private val pendingChangeDao: PendingChangeDao,
    private val tokenStorage: TokenStorage,
    private val networkMonitor: NetworkMonitor,
    private val reminderScheduler: ReminderScheduler,
) {

    // ── Contexts ──

    suspend fun getContexts(): ApiResult<List<ContextDto>> {
        if (networkMonitor.isCurrentlyOnline()) {
            val result = safeCall { api.getContexts() }
            if (result is ApiResult.Success) {
                val userId = tokenStorage.getUserId() ?: ""
                if (userId.isNotBlank()) {
                    ensureUserExists(userId)
                    try {
                        contextDao.insertAll(result.data.map { it.toEntity(userId) })
                    } catch (e: Exception) {
                        Log.e("GtdRepository", "Failed to cache contexts locally", e)
                    }
                }
                return result
            }
        }
        return loadContextsFromRoom()
    }

    private suspend fun loadContextsFromRoom(): ApiResult<List<ContextDto>> {
        val userId = tokenStorage.getUserId() ?: return ApiResult.Error("Not logged in")
        val entities = contextDao.getByUserId(userId)
        return if (entities.isNotEmpty()) {
            ApiResult.Success(entities.map { it.toDto() })
        } else {
            ApiResult.Error("No data available offline")
        }
    }

    suspend fun createContext(name: String, theme: String, icon: String): ApiResult<ContextDto> {
        if (networkMonitor.isCurrentlyOnline()) {
            val result = safeCall { api.createContext(CreateContextRequest(name, theme, icon)) }
            if (result is ApiResult.Success) {
                val userId = tokenStorage.getUserId() ?: ""
                if (userId.isNotBlank()) {
                    try {
                        ensureUserExists(userId)
                        contextDao.insert(result.data.toEntity(userId))
                    } catch (e: Exception) {
                        Log.e("GtdRepository", "Failed to cache context locally", e)
                    }
                }
            }
            return result
        }
        val userId = tokenStorage.getUserId() ?: return ApiResult.Error("Not logged in")
        val localId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val entity = ContextEntity(
            id = localId,
            userId = userId,
            name = name,
            theme = theme,
            icon = icon,
            sortOrder = 0,
            createdAt = now,
            updatedAt = now,
        )
        try {
            ensureUserExists(userId)
            contextDao.insert(entity)
        } catch (e: Exception) {
            Log.e("GtdRepository", "Failed to insert context locally", e)
            return ApiResult.Error("Local database error: ${e.message}")
        }
        trackChange("CONTEXT", localId, "CREATE", null, name)
        return ApiResult.Success(entity.toDto())
    }

    // ── Task counts ──

    suspend fun getTaskCounts(contextId: String): ApiResult<TaskCountsDto> {
        if (networkMonitor.isCurrentlyOnline()) {
            val result = safeCall { api.getTaskCounts(contextId) }
            if (result is ApiResult.Success) return result
        }
        return loadTaskCountsFromRoom(contextId)
    }

    private suspend fun loadTaskCountsFromRoom(contextId: String): ApiResult<TaskCountsDto> {
        val gtdLists = listOf(
            "INBOX", "NEXT_ACTIONS", "PROJECTS", "WAITING_FOR",
            "SOMEDAY_MAYBE", "REFERENCE", "CALENDAR", "DONE",
        )
        val byGtdList = gtdLists.associateWith { taskDao.countByGtdList(contextId, it) }
            .filter { it.value > 0 }
        val total = byGtdList.values.sum()
        return ApiResult.Success(TaskCountsDto(contextId = contextId, byGtdList = byGtdList, total = total))
    }

    // ── Tasks ──

    suspend fun getTasks(contextId: String, gtdList: String? = null): ApiResult<List<TaskDto>> {
        if (networkMonitor.isCurrentlyOnline()) {
            val result = safeCall { api.getTasks(contextId, gtdList) }
            if (result is ApiResult.Success) {
                cacheTasksLocally(result.data)
                return result
            }
        }
        return loadTasksFromRoom(contextId, gtdList)
    }

    private suspend fun loadTasksFromRoom(contextId: String, gtdList: String?): ApiResult<List<TaskDto>> {
        val entities = if (gtdList != null) {
            taskDao.getByGtdList(contextId, gtdList)
        } else {
            taskDao.getByContextId(contextId)
        }
        val tasks = entities.map { entity ->
            val subtaskEntities = taskDao.getSubtasks(entity.id)
            entity.toDto(
                subtasks = subtaskEntities.map { it.toDto() },
                progress = computeProgress(entity.id),
            )
        }
        return ApiResult.Success(tasks)
    }

    suspend fun createTask(contextId: String, title: String): ApiResult<TaskDto> {
        if (networkMonitor.isCurrentlyOnline()) {
            val result = safeCall { api.createTask(contextId, CreateTaskRequest(title = title)) }
            if (result is ApiResult.Success) {
                try {
                    ensureContextExists(contextId)
                    taskDao.insert(result.data.toEntity())
                } catch (e: Exception) {
                    Log.e("GtdRepository", "Failed to cache task locally", e)
                }
            }
            return result
        }
        val localId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val entity = TaskEntity(
            id = localId,
            contextId = contextId,
            title = title,
            gtdList = "INBOX",
            nestingLevel = 1,
            createdAt = now,
            updatedAt = now,
        )
        try {
            ensureContextExists(contextId)
            taskDao.insert(entity)
        } catch (e: Exception) {
            Log.e("GtdRepository", "Failed to insert task locally", e)
            return ApiResult.Error("Local database error: ${e.message}")
        }
        trackChange("TASK", localId, "CREATE", null, title)
        return ApiResult.Success(entity.toDto())
    }

    suspend fun getTask(taskId: String): ApiResult<TaskDto> {
        if (networkMonitor.isCurrentlyOnline()) {
            val result = safeCall { api.getTask(taskId) }
            if (result is ApiResult.Success) {
                cacheTasksLocally(listOf(result.data))
                return result
            }
        }
        return loadTaskFromRoom(taskId)
    }

    private suspend fun loadTaskFromRoom(taskId: String): ApiResult<TaskDto> {
        val entity = taskDao.getById(taskId) ?: return ApiResult.Error("Task not found")
        val subtaskEntities = taskDao.getSubtasks(taskId)
        val subtasks = subtaskEntities.map { sub ->
            val subSubs = taskDao.getSubtasks(sub.id)
            sub.toDto(subtasks = subSubs.map { it.toDto() })
        }
        return ApiResult.Success(
            entity.toDto(subtasks = subtasks, progress = computeProgress(taskId))
        )
    }

    suspend fun updateTask(taskId: String, request: UpdateTaskRequest): ApiResult<TaskDto> {
        if (networkMonitor.isCurrentlyOnline()) {
            val result = safeCall { api.updateTask(taskId, request) }
            if (result is ApiResult.Success) {
                try {
                    taskDao.insert(result.data.toEntity())
                } catch (e: Exception) {
                    Log.e("GtdRepository", "Failed to cache updated task locally", e)
                }
            }
            return result
        }
        val entity = taskDao.getById(taskId) ?: return ApiResult.Error("Task not found")
        val now = System.currentTimeMillis()
        val updated = entity.copy(
            title = request.title ?: entity.title,
            notes = request.notes ?: entity.notes,
            dueDate = request.dueDate ?: entity.dueDate,
            categoryId = request.categoryId ?: entity.categoryId,
            gtdList = request.gtdList ?: entity.gtdList,
            sortOrder = request.sortOrder ?: entity.sortOrder,
            recurrenceRule = request.recurrenceRule ?: entity.recurrenceRule,
            updatedAt = now,
            version = entity.version + 1,
        )
        taskDao.update(updated)
        trackFieldChanges(taskId, entity, updated, now)
        return ApiResult.Success(updated.toDto())
    }

    suspend fun deleteTask(taskId: String): ApiResult<Unit> {
        val now = System.currentTimeMillis()
        if (networkMonitor.isCurrentlyOnline()) {
            val result = safeCall { api.deleteTask(taskId) }
            if (result is ApiResult.Success) {
                taskDao.softDelete(taskId, now)
                taskDao.softDeleteByParent(taskId, now)
                return result
            }
        }
        taskDao.softDelete(taskId, now)
        taskDao.softDeleteByParent(taskId, now)
        trackChange("TASK", taskId, "DELETE", null, null)
        return ApiResult.Success(Unit)
    }

    suspend fun moveTask(taskId: String, gtdList: String): ApiResult<TaskDto> {
        if (networkMonitor.isCurrentlyOnline()) {
            val result = safeCall { api.moveTask(taskId, MoveTaskRequest(gtdList)) }
            if (result is ApiResult.Success) {
                try {
                    taskDao.insert(result.data.toEntity())
                } catch (e: Exception) {
                    Log.e("GtdRepository", "Failed to cache moved task locally", e)
                }
            }
            return result
        }
        val entity = taskDao.getById(taskId) ?: return ApiResult.Error("Task not found")
        val now = System.currentTimeMillis()
        val updated = entity.copy(gtdList = gtdList, updatedAt = now, version = entity.version + 1)
        taskDao.update(updated)
        trackChange("TASK", taskId, "MOVE", entity.gtdList, gtdList, fieldName = "gtdList")
        return ApiResult.Success(updated.toDto())
    }

    suspend fun completeTask(taskId: String): ApiResult<TaskDto> {
        val now = System.currentTimeMillis()
        if (networkMonitor.isCurrentlyOnline()) {
            val result = safeCall { api.completeTask(taskId) }
            if (result is ApiResult.Success) {
                try {
                    taskDao.insert(result.data.toEntity())
                } catch (e: Exception) {
                    Log.e("GtdRepository", "Failed to cache completed task locally", e)
                }
                return result
            }
        }
        val entity = taskDao.getById(taskId) ?: return ApiResult.Error("Task not found")
        val updated = entity.copy(
            isCompleted = true,
            completedAt = now,
            gtdList = "DONE",
            updatedAt = now,
            version = entity.version + 1,
        )
        taskDao.update(updated)
        trackChange("TASK", taskId, "COMPLETE", "false", "true")
        return ApiResult.Success(updated.toDto())
    }

    suspend fun reopenTask(taskId: String): ApiResult<TaskDto> {
        val now = System.currentTimeMillis()
        if (networkMonitor.isCurrentlyOnline()) {
            val result = safeCall { api.reopenTask(taskId) }
            if (result is ApiResult.Success) {
                try {
                    taskDao.insert(result.data.toEntity())
                } catch (e: Exception) {
                    Log.e("GtdRepository", "Failed to cache reopened task locally", e)
                }
                return result
            }
        }
        val entity = taskDao.getById(taskId) ?: return ApiResult.Error("Task not found")
        val updated = entity.copy(
            isCompleted = false,
            completedAt = null,
            gtdList = "INBOX",
            updatedAt = now,
            version = entity.version + 1,
        )
        taskDao.update(updated)
        trackChange("TASK", taskId, "REOPEN", "true", "false")
        return ApiResult.Success(updated.toDto())
    }

    // ── Subtasks ──

    suspend fun createSubtask(parentId: String, title: String): ApiResult<TaskDto> {
        if (networkMonitor.isCurrentlyOnline()) {
            val result = safeCall { api.createSubtask(parentId, CreateTaskRequest(title = title)) }
            if (result is ApiResult.Success) {
                try {
                    taskDao.insert(result.data.toEntity())
                } catch (e: Exception) {
                    Log.e("GtdRepository", "Failed to cache subtask locally", e)
                }
            }
            return result
        }
        val parent = taskDao.getById(parentId) ?: return ApiResult.Error("Parent not found")
        if (parent.nestingLevel >= 4) return ApiResult.Error("Max nesting level reached")
        val localId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val entity = TaskEntity(
            id = localId,
            contextId = parent.contextId,
            parentTaskId = parentId,
            title = title,
            gtdList = parent.gtdList,
            nestingLevel = parent.nestingLevel + 1,
            createdAt = now,
            updatedAt = now,
        )
        taskDao.insert(entity)
        trackChange("TASK", localId, "CREATE_SUBTASK", parentId, title)
        return ApiResult.Success(entity.toDto())
    }

    suspend fun getSubtasks(taskId: String): ApiResult<List<TaskDto>> {
        if (networkMonitor.isCurrentlyOnline()) {
            val result = safeCall { api.getSubtasks(taskId) }
            if (result is ApiResult.Success) {
                try {
                    result.data.forEach { taskDao.insert(it.toEntity()) }
                } catch (e: Exception) {
                    Log.e("GtdRepository", "Failed to cache subtasks locally", e)
                }
                return result
            }
        }
        val entities = taskDao.getSubtasks(taskId)
        return ApiResult.Success(entities.map { it.toDto() })
    }

    // ── Categories ──

    suspend fun getCategories(contextId: String): ApiResult<List<CategoryDto>> {
        if (networkMonitor.isCurrentlyOnline()) {
            val result = safeCall { api.getCategories(contextId) }
            if (result is ApiResult.Success) {
                categoryDao.insertAll(result.data.map { it.toEntity() })
                return result
            }
        }
        return loadCategoriesFromRoom(contextId)
    }

    suspend fun createCategory(contextId: String, name: String, icon: String?, color: String?): ApiResult<CategoryDto> {
        val request = CreateCategoryRequest(name = name, icon = icon, color = color)
        if (networkMonitor.isCurrentlyOnline()) {
            val result = safeCall { api.createCategory(contextId, request) }
            if (result is ApiResult.Success) {
                categoryDao.insert(result.data.toEntity())
            }
            return result
        }
        val localId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val entity = CategoryEntity(
            id = localId,
            contextId = contextId,
            name = name,
            icon = icon,
            color = color,
            createdAt = now,
            updatedAt = now,
        )
        categoryDao.insert(entity)
        trackChange("CATEGORY", localId, "CREATE", null, name)
        return ApiResult.Success(entity.toDto())
    }

    suspend fun updateCategory(categoryId: String, name: String, icon: String?, color: String?): ApiResult<CategoryDto> {
        val request = CreateCategoryRequest(name = name, icon = icon, color = color)
        if (networkMonitor.isCurrentlyOnline()) {
            val result = safeCall { api.updateCategory(categoryId, request) }
            if (result is ApiResult.Success) {
                categoryDao.insert(result.data.toEntity())
            }
            return result
        }
        val entity = categoryDao.getById(categoryId) ?: return ApiResult.Error("Category not found")
        val now = System.currentTimeMillis()
        val updated = entity.copy(name = name, icon = icon, color = color, updatedAt = now)
        categoryDao.update(updated)
        trackChange("CATEGORY", categoryId, "UPDATE", entity.name, name)
        return ApiResult.Success(updated.toDto())
    }

    suspend fun deleteCategory(categoryId: String): ApiResult<Unit> {
        if (networkMonitor.isCurrentlyOnline()) {
            val result = safeCall { api.deleteCategory(categoryId) }
            if (result is ApiResult.Success) {
                categoryDao.softDelete(categoryId, System.currentTimeMillis())
                return result
            }
        }
        categoryDao.softDelete(categoryId, System.currentTimeMillis())
        trackChange("CATEGORY", categoryId, "DELETE", null, null)
        return ApiResult.Success(Unit)
    }

    // ── Reminders ──

    suspend fun getReminders(taskId: String): ApiResult<List<ReminderDto>> {
        if (networkMonitor.isCurrentlyOnline()) {
            val result = safeCall { api.getReminders(taskId) }
            if (result is ApiResult.Success) {
                reminderDao.insertAll(result.data.map { it.toEntity() })
                return result
            }
        }
        return loadRemindersFromRoom(taskId)
    }

    suspend fun createReminder(taskId: String, remindAt: String, offsetType: String?, offsetValue: Int?): ApiResult<ReminderDto> {
        val request = CreateReminderRequest(remindAt = remindAt, offsetType = offsetType, offsetValue = offsetValue)
        if (networkMonitor.isCurrentlyOnline()) {
            val result = safeCall { api.createReminder(taskId, request) }
            if (result is ApiResult.Success) {
                val reminderEntity = result.data.toEntity()
                reminderDao.insert(reminderEntity)
                scheduleAlarmForReminder(reminderEntity.id, taskId, reminderEntity.remindAt)
            }
            return result
        }
        val localId = UUID.randomUUID().toString()
        val triggerAtMillis = java.time.Instant.parse(remindAt).toEpochMilli()
        val entity = ReminderEntity(
            id = localId,
            taskId = taskId,
            remindAt = triggerAtMillis,
            offsetType = offsetType,
            offsetValue = offsetValue,
            createdAt = System.currentTimeMillis(),
        )
        reminderDao.insert(entity)
        scheduleAlarmForReminder(localId, taskId, triggerAtMillis)
        trackChange("REMINDER", localId, "CREATE", null, remindAt)
        return ApiResult.Success(entity.toDto())
    }

    private suspend fun scheduleAlarmForReminder(reminderId: String, taskId: String, triggerAtMillis: Long) {
        val task = taskDao.getById(taskId) ?: return
        reminderScheduler.scheduleReminder(
            reminderId = reminderId,
            taskId = taskId,
            contextId = task.contextId,
            taskTitle = task.title,
            triggerAtMillis = triggerAtMillis,
        )
    }

    suspend fun deleteReminder(reminderId: String): ApiResult<Unit> {
        reminderScheduler.cancelReminder(reminderId)
        if (networkMonitor.isCurrentlyOnline()) {
            val result = safeCall { api.deleteReminder(reminderId) }
            if (result is ApiResult.Success) {
                reminderDao.delete(reminderId)
                return result
            }
        }
        reminderDao.delete(reminderId)
        trackChange("REMINDER", reminderId, "DELETE", null, null)
        return ApiResult.Success(Unit)
    }

    // ── Export ──

    suspend fun exportData(contextId: String? = null): ApiResult<String> {
        if (!networkMonitor.isCurrentlyOnline()) {
            return ApiResult.Error("Export requires internet connection")
        }
        return try {
            val response = api.exportData(contextId)
            if (response.isSuccessful) {
                val body = response.body()?.string() ?: return ApiResult.Error("Empty response")
                ApiResult.Success(body)
            } else {
                ApiResult.Error(parseErrorMessage(response.errorBody()?.string()))
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Export failed")
        }
    }

    // ── Context update ──

    suspend fun updateContext(contextId: String, name: String, theme: String, icon: String): ApiResult<ContextDto> {
        val request = CreateContextRequest(name = name, theme = theme, icon = icon)
        if (networkMonitor.isCurrentlyOnline()) {
            val result = safeCall { api.updateContext(contextId, request) }
            if (result is ApiResult.Success) {
                val userId = tokenStorage.getUserId() ?: ""
                if (userId.isNotBlank()) {
                    try {
                        ensureUserExists(userId)
                        contextDao.insert(result.data.toEntity(userId))
                    } catch (_: Exception) { }
                }
            }
            return result
        }
        val entity = contextDao.getById(contextId) ?: return ApiResult.Error("Context not found")
        val now = System.currentTimeMillis()
        val updated = entity.copy(name = name, theme = theme, icon = icon, updatedAt = now)
        contextDao.insert(updated)
        trackChange("CONTEXT", contextId, "UPDATE", entity.theme, theme)
        return ApiResult.Success(updated.toDto())
    }

    private suspend fun loadCategoriesFromRoom(contextId: String): ApiResult<List<CategoryDto>> {
        val entities = categoryDao.getByContextId(contextId)
        return ApiResult.Success(entities.map { it.toDto() })
    }

    private suspend fun loadRemindersFromRoom(taskId: String): ApiResult<List<ReminderDto>> {
        val entities = reminderDao.getByTaskId(taskId)
        return ApiResult.Success(entities.map { it.toDto() })
    }

    // ── Sync helpers ──

    suspend fun getUnsyncedChanges(): List<PendingChangeEntity> =
        pendingChangeDao.getUnsyncedChanges()

    suspend fun markChangesSynced(ids: List<Long>) =
        pendingChangeDao.markSynced(ids)

    suspend fun deleteSyncedChanges() =
        pendingChangeDao.deleteSyncedChanges()

    private suspend fun trackChange(
        entityType: String,
        entityId: String,
        changeType: String,
        oldValue: String?,
        newValue: String?,
        fieldName: String? = null,
    ) {
        pendingChangeDao.insert(
            PendingChangeEntity(
                entityType = entityType,
                entityId = entityId,
                changeType = changeType,
                fieldName = fieldName,
                oldValue = oldValue,
                newValue = newValue,
                createdAt = System.currentTimeMillis(),
            )
        )
    }

    private suspend fun trackFieldChanges(
        taskId: String,
        old: TaskEntity,
        new: TaskEntity,
        timestamp: Long,
    ) {
        val fields = listOf(
            Triple("title", old.title, new.title),
            Triple("notes", old.notes, new.notes),
            Triple("dueDate", old.dueDate, new.dueDate),
            Triple("categoryId", old.categoryId, new.categoryId),
            Triple("gtdList", old.gtdList, new.gtdList),
            Triple("sortOrder", old.sortOrder.toString(), new.sortOrder.toString()),
            Triple("recurrenceRule", old.recurrenceRule, new.recurrenceRule),
        )
        for ((fieldName, oldVal, newVal) in fields) {
            if (oldVal != newVal) {
                pendingChangeDao.insert(
                    PendingChangeEntity(
                        entityType = "TASK",
                        entityId = taskId,
                        changeType = "FIELD_UPDATE",
                        fieldName = fieldName,
                        oldValue = oldVal,
                        newValue = newVal,
                        createdAt = timestamp,
                    )
                )
            }
        }
    }

    private suspend fun cacheTasksLocally(tasks: List<TaskDto>) {
        val entities = tasks.flattenWithSubtasks()
        if (entities.isNotEmpty()) {
            try {
                taskDao.insertAll(entities)
            } catch (e: Exception) {
                Log.e("GtdRepository", "Failed to cache tasks locally", e)
            }
        }
    }

    private suspend fun computeProgress(taskId: String): Int? {
        val subtasks = taskDao.getSubtasks(taskId)
        if (subtasks.isEmpty()) return null
        val completed = subtasks.count { it.isCompleted }
        return (completed * 100) / subtasks.size
    }

    // ── FK safety helpers ──

    private suspend fun ensureUserExists(userId: String) {
        if (userDao.getById(userId) == null) {
            val email = tokenStorage.getUserEmail() ?: ""
            userDao.insert(UserEntity(id = userId, email = email, passwordHash = ""))
        }
    }

    private suspend fun ensureContextExists(contextId: String) {
        if (contextDao.getById(contextId) == null) {
            val userId = tokenStorage.getUserId() ?: ""
            if (userId.isNotBlank()) {
                ensureUserExists(userId)
                contextDao.insert(
                    ContextEntity(
                        id = contextId,
                        userId = userId,
                        name = "",
                        theme = "MINIMALIST",
                        icon = "",
                        sortOrder = 0,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                    )
                )
            }
        }
    }

    // ── Network helpers ──

    private suspend fun <T> safeCall(block: suspend () -> retrofit2.Response<T>): ApiResult<T> {
        return try {
            val response = block()
            if (response.isSuccessful) {
                val body = response.body() ?: return ApiResult.Error("Empty response")
                ApiResult.Success(body)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                ApiResult.Error(errorMsg, response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    private fun parseErrorMessage(errorBody: String?): String {
        if (errorBody.isNullOrBlank()) return "Unknown error"
        return try {
            val obj = json.parseToJsonElement(errorBody).jsonObject
            obj["message"]?.jsonPrimitive?.content ?: "Unknown error"
        } catch (_: Exception) {
            "Unknown error"
        }
    }
}
