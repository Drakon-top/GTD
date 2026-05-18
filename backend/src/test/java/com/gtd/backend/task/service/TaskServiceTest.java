package com.gtd.backend.task.service;

import com.gtd.backend.auth.model.User;
import com.gtd.backend.context.exception.ContextAccessDeniedException;
import com.gtd.backend.context.exception.ContextNotFoundException;
import com.gtd.backend.context.model.Context;
import com.gtd.backend.context.model.ContextTheme;
import com.gtd.backend.context.repository.ContextRepository;
import com.gtd.backend.reminder.model.Reminder;
import com.gtd.backend.reminder.model.ReminderOffsetType;
import com.gtd.backend.reminder.repository.ReminderRepository;
import com.gtd.backend.task.dto.CreateTaskRequest;
import com.gtd.backend.task.dto.TaskCountsResponse;
import com.gtd.backend.task.dto.TaskResponse;
import com.gtd.backend.task.dto.UpdateTaskRequest;
import com.gtd.backend.task.exception.MaxNestingLevelException;
import com.gtd.backend.task.exception.TaskAccessDeniedException;
import com.gtd.backend.task.exception.TaskNotFoundException;
import com.gtd.backend.task.model.GtdList;
import com.gtd.backend.task.model.Task;
import com.gtd.backend.task.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ContextRepository contextRepository;

    @Mock
    private ReminderRepository reminderRepository;

    @InjectMocks
    private TaskService taskService;

    private final UUID userId = UUID.randomUUID();
    private final UUID contextId = UUID.randomUUID();
    private final UUID taskId = UUID.randomUUID();

    @Test
    void shouldReturnTasksForContext() {
        Context context = buildContext(contextId, userId);
        Task task = buildTask(taskId, context, "Buy groceries", GtdList.INBOX);
        when(contextRepository.findByIdAndIsDeletedFalse(contextId)).thenReturn(Optional.of(context));
        when(taskRepository.findByContextIdAndParentTaskIsNullAndIsDeletedFalseOrderBySortOrderAsc(contextId))
                .thenReturn(List.of(task));

        List<TaskResponse> result = taskService.getTasks(contextId, null, userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Buy groceries");
        assertThat(result.get(0).getGtdList()).isEqualTo(GtdList.INBOX);
    }

    @Test
    void shouldFilterTasksByGtdList() {
        Context context = buildContext(contextId, userId);
        Task task = buildTask(taskId, context, "Next task", GtdList.NEXT_ACTIONS);
        when(contextRepository.findByIdAndIsDeletedFalse(contextId)).thenReturn(Optional.of(context));
        when(taskRepository.findByContextIdAndGtdListAndParentTaskIsNullAndIsDeletedFalseOrderBySortOrderAsc(contextId, GtdList.NEXT_ACTIONS))
                .thenReturn(List.of(task));

        List<TaskResponse> result = taskService.getTasks(contextId, GtdList.NEXT_ACTIONS, userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getGtdList()).isEqualTo(GtdList.NEXT_ACTIONS);
    }

    @Test
    void shouldReturnEmptyListWhenNoTasks() {
        Context context = buildContext(contextId, userId);
        when(contextRepository.findByIdAndIsDeletedFalse(contextId)).thenReturn(Optional.of(context));
        when(taskRepository.findByContextIdAndParentTaskIsNullAndIsDeletedFalseOrderBySortOrderAsc(contextId))
                .thenReturn(List.of());

        List<TaskResponse> result = taskService.getTasks(contextId, null, userId);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldThrowContextNotFoundOnGetTasks() {
        when(contextRepository.findByIdAndIsDeletedFalse(contextId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTasks(contextId, null, userId))
                .isInstanceOf(ContextNotFoundException.class);
    }

    @Test
    void shouldThrowContextAccessDeniedOnGetTasks() {
        UUID otherUserId = UUID.randomUUID();
        Context context = buildContext(contextId, otherUserId);
        when(contextRepository.findByIdAndIsDeletedFalse(contextId)).thenReturn(Optional.of(context));

        assertThatThrownBy(() -> taskService.getTasks(contextId, null, userId))
                .isInstanceOf(ContextAccessDeniedException.class);
    }

    @Test
    void shouldGetTaskByIdWithSubtasks() {
        Context context = buildContext(contextId, userId);
        Task task = buildTask(taskId, context, "Parent task", GtdList.INBOX);
        UUID subtaskId = UUID.randomUUID();
        Task subtask = buildTask(subtaskId, context, "Subtask 1", GtdList.INBOX);
        subtask.setParentTask(task);
        subtask.setNestingLevel(2);

        when(taskRepository.findByIdAndIsDeletedFalse(taskId)).thenReturn(Optional.of(task));
        when(taskRepository.findByParentTaskIdAndIsDeletedFalseOrderBySortOrderAsc(taskId))
                .thenReturn(List.of(subtask));

        TaskResponse result = taskService.getTask(taskId, userId);

        assertThat(result.getTitle()).isEqualTo("Parent task");
        assertThat(result.getSubtasks()).hasSize(1);
        assertThat(result.getSubtasks().get(0).getTitle()).isEqualTo("Subtask 1");
    }

    @Test
    void shouldThrowNotFoundWhenTaskDoesNotExist() {
        when(taskRepository.findByIdAndIsDeletedFalse(taskId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTask(taskId, userId))
                .isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    void shouldThrowAccessDeniedWhenTaskBelongsToAnotherUser() {
        UUID otherUserId = UUID.randomUUID();
        Context context = buildContext(contextId, otherUserId);
        Task task = buildTask(taskId, context, "Other's task", GtdList.INBOX);
        when(taskRepository.findByIdAndIsDeletedFalse(taskId)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.getTask(taskId, userId))
                .isInstanceOf(TaskAccessDeniedException.class);
    }

    @Test
    void shouldCreateTaskWithDefaults() {
        Context context = buildContext(contextId, userId);
        when(contextRepository.findByIdAndIsDeletedFalse(contextId)).thenReturn(Optional.of(context));
        when(taskRepository.countByContextIdAndIsDeletedFalse(contextId)).thenReturn(0);
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task t = invocation.getArgument(0);
            t.setId(taskId);
            t.setCreatedAt(Instant.now());
            t.setUpdatedAt(Instant.now());
            return t;
        });

        CreateTaskRequest request = CreateTaskRequest.builder()
                .title("Buy groceries")
                .build();

        TaskResponse result = taskService.createTask(contextId, request, userId);

        assertThat(result.getTitle()).isEqualTo("Buy groceries");
        assertThat(result.getGtdList()).isEqualTo(GtdList.INBOX);
        assertThat(result.getNestingLevel()).isEqualTo(1);
        assertThat(result.getSortOrder()).isEqualTo(0);
    }

    @Test
    void shouldCreateTaskWithExplicitGtdList() {
        Context context = buildContext(contextId, userId);
        when(contextRepository.findByIdAndIsDeletedFalse(contextId)).thenReturn(Optional.of(context));
        when(taskRepository.countByContextIdAndIsDeletedFalse(contextId)).thenReturn(3);
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task t = invocation.getArgument(0);
            t.setId(taskId);
            t.setCreatedAt(Instant.now());
            t.setUpdatedAt(Instant.now());
            return t;
        });

        CreateTaskRequest request = CreateTaskRequest.builder()
                .title("Important task")
                .gtdList(GtdList.NEXT_ACTIONS)
                .notes("Some notes")
                .build();

        TaskResponse result = taskService.createTask(contextId, request, userId);

        assertThat(result.getGtdList()).isEqualTo(GtdList.NEXT_ACTIONS);
        assertThat(result.getNotes()).isEqualTo("Some notes");
        assertThat(result.getSortOrder()).isEqualTo(3);
    }

    @Test
    void shouldTrimTitleOnCreate() {
        Context context = buildContext(contextId, userId);
        when(contextRepository.findByIdAndIsDeletedFalse(contextId)).thenReturn(Optional.of(context));
        when(taskRepository.countByContextIdAndIsDeletedFalse(contextId)).thenReturn(0);
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task t = invocation.getArgument(0);
            t.setId(taskId);
            t.setCreatedAt(Instant.now());
            t.setUpdatedAt(Instant.now());
            return t;
        });

        CreateTaskRequest request = CreateTaskRequest.builder()
                .title("  Buy groceries  ")
                .build();

        TaskResponse result = taskService.createTask(contextId, request, userId);

        assertThat(result.getTitle()).isEqualTo("Buy groceries");
    }

    @Test
    void shouldThrowContextNotFoundOnCreateTask() {
        when(contextRepository.findByIdAndIsDeletedFalse(contextId)).thenReturn(Optional.empty());

        CreateTaskRequest request = CreateTaskRequest.builder().title("test").build();

        assertThatThrownBy(() -> taskService.createTask(contextId, request, userId))
                .isInstanceOf(ContextNotFoundException.class);
        verify(taskRepository, never()).save(any());
    }

    @Test
    void shouldUpdateTaskFields() {
        Context context = buildContext(contextId, userId);
        Task task = buildTask(taskId, context, "Old title", GtdList.INBOX);
        when(taskRepository.findByIdAndIsDeletedFalse(taskId)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Instant dueDate = Instant.parse("2026-06-01T10:00:00Z");
        UpdateTaskRequest request = UpdateTaskRequest.builder()
                .title("New title")
                .notes("Updated notes")
                .gtdList(GtdList.NEXT_ACTIONS)
                .dueDate(dueDate)
                .sortOrder(5)
                .build();

        TaskResponse result = taskService.updateTask(taskId, request, userId);

        assertThat(result.getTitle()).isEqualTo("New title");
        assertThat(result.getNotes()).isEqualTo("Updated notes");
        assertThat(result.getGtdList()).isEqualTo(GtdList.NEXT_ACTIONS);
        assertThat(result.getDueDate()).isEqualTo(dueDate);
        assertThat(result.getSortOrder()).isEqualTo(5);
    }

    @Test
    void shouldUpdateOnlyProvidedFields() {
        Context context = buildContext(contextId, userId);
        Task task = buildTask(taskId, context, "Original title", GtdList.INBOX);
        task.setNotes("Original notes");
        when(taskRepository.findByIdAndIsDeletedFalse(taskId)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateTaskRequest request = UpdateTaskRequest.builder()
                .title("Updated title")
                .build();

        TaskResponse result = taskService.updateTask(taskId, request, userId);

        assertThat(result.getTitle()).isEqualTo("Updated title");
        assertThat(result.getNotes()).isEqualTo("Original notes");
        assertThat(result.getGtdList()).isEqualTo(GtdList.INBOX);
    }

    @Test
    void shouldTrimTitleOnUpdate() {
        Context context = buildContext(contextId, userId);
        Task task = buildTask(taskId, context, "Old", GtdList.INBOX);
        when(taskRepository.findByIdAndIsDeletedFalse(taskId)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateTaskRequest request = UpdateTaskRequest.builder()
                .title("  Trimmed  ")
                .build();

        TaskResponse result = taskService.updateTask(taskId, request, userId);

        assertThat(result.getTitle()).isEqualTo("Trimmed");
    }

    @Test
    void shouldThrowNotFoundOnUpdate() {
        when(taskRepository.findByIdAndIsDeletedFalse(taskId)).thenReturn(Optional.empty());

        UpdateTaskRequest request = UpdateTaskRequest.builder().title("new").build();

        assertThatThrownBy(() -> taskService.updateTask(taskId, request, userId))
                .isInstanceOf(TaskNotFoundException.class);
        verify(taskRepository, never()).save(any());
    }

    @Test
    void shouldThrowAccessDeniedOnUpdate() {
        UUID otherUserId = UUID.randomUUID();
        Context context = buildContext(contextId, otherUserId);
        Task task = buildTask(taskId, context, "Other's task", GtdList.INBOX);
        when(taskRepository.findByIdAndIsDeletedFalse(taskId)).thenReturn(Optional.of(task));

        UpdateTaskRequest request = UpdateTaskRequest.builder().title("new").build();

        assertThatThrownBy(() -> taskService.updateTask(taskId, request, userId))
                .isInstanceOf(TaskAccessDeniedException.class);
        verify(taskRepository, never()).save(any());
    }

    @Test
    void shouldMoveTaskToNewGtdList() {
        Context context = buildContext(contextId, userId);
        Task task = buildTask(taskId, context, "My task", GtdList.INBOX);
        when(taskRepository.findByIdAndIsDeletedFalse(taskId)).thenReturn(Optional.of(task));
        when(taskRepository.saveAndFlush(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse result = taskService.moveTask(taskId, GtdList.NEXT_ACTIONS, userId);

        assertThat(result.getGtdList()).isEqualTo(GtdList.NEXT_ACTIONS);
        verify(taskRepository).saveAndFlush(task);
    }

    @Test
    void shouldThrowNotFoundOnMoveWhenTaskDeleted() {
        when(taskRepository.findByIdAndIsDeletedFalse(taskId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.moveTask(taskId, GtdList.NEXT_ACTIONS, userId))
                .isInstanceOf(TaskNotFoundException.class);
        verify(taskRepository, never()).save(any());
    }

    @Test
    void shouldThrowAccessDeniedOnMoveWhenNotOwner() {
        UUID otherUserId = UUID.randomUUID();
        Context context = buildContext(contextId, otherUserId);
        Task task = buildTask(taskId, context, "Other's task", GtdList.INBOX);
        when(taskRepository.findByIdAndIsDeletedFalse(taskId)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.moveTask(taskId, GtdList.NEXT_ACTIONS, userId))
                .isInstanceOf(TaskAccessDeniedException.class);
        verify(taskRepository, never()).save(any());
    }

    @Test
    void shouldCompleteTask() {
        Context context = buildContext(contextId, userId);
        Task task = buildTask(taskId, context, "Complete me", GtdList.NEXT_ACTIONS);
        when(taskRepository.findByIdAndIsDeletedFalse(taskId)).thenReturn(Optional.of(task));
        when(taskRepository.saveAndFlush(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse result = taskService.completeTask(taskId, userId);

        assertThat(result.isCompleted()).isTrue();
        assertThat(result.getCompletedAt()).isNotNull();
        assertThat(result.getGtdList()).isEqualTo(GtdList.DONE);
        verify(taskRepository).saveAndFlush(task);
    }

    @Test
    void shouldThrowNotFoundOnCompleteWhenTaskDeleted() {
        when(taskRepository.findByIdAndIsDeletedFalse(taskId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.completeTask(taskId, userId))
                .isInstanceOf(TaskNotFoundException.class);
        verify(taskRepository, never()).save(any());
    }

    @Test
    void shouldThrowAccessDeniedOnCompleteWhenNotOwner() {
        UUID otherUserId = UUID.randomUUID();
        Context context = buildContext(contextId, otherUserId);
        Task task = buildTask(taskId, context, "Other's task", GtdList.INBOX);
        when(taskRepository.findByIdAndIsDeletedFalse(taskId)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.completeTask(taskId, userId))
                .isInstanceOf(TaskAccessDeniedException.class);
        verify(taskRepository, never()).save(any());
    }

    @Test
    void shouldIncrementVersionOnMove() {
        Context context = buildContext(contextId, userId);
        Task task = buildTask(taskId, context, "My task", GtdList.INBOX);
        task.setVersion(3);
        when(taskRepository.findByIdAndIsDeletedFalse(taskId)).thenReturn(Optional.of(task));
        when(taskRepository.saveAndFlush(any(Task.class))).thenAnswer(invocation -> {
            Task t = invocation.getArgument(0);
            t.setVersion(t.getVersion() + 1);
            return t;
        });

        TaskResponse result = taskService.moveTask(taskId, GtdList.WAITING_FOR, userId);

        assertThat(result.getVersion()).isEqualTo(4);
    }

    @Test
    void shouldSoftDeleteTask() {
        Context context = buildContext(contextId, userId);
        Task task = buildTask(taskId, context, "To delete", GtdList.INBOX);
        when(taskRepository.findByIdAndIsDeletedFalse(taskId)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        taskService.deleteTask(taskId, userId);

        assertThat(task.isDeleted()).isTrue();
        verify(taskRepository).save(task);
    }

    @Test
    void shouldThrowNotFoundOnDelete() {
        when(taskRepository.findByIdAndIsDeletedFalse(taskId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.deleteTask(taskId, userId))
                .isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    void shouldThrowAccessDeniedOnDelete() {
        UUID otherUserId = UUID.randomUUID();
        Context context = buildContext(contextId, otherUserId);
        Task task = buildTask(taskId, context, "Other's task", GtdList.INBOX);
        when(taskRepository.findByIdAndIsDeletedFalse(taskId)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.deleteTask(taskId, userId))
                .isInstanceOf(TaskAccessDeniedException.class);
    }

    @Test
    void shouldCreateSubtask() {
        Context context = buildContext(contextId, userId);
        Task parentTask = buildTask(taskId, context, "Parent", GtdList.INBOX);
        parentTask.setNestingLevel(1);
        when(taskRepository.findByIdAndIsDeletedFalse(taskId)).thenReturn(Optional.of(parentTask));
        when(taskRepository.countByParentTaskIdAndIsDeletedFalse(taskId)).thenReturn(0);
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task t = invocation.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        CreateTaskRequest request = CreateTaskRequest.builder().title("Subtask 1").build();
        TaskResponse result = taskService.createSubtask(taskId, request, userId);

        assertThat(result.getNestingLevel()).isEqualTo(2);
        assertThat(result.getTitle()).isEqualTo("Subtask 1");
        assertThat(result.getContextId()).isEqualTo(contextId);
        assertThat(result.getParentTaskId()).isEqualTo(taskId);
    }

    @Test
    void shouldCreateSubtaskAtLevel4() {
        Context context = buildContext(contextId, userId);
        Task parentTask = buildTask(taskId, context, "Level 3 task", GtdList.INBOX);
        parentTask.setNestingLevel(3);
        when(taskRepository.findByIdAndIsDeletedFalse(taskId)).thenReturn(Optional.of(parentTask));
        when(taskRepository.countByParentTaskIdAndIsDeletedFalse(taskId)).thenReturn(0);
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task t = invocation.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        CreateTaskRequest request = CreateTaskRequest.builder().title("Level 4 subtask").build();
        TaskResponse result = taskService.createSubtask(taskId, request, userId);

        assertThat(result.getNestingLevel()).isEqualTo(4);
    }

    @Test
    void shouldThrowMaxNestingLevelWhenParentIsLevel4() {
        Context context = buildContext(contextId, userId);
        Task parentTask = buildTask(taskId, context, "Level 4 task", GtdList.INBOX);
        parentTask.setNestingLevel(4);
        when(taskRepository.findByIdAndIsDeletedFalse(taskId)).thenReturn(Optional.of(parentTask));

        CreateTaskRequest request = CreateTaskRequest.builder().title("Level 5 attempt").build();

        assertThatThrownBy(() -> taskService.createSubtask(taskId, request, userId))
                .isInstanceOf(MaxNestingLevelException.class);
        verify(taskRepository, never()).save(any());
    }

    @Test
    void shouldThrowNotFoundOnCreateSubtaskWhenParentNotFound() {
        when(taskRepository.findByIdAndIsDeletedFalse(taskId)).thenReturn(Optional.empty());

        CreateTaskRequest request = CreateTaskRequest.builder().title("Orphan").build();

        assertThatThrownBy(() -> taskService.createSubtask(taskId, request, userId))
                .isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    void shouldThrowAccessDeniedOnCreateSubtaskWhenNotOwner() {
        UUID otherUserId = UUID.randomUUID();
        Context context = buildContext(contextId, otherUserId);
        Task parentTask = buildTask(taskId, context, "Other's task", GtdList.INBOX);
        when(taskRepository.findByIdAndIsDeletedFalse(taskId)).thenReturn(Optional.of(parentTask));

        CreateTaskRequest request = CreateTaskRequest.builder().title("My subtask").build();

        assertThatThrownBy(() -> taskService.createSubtask(taskId, request, userId))
                .isInstanceOf(TaskAccessDeniedException.class);
    }

    @Test
    void shouldGetSubtasks() {
        Context context = buildContext(contextId, userId);
        Task parentTask = buildTask(taskId, context, "Parent", GtdList.INBOX);
        UUID subtask1Id = UUID.randomUUID();
        UUID subtask2Id = UUID.randomUUID();
        Task subtask1 = buildTask(subtask1Id, context, "Sub 1", GtdList.INBOX);
        subtask1.setParentTask(parentTask);
        subtask1.setNestingLevel(2);
        Task subtask2 = buildTask(subtask2Id, context, "Sub 2", GtdList.INBOX);
        subtask2.setParentTask(parentTask);
        subtask2.setNestingLevel(2);

        when(taskRepository.findByIdAndIsDeletedFalse(taskId)).thenReturn(Optional.of(parentTask));
        when(taskRepository.findByParentTaskIdAndIsDeletedFalseOrderBySortOrderAsc(taskId))
                .thenReturn(List.of(subtask1, subtask2));

        List<TaskResponse> result = taskService.getSubtasks(taskId, userId);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTitle()).isEqualTo("Sub 1");
        assertThat(result.get(1).getTitle()).isEqualTo("Sub 2");
    }

    @Test
    void shouldThrowNotFoundOnGetSubtasksWhenParentNotFound() {
        when(taskRepository.findByIdAndIsDeletedFalse(taskId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getSubtasks(taskId, userId))
                .isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    void shouldCascadeSoftDeleteToSubtasks() {
        Context context = buildContext(contextId, userId);
        Task parentTask = buildTask(taskId, context, "Parent", GtdList.INBOX);
        UUID childId = UUID.randomUUID();
        Task childTask = buildTask(childId, context, "Child", GtdList.INBOX);
        childTask.setParentTask(parentTask);

        when(taskRepository.findByIdAndIsDeletedFalse(taskId)).thenReturn(Optional.of(parentTask));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(taskRepository.findByParentTaskIdAndIsDeletedFalse(taskId)).thenReturn(List.of(childTask));
        when(taskRepository.findByParentTaskIdAndIsDeletedFalse(childId)).thenReturn(List.of());

        taskService.deleteTask(taskId, userId);

        assertThat(parentTask.isDeleted()).isTrue();
        assertThat(childTask.isDeleted()).isTrue();
    }

    @Test
    void shouldReturnHasIncompleteSubtasksOnComplete() {
        Context context = buildContext(contextId, userId);
        Task task = buildTask(taskId, context, "Parent", GtdList.NEXT_ACTIONS);
        when(taskRepository.findByIdAndIsDeletedFalse(taskId)).thenReturn(Optional.of(task));
        when(taskRepository.saveAndFlush(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(taskRepository.countByParentTaskIdAndIsCompletedFalseAndIsDeletedFalse(taskId)).thenReturn(2);

        TaskResponse result = taskService.completeTask(taskId, userId);

        assertThat(result.isCompleted()).isTrue();
        assertThat(result.getHasIncompleteSubtasks()).isTrue();
    }

    @Test
    void shouldNotSetHasIncompleteSubtasksWhenAllComplete() {
        Context context = buildContext(contextId, userId);
        Task task = buildTask(taskId, context, "Parent", GtdList.NEXT_ACTIONS);
        when(taskRepository.findByIdAndIsDeletedFalse(taskId)).thenReturn(Optional.of(task));
        when(taskRepository.saveAndFlush(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(taskRepository.countByParentTaskIdAndIsCompletedFalseAndIsDeletedFalse(taskId)).thenReturn(0);

        TaskResponse result = taskService.completeTask(taskId, userId);

        assertThat(result.isCompleted()).isTrue();
        assertThat(result.getHasIncompleteSubtasks()).isNull();
    }

    @Test
    void shouldInheritContextFromParentOnCreateSubtask() {
        Context context = buildContext(contextId, userId);
        Task parentTask = buildTask(taskId, context, "Parent", GtdList.NEXT_ACTIONS);
        parentTask.setNestingLevel(2);
        when(taskRepository.findByIdAndIsDeletedFalse(taskId)).thenReturn(Optional.of(parentTask));
        when(taskRepository.countByParentTaskIdAndIsDeletedFalse(taskId)).thenReturn(3);
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task t = invocation.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        CreateTaskRequest request = CreateTaskRequest.builder().title("Sub").build();
        TaskResponse result = taskService.createSubtask(taskId, request, userId);

        assertThat(result.getNestingLevel()).isEqualTo(3);
        assertThat(result.getContextId()).isEqualTo(contextId);
        assertThat(result.getSortOrder()).isEqualTo(3);
    }

    @Test
    void shouldComputeProgressForProjectWith50Percent() {
        Context context = buildContext(contextId, userId);
        Task project = buildTask(taskId, context, "Project", GtdList.PROJECTS);
        UUID sub1Id = UUID.randomUUID();
        UUID sub2Id = UUID.randomUUID();
        Task sub1 = buildTask(sub1Id, context, "Sub 1", GtdList.INBOX);
        sub1.setCompleted(true);
        Task sub2 = buildTask(sub2Id, context, "Sub 2", GtdList.INBOX);

        when(taskRepository.findByParentTaskIdAndIsDeletedFalse(taskId)).thenReturn(List.of(sub1, sub2));
        when(taskRepository.findByParentTaskIdAndIsDeletedFalse(sub1Id)).thenReturn(List.of());
        when(taskRepository.findByParentTaskIdAndIsDeletedFalse(sub2Id)).thenReturn(List.of());

        Integer progress = taskService.computeProgress(taskId);

        assertThat(progress).isEqualTo(50);
    }

    @Test
    void shouldComputeProgressForProjectWith0Percent() {
        Context context = buildContext(contextId, userId);
        Task project = buildTask(taskId, context, "Project", GtdList.PROJECTS);
        UUID sub1Id = UUID.randomUUID();
        UUID sub2Id = UUID.randomUUID();
        Task sub1 = buildTask(sub1Id, context, "Sub 1", GtdList.INBOX);
        Task sub2 = buildTask(sub2Id, context, "Sub 2", GtdList.INBOX);

        when(taskRepository.findByParentTaskIdAndIsDeletedFalse(taskId)).thenReturn(List.of(sub1, sub2));
        when(taskRepository.findByParentTaskIdAndIsDeletedFalse(sub1Id)).thenReturn(List.of());
        when(taskRepository.findByParentTaskIdAndIsDeletedFalse(sub2Id)).thenReturn(List.of());

        Integer progress = taskService.computeProgress(taskId);

        assertThat(progress).isEqualTo(0);
    }

    @Test
    void shouldComputeProgressFor100Percent() {
        Context context = buildContext(contextId, userId);
        UUID sub1Id = UUID.randomUUID();
        Task sub1 = buildTask(sub1Id, context, "Sub 1", GtdList.DONE);
        sub1.setCompleted(true);

        when(taskRepository.findByParentTaskIdAndIsDeletedFalse(taskId)).thenReturn(List.of(sub1));
        when(taskRepository.findByParentTaskIdAndIsDeletedFalse(sub1Id)).thenReturn(List.of());

        Integer progress = taskService.computeProgress(taskId);

        assertThat(progress).isEqualTo(100);
    }

    @Test
    void shouldReturnNullProgressWhenNoSubtasks() {
        when(taskRepository.findByParentTaskIdAndIsDeletedFalse(taskId)).thenReturn(List.of());

        Integer progress = taskService.computeProgress(taskId);

        assertThat(progress).isNull();
    }

    @Test
    void shouldComputeProgressRecursivelyAcrossAllLevels() {
        Context context = buildContext(contextId, userId);
        UUID sub1Id = UUID.randomUUID();
        UUID sub2Id = UUID.randomUUID();
        UUID grandchild1Id = UUID.randomUUID();
        UUID grandchild2Id = UUID.randomUUID();

        Task sub1 = buildTask(sub1Id, context, "Sub 1", GtdList.INBOX);
        sub1.setCompleted(true);
        Task sub2 = buildTask(sub2Id, context, "Sub 2", GtdList.INBOX);

        Task grandchild1 = buildTask(grandchild1Id, context, "Grandchild 1", GtdList.INBOX);
        grandchild1.setCompleted(true);
        Task grandchild2 = buildTask(grandchild2Id, context, "Grandchild 2", GtdList.INBOX);

        when(taskRepository.findByParentTaskIdAndIsDeletedFalse(taskId)).thenReturn(List.of(sub1, sub2));
        when(taskRepository.findByParentTaskIdAndIsDeletedFalse(sub1Id)).thenReturn(List.of());
        when(taskRepository.findByParentTaskIdAndIsDeletedFalse(sub2Id)).thenReturn(List.of(grandchild1, grandchild2));
        when(taskRepository.findByParentTaskIdAndIsDeletedFalse(grandchild1Id)).thenReturn(List.of());
        when(taskRepository.findByParentTaskIdAndIsDeletedFalse(grandchild2Id)).thenReturn(List.of());

        // 4 descendants total: sub1 (completed) + sub2 (not) + grandchild1 (completed) + grandchild2 (not) = 2/4 = 50%
        Integer progress = taskService.computeProgress(taskId);

        assertThat(progress).isEqualTo(50);
    }

    @Test
    void shouldIncludeProgressFieldForProjectTaskInGetTasks() {
        Context context = buildContext(contextId, userId);
        Task project = buildTask(taskId, context, "My Project", GtdList.PROJECTS);

        UUID sub1Id = UUID.randomUUID();
        Task sub1 = buildTask(sub1Id, context, "Sub 1", GtdList.INBOX);
        sub1.setCompleted(true);

        when(contextRepository.findByIdAndIsDeletedFalse(contextId)).thenReturn(Optional.of(context));
        when(taskRepository.findByContextIdAndParentTaskIsNullAndIsDeletedFalseOrderBySortOrderAsc(contextId))
                .thenReturn(List.of(project));
        when(taskRepository.findByParentTaskIdAndIsDeletedFalse(taskId)).thenReturn(List.of(sub1));
        when(taskRepository.findByParentTaskIdAndIsDeletedFalse(sub1Id)).thenReturn(List.of());

        List<TaskResponse> result = taskService.getTasks(contextId, null, userId);

        assertThat(result.get(0).getProgress()).isEqualTo(100);
    }

    @Test
    void shouldNotIncludeProgressForNonProjectTask() {
        Context context = buildContext(contextId, userId);
        Task inboxTask = buildTask(taskId, context, "Inbox task", GtdList.INBOX);

        when(contextRepository.findByIdAndIsDeletedFalse(contextId)).thenReturn(Optional.of(context));
        when(taskRepository.findByContextIdAndParentTaskIsNullAndIsDeletedFalseOrderBySortOrderAsc(contextId))
                .thenReturn(List.of(inboxTask));

        List<TaskResponse> result = taskService.getTasks(contextId, null, userId);

        assertThat(result.get(0).getProgress()).isNull();
    }

    @Test
    void shouldGetTaskCountsGroupedByGtdList() {
        Context context = buildContext(contextId, userId);
        when(contextRepository.findByIdAndIsDeletedFalse(contextId)).thenReturn(Optional.of(context));
        when(taskRepository.countByContextIdGroupedByGtdList(contextId)).thenReturn(List.of(
                new Object[]{GtdList.INBOX, 5L},
                new Object[]{GtdList.NEXT_ACTIONS, 3L},
                new Object[]{GtdList.PROJECTS, 2L}
        ));
        when(taskRepository.countByContextIdGroupedByCategory(contextId)).thenReturn(List.of());
        when(taskRepository.countByContextIdAndIsDeletedFalse(contextId)).thenReturn(10);

        TaskCountsResponse result = taskService.getTaskCounts(contextId, userId);

        assertThat(result.getContextId()).isEqualTo(contextId);
        assertThat(result.getByGtdList().get("INBOX")).isEqualTo(5);
        assertThat(result.getByGtdList().get("NEXT_ACTIONS")).isEqualTo(3);
        assertThat(result.getByGtdList().get("PROJECTS")).isEqualTo(2);
        assertThat(result.getByGtdList().get("DONE")).isEqualTo(0);
        assertThat(result.getTotal()).isEqualTo(10);
    }

    @Test
    void shouldGetTaskCountsGroupedByCategory() {
        Context context = buildContext(contextId, userId);
        UUID cat1Id = UUID.randomUUID();
        UUID cat2Id = UUID.randomUUID();
        when(contextRepository.findByIdAndIsDeletedFalse(contextId)).thenReturn(Optional.of(context));
        when(taskRepository.countByContextIdGroupedByGtdList(contextId)).thenReturn(List.of());
        when(taskRepository.countByContextIdGroupedByCategory(contextId)).thenReturn(List.of(
                new Object[]{cat1Id.toString(), 3L},
                new Object[]{cat2Id.toString(), 7L}
        ));
        when(taskRepository.countByContextIdAndIsDeletedFalse(contextId)).thenReturn(10);

        TaskCountsResponse result = taskService.getTaskCounts(contextId, userId);

        assertThat(result.getByCategory()).hasSize(2);
        assertThat(result.getByCategory().get(cat1Id.toString())).isEqualTo(3);
        assertThat(result.getByCategory().get(cat2Id.toString())).isEqualTo(7);
    }

    @Test
    void shouldThrowContextNotFoundOnGetTaskCounts() {
        when(contextRepository.findByIdAndIsDeletedFalse(contextId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTaskCounts(contextId, userId))
                .isInstanceOf(ContextNotFoundException.class);
    }

    @Test
    void shouldThrowAccessDeniedOnGetTaskCounts() {
        UUID otherUserId = UUID.randomUUID();
        Context context = buildContext(contextId, otherUserId);
        when(contextRepository.findByIdAndIsDeletedFalse(contextId)).thenReturn(Optional.of(context));

        assertThatThrownBy(() -> taskService.getTaskCounts(contextId, userId))
                .isInstanceOf(ContextAccessDeniedException.class);
    }

    private Context buildContext(UUID id, UUID ownerId) {
        User user = User.builder().id(ownerId).build();
        return Context.builder()
                .id(id)
                .user(user)
                .name("Test Context")
                .theme(ContextTheme.FORMAL)
                .icon("icon")
                .sortOrder(0)
                .isDeleted(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void shouldCreateNextInstanceWhenCompletingRecurringTask() {
        Context context = buildContext(contextId, userId);
        Task task = buildTask(taskId, context, "Daily standup", GtdList.NEXT_ACTIONS);
        task.setRecurrenceRule("{\"type\":\"daily\",\"time\":\"09:00\"}");
        task.setCategoryId(UUID.randomUUID());

        when(taskRepository.findByIdAndIsDeletedFalse(taskId)).thenReturn(Optional.of(task));
        when(taskRepository.countByParentTaskIdAndIsCompletedFalseAndIsDeletedFalse(taskId)).thenReturn(0);
        when(taskRepository.countByContextIdAndIsDeletedFalse(contextId)).thenReturn(5);
        when(reminderRepository.findByTaskIdOrderByRemindAtAsc(taskId)).thenReturn(List.of());

        UUID nextId = UUID.randomUUID();
        when(taskRepository.saveAndFlush(any(Task.class))).thenAnswer(invocation -> {
            Task t = invocation.getArgument(0);
            if (t.getId() == null) {
                t.setId(nextId);
            }
            return t;
        });

        TaskResponse result = taskService.completeTask(taskId, userId);

        assertThat(result.isCompleted()).isTrue();
        assertThat(result.getGtdList()).isEqualTo(GtdList.DONE);
        assertThat(result.getNextInstanceId()).isEqualTo(nextId);
        assertThat(result.getIsRecurring()).isTrue();
    }

    @Test
    void shouldNotCreateNextInstanceWhenCompletingNonRecurringTask() {
        Context context = buildContext(contextId, userId);
        Task task = buildTask(taskId, context, "One-off task", GtdList.INBOX);

        when(taskRepository.findByIdAndIsDeletedFalse(taskId)).thenReturn(Optional.of(task));
        when(taskRepository.saveAndFlush(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(taskRepository.countByParentTaskIdAndIsCompletedFalseAndIsDeletedFalse(taskId)).thenReturn(0);

        TaskResponse result = taskService.completeTask(taskId, userId);

        assertThat(result.isCompleted()).isTrue();
        assertThat(result.getNextInstanceId()).isNull();
        assertThat(result.getIsRecurring()).isNull();
        verify(reminderRepository, never()).findByTaskIdOrderByRemindAtAsc(any());
    }

    @Test
    void shouldPreserveOriginalGtdListOnNextInstance() {
        Context context = buildContext(contextId, userId);
        Task task = buildTask(taskId, context, "Weekly review", GtdList.NEXT_ACTIONS);
        task.setRecurrenceRule("{\"type\":\"weekly\",\"dayOfWeek\":\"FRIDAY\"}");

        when(taskRepository.findByIdAndIsDeletedFalse(taskId)).thenReturn(Optional.of(task));
        when(taskRepository.countByParentTaskIdAndIsCompletedFalseAndIsDeletedFalse(taskId)).thenReturn(0);
        when(taskRepository.countByContextIdAndIsDeletedFalse(contextId)).thenReturn(3);
        when(reminderRepository.findByTaskIdOrderByRemindAtAsc(taskId)).thenReturn(List.of());

        UUID nextId = UUID.randomUUID();
        when(taskRepository.saveAndFlush(any(Task.class))).thenAnswer(invocation -> {
            Task t = invocation.getArgument(0);
            if (t.getId() == null) {
                t.setId(nextId);
                t.setCreatedAt(Instant.now());
                t.setUpdatedAt(Instant.now());
            }
            return t;
        });

        taskService.completeTask(taskId, userId);

        verify(taskRepository, times(2)).saveAndFlush(any(Task.class));
    }

    @Test
    void shouldCopyRemindersToNextRecurringInstance() {
        Context context = buildContext(contextId, userId);
        Task task = buildTask(taskId, context, "Daily standup", GtdList.NEXT_ACTIONS);
        task.setRecurrenceRule("{\"type\":\"daily\"}");

        Reminder reminder1 = Reminder.builder()
                .id(UUID.randomUUID())
                .task(task)
                .remindAt(Instant.parse("2026-06-01T08:00:00Z"))
                .offsetType(ReminderOffsetType.HOURS_BEFORE)
                .offsetValue(1)
                .build();
        Reminder reminder2 = Reminder.builder()
                .id(UUID.randomUUID())
                .task(task)
                .remindAt(Instant.parse("2026-06-01T08:30:00Z"))
                .build();

        when(taskRepository.findByIdAndIsDeletedFalse(taskId)).thenReturn(Optional.of(task));
        when(taskRepository.countByParentTaskIdAndIsCompletedFalseAndIsDeletedFalse(taskId)).thenReturn(0);
        when(taskRepository.countByContextIdAndIsDeletedFalse(contextId)).thenReturn(0);
        when(reminderRepository.findByTaskIdOrderByRemindAtAsc(taskId)).thenReturn(List.of(reminder1, reminder2));
        when(reminderRepository.save(any(Reminder.class))).thenAnswer(invocation -> {
            Reminder r = invocation.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        UUID nextId = UUID.randomUUID();
        when(taskRepository.saveAndFlush(any(Task.class))).thenAnswer(invocation -> {
            Task t = invocation.getArgument(0);
            if (t.getId() == null) {
                t.setId(nextId);
            }
            return t;
        });

        TaskResponse result = taskService.completeTask(taskId, userId);

        assertThat(result.getNextInstanceId()).isEqualTo(nextId);
        verify(reminderRepository, times(2)).save(any(Reminder.class));
    }

    @Test
    void shouldInheritRecurrenceRuleInNextInstance() {
        Context context = buildContext(contextId, userId);
        Task task = buildTask(taskId, context, "Recurring", GtdList.INBOX);
        String rule = "{\"type\":\"daily\",\"time\":\"09:00\"}";
        task.setRecurrenceRule(rule);
        task.setNotes("Some notes");
        UUID catId = UUID.randomUUID();
        task.setCategoryId(catId);

        when(taskRepository.findByIdAndIsDeletedFalse(taskId)).thenReturn(Optional.of(task));
        when(taskRepository.countByParentTaskIdAndIsCompletedFalseAndIsDeletedFalse(taskId)).thenReturn(0);
        when(taskRepository.countByContextIdAndIsDeletedFalse(contextId)).thenReturn(0);
        when(reminderRepository.findByTaskIdOrderByRemindAtAsc(taskId)).thenReturn(List.of());

        UUID nextId = UUID.randomUUID();
        when(taskRepository.saveAndFlush(any(Task.class))).thenAnswer(invocation -> {
            Task t = invocation.getArgument(0);
            if (t.getId() == null) {
                t.setId(nextId);
                t.setCreatedAt(Instant.now());
                t.setUpdatedAt(Instant.now());
                assertThat(t.getRecurrenceRule()).isEqualTo(rule);
                assertThat(t.getTitle()).isEqualTo("Recurring");
                assertThat(t.getNotes()).isEqualTo("Some notes");
                assertThat(t.getCategoryId()).isEqualTo(catId);
                assertThat(t.isCompleted()).isFalse();
                assertThat(t.getGtdList()).isEqualTo(GtdList.INBOX);
            }
            return t;
        });

        taskService.completeTask(taskId, userId);
    }

    @Test
    void shouldCreateTaskWithRecurrenceRule() {
        Context context = buildContext(contextId, userId);
        when(contextRepository.findByIdAndIsDeletedFalse(contextId)).thenReturn(Optional.of(context));
        when(taskRepository.countByContextIdAndIsDeletedFalse(contextId)).thenReturn(0);
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task t = invocation.getArgument(0);
            t.setId(taskId);
            t.setCreatedAt(Instant.now());
            t.setUpdatedAt(Instant.now());
            return t;
        });

        CreateTaskRequest request = CreateTaskRequest.builder()
                .title("Daily standup")
                .recurrenceRule("{\"type\":\"daily\",\"time\":\"09:00\"}")
                .build();

        TaskResponse result = taskService.createTask(contextId, request, userId);

        assertThat(result.getRecurrenceRule()).isEqualTo("{\"type\":\"daily\",\"time\":\"09:00\"}");
    }

    @Test
    void shouldUpdateRecurrenceRule() {
        Context context = buildContext(contextId, userId);
        Task task = buildTask(taskId, context, "My task", GtdList.INBOX);
        when(taskRepository.findByIdAndIsDeletedFalse(taskId)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setRecurrenceRule("{\"type\":\"weekly\"}");

        TaskResponse result = taskService.updateTask(taskId, request, userId);

        assertThat(result.getRecurrenceRule()).isEqualTo("{\"type\":\"weekly\"}");
    }

    @Test
    void shouldStopRecurrenceWhenSetToEmptyString() {
        Context context = buildContext(contextId, userId);
        Task task = buildTask(taskId, context, "Recurring task", GtdList.INBOX);
        task.setRecurrenceRule("{\"type\":\"daily\"}");
        when(taskRepository.findByIdAndIsDeletedFalse(taskId)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setRecurrenceRule("");

        TaskResponse result = taskService.updateTask(taskId, request, userId);

        assertThat(result.getRecurrenceRule()).isNull();
    }

    @Test
    void shouldNotChangeRecurrenceRuleWhenNotProvided() {
        Context context = buildContext(contextId, userId);
        Task task = buildTask(taskId, context, "Recurring task", GtdList.INBOX);
        task.setRecurrenceRule("{\"type\":\"daily\"}");
        when(taskRepository.findByIdAndIsDeletedFalse(taskId)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateTaskRequest request = UpdateTaskRequest.builder()
                .title("Updated title")
                .build();

        TaskResponse result = taskService.updateTask(taskId, request, userId);

        assertThat(result.getRecurrenceRule()).isEqualTo("{\"type\":\"daily\"}");
        assertThat(result.getTitle()).isEqualTo("Updated title");
    }

    private Task buildTask(UUID id, Context context, String title, GtdList gtdList) {
        return Task.builder()
                .id(id)
                .context(context)
                .title(title)
                .gtdList(gtdList)
                .nestingLevel(1)
                .sortOrder(0)
                .isCompleted(false)
                .isDeleted(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .version(0)
                .build();
    }
}
