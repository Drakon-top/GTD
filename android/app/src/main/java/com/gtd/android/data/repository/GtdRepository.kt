package com.gtd.android.data.repository

import com.gtd.android.data.NetworkMonitor
import com.gtd.android.data.flattenWithSubtasks
import com.gtd.android.data.local.TokenStorage
import com.gtd.android.data.local.dao.CategoryDao
import com.gtd.android.data.local.dao.ContextDao
import com.gtd.android.data.local.dao.PendingChangeDao
import com.gtd.android.data.local.dao.TaskDao
import com.gtd.android.data.local.entity.ContextEntity
import com.gtd.android.data.local.entity.PendingChangeEntity
import com.gtd.android.data.local.entity.TaskEntity
import com.gtd.android.data.remote.api.GtdApi
import com.gtd.android.data.remote.dto.CategoryDto
import com.gtd.android.data.remote.dto.ContextDto
import com.gtd.android.data.remote.dto.CreateContextRequest
import com.gtd.android.data.remote.dto.CreateTaskRequest
import com.gtd.android.data.remote.dto.MoveTaskRequest
import com.gtd.android.data.remote.dto.TaskCountsDto
import com.gtd.android.data.remote.dto.TaskDto
import com.gtd.android.data.remote.dto.UpdateTaskRequest
import com.gtd.android.data.toDto
import com.gtd.android.data.toEntity
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
    private val contextDao: ContextDao,
    private val taskDao: TaskDao,
    private val categoryDao: CategoryDao,
    private val pendingChangeDao: PendingChangeDao,
    private val tokenStorage: TokenStorage,
    private val networkMonitor: NetworkMonitor,
) {

    // ── Contexts ──

    suspend fun getContexts(): ApiResult<List<ContextDto>> {
        if (networkMonitor.isCurrentlyOnline()) {
            val result = safeCall { api.getContexts() }
            if (result is ApiResult.Success) {
                val userId = tokenStorage.getUserId() ?: ""
                contextDao.insertAll(result.data.map { it.toEntity(userId) })
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
                contextDao.insert(result.data.toEntity(userId))
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
        contextDao.insert(entity)
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
                taskDao.insert(result.data.toEntity())
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
        taskDao.insert(entity)
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
                taskDao.insert(result.data.toEntity())
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
        trackChange("TASK", taskId, "UPDATE", entity.title, updated.title)
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
                taskDao.insert(result.data.toEntity())
            }
            return result
        }
        val entity = taskDao.getById(taskId) ?: return ApiResult.Error("Task not found")
        val now = System.currentTimeMillis()
        val updated = entity.copy(gtdList = gtdList, updatedAt = now, version = entity.version + 1)
        taskDao.update(updated)
        trackChange("TASK", taskId, "MOVE", entity.gtdList, gtdList)
        return ApiResult.Success(updated.toDto())
    }

    suspend fun completeTask(taskId: String): ApiResult<TaskDto> {
        val now = System.currentTimeMillis()
        if (networkMonitor.isCurrentlyOnline()) {
            val result = safeCall { api.completeTask(taskId) }
            if (result is ApiResult.Success) {
                taskDao.insert(result.data.toEntity())
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

    // ── Subtasks ──

    suspend fun createSubtask(parentId: String, title: String): ApiResult<TaskDto> {
        if (networkMonitor.isCurrentlyOnline()) {
            val result = safeCall { api.createSubtask(parentId, CreateTaskRequest(title = title)) }
            if (result is ApiResult.Success) {
                taskDao.insert(result.data.toEntity())
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
                result.data.forEach { taskDao.insert(it.toEntity()) }
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

    private suspend fun loadCategoriesFromRoom(contextId: String): ApiResult<List<CategoryDto>> {
        val entities = categoryDao.getByContextId(contextId)
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
    ) {
        pendingChangeDao.insert(
            PendingChangeEntity(
                entityType = entityType,
                entityId = entityId,
                changeType = changeType,
                oldValue = oldValue,
                newValue = newValue,
                createdAt = System.currentTimeMillis(),
            )
        )
    }

    private suspend fun cacheTasksLocally(tasks: List<TaskDto>) {
        val entities = tasks.flattenWithSubtasks()
        if (entities.isNotEmpty()) {
            taskDao.insertAll(entities)
        }
    }

    private suspend fun computeProgress(taskId: String): Int? {
        val subtasks = taskDao.getSubtasks(taskId)
        if (subtasks.isEmpty()) return null
        val completed = subtasks.count { it.isCompleted }
        return (completed * 100) / subtasks.size
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
