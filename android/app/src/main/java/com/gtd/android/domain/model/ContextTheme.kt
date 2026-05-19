package com.gtd.android.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ContextTheme {
    @SerialName("MINIMALIST") MINIMALIST,
    @SerialName("DESIGN") DESIGN,
    @SerialName("FORMAL") FORMAL,
    @SerialName("NATURE") NATURE,
    @SerialName("DARK") DARK,
    @SerialName("DRAGONS") DRAGONS,
    @SerialName("ICE_DRAGONS") ICE_DRAGONS,
}
