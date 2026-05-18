package com.gtd.android.domain.model

import kotlinx.datetime.Instant

data class Reminder(
    val id: String,
    val taskId: String,
    val remindAt: Instant,
    val offsetType: String? = null,
    val offsetValue: Int? = null,
    val isSent: Boolean = false,
    val createdAt: Instant? = null,
)
