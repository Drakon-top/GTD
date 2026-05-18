package com.gtd.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CategoryDto(
    val id: String,
    @SerialName("contextId") val contextId: String,
    val name: String,
    val icon: String? = null,
    val color: String? = null,
    @SerialName("sortOrder") val sortOrder: Int = 0,
    @SerialName("taskCount") val taskCount: Int = 0,
    @SerialName("isDeleted") val isDeleted: Boolean = false,
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("updatedAt") val updatedAt: String? = null,
)

@Serializable
data class CreateCategoryRequest(
    val name: String,
    val icon: String? = null,
    val color: String? = null,
    @SerialName("sortOrder") val sortOrder: Int? = null,
)
