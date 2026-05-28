package com.gtd.android.data.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gtd.android.data.local.TokenStorage
import com.gtd.android.data.local.dao.ContextDao
import com.gtd.android.data.local.dao.PendingChangeDao
import com.gtd.android.data.local.dao.TaskDao
import com.gtd.android.data.local.entity.PendingChangeEntity
import com.gtd.android.data.remote.api.GtdApi
import com.gtd.android.data.remote.dto.CreateContextRequest
import com.gtd.android.data.remote.dto.CreateTaskRequest
import com.gtd.android.data.remote.dto.SyncChangeRequest
import com.gtd.android.data.remote.dto.SyncPushRequest
import com.gtd.android.data.toEntity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Instant

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val api: GtdApi,
    private val pendingChangeDao: PendingChangeDao,
    private val taskDao: TaskDao,
    private val contextDao: ContextDao,
    private val tokenStorage: TokenStorage,
) : CoroutineWorker(appContext, params) {

    companion object {
        const val TAG = "SyncWorker"
        const val WORK_NAME = "gtd_sync"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting sync...")

        val changes = pendingChangeDao.getUnsyncedChanges()
        if (changes.isEmpty()) {
            Log.d(TAG, "No pending changes, pulling server updates...")
            pullServerChanges()
            return Result.success()
        }

        Log.d(TAG, "Processing ${changes.size} pending changes")

        val (fieldUpdates, crudChanges) = changes.partition { it.changeType == "FIELD_UPDATE" }
        val syncedIds = mutableListOf<Long>()

        // Phase 1: Handle CRUD operations that can't use field-level sync
        for (change in crudChanges) {
            val success = syncCrudChange(change)
            if (success) {
                syncedIds.add(change.id)
            } else {
                Log.w(TAG, "Failed CRUD sync: ${change.id} ${change.changeType} ${change.entityType}")
            }
        }

        // Phase 2: Push field-level updates via /sync/push
        if (fieldUpdates.isNotEmpty()) {
            val pushed = pushFieldChanges(fieldUpdates)
            syncedIds.addAll(pushed)
        }

        // Phase 3: Mark synced and clean up
        if (syncedIds.isNotEmpty()) {
            pendingChangeDao.markSynced(syncedIds)
            pendingChangeDao.deleteSyncedChanges()
            Log.d(TAG, "Synced ${syncedIds.size}/${changes.size} changes")
        }

        // Phase 4: Pull server changes to reconcile
        pullServerChanges()

        val remaining = pendingChangeDao.getUnsyncedChanges()
        return if (remaining.isEmpty()) Result.success() else Result.retry()
    }

    private suspend fun pushFieldChanges(changes: List<PendingChangeEntity>): List<Long> {
        val syncedIds = mutableListOf<Long>()

        val syncRequests = changes.mapNotNull { change ->
            if (change.fieldName == null) return@mapNotNull null
            SyncChangeRequest(
                entityType = change.entityType,
                entityId = change.entityId,
                fieldName = change.fieldName,
                oldValue = change.oldValue,
                newValue = change.newValue,
                deviceSource = "ANDROID",
                clientTimestamp = Instant.ofEpochMilli(change.createdAt).toString(),
            )
        }

        if (syncRequests.isEmpty()) return syncedIds

        try {
            val response = api.syncPush(SyncPushRequest(changes = syncRequests))
            if (response.isSuccessful) {
                val pushResponse = response.body() ?: return syncedIds
                Log.d(TAG, "Push result: ${pushResponse.appliedCount} applied, ${pushResponse.conflictCount} conflicts")

                for ((index, result) in pushResponse.results.withIndex()) {
                    val originalChange = changes.getOrNull(index) ?: continue

                    if (result.applied) {
                        syncedIds.add(originalChange.id)
                        // Update local version if server provides it
                        result.newVersion?.let { newVersion ->
                            val task = taskDao.getById(result.entityId)
                            if (task != null && task.version < newVersion) {
                                taskDao.insert(task.copy(version = newVersion))
                            }
                        }
                    } else if (result.conflictStatus == "RESOLVED_NOTIFY") {
                        // Server won the conflict — apply server value locally
                        syncedIds.add(originalChange.id)
                        applyServerValue(result.entityId, originalChange.fieldName, result.serverValue)
                        Log.i(TAG, "Conflict resolved (server wins): ${result.entityId}.${originalChange.fieldName}")
                    } else if (result.error != null) {
                        Log.w(TAG, "Push error for ${result.entityId}: ${result.error}")
                        syncedIds.add(originalChange.id)
                    } else {
                        // No-op (client hadn't actually changed) — still mark as synced
                        syncedIds.add(originalChange.id)
                    }
                }

                // Store server timestamp for future pulls
                tokenStorage.saveLastSyncTimestamp(pushResponse.serverTimestamp)
            } else {
                Log.w(TAG, "Push failed: HTTP ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pushing field changes", e)
        }

        return syncedIds
    }

    private suspend fun pullServerChanges() {
        val since = tokenStorage.getLastSyncTimestamp() ?: Instant.EPOCH.toString()

        try {
            val response = api.syncPull(since)
            if (response.isSuccessful) {
                val pullResponse = response.body() ?: return
                Log.d(TAG, "Pulled ${pullResponse.changeCount} server changes")

                for (entry in pullResponse.changes) {
                    // Skip changes made by this device to avoid echo
                    if (entry.deviceSource == "ANDROID") continue

                    if (entry.entityType == "TASK") {
                        applyServerValue(entry.entityId, entry.fieldName, entry.newValue)
                    }
                }

                tokenStorage.saveLastSyncTimestamp(pullResponse.serverTimestamp)
            } else {
                Log.w(TAG, "Pull failed: HTTP ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pulling server changes", e)
        }
    }

    private suspend fun applyServerValue(entityId: String, fieldName: String?, newValue: String?) {
        if (fieldName == null) return
        val task = taskDao.getById(entityId) ?: return

        val updated = when (fieldName) {
            "title" -> task.copy(title = newValue ?: task.title)
            "notes" -> task.copy(notes = newValue)
            "gtdList" -> task.copy(gtdList = newValue ?: task.gtdList)
            "dueDate" -> task.copy(dueDate = newValue)
            "categoryId" -> task.copy(categoryId = newValue)
            "sortOrder" -> task.copy(sortOrder = newValue?.toIntOrNull() ?: task.sortOrder)
            "recurrenceRule" -> task.copy(recurrenceRule = newValue)
            else -> return
        }

        taskDao.insert(updated.copy(updatedAt = System.currentTimeMillis()))
    }

    private suspend fun syncCrudChange(change: PendingChangeEntity): Boolean {
        return try {
            when (change.entityType) {
                "CONTEXT" -> syncContextChange(change)
                "TASK" -> syncTaskCrudChange(change)
                else -> true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing CRUD change ${change.id}", e)
            false
        }
    }

    private suspend fun syncContextChange(change: PendingChangeEntity): Boolean {
        val entity = contextDao.getById(change.entityId) ?: return true

        return when (change.changeType) {
            "UPDATE" -> {
                val request = CreateContextRequest(
                    name = entity.name,
                    theme = entity.theme,
                    icon = entity.icon,
                    sortOrder = entity.sortOrder,
                )
                val response = api.updateContext(change.entityId, request)
                if (response.isSuccessful) {
                    val userId = tokenStorage.getUserId() ?: ""
                    response.body()?.let { dto ->
                        contextDao.insert(dto.toEntity(userId))
                    }
                    true
                } else {
                    Log.w(TAG, "Context UPDATE sync failed: HTTP ${response.code()}")
                    false
                }
            }
            else -> {
                Log.w(TAG, "Context sync not implemented for: ${change.changeType}")
                true
            }
        }
    }

    private suspend fun syncTaskCrudChange(change: PendingChangeEntity): Boolean {
        val entity = taskDao.getById(change.entityId)

        return when (change.changeType) {
            "CREATE" -> {
                if (entity == null) return true
                val response = api.createTask(
                    entity.contextId,
                    CreateTaskRequest(title = entity.title),
                )
                if (response.isSuccessful) {
                    val serverTask = response.body() ?: return false
                    taskDao.insert(serverTask.toEntity())
                    true
                } else false
            }
            "CREATE_SUBTASK" -> {
                if (entity == null) return true
                val parentId = change.oldValue ?: return true
                val response = api.createSubtask(
                    parentId,
                    CreateTaskRequest(title = entity.title),
                )
                if (response.isSuccessful) {
                    val serverTask = response.body() ?: return false
                    taskDao.insert(serverTask.toEntity())
                    true
                } else false
            }
            "COMPLETE" -> {
                val response = api.completeTask(change.entityId)
                if (response.isSuccessful) {
                    val serverTask = response.body() ?: return false
                    taskDao.insert(serverTask.toEntity())
                    true
                } else false
            }
            "REOPEN" -> {
                val response = api.reopenTask(change.entityId)
                if (response.isSuccessful) {
                    val serverTask = response.body() ?: return false
                    taskDao.insert(serverTask.toEntity())
                    true
                } else false
            }
            "DELETE" -> {
                val response = api.deleteTask(change.entityId)
                response.isSuccessful || response.code() == 404
            }
            "UPDATE" -> {
                // Legacy UPDATE entries (pre-field-level) — push full entity
                if (entity == null) return true
                val response = api.updateTask(
                    change.entityId,
                    com.gtd.android.data.remote.dto.UpdateTaskRequest(
                        title = entity.title,
                        notes = entity.notes,
                        dueDate = entity.dueDate,
                        categoryId = entity.categoryId,
                        gtdList = entity.gtdList,
                        sortOrder = entity.sortOrder,
                        recurrenceRule = entity.recurrenceRule,
                    ),
                )
                if (response.isSuccessful) {
                    val serverTask = response.body() ?: return false
                    taskDao.insert(serverTask.toEntity())
                    true
                } else false
            }
            "MOVE" -> {
                // Legacy MOVE entries — push via field sync
                val newList = change.newValue ?: return true
                val syncRequest = SyncPushRequest(
                    changes = listOf(
                        SyncChangeRequest(
                            entityType = "TASK",
                            entityId = change.entityId,
                            fieldName = "gtdList",
                            oldValue = change.oldValue,
                            newValue = newList,
                            deviceSource = "ANDROID",
                            clientTimestamp = Instant.ofEpochMilli(change.createdAt).toString(),
                        )
                    )
                )
                val response = api.syncPush(syncRequest)
                response.isSuccessful
            }
            else -> {
                Log.w(TAG, "Unknown CRUD change type: ${change.changeType}")
                true
            }
        }
    }
}
