package com.gtd.android

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gtd.android.data.local.GtdDatabase
import com.gtd.android.data.local.dao.ContextDao
import com.gtd.android.data.local.dao.UserDao
import com.gtd.android.data.local.entity.ContextEntity
import com.gtd.android.data.local.entity.UserEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GtdDatabaseTest {

    private lateinit var db: GtdDatabase
    private lateinit var userDao: UserDao
    private lateinit var contextDao: ContextDao

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, GtdDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        userDao = db.userDao()
        contextDao = db.contextDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun shouldInsertAndRetrieveUser() = runTest {
        val user = UserEntity(id = "u1", email = "test@test.com", passwordHash = "hash")
        userDao.insert(user)

        val result = userDao.getById("u1")
        assertEquals("test@test.com", result?.email)
    }

    @Test
    fun shouldInsertAndObserveContexts() = runTest {
        val user = UserEntity(id = "u1", email = "test@test.com", passwordHash = "hash")
        userDao.insert(user)

        val ctx = ContextEntity(
            id = "c1",
            userId = "u1",
            name = "Work",
            theme = "FORMAL",
            icon = "briefcase",
            sortOrder = 0,
        )
        contextDao.insert(ctx)

        val contexts = contextDao.observeByUserId("u1").first()
        assertEquals(1, contexts.size)
        assertEquals("Work", contexts[0].name)
    }
}
