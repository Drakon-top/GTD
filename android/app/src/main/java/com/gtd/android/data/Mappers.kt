package com.gtd.android.data

import com.gtd.android.data.local.entity.CategoryEntity
import com.gtd.android.data.local.entity.ContextEntity
import com.gtd.android.data.local.entity.TaskEntity
import com.gtd.android.data.remote.dto.CategoryDto
import com.gtd.android.data.remote.dto.ContextDto
import com.gtd.android.data.remote.dto.TaskDto
import java.time.Instant

fun ContextDto.toEntity(userId: String): ContextEntity = ContextEntity(
    id = id,
    userId = this.userId ?: userId,
    name = name,
    theme = theme,
    icon = icon,
    sortOrder = sortOrder,
    isDeleted = isDeleted,
    createdAt = createdAt?.toEpochMillis(),
    updatedAt = updatedAt?.toEpochMillis(),
)

fun ContextEntity.toDto(): ContextDto = ContextDto(
    id = id,
    userId = userId,
    name = name,
    theme = theme,
    icon = icon,
    sortOrder = sortOrder,
    isDeleted = isDeleted,
)

fun TaskDto.toEntity(): TaskEntity = TaskEntity(
    id = id,
    contextId = contextId,
    parentTaskId = parentTaskId,
    gtdList = gtdList,
    categoryId = categoryId,
    title = title,
    notes = notes,
    dueDate = dueDate,
    reminderSettings = reminderSettings,
    recurrenceRule = recurrenceRule,
    nestingLevel = nestingLevel,
    sortOrder = sortOrder,
    isCompleted = isCompleted,
    completedAt = completedAt?.toEpochMillis(),
    isDeleted = isDeleted,
    createdAt = createdAt?.toEpochMillis(),
    updatedAt = updatedAt?.toEpochMillis(),
    version = version,
)

fun TaskEntity.toDto(subtasks: List<TaskDto>? = null, progress: Int? = null): TaskDto = TaskDto(
    id = id,
    contextId = contextId,
    parentTaskId = parentTaskId,
    gtdList = gtdList,
    categoryId = categoryId,
    title = title,
    notes = notes,
    dueDate = dueDate,
    reminderSettings = reminderSettings,
    recurrenceRule = recurrenceRule,
    nestingLevel = nestingLevel,
    sortOrder = sortOrder,
    isCompleted = isCompleted,
    completedAt = completedAt?.let { Instant.ofEpochMilli(it).toString() },
    isDeleted = isDeleted,
    createdAt = createdAt?.let { Instant.ofEpochMilli(it).toString() },
    updatedAt = updatedAt?.let { Instant.ofEpochMilli(it).toString() },
    version = version,
    subtasks = subtasks,
    progress = progress,
)

fun CategoryDto.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    contextId = contextId,
    name = name,
    icon = icon,
    color = color,
    sortOrder = sortOrder,
    isDeleted = isDeleted,
    createdAt = createdAt?.toEpochMillis(),
    updatedAt = updatedAt?.toEpochMillis(),
)

fun CategoryEntity.toDto(): CategoryDto = CategoryDto(
    id = id,
    contextId = contextId,
    name = name,
    icon = icon,
    color = color,
    sortOrder = sortOrder,
    isDeleted = isDeleted,
)

private fun String.toEpochMillis(): Long? = try {
    Instant.parse(this).toEpochMilli()
} catch (_: Exception) {
    null
}

fun List<TaskDto>.flattenWithSubtasks(): List<TaskEntity> {
    val result = mutableListOf<TaskEntity>()
    fun flatten(tasks: List<TaskDto>) {
        for (task in tasks) {
            result.add(task.toEntity())
            task.subtasks?.let { flatten(it) }
        }
    }
    flatten(this)
    return result
}
