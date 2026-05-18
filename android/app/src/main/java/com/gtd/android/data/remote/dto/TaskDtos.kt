package com.gtd.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TaskDto(
    val id: String,
    @SerialName("contextId") val contextId: String,
    @SerialName("parentTaskId") val parentTaskId: String? = null,
    @SerialName("gtdList") val gtdList: String = "INBOX",
    @SerialName("categoryId") val categoryId: String? = null,
    val title: String,
    val notes: String? = null,
    @SerialName("dueDate") val dueDate: String? = null,
    @SerialName("reminderSettings") val reminderSettings: String? = null,
    @SerialName("recurrenceRule") val recurrenceRule: String? = null,
    @SerialName("nestingLevel") val nestingLevel: Int = 1,
    @SerialName("sortOrder") val sortOrder: Int = 0,
    @SerialName("isCompleted") val isCompleted: Boolean = false,
    @SerialName("completedAt") val completedAt: String? = null,
    @SerialName("isDeleted") val isDeleted: Boolean = false,
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("updatedAt") val updatedAt: String? = null,
    val version: Long = 0,
    val subtasks: List<TaskDto>? = null,
    val progress: Int? = null,
)

@Serializable
data class CreateTaskRequest(
    val title: String,
    val notes: String? = null,
    @SerialName("dueDate") val dueDate: String? = null,
    @SerialName("categoryId") val categoryId: String? = null,
    @SerialName("gtdList") val gtdList: String? = null,
)

@Serializable
data class UpdateTaskRequest(
    val title: String? = null,
    val notes: String? = null,
    @SerialName("dueDate") val dueDate: String? = null,
    @SerialName("categoryId") val categoryId: String? = null,
    @SerialName("gtdList") val gtdList: String? = null,
    @SerialName("sortOrder") val sortOrder: Int? = null,
    @SerialName("recurrenceRule") val recurrenceRule: String? = null,
)

@Serializable
data class MoveTaskRequest(
    @SerialName("gtdList") val gtdList: String,
)
