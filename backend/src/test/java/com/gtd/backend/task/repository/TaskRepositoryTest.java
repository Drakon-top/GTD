package com.gtd.backend.task.repository;

import com.gtd.backend.auth.model.User;
import com.gtd.backend.auth.repository.UserRepository;
import com.gtd.backend.context.model.Context;
import com.gtd.backend.context.model.ContextTheme;
import com.gtd.backend.context.repository.ContextRepository;
import com.gtd.backend.task.model.GtdList;
import com.gtd.backend.task.model.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class TaskRepositoryTest {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ContextRepository contextRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private Context testContext;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        contextRepository.deleteAll();
        userRepository.deleteAll();

        testUser = userRepository.saveAndFlush(
                User.builder()
                        .email("task-test@example.com")
                        .passwordHash("$2a$10$hash")
                        .build()
        );

        testContext = contextRepository.saveAndFlush(
                Context.builder()
                        .user(testUser)
                        .name("Work")
                        .theme(ContextTheme.FORMAL)
                        .sortOrder(0)
                        .build()
        );
    }

    @Test
    void shouldSaveTask_whenValidData() {
        Task task = buildTask("Buy groceries", GtdList.INBOX, 1);

        Task saved = taskRepository.saveAndFlush(task);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTitle()).isEqualTo("Buy groceries");
        assertThat(saved.getGtdList()).isEqualTo(GtdList.INBOX);
        assertThat(saved.getNestingLevel()).isEqualTo(1);
        assertThat(saved.getSortOrder()).isZero();
        assertThat(saved.isCompleted()).isFalse();
        assertThat(saved.isDeleted()).isFalse();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getVersion()).isZero();
    }

    @Test
    void shouldFindByContextAndNotDeleted_whenTasksExist() {
        taskRepository.saveAndFlush(buildTask("Task 1", GtdList.INBOX, 1));
        taskRepository.saveAndFlush(buildTask("Task 2", GtdList.NEXT_ACTIONS, 1));
        Task deleted = buildTask("Deleted", GtdList.INBOX, 1);
        deleted.setDeleted(true);
        taskRepository.saveAndFlush(deleted);

        List<Task> found = taskRepository
                .findByContextIdAndIsDeletedFalseOrderBySortOrderAsc(testContext.getId());

        assertThat(found).hasSize(2);
        assertThat(found).extracting(Task::getTitle).containsExactly("Task 1", "Task 2");
    }

    @Test
    void shouldFilterByGtdList_whenQueried() {
        taskRepository.saveAndFlush(buildTask("Inbox Task", GtdList.INBOX, 1));
        taskRepository.saveAndFlush(buildTask("Next Action", GtdList.NEXT_ACTIONS, 1));
        taskRepository.saveAndFlush(buildTask("Another Inbox", GtdList.INBOX, 1));

        List<Task> inboxTasks = taskRepository
                .findByContextIdAndGtdListAndIsDeletedFalseOrderBySortOrderAsc(
                        testContext.getId(), GtdList.INBOX);

        assertThat(inboxTasks).hasSize(2);
        assertThat(inboxTasks).extracting(Task::getTitle)
                .containsExactly("Inbox Task", "Another Inbox");
    }

    @Test
    void shouldFindSubtasks_whenParentExists() {
        Task parent = taskRepository.saveAndFlush(buildTask("Parent", GtdList.PROJECTS, 1));

        Task subtask1 = buildTask("Subtask 1", GtdList.PROJECTS, 2);
        subtask1.setParentTask(parent);
        subtask1.setNestingLevel(2);
        taskRepository.saveAndFlush(subtask1);

        Task subtask2 = buildTask("Subtask 2", GtdList.PROJECTS, 2);
        subtask2.setParentTask(parent);
        subtask2.setNestingLevel(2);
        taskRepository.saveAndFlush(subtask2);

        List<Task> subtasks = taskRepository
                .findByParentTaskIdAndIsDeletedFalseOrderBySortOrderAsc(parent.getId());

        assertThat(subtasks).hasSize(2);
        assertThat(subtasks).extracting(Task::getTitle)
                .containsExactly("Subtask 1", "Subtask 2");
    }

    @Test
    void shouldNotReturnDeletedSubtasks() {
        Task parent = taskRepository.saveAndFlush(buildTask("Parent", GtdList.PROJECTS, 1));

        Task active = buildTask("Active Subtask", GtdList.PROJECTS, 2);
        active.setParentTask(parent);
        active.setNestingLevel(2);
        taskRepository.saveAndFlush(active);

        Task deleted = buildTask("Deleted Subtask", GtdList.PROJECTS, 2);
        deleted.setParentTask(parent);
        deleted.setNestingLevel(2);
        deleted.setDeleted(true);
        taskRepository.saveAndFlush(deleted);

        List<Task> subtasks = taskRepository
                .findByParentTaskIdAndIsDeletedFalseOrderBySortOrderAsc(parent.getId());

        assertThat(subtasks).hasSize(1);
        assertThat(subtasks.get(0).getTitle()).isEqualTo("Active Subtask");
    }

    @Test
    void shouldFindByIdAndNotDeleted_whenTaskExists() {
        Task saved = taskRepository.saveAndFlush(buildTask("Find Me", GtdList.INBOX, 1));

        Optional<Task> found = taskRepository.findByIdAndIsDeletedFalse(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Find Me");
    }

    @Test
    void shouldReturnEmpty_whenTaskIsDeleted() {
        Task deleted = buildTask("Deleted", GtdList.INBOX, 1);
        deleted.setDeleted(true);
        Task saved = taskRepository.saveAndFlush(deleted);

        Optional<Task> found = taskRepository.findByIdAndIsDeletedFalse(saved.getId());

        assertThat(found).isEmpty();
    }

    @Test
    void shouldCountActiveTasksInContext() {
        taskRepository.saveAndFlush(buildTask("Task 1", GtdList.INBOX, 1));
        taskRepository.saveAndFlush(buildTask("Task 2", GtdList.NEXT_ACTIONS, 1));
        Task deleted = buildTask("Deleted", GtdList.INBOX, 1);
        deleted.setDeleted(true);
        taskRepository.saveAndFlush(deleted);

        int count = taskRepository.countByContextIdAndIsDeletedFalse(testContext.getId());

        assertThat(count).isEqualTo(2);
    }

    @Test
    void shouldCountByGtdListInContext() {
        taskRepository.saveAndFlush(buildTask("Inbox 1", GtdList.INBOX, 1));
        taskRepository.saveAndFlush(buildTask("Inbox 2", GtdList.INBOX, 1));
        taskRepository.saveAndFlush(buildTask("Next", GtdList.NEXT_ACTIONS, 1));

        int inboxCount = taskRepository
                .countByContextIdAndGtdListAndIsDeletedFalse(testContext.getId(), GtdList.INBOX);
        int nextCount = taskRepository
                .countByContextIdAndGtdListAndIsDeletedFalse(testContext.getId(), GtdList.NEXT_ACTIONS);

        assertThat(inboxCount).isEqualTo(2);
        assertThat(nextCount).isEqualTo(1);
    }

    @Test
    void shouldOrderBySortOrder_whenMultipleTasks() {
        Task t3 = buildTask("Third", GtdList.INBOX, 1);
        t3.setSortOrder(2);
        taskRepository.saveAndFlush(t3);

        Task t1 = buildTask("First", GtdList.INBOX, 1);
        t1.setSortOrder(0);
        taskRepository.saveAndFlush(t1);

        Task t2 = buildTask("Second", GtdList.INBOX, 1);
        t2.setSortOrder(1);
        taskRepository.saveAndFlush(t2);

        List<Task> found = taskRepository
                .findByContextIdAndIsDeletedFalseOrderBySortOrderAsc(testContext.getId());

        assertThat(found).hasSize(3);
        assertThat(found.get(0).getTitle()).isEqualTo("First");
        assertThat(found.get(1).getTitle()).isEqualTo("Second");
        assertThat(found.get(2).getTitle()).isEqualTo("Third");
    }

    @Test
    void shouldPersistAllGtdListValues() {
        for (GtdList gtdList : GtdList.values()) {
            Task task = buildTask("Task-" + gtdList.name(), gtdList, 1);
            Task saved = taskRepository.saveAndFlush(task);
            assertThat(saved.getGtdList()).isEqualTo(gtdList);
        }

        List<Task> all = taskRepository
                .findByContextIdAndIsDeletedFalseOrderBySortOrderAsc(testContext.getId());
        assertThat(all).hasSize(GtdList.values().length);
    }

    @Test
    void shouldSupportSelfReference_whenSubtaskCreated() {
        Task parent = taskRepository.saveAndFlush(buildTask("Parent", GtdList.PROJECTS, 1));

        Task child = buildTask("Child", GtdList.PROJECTS, 2);
        child.setParentTask(parent);
        child.setNestingLevel(2);
        Task savedChild = taskRepository.saveAndFlush(child);

        Task grandchild = buildTask("Grandchild", GtdList.PROJECTS, 3);
        grandchild.setParentTask(savedChild);
        grandchild.setNestingLevel(3);
        Task savedGrandchild = taskRepository.saveAndFlush(grandchild);

        Task fetched = taskRepository.findById(savedGrandchild.getId()).orElseThrow();
        assertThat(fetched.getParentTask().getId()).isEqualTo(savedChild.getId());
        assertThat(fetched.getNestingLevel()).isEqualTo(3);
    }

    @Test
    void shouldPersistOptionalFields() {
        Task task = buildTask("Full Task", GtdList.CALENDAR, 1);
        task.setNotes("Important notes here");
        task.setDueDate(Instant.parse("2026-06-01T10:00:00Z"));
        task.setReminderSettings("{\"type\":\"before\",\"minutes\":30}");
        task.setRecurrenceRule("{\"type\":\"daily\",\"time\":\"09:00\"}");

        Task saved = taskRepository.saveAndFlush(task);

        Task fetched = taskRepository.findById(saved.getId()).orElseThrow();
        assertThat(fetched.getNotes()).isEqualTo("Important notes here");
        assertThat(fetched.getDueDate()).isEqualTo(Instant.parse("2026-06-01T10:00:00Z"));
        assertThat(fetched.getReminderSettings()).contains("before");
        assertThat(fetched.getRecurrenceRule()).contains("daily");
    }

    @Test
    void shouldSetTimestamps_whenPersisting() {
        Task saved = taskRepository.saveAndFlush(buildTask("Timestamped", GtdList.INBOX, 1));

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getCreatedAt()).isEqualTo(saved.getUpdatedAt());
    }

    @Test
    void shouldIsolateTasksByContext() {
        Context otherContext = contextRepository.saveAndFlush(
                Context.builder()
                        .user(testUser)
                        .name("Home")
                        .theme(ContextTheme.NATURE)
                        .sortOrder(1)
                        .build()
        );

        taskRepository.saveAndFlush(buildTask("Work Task", GtdList.INBOX, 1));

        Task homeTask = Task.builder()
                .context(otherContext)
                .title("Home Task")
                .gtdList(GtdList.INBOX)
                .nestingLevel(1)
                .build();
        taskRepository.saveAndFlush(homeTask);

        List<Task> workTasks = taskRepository
                .findByContextIdAndIsDeletedFalseOrderBySortOrderAsc(testContext.getId());
        List<Task> homeTasks = taskRepository
                .findByContextIdAndIsDeletedFalseOrderBySortOrderAsc(otherContext.getId());

        assertThat(workTasks).hasSize(1);
        assertThat(workTasks.get(0).getTitle()).isEqualTo("Work Task");
        assertThat(homeTasks).hasSize(1);
        assertThat(homeTasks.get(0).getTitle()).isEqualTo("Home Task");
    }

    @Test
    void shouldMaintainForeignKeyToContext() {
        Task saved = taskRepository.saveAndFlush(buildTask("FK Test", GtdList.INBOX, 1));

        Task fetched = taskRepository.findById(saved.getId()).orElseThrow();
        assertThat(fetched.getContext().getId()).isEqualTo(testContext.getId());
    }

    private Task buildTask(String title, GtdList gtdList, int nestingLevel) {
        return Task.builder()
                .context(testContext)
                .title(title)
                .gtdList(gtdList)
                .nestingLevel(nestingLevel)
                .build();
    }
}
