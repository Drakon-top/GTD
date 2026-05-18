package com.gtd.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ContextDto(
    val id: String,
    @SerialName("userId") val userId: String? = null,
    val name: String,
    val theme: String,
    val icon: String,
    @SerialName("sortOrder") val sortOrder: Int = 0,
    @SerialName("isDeleted") val isDeleted: Boolean = false,
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("updatedAt") val updatedAt: String? = null,
    @SerialName("inboxCount") val inboxCount: Int? = null,
)

@Serializable
data class CreateContextRequest(
    val name: String,
    val theme: String,
    val icon: String,
    @SerialName("sortOrder") val sortOrder: Int? = null,
)
