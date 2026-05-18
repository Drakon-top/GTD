package com.gtd.android.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["task_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("task_id")],
)
data class ReminderEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "task_id") val taskId: String,
    @ColumnInfo(name = "remind_at") val remindAt: Long,
    @ColumnInfo(name = "offset_type") val offsetType: String? = null,
    @ColumnInfo(name = "offset_value") val offsetValue: Int? = null,
    @ColumnInfo(name = "is_sent") val isSent: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long? = null,
)
