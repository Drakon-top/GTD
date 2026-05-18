package com.gtd.android.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_changes")
data class PendingChangeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "entity_type") val entityType: String,
    @ColumnInfo(name = "entity_id") val entityId: String,
    @ColumnInfo(name = "field_name") val fieldName: String? = null,
    @ColumnInfo(name = "old_value") val oldValue: String? = null,
    @ColumnInfo(name = "new_value") val newValue: String? = null,
    @ColumnInfo(name = "change_type") val changeType: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "is_synced") val isSynced: Boolean = false,
)
