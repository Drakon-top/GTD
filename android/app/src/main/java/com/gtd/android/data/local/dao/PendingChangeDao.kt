package com.gtd.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.gtd.android.data.local.entity.PendingChangeEntity

@Dao
interface PendingChangeDao {
    @Insert
    suspend fun insert(change: PendingChangeEntity)

    @Query("SELECT * FROM pending_changes WHERE is_synced = 0 ORDER BY created_at")
    suspend fun getUnsyncedChanges(): List<PendingChangeEntity>

    @Query("UPDATE pending_changes SET is_synced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>)

    @Query("DELETE FROM pending_changes WHERE is_synced = 1")
    suspend fun deleteSyncedChanges()

    @Query("DELETE FROM pending_changes")
    suspend fun deleteAll()
}
