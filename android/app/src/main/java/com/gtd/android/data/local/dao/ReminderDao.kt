package com.gtd.android.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.gtd.android.data.local.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Upsert
    suspend fun insert(reminder: ReminderEntity)

    @Upsert
    suspend fun insertAll(reminders: List<ReminderEntity>)

    @Update
    suspend fun update(reminder: ReminderEntity)

    @Query("SELECT * FROM reminders WHERE task_id = :taskId ORDER BY remind_at")
    fun observeByTaskId(taskId: String): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE task_id = :taskId ORDER BY remind_at")
    suspend fun getByTaskId(taskId: String): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getById(id: String): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE remind_at <= :now AND is_sent = 0")
    suspend fun getDueReminders(now: Long): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE is_sent = 0 AND remind_at > :after")
    suspend fun getUnsent(after: Long = 0): List<ReminderEntity>

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM reminders")
    suspend fun deleteAll()
}
