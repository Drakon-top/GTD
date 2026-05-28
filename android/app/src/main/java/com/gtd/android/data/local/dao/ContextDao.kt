package com.gtd.android.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.gtd.android.data.local.entity.ContextEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContextDao {
    @Upsert
    suspend fun insert(context: ContextEntity)

    @Upsert
    suspend fun insertAll(contexts: List<ContextEntity>)

    @Update
    suspend fun update(context: ContextEntity)

    @Query("SELECT * FROM contexts WHERE user_id = :userId AND is_deleted = 0 ORDER BY sort_order")
    fun observeByUserId(userId: String): Flow<List<ContextEntity>>

    @Query("SELECT * FROM contexts WHERE user_id = :userId AND is_deleted = 0 ORDER BY sort_order")
    suspend fun getByUserId(userId: String): List<ContextEntity>

    @Query("SELECT * FROM contexts WHERE id = :id")
    suspend fun getById(id: String): ContextEntity?

    @Query("SELECT COUNT(*) FROM contexts WHERE user_id = :userId AND is_deleted = 0")
    suspend fun countByUserId(userId: String): Int

    @Query("UPDATE contexts SET is_deleted = 1, updated_at = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, updatedAt: Long)

    @Query("DELETE FROM contexts")
    suspend fun deleteAll()
}
