package com.gtd.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TaskCountsDto(
    @SerialName("contextId") val contextId: String,
    @SerialName("byGtdList") val byGtdList: Map<String, Int> = emptyMap(),
    @SerialName("byCategory") val byCategory: Map<String, Int> = emptyMap(),
    val total: Int = 0,
)
