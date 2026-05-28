package com.gtd.android.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.gtd.android.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Upsert
    suspend fun insert(category: CategoryEntity)

    @Upsert
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Update
    suspend fun update(category: CategoryEntity)

    @Query("SELECT * FROM categories WHERE context_id = :contextId AND is_deleted = 0 ORDER BY sort_order")
    fun observeByContextId(contextId: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE context_id = :contextId AND is_deleted = 0 ORDER BY sort_order")
    suspend fun getByContextId(contextId: String): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: String): CategoryEntity?

    @Query("UPDATE categories SET is_deleted = 1, updated_at = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, updatedAt: Long)

    @Query("DELETE FROM categories")
    suspend fun deleteAll()
}
