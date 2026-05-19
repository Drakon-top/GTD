package com.gtd.android.data.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gtd.android.data.local.dao.PendingChangeDao
import com.gtd.android.data.local.dao.TaskDao
import com.gtd.android.data.local.entity.PendingChangeEntity
import com.gtd.android.data.remote.api.GtdApi
import com.gtd.android.data.remote.dto.CreateContextRequest
import com.gtd.android.data.remote.dto.CreateTaskRequest
import com.gtd.android.data.remote.dto.MoveTaskRequest
import com.gtd.android.data.remote.dto.UpdateTaskRequest
import com.gtd.android.data.toEntity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val api: GtdApi,
    private val pendingChangeDao: PendingChangeDao,
    private val taskDao: TaskDao,
) : CoroutineWorker(appContext, params) {

    companion object {
        const val TAG = "SyncWorker"
        const val WORK_NAME = "gtd_sync"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting sync...")

        val changes = pendingChangeDao.getUnsyncedChanges()
        if (changes.isEmpty()) {
            Log.d(TAG, "No pending changes to sync")
            return Result.success()
        }

        Log.d(TAG, "Syncing ${changes.size} pending changes")
        val syncedIds = mutableListOf<Long>()

        for (change in changes) {
            val success = syncChange(change)
            if (success) {
                syncedIds.add(change.id)
            } else {
                Log.w(TAG, "Failed to sync change ${change.id}: ${change.changeType} ${change.entityType}")
            }
        }

        if (syncedIds.isNotEmpty()) {
            pendingChangeDao.markSynced(syncedIds)
            pendingChangeDao.deleteSyncedChanges()
            Log.d(TAG, "Synced ${syncedIds.size}/${changes.size} changes")
        }

        val remaining = pendingChangeDao.getUnsyncedChanges()
        return if (remaining.isEmpty()) Result.success() else Result.retry()
    }

    private suspend fun syncChange(change: PendingChangeEntity): Boolean {
        return try {
            when (change.entityType) {
                "CONTEXT" -> syncContextChange(change)
                "TASK" -> syncTaskChange(change)
                else -> {
                    Log.w(TAG, "Unknown entity type: ${change.entityType}")
                    true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing change ${change.id}", e)
            false
        }
    }

    private suspend fun syncContextChange(change: PendingChangeEntity): Boolean {
        return when (change.changeType) {
            "CREATE" -> {
                val entity = taskDao.getById(change.entityId)
                if (entity != null) {
                    Log.w(TAG, "Context create sync not fully implemented — requires context DAO read")
                }
                true
            }
            else -> true
        }
    }

    private suspend fun syncTaskChange(change: PendingChangeEntity): Boolean {
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
            "UPDATE" -> {
                if (entity == null) return true
                val response = api.updateTask(
                    change.entityId,
                    UpdateTaskRequest(
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
                val newList = change.newValue ?: return true
                val response = api.moveTask(change.entityId, MoveTaskRequest(newList))
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
            "DELETE" -> {
                val response = api.deleteTask(change.entityId)
                response.isSuccessful || response.code() == 404
            }
            else -> {
                Log.w(TAG, "Unknown change type: ${change.changeType}")
                true
            }
        }
    }
}
