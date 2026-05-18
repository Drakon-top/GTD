package com.gtd.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReminderDto(
    val id: String,
    @SerialName("taskId") val taskId: String,
    @SerialName("remindAt") val remindAt: String,
    @SerialName("offsetType") val offsetType: String? = null,
    @SerialName("offsetValue") val offsetValue: Int? = null,
    @SerialName("isSent") val isSent: Boolean = false,
    @SerialName("createdAt") val createdAt: String? = null,
)

@Serializable
data class CreateReminderRequest(
    @SerialName("remindAt") val remindAt: String,
    @SerialName("offsetType") val offsetType: String? = null,
    @SerialName("offsetValue") val offsetValue: Int? = null,
)
