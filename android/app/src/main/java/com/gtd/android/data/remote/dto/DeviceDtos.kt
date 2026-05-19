package com.gtd.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RegisterDeviceTokenRequest(
    val token: String,
    @SerialName("deviceType") val deviceType: String,
    @SerialName("deviceName") val deviceName: String? = null,
)

@Serializable
data class DeviceTokenResponse(
    val id: String,
    val token: String,
    @SerialName("deviceType") val deviceType: String,
    @SerialName("deviceName") val deviceName: String? = null,
    @SerialName("createdAt") val createdAt: String? = null,
)
