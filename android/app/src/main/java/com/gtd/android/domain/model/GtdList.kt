package com.gtd.android.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class GtdList {
    @SerialName("INBOX") INBOX,
    @SerialName("NEXT_ACTIONS") NEXT_ACTIONS,
    @SerialName("PROJECTS") PROJECTS,
    @SerialName("WAITING_FOR") WAITING_FOR,
    @SerialName("SOMEDAY_MAYBE") SOMEDAY_MAYBE,
    @SerialName("REFERENCE") REFERENCE,
    @SerialName("CALENDAR") CALENDAR,
    @SerialName("DONE") DONE,
}
