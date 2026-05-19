package com.gtd.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gtd.android.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<TaskEntity>)

    @Update
    suspend fun update(task: TaskEntity)

    @Query(
        """
        SELECT * FROM tasks 
        WHERE context_id = :contextId AND is_deleted = 0 AND parent_task_id IS NULL
        ORDER BY sort_order
        """
    )
    fun observeByContextId(contextId: String): Flow<List<TaskEntity>>

    @Query(
        """
        SELECT * FROM tasks 
        WHERE context_id = :contextId AND is_deleted = 0 AND parent_task_id IS NULL
        ORDER BY sort_order
        """
    )
    suspend fun getByContextId(contextId: String): List<TaskEntity>

    @Query(
        """
        SELECT * FROM tasks 
        WHERE context_id = :contextId AND gtd_list = :gtdList AND is_deleted = 0 AND parent_task_id IS NULL
        ORDER BY sort_order
        """
    )
    fun observeByGtdList(contextId: String, gtdList: String): Flow<List<TaskEntity>>

    @Query(
        """
        SELECT * FROM tasks 
        WHERE context_id = :contextId AND gtd_list = :gtdList AND is_deleted = 0 AND parent_task_id IS NULL
        ORDER BY sort_order
        """
    )
    suspend fun getByGtdList(contextId: String, gtdList: String): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE parent_task_id = :parentId AND is_deleted = 0 ORDER BY sort_order")
    fun observeSubtasks(parentId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE parent_task_id = :parentId AND is_deleted = 0 ORDER BY sort_order")
    suspend fun getSubtasks(parentId: String): List<TaskEntity>

    @Query("SELECT COUNT(*) FROM tasks WHERE context_id = :contextId AND gtd_list = :gtdList AND is_deleted = 0")
    suspend fun countByGtdList(contextId: String, gtdList: String): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE category_id = :categoryId AND is_deleted = 0")
    suspend fun countByCategory(categoryId: String): Int

    @Query("UPDATE tasks SET is_deleted = 1, updated_at = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, updatedAt: Long)

    @Query("UPDATE tasks SET is_deleted = 1, updated_at = :updatedAt WHERE parent_task_id = :parentId")
    suspend fun softDeleteByParent(parentId: String, updatedAt: Long)

    @Query(
        """
        SELECT * FROM tasks 
        WHERE updated_at > :since AND context_id IN (
            SELECT id FROM contexts WHERE user_id = :userId
        )
        """
    )
    suspend fun getChangedSince(userId: String, since: Long): List<TaskEntity>

    @Query("DELETE FROM tasks")
    suspend fun deleteAll()
}
