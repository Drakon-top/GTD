package com.gtd.android.di

import android.content.Context
import androidx.room.Room
import com.gtd.android.data.local.GtdDatabase
import com.gtd.android.data.local.dao.CategoryDao
import com.gtd.android.data.local.dao.ContextDao
import com.gtd.android.data.local.dao.PendingChangeDao
import com.gtd.android.data.local.dao.ReminderDao
import com.gtd.android.data.local.dao.TaskDao
import com.gtd.android.data.local.dao.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): GtdDatabase =
        Room.databaseBuilder(context, GtdDatabase::class.java, "gtd_database")
            .fallbackToDestructiveMigration()
            .addMigrations(*GtdDatabase.MIGRATIONS)
            .build()

    @Provides
    fun provideUserDao(db: GtdDatabase): UserDao = db.userDao()

    @Provides
    fun provideContextDao(db: GtdDatabase): ContextDao = db.contextDao()

    @Provides
    fun provideTaskDao(db: GtdDatabase): TaskDao = db.taskDao()

    @Provides
    fun provideCategoryDao(db: GtdDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideReminderDao(db: GtdDatabase): ReminderDao = db.reminderDao()

    @Provides
    fun providePendingChangeDao(db: GtdDatabase): PendingChangeDao = db.pendingChangeDao()
}
