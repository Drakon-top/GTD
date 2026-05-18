package com.gtd.android.domain.model

import kotlinx.datetime.Instant

data class Context(
    val id: String,
    val userId: String,
    val name: String,
    val theme: ContextTheme,
    val icon: String,
    val sortOrder: Int,
    val isDeleted: Boolean = false,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
    val inboxCount: Int = 0,
)
