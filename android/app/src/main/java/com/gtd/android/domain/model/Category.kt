package com.gtd.android.domain.model

import kotlinx.datetime.Instant

data class Category(
    val id: String,
    val contextId: String,
    val name: String,
    val icon: String? = null,
    val color: String? = null,
    val sortOrder: Int = 0,
    val taskCount: Int = 0,
    val isDeleted: Boolean = false,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
)
