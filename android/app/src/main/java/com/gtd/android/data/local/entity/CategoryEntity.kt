package com.gtd.android.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    foreignKeys = [
        ForeignKey(
            entity = ContextEntity::class,
            parentColumns = ["id"],
            childColumns = ["context_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("context_id")],
)
data class CategoryEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "context_id") val contextId: String,
    val name: String,
    val icon: String? = null,
    val color: String? = null,
    @ColumnInfo(name = "sort_order") val sortOrder: Int = 0,
    @ColumnInfo(name = "is_deleted") val isDeleted: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long? = null,
    @ColumnInfo(name = "updated_at") val updatedAt: Long? = null,
)
