package com.gtd.backend.export.service;

import com.gtd.backend.category.model.Category;
import com.gtd.backend.category.repository.CategoryRepository;
import com.gtd.backend.context.exception.ContextAccessDeniedException;
import com.gtd.backend.context.exception.ContextNotFoundException;
import com.gtd.backend.context.model.Context;
import com.gtd.backend.context.model.ContextTheme;
import com.gtd.backend.context.repository.ContextRepository;
import com.gtd.backend.export.dto.ExportResponse;
import com.gtd.backend.reminder.model.Reminder;
import com.gtd.backend.reminder.model.ReminderOffsetType;
import com.gtd.backend.reminder.repository.ReminderRepository;
import com.gtd.backend.task.model.GtdList;
import com.gtd.backend.task.model.Task;
import com.gtd.backend.task.repository.TaskRepository;
import com.gtd.backend.auth.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportServiceTest {

    @Mock
    private ContextRepository contextRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ReminderRepository reminderRepository;

    @InjectMocks
    private ExportService exportService;

    private UUID userId;
    private UUID otherUserId;
    private UUID contextId;
    private Context context;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();
        contextId = UUID.randomUUID();

        user = User.builder().id(userId).email("test@test.com").build();

        context = Context.builder()
                .id(contextId)
                .user(user)
                .name("Work")
                .theme(ContextTheme.FORMAL)
                .icon("briefcase")
                .sortOrder(0)
                .build();
    }

    @Test
    void exportAll_shouldReturnAllContextsWithData() {
        UUID taskId = UUID.randomUUID();
        UUID catId = UUID.randomUUID();
        UUID reminderId = UUID.randomUUID();

        Category category = Category.builder()
                .id(catId).context(context).name("Books").icon("book").color("#FF5733").sortOrder(0).build();

        Task task = Task.builder()
                .id(taskId).context(context).title("Read Dune").gtdList(GtdList.NEXT_ACTIONS)
                .nestingLevel(1).sortOrder(0).build();

        Reminder reminder = Reminder.builder()
                .id(reminderId).task(task).remindAt(Instant.now().plusSeconds(3600))
                .offsetType(ReminderOffsetType.HOURS_BEFORE).offsetValue(1).build();

        when(contextRepository.findByUserIdAndIsDeletedFalseOrderBySortOrderAsc(userId))
                .thenReturn(List.of(context));
        when(categoryRepository.findByContextIdAndIsDeletedFalseOrderBySortOrderAsc(contextId))
                .thenReturn(List.of(category));
        when(taskRepository.findByContextIdAndParentTaskIsNullAndIsDeletedFalseOrderBySortOrderAsc(contextId))
                .thenReturn(List.of(task));
        when(taskRepository.findByParentTaskIdAndIsDeletedFalseOrderBySortOrderAsc(taskId))
                .thenReturn(Collections.emptyList());
        when(reminderRepository.findByTaskIdOrderByRemindAtAsc(taskId))
                .thenReturn(List.of(reminder));

        ExportResponse response = exportService.exportAll(userId);

        assertThat(response.getVersion()).isEqualTo("1.0");
        assertThat(response.getExportDate()).isNotNull();
        assertThat(response.getContexts()).hasSize(1);
        assertThat(response.getContexts().get(0).getName()).isEqualTo("Work");
        assertThat(response.getContexts().get(0).getCategories()).hasSize(1);
        assertThat(response.getContexts().get(0).getCategories().get(0).getName()).isEqualTo("Books");
        assertThat(response.getContexts().get(0).getTasks()).hasSize(1);
        assertThat(response.getContexts().get(0).getTasks().get(0).getTitle()).isEqualTo("Read Dune");
        assertThat(response.getContexts().get(0).getTasks().get(0).getReminders()).hasSize(1);
        assertThat(response.getContexts().get(0).getTasks().get(0).getReminders().get(0).getOffsetType())
                .isEqualTo(ReminderOffsetType.HOURS_BEFORE);
    }

    @Test
    void exportAll_shouldReturnEmptyListWhenNoContexts() {
        when(contextRepository.findByUserIdAndIsDeletedFalseOrderBySortOrderAsc(userId))
                .thenReturn(Collections.emptyList());

        ExportResponse response = exportService.exportAll(userId);

        assertThat(response.getContexts()).isEmpty();
        assertThat(response.getVersion()).isEqualTo("1.0");
    }

    @Test
    void exportAll_shouldReturnMultipleContexts() {
        Context context2 = Context.builder()
                .id(UUID.randomUUID()).user(user).name("Home").theme(ContextTheme.NATURE).icon("house").sortOrder(1).build();

        when(contextRepository.findByUserIdAndIsDeletedFalseOrderBySortOrderAsc(userId))
                .thenReturn(List.of(context, context2));
        when(categoryRepository.findByContextIdAndIsDeletedFalseOrderBySortOrderAsc(context.getId()))
                .thenReturn(Collections.emptyList());
        when(categoryRepository.findByContextIdAndIsDeletedFalseOrderBySortOrderAsc(context2.getId()))
                .thenReturn(Collections.emptyList());
        when(taskRepository.findByContextIdAndParentTaskIsNullAndIsDeletedFalseOrderBySortOrderAsc(context.getId()))
                .thenReturn(Collections.emptyList());
        when(taskRepository.findByContextIdAndParentTaskIsNullAndIsDeletedFalseOrderBySortOrderAsc(context2.getId()))
                .thenReturn(Collections.emptyList());

        ExportResponse response = exportService.exportAll(userId);

        assertThat(response.getContexts()).hasSize(2);
        assertThat(response.getContexts().get(0).getName()).isEqualTo("Work");
        assertThat(response.getContexts().get(1).getName()).isEqualTo("Home");
    }

    @Test
    void exportAll_shouldIncludeNestedSubtasks() {
        UUID parentId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();

        Task parent = Task.builder()
                .id(parentId).context(context).title("Parent").gtdList(GtdList.PROJECTS)
                .nestingLevel(1).sortOrder(0).build();

        Task child = Task.builder()
                .id(childId).context(context).title("Child").gtdList(GtdList.PROJECTS)
                .nestingLevel(2).sortOrder(0).parentTask(parent).build();

        when(contextRepository.findByUserIdAndIsDeletedFalseOrderBySortOrderAsc(userId))
                .thenReturn(List.of(context));
        when(categoryRepository.findByContextIdAndIsDeletedFalseOrderBySortOrderAsc(contextId))
                .thenReturn(Collections.emptyList());
        when(taskRepository.findByContextIdAndParentTaskIsNullAndIsDeletedFalseOrderBySortOrderAsc(contextId))
                .thenReturn(List.of(parent));
        when(taskRepository.findByParentTaskIdAndIsDeletedFalseOrderBySortOrderAsc(parentId))
                .thenReturn(List.of(child));
        when(taskRepository.findByParentTaskIdAndIsDeletedFalseOrderBySortOrderAsc(childId))
                .thenReturn(Collections.emptyList());
        when(reminderRepository.findByTaskIdOrderByRemindAtAsc(parentId))
                .thenReturn(Collections.emptyList());
        when(reminderRepository.findByTaskIdOrderByRemindAtAsc(childId))
                .thenReturn(Collections.emptyList());

        ExportResponse response = exportService.exportAll(userId);

        assertThat(response.getContexts().get(0).getTasks()).hasSize(1);
        assertThat(response.getContexts().get(0).getTasks().get(0).getSubtasks()).hasSize(1);
        assertThat(response.getContexts().get(0).getTasks().get(0).getSubtasks().get(0).getTitle()).isEqualTo("Child");
    }

    @Test
    void exportContext_shouldReturnSingleContext() {
        when(contextRepository.findByIdAndIsDeletedFalse(contextId))
                .thenReturn(Optional.of(context));
        when(categoryRepository.findByContextIdAndIsDeletedFalseOrderBySortOrderAsc(contextId))
                .thenReturn(Collections.emptyList());
        when(taskRepository.findByContextIdAndParentTaskIsNullAndIsDeletedFalseOrderBySortOrderAsc(contextId))
                .thenReturn(Collections.emptyList());

        ExportResponse response = exportService.exportContext(contextId, userId);

        assertThat(response.getContexts()).hasSize(1);
        assertThat(response.getContexts().get(0).getName()).isEqualTo("Work");
    }

    @Test
    void exportContext_shouldThrow404WhenContextNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(contextRepository.findByIdAndIsDeletedFalse(unknownId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> exportService.exportContext(unknownId, userId))
                .isInstanceOf(ContextNotFoundException.class);
    }

    @Test
    void exportContext_shouldThrow403WhenNotOwner() {
        when(contextRepository.findByIdAndIsDeletedFalse(contextId))
                .thenReturn(Optional.of(context));

        assertThatThrownBy(() -> exportService.exportContext(contextId, otherUserId))
                .isInstanceOf(ContextAccessDeniedException.class);
    }

    @Test
    void exportAll_shouldMapAllTaskFields() {
        UUID taskId = UUID.randomUUID();
        UUID catId = UUID.randomUUID();
        Instant dueDate = Instant.now().plusSeconds(86400);
        Instant completedAt = Instant.now();
        Instant createdAt = Instant.now().minusSeconds(3600);
        Instant updatedAt = Instant.now();

        Task task = Task.builder()
                .id(taskId).context(context).title("Full task")
                .gtdList(GtdList.DONE).categoryId(catId).notes("Some notes")
                .dueDate(dueDate).recurrenceRule("{\"type\":\"daily\"}")
                .nestingLevel(1).sortOrder(3).isCompleted(true).completedAt(completedAt)
                .createdAt(createdAt).updatedAt(updatedAt).build();

        when(contextRepository.findByUserIdAndIsDeletedFalseOrderBySortOrderAsc(userId))
                .thenReturn(List.of(context));
        when(categoryRepository.findByContextIdAndIsDeletedFalseOrderBySortOrderAsc(contextId))
                .thenReturn(Collections.emptyList());
        when(taskRepository.findByContextIdAndParentTaskIsNullAndIsDeletedFalseOrderBySortOrderAsc(contextId))
                .thenReturn(List.of(task));
        when(taskRepository.findByParentTaskIdAndIsDeletedFalseOrderBySortOrderAsc(taskId))
                .thenReturn(Collections.emptyList());
        when(reminderRepository.findByTaskIdOrderByRemindAtAsc(taskId))
                .thenReturn(Collections.emptyList());

        ExportResponse response = exportService.exportAll(userId);

        var exportedTask = response.getContexts().get(0).getTasks().get(0);
        assertThat(exportedTask.getId()).isEqualTo(taskId);
        assertThat(exportedTask.getGtdList()).isEqualTo(GtdList.DONE);
        assertThat(exportedTask.getCategoryId()).isEqualTo(catId);
        assertThat(exportedTask.getTitle()).isEqualTo("Full task");
        assertThat(exportedTask.getNotes()).isEqualTo("Some notes");
        assertThat(exportedTask.getDueDate()).isEqualTo(dueDate);
        assertThat(exportedTask.getRecurrenceRule()).isEqualTo("{\"type\":\"daily\"}");
        assertThat(exportedTask.getNestingLevel()).isEqualTo(1);
        assertThat(exportedTask.getSortOrder()).isEqualTo(3);
        assertThat(exportedTask.isCompleted()).isTrue();
        assertThat(exportedTask.getCompletedAt()).isEqualTo(completedAt);
        assertThat(exportedTask.getCreatedAt()).isEqualTo(createdAt);
        assertThat(exportedTask.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void exportAll_shouldMapAllCategoryFields() {
        UUID catId = UUID.randomUUID();
        Instant createdAt = Instant.now().minusSeconds(3600);
        Instant updatedAt = Instant.now();

        Category category = Category.builder()
                .id(catId).context(context).name("Books").icon("book")
                .color("#FF5733").sortOrder(2).createdAt(createdAt).updatedAt(updatedAt).build();

        when(contextRepository.findByUserIdAndIsDeletedFalseOrderBySortOrderAsc(userId))
                .thenReturn(List.of(context));
        when(categoryRepository.findByContextIdAndIsDeletedFalseOrderBySortOrderAsc(contextId))
                .thenReturn(List.of(category));
        when(taskRepository.findByContextIdAndParentTaskIsNullAndIsDeletedFalseOrderBySortOrderAsc(contextId))
                .thenReturn(Collections.emptyList());

        ExportResponse response = exportService.exportAll(userId);

        var exportedCat = response.getContexts().get(0).getCategories().get(0);
        assertThat(exportedCat.getId()).isEqualTo(catId);
        assertThat(exportedCat.getName()).isEqualTo("Books");
        assertThat(exportedCat.getIcon()).isEqualTo("book");
        assertThat(exportedCat.getColor()).isEqualTo("#FF5733");
        assertThat(exportedCat.getSortOrder()).isEqualTo(2);
        assertThat(exportedCat.getCreatedAt()).isEqualTo(createdAt);
        assertThat(exportedCat.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void exportAll_shouldMapAllReminderFields() {
        UUID taskId = UUID.randomUUID();
        UUID reminderId = UUID.randomUUID();
        Instant remindAt = Instant.now().plusSeconds(3600);
        Instant createdAt = Instant.now().minusSeconds(100);

        Task task = Task.builder()
                .id(taskId).context(context).title("Task").gtdList(GtdList.INBOX)
                .nestingLevel(1).sortOrder(0).build();

        Reminder reminder = Reminder.builder()
                .id(reminderId).task(task).remindAt(remindAt)
                .offsetType(ReminderOffsetType.DAYS_BEFORE).offsetValue(3)
                .isSent(true).createdAt(createdAt).build();

        when(contextRepository.findByUserIdAndIsDeletedFalseOrderBySortOrderAsc(userId))
                .thenReturn(List.of(context));
        when(categoryRepository.findByContextIdAndIsDeletedFalseOrderBySortOrderAsc(contextId))
                .thenReturn(Collections.emptyList());
        when(taskRepository.findByContextIdAndParentTaskIsNullAndIsDeletedFalseOrderBySortOrderAsc(contextId))
                .thenReturn(List.of(task));
        when(taskRepository.findByParentTaskIdAndIsDeletedFalseOrderBySortOrderAsc(taskId))
                .thenReturn(Collections.emptyList());
        when(reminderRepository.findByTaskIdOrderByRemindAtAsc(taskId))
                .thenReturn(List.of(reminder));

        ExportResponse response = exportService.exportAll(userId);

        var exportedReminder = response.getContexts().get(0).getTasks().get(0).getReminders().get(0);
        assertThat(exportedReminder.getId()).isEqualTo(reminderId);
        assertThat(exportedReminder.getRemindAt()).isEqualTo(remindAt);
        assertThat(exportedReminder.getOffsetType()).isEqualTo(ReminderOffsetType.DAYS_BEFORE);
        assertThat(exportedReminder.getOffsetValue()).isEqualTo(3);
        assertThat(exportedReminder.isSent()).isTrue();
        assertThat(exportedReminder.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void exportAll_shouldMapContextFields() {
        when(contextRepository.findByUserIdAndIsDeletedFalseOrderBySortOrderAsc(userId))
                .thenReturn(List.of(context));
        when(categoryRepository.findByContextIdAndIsDeletedFalseOrderBySortOrderAsc(contextId))
                .thenReturn(Collections.emptyList());
        when(taskRepository.findByContextIdAndParentTaskIsNullAndIsDeletedFalseOrderBySortOrderAsc(contextId))
                .thenReturn(Collections.emptyList());

        ExportResponse response = exportService.exportAll(userId);

        var exportedCtx = response.getContexts().get(0);
        assertThat(exportedCtx.getId()).isEqualTo(contextId);
        assertThat(exportedCtx.getName()).isEqualTo("Work");
        assertThat(exportedCtx.getTheme()).isEqualTo(ContextTheme.FORMAL);
        assertThat(exportedCtx.getIcon()).isEqualTo("briefcase");
        assertThat(exportedCtx.getSortOrder()).isEqualTo(0);
    }
}
