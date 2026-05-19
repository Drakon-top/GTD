package com.gtd.android

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gtd.android.data.local.GtdDatabase
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GtdDatabaseTest {

    private lateinit var db: GtdDatabase
    private lateinit var userDao: UserDao
    private lateinit var contextDao: ContextDao
    private lateinit var taskDao: TaskDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var reminderDao: ReminderDao
    private lateinit var pendingChangeDao: PendingChangeDao

    private val testUser = UserEntity(id = "u1", email = "test@test.com", passwordHash = "hash")
    private val testContext = ContextEntity(
        id = "c1", userId = "u1", name = "Work", theme = "FORMAL",
        icon = "briefcase", sortOrder = 0, createdAt = 1000L, updatedAt = 1000L,
    )

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, GtdDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        userDao = db.userDao()
        contextDao = db.contextDao()
        taskDao = db.taskDao()
        categoryDao = db.categoryDao()
        reminderDao = db.reminderDao()
        pendingChangeDao = db.pendingChangeDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ---- UserDao ----

    @Test
    fun shouldInsertAndRetrieveUser() = runTest {
        userDao.insert(testUser)
        val result = userDao.getById("u1")
        assertEquals("test@test.com", result?.email)
    }

    @Test
    fun shouldFindUserByEmail() = runTest {
        userDao.insert(testUser)
        val result = userDao.getByEmail("test@test.com")
        assertEquals("u1", result?.id)
    }

    @Test
    fun shouldObserveCurrentUser() = runTest {
        userDao.insert(testUser)
        val user = userDao.observeCurrentUser().first()
        assertNotNull(user)
        assertEquals("u1", user?.id)
    }

    @Test
    fun shouldReturnNullForMissingUser() = runTest {
        assertNull(userDao.getById("nonexistent"))
    }

    // ---- ContextDao ----

    @Test
    fun shouldInsertAndObserveContexts() = runTest {
        userDao.insert(testUser)
        contextDao.insert(testContext)
        val contexts = contextDao.observeByUserId("u1").first()
        assertEquals(1, contexts.size)
        assertEquals("Work", contexts[0].name)
    }

    @Test
    fun shouldSoftDeleteContext() = runTest {
        userDao.insert(testUser)
        contextDao.insert(testContext)
        contextDao.softDelete("c1", System.currentTimeMillis())
        val contexts = contextDao.observeByUserId("u1").first()
        assertEquals(0, contexts.size)
    }

    @Test
    fun shouldCountActiveContexts() = runTest {
        userDao.insert(testUser)
        contextDao.insert(testContext)
        contextDao.insert(testContext.copy(id = "c2", name = "Personal", sortOrder = 1))
        assertEquals(2, contextDao.countByUserId("u1"))
    }

    @Test
    fun shouldUpdateContext() = runTest {
        userDao.insert(testUser)
        contextDao.insert(testContext)
        contextDao.update(testContext.copy(name = "Office"))
        val result = contextDao.getById("c1")
        assertEquals("Office", result?.name)
    }

    // ---- TaskDao ----

    @Test
    fun shouldInsertAndObserveTasks() = runTest {
        seedContextWithUser()
        val task = createTask("t1", "Buy groceries")
        taskDao.insert(task)
        val tasks = taskDao.observeByContextId("c1").first()
        assertEquals(1, tasks.size)
        assertEquals("Buy groceries", tasks[0].title)
    }

    @Test
    fun shouldFilterTasksByGtdList() = runTest {
        seedContextWithUser()
        taskDao.insert(createTask("t1", "Task A", gtdList = "INBOX"))
        taskDao.insert(createTask("t2", "Task B", gtdList = "NEXT_ACTIONS"))
        val inbox = taskDao.observeByGtdList("c1", "INBOX").first()
        assertEquals(1, inbox.size)
        assertEquals("Task A", inbox[0].title)
    }

    @Test
    fun shouldTrackSubtasks() = runTest {
        seedContextWithUser()
        taskDao.insert(createTask("t1", "Parent"))
        taskDao.insert(createTask("t2", "Child 1", parentId = "t1", nestingLevel = 2))
        taskDao.insert(createTask("t3", "Child 2", parentId = "t1", nestingLevel = 2))
        val subtasks = taskDao.getSubtasks("t1")
        assertEquals(2, subtasks.size)
    }

    @Test
    fun shouldSoftDeleteTaskAndChildren() = runTest {
        seedContextWithUser()
        taskDao.insert(createTask("t1", "Parent"))
        taskDao.insert(createTask("t2", "Child", parentId = "t1", nestingLevel = 2))
        val now = System.currentTimeMillis()
        taskDao.softDelete("t1", now)
        taskDao.softDeleteByParent("t1", now)

        assertNull(taskDao.observeByContextId("c1").first().find { it.id == "t1" })
        val children = taskDao.observeSubtasks("t1").first()
        assertEquals(0, children.size)
    }

    @Test
    fun shouldCountTasksByGtdList() = runTest {
        seedContextWithUser()
        taskDao.insert(createTask("t1", "A"))
        taskDao.insert(createTask("t2", "B"))
        taskDao.insert(createTask("t3", "C", gtdList = "NEXT_ACTIONS"))
        assertEquals(2, taskDao.countByGtdList("c1", "INBOX"))
        assertEquals(1, taskDao.countByGtdList("c1", "NEXT_ACTIONS"))
    }

    @Test
    fun shouldCountTasksByCategory() = runTest {
        seedContextWithUser()
        categoryDao.insert(createCategory("cat1", "Books"))
        taskDao.insert(createTask("t1", "A", categoryId = "cat1"))
        taskDao.insert(createTask("t2", "B", categoryId = "cat1"))
        assertEquals(2, taskDao.countByCategory("cat1"))
    }

    @Test
    fun shouldExcludeSubtasksFromTopLevelObserve() = runTest {
        seedContextWithUser()
        taskDao.insert(createTask("t1", "Parent"))
        taskDao.insert(createTask("t2", "Child", parentId = "t1", nestingLevel = 2))
        val topLevel = taskDao.observeByContextId("c1").first()
        assertEquals(1, topLevel.size)
        assertEquals("Parent", topLevel[0].title)
    }

    @Test
    fun shouldGetChangedSince() = runTest {
        seedContextWithUser()
        taskDao.insert(createTask("t1", "Old", updatedAt = 500L))
        taskDao.insert(createTask("t2", "New", updatedAt = 2000L))
        val changed = taskDao.getChangedSince("u1", 1000L)
        assertEquals(1, changed.size)
        assertEquals("t2", changed[0].id)
    }

    // ---- CategoryDao ----

    @Test
    fun shouldInsertAndObserveCategories() = runTest {
        seedContextWithUser()
        categoryDao.insert(createCategory("cat1", "Books"))
        val categories = categoryDao.observeByContextId("c1").first()
        assertEquals(1, categories.size)
        assertEquals("Books", categories[0].name)
    }

    @Test
    fun shouldSoftDeleteCategory() = runTest {
        seedContextWithUser()
        categoryDao.insert(createCategory("cat1", "Books"))
        categoryDao.softDelete("cat1", System.currentTimeMillis())
        val categories = categoryDao.observeByContextId("c1").first()
        assertEquals(0, categories.size)
    }

    @Test
    fun shouldUpdateCategory() = runTest {
        seedContextWithUser()
        val cat = createCategory("cat1", "Books")
        categoryDao.insert(cat)
        categoryDao.update(cat.copy(name = "Reading", color = "#00FF00"))
        val result = categoryDao.getById("cat1")
        assertEquals("Reading", result?.name)
        assertEquals("#00FF00", result?.color)
    }

    // ---- ReminderDao ----

    @Test
    fun shouldInsertAndObserveReminders() = runTest {
        seedContextWithUser()
        taskDao.insert(createTask("t1", "Task"))
        reminderDao.insert(createReminder("r1", "t1", remindAt = 5000L))
        val reminders = reminderDao.observeByTaskId("t1").first()
        assertEquals(1, reminders.size)
    }

    @Test
    fun shouldGetDueReminders() = runTest {
        seedContextWithUser()
        taskDao.insert(createTask("t1", "Task"))
        reminderDao.insert(createReminder("r1", "t1", remindAt = 1000L))
        reminderDao.insert(createReminder("r2", "t1", remindAt = 9000L))
        val due = reminderDao.getDueReminders(5000L)
        assertEquals(1, due.size)
        assertEquals("r1", due[0].id)
    }

    @Test
    fun shouldDeleteReminder() = runTest {
        seedContextWithUser()
        taskDao.insert(createTask("t1", "Task"))
        reminderDao.insert(createReminder("r1", "t1", remindAt = 1000L))
        reminderDao.delete("r1")
        assertNull(reminderDao.getById("r1"))
    }

    // ---- PendingChangeDao ----

    @Test
    fun shouldTrackUnsyncedChanges() = runTest {
        pendingChangeDao.insert(createPendingChange("TASK", "t1", "CREATE"))
        pendingChangeDao.insert(createPendingChange("TASK", "t2", "UPDATE"))
        val unsynced = pendingChangeDao.getUnsyncedChanges()
        assertEquals(2, unsynced.size)
    }

    @Test
    fun shouldMarkChangesAsSynced() = runTest {
        pendingChangeDao.insert(createPendingChange("TASK", "t1", "CREATE"))
        val changes = pendingChangeDao.getUnsyncedChanges()
        pendingChangeDao.markSynced(changes.map { it.id })
        val remaining = pendingChangeDao.getUnsyncedChanges()
        assertEquals(0, remaining.size)
    }

    @Test
    fun shouldDeleteSyncedChanges() = runTest {
        pendingChangeDao.insert(createPendingChange("TASK", "t1", "CREATE"))
        val changes = pendingChangeDao.getUnsyncedChanges()
        pendingChangeDao.markSynced(changes.map { it.id })
        pendingChangeDao.deleteSyncedChanges()
        val all = pendingChangeDao.getUnsyncedChanges()
        assertTrue(all.isEmpty())
    }

    // ---- Foreign key cascade: task deletion cascades to reminders ----

    @Test
    fun shouldCascadeDeleteRemindersWhenTaskDeleted() = runTest {
        seedContextWithUser()
        taskDao.insert(createTask("t1", "Task"))
        reminderDao.insert(createReminder("r1", "t1", remindAt = 1000L))
        reminderDao.insert(createReminder("r2", "t1", remindAt = 2000L))
        taskDao.deleteAll()
        assertNull(reminderDao.getById("r1"))
        assertNull(reminderDao.getById("r2"))
    }

    // ---- Persistence: data survives reinsert (upsert via REPLACE) ----

    @Test
    fun shouldUpsertUserOnConflict() = runTest {
        userDao.insert(testUser)
        userDao.insert(testUser.copy(email = "new@test.com"))
        val result = userDao.getById("u1")
        assertEquals("new@test.com", result?.email)
    }

    // ---- Helpers ----

    private suspend fun seedContextWithUser() {
        userDao.insert(testUser)
        contextDao.insert(testContext)
    }

    private fun createTask(
        id: String,
        title: String,
        gtdList: String = "INBOX",
        parentId: String? = null,
        nestingLevel: Int = 1,
        categoryId: String? = null,
        updatedAt: Long? = null,
    ) = TaskEntity(
        id = id, contextId = "c1", parentTaskId = parentId,
        gtdList = gtdList, categoryId = categoryId, title = title,
        nestingLevel = nestingLevel, createdAt = 1000L, updatedAt = updatedAt,
    )

    private fun createCategory(id: String, name: String) = CategoryEntity(
        id = id, contextId = "c1", name = name, icon = "book",
        color = "#FF0000", sortOrder = 0, createdAt = 1000L, updatedAt = 1000L,
    )

    private fun createReminder(id: String, taskId: String, remindAt: Long) = ReminderEntity(
        id = id, taskId = taskId, remindAt = remindAt, createdAt = 1000L,
    )

    private fun createPendingChange(entityType: String, entityId: String, changeType: String) =
        PendingChangeEntity(
            entityType = entityType, entityId = entityId, changeType = changeType,
            createdAt = System.currentTimeMillis(),
        )
}
