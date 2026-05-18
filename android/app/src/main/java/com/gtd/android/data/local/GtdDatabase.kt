package com.gtd.android.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.gtd.android.data.local.dao.CategoryDao
import com.gtd.android.data.local.dao.ContextDao
import com.gtd.android.data.local.dao.PendingChangeDao
import com.gtd.android.data.local.dao.ReminderDao
import com.gtd.android.data.local.dao.TaskDao
import com.gtd.android.data.local.dao.UserDao
import com.gtd.android.data.local.entity.CategoryEntity
import com.gtd.android.data.local.entity.ContextEntity
import com.gtd.android.data.local.entity.PendingChangeEntity
import com.gtd.android.data.local.entity.ReminderEntity
import com.gtd.android.data.local.entity.TaskEntity
import com.gtd.android.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        ContextEntity::class,
        TaskEntity::class,
        CategoryEntity::class,
        ReminderEntity::class,
        PendingChangeEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class GtdDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun contextDao(): ContextDao
    abstract fun taskDao(): TaskDao
    abstract fun categoryDao(): CategoryDao
    abstract fun reminderDao(): ReminderDao
    abstract fun pendingChangeDao(): PendingChangeDao
}
