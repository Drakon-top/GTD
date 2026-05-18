package com.gtd.android.domain.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

data class Task(
    val id: String,
    val contextId: String,
    val parentTaskId: String? = null,
    val gtdList: GtdList = GtdList.INBOX,
    val categoryId: String? = null,
    val title: String,
    val notes: String? = null,
    val dueDate: LocalDate? = null,
    val nestingLevel: Int = 1,
    val sortOrder: Int = 0,
    val isCompleted: Boolean = false,
    val completedAt: Instant? = null,
    val isDeleted: Boolean = false,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
    val version: Long = 0,
    val subtasks: List<Task> = emptyList(),
    val progress: Int? = null,
)
