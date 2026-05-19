package com.gtd.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SyncPushRequest(
    val changes: List<SyncChangeRequest>,
)

@Serializable
data class SyncChangeRequest(
    @SerialName("entityType") val entityType: String,
    @SerialName("entityId") val entityId: String,
    @SerialName("fieldName") val fieldName: String,
    @SerialName("oldValue") val oldValue: String? = null,
    @SerialName("newValue") val newValue: String? = null,
    @SerialName("deviceSource") val deviceSource: String = "ANDROID",
    @SerialName("clientTimestamp") val clientTimestamp: String,
    @SerialName("expectedVersion") val expectedVersion: Int? = null,
)

@Serializable
data class SyncPushResponse(
    @SerialName("serverTimestamp") val serverTimestamp: String,
    val results: List<SyncChangeResult>,
    @SerialName("appliedCount") val appliedCount: Int,
    @SerialName("conflictCount") val conflictCount: Int,
)

@Serializable
data class SyncChangeResult(
    @SerialName("entityType") val entityType: String,
    @SerialName("entityId") val entityId: String,
    @SerialName("fieldName") val fieldName: String? = null,
    val applied: Boolean = false,
    @SerialName("conflictStatus") val conflictStatus: String? = null,
    @SerialName("serverValue") val serverValue: String? = null,
    @SerialName("newVersion") val newVersion: Long? = null,
    val error: String? = null,
)

@Serializable
data class SyncPullResponse(
    @SerialName("serverTimestamp") val serverTimestamp: String,
    val changes: List<SyncLogEntry>,
    @SerialName("changeCount") val changeCount: Int,
)

@Serializable
data class SyncLogEntry(
    val id: String,
    @SerialName("entityType") val entityType: String,
    @SerialName("entityId") val entityId: String,
    @SerialName("fieldName") val fieldName: String,
    @SerialName("oldValue") val oldValue: String? = null,
    @SerialName("newValue") val newValue: String? = null,
    @SerialName("deviceSource") val deviceSource: String? = null,
    @SerialName("conflictStatus") val conflictStatus: String? = null,
    @SerialName("createdAt") val createdAt: String? = null,
)
