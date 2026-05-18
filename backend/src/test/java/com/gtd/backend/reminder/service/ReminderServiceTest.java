package com.gtd.backend.reminder.service;

import com.gtd.backend.auth.model.User;
import com.gtd.backend.context.model.Context;
import com.gtd.backend.context.model.ContextTheme;
import com.gtd.backend.reminder.dto.CreateReminderRequest;
import com.gtd.backend.reminder.dto.ReminderResponse;
import com.gtd.backend.reminder.dto.UpdateReminderRequest;
import com.gtd.backend.reminder.exception.ReminderAccessDeniedException;
import com.gtd.backend.reminder.exception.ReminderNotFoundException;
import com.gtd.backend.reminder.model.Reminder;
import com.gtd.backend.reminder.model.ReminderOffsetType;
import com.gtd.backend.reminder.repository.ReminderRepository;
import com.gtd.backend.task.exception.TaskAccessDeniedException;
import com.gtd.backend.task.exception.TaskNotFoundException;
import com.gtd.backend.task.model.Task;
import com.gtd.backend.task.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReminderServiceTest {

    @Mock
    private ReminderRepository reminderRepository;

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private ReminderService reminderService;

    private final UUID userId = UUID.randomUUID();
    private final UUID taskId = UUID.randomUUID();
    private final UUID reminderId = UUID.randomUUID();

    @Test
    void shouldReturnRemindersForTask() {
        Task task = buildTask(taskId, userId);
        Reminder reminder1 = buildReminder(UUID.randomUUID(), task, Instant.now().plus(1, ChronoUnit.HOURS));
        Reminder reminder2 = buildReminder(UUID.randomUUID(), task, Instant.now().plus(2, ChronoUnit.HOURS));
        when(taskRepository.findByIdAndIsDeletedFalse(taskId)).thenReturn(Optional.of(task));
        when(reminderRepository.findByTaskIdOrderByRemindAtAsc(taskId)).thenReturn(List.of(reminder1, reminder2));

        List<ReminderResponse> result = reminderService.getReminders(taskId, userId);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTaskId()).isEqualTo(taskId);
    }

    @Test
    void shouldReturnEmptyListWhenNoReminders() {
        Task task = buildTask(taskId, userId);
        when(taskRepository.findByIdAndIsDeletedFalse(taskId)).thenReturn(Optional.of(task));
        when(reminderRepository.findByTaskIdOrderByRemindAtAsc(taskId)).thenReturn(List.of());

        List<ReminderResponse> result = reminderService.getReminders(taskId, userId);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldThrowTaskNotFoundWhenListingReminders() {
        when(taskRepository.findByIdAndIsDeletedFalse(taskId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reminderService.getReminders(taskId, userId))
                .isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    void shouldThrowTaskAccessDeniedWhenListingReminders() {
        UUID otherUserId = UUID.randomUUID();
        Task task = buildTask(taskId, otherUserId);
        when(taskRepository.findByIdAndIsDeletedFalse(taskId)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> reminderService.getReminders(taskId, userId))
                .isInstanceOf(TaskAccessDeniedException.class);
    }

    @Test
    void shouldCreateReminder() {
        Task task = buildTask(taskId, userId);
        Instant remindAt = Instant.now().plus(1, ChronoUnit.DAYS);
        when(taskRepository.findByIdAndIsDeletedFalse(taskId)).thenReturn(Optional.of(task));
        when(reminderRepository.save(any(Reminder.class))).thenAnswer(invocation -> {
            Reminder r = invocation.getArgument(0);
            r.setId(reminderId);
            r.setCreatedAt(Instant.now());
            return r;
        });

        CreateReminderRequest request = CreateReminderRequest.builder()
                .remindAt(remindAt)
                .offsetType(ReminderOffsetType.HOURS_BEFORE)
                .offsetValue(2)
                .build();

        ReminderResponse result = reminderService.createReminder(taskId, request, userId);

        assertThat(result.getId()).isEqualTo(reminderId);
        assertThat(result.getTaskId()).isEqualTo(taskId);
        assertThat(result.getRemindAt()).isEqualTo(remindAt);
        assertThat(result.getOffsetType()).isEqualTo(ReminderOffsetType.HOURS_BEFORE);
        assertThat(result.getOffsetValue()).isEqualTo(2);
        assertThat(result.isSent()).isFalse();
    }

    @Test
    void shouldCreateReminderWithOnlyRemindAt() {
        Task task = buildTask(taskId, userId);
        Instant remindAt = Instant.now().plus(1, ChronoUnit.HOURS);
        when(taskRepository.findByIdAndIsDeletedFalse(taskId)).thenReturn(Optional.of(task));
        when(reminderRepository.save(any(Reminder.class))).thenAnswer(invocation -> {
            Reminder r = invocation.getArgument(0);
            r.setId(reminderId);
            r.setCreatedAt(Instant.now());
            return r;
        });

        CreateReminderRequest request = CreateReminderRequest.builder()
                .remindAt(remindAt)
                .build();

        ReminderResponse result = reminderService.createReminder(taskId, request, userId);

        assertThat(result.getRemindAt()).isEqualTo(remindAt);
        assertThat(result.getOffsetType()).isNull();
        assertThat(result.getOffsetValue()).isNull();
    }

    @Test
    void shouldThrowTaskNotFoundOnCreate() {
        when(taskRepository.findByIdAndIsDeletedFalse(taskId)).thenReturn(Optional.empty());

        CreateReminderRequest request = CreateReminderRequest.builder()
                .remindAt(Instant.now())
                .build();

        assertThatThrownBy(() -> reminderService.createReminder(taskId, request, userId))
                .isInstanceOf(TaskNotFoundException.class);
        verify(reminderRepository, never()).save(any());
    }

    @Test
    void shouldThrowTaskAccessDeniedOnCreate() {
        UUID otherUserId = UUID.randomUUID();
        Task task = buildTask(taskId, otherUserId);
        when(taskRepository.findByIdAndIsDeletedFalse(taskId)).thenReturn(Optional.of(task));

        CreateReminderRequest request = CreateReminderRequest.builder()
                .remindAt(Instant.now())
                .build();

        assertThatThrownBy(() -> reminderService.createReminder(taskId, request, userId))
                .isInstanceOf(TaskAccessDeniedException.class);
        verify(reminderRepository, never()).save(any());
    }

    @Test
    void shouldUpdateReminderRemindAt() {
        Task task = buildTask(taskId, userId);
        Reminder reminder = buildReminder(reminderId, task, Instant.now().plus(1, ChronoUnit.HOURS));
        reminder.setSent(true);
        Instant newRemindAt = Instant.now().plus(3, ChronoUnit.HOURS);
        when(reminderRepository.findById(reminderId)).thenReturn(Optional.of(reminder));
        when(reminderRepository.save(any(Reminder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateReminderRequest request = UpdateReminderRequest.builder()
                .remindAt(newRemindAt)
                .build();

        ReminderResponse result = reminderService.updateReminder(reminderId, request, userId);

        assertThat(result.getRemindAt()).isEqualTo(newRemindAt);
        assertThat(result.isSent()).isFalse();
    }

    @Test
    void shouldUpdateReminderOffsetFields() {
        Task task = buildTask(taskId, userId);
        Reminder reminder = buildReminder(reminderId, task, Instant.now().plus(1, ChronoUnit.HOURS));
        when(reminderRepository.findById(reminderId)).thenReturn(Optional.of(reminder));
        when(reminderRepository.save(any(Reminder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateReminderRequest request = UpdateReminderRequest.builder()
                .offsetType(ReminderOffsetType.DAYS_BEFORE)
                .offsetValue(3)
                .build();

        ReminderResponse result = reminderService.updateReminder(reminderId, request, userId);

        assertThat(result.getOffsetType()).isEqualTo(ReminderOffsetType.DAYS_BEFORE);
        assertThat(result.getOffsetValue()).isEqualTo(3);
        assertThat(result.isSent()).isFalse();
    }

    @Test
    void shouldThrowReminderNotFoundOnUpdate() {
        when(reminderRepository.findById(reminderId)).thenReturn(Optional.empty());

        UpdateReminderRequest request = UpdateReminderRequest.builder()
                .remindAt(Instant.now())
                .build();

        assertThatThrownBy(() -> reminderService.updateReminder(reminderId, request, userId))
                .isInstanceOf(ReminderNotFoundException.class);
        verify(reminderRepository, never()).save(any());
    }

    @Test
    void shouldThrowAccessDeniedOnUpdateWhenNotOwner() {
        UUID otherUserId = UUID.randomUUID();
        Task task = buildTask(taskId, otherUserId);
        Reminder reminder = buildReminder(reminderId, task, Instant.now());
        when(reminderRepository.findById(reminderId)).thenReturn(Optional.of(reminder));

        UpdateReminderRequest request = UpdateReminderRequest.builder()
                .remindAt(Instant.now())
                .build();

        assertThatThrownBy(() -> reminderService.updateReminder(reminderId, request, userId))
                .isInstanceOf(ReminderAccessDeniedException.class);
        verify(reminderRepository, never()).save(any());
    }

    @Test
    void shouldDeleteReminder() {
        Task task = buildTask(taskId, userId);
        Reminder reminder = buildReminder(reminderId, task, Instant.now());
        when(reminderRepository.findById(reminderId)).thenReturn(Optional.of(reminder));

        reminderService.deleteReminder(reminderId, userId);

        verify(reminderRepository).delete(reminder);
    }

    @Test
    void shouldThrowReminderNotFoundOnDelete() {
        when(reminderRepository.findById(reminderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reminderService.deleteReminder(reminderId, userId))
                .isInstanceOf(ReminderNotFoundException.class);
        verify(reminderRepository, never()).delete(any());
    }

    @Test
    void shouldThrowAccessDeniedOnDeleteWhenNotOwner() {
        UUID otherUserId = UUID.randomUUID();
        Task task = buildTask(taskId, otherUserId);
        Reminder reminder = buildReminder(reminderId, task, Instant.now());
        when(reminderRepository.findById(reminderId)).thenReturn(Optional.of(reminder));

        assertThatThrownBy(() -> reminderService.deleteReminder(reminderId, userId))
                .isInstanceOf(ReminderAccessDeniedException.class);
        verify(reminderRepository, never()).delete(any());
    }

    private Task buildTask(UUID id, UUID ownerId) {
        User user = User.builder().id(ownerId).build();
        Context context = Context.builder()
                .id(UUID.randomUUID())
                .user(user)
                .name("Work")
                .theme(ContextTheme.FORMAL)
                .icon("briefcase")
                .sortOrder(0)
                .isDeleted(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        return Task.builder()
                .id(id)
                .context(context)
                .title("Test task")
                .nestingLevel(1)
                .sortOrder(0)
                .isCompleted(false)
                .isDeleted(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .version(0)
                .build();
    }

    private Reminder buildReminder(UUID id, Task task, Instant remindAt) {
        return Reminder.builder()
                .id(id)
                .task(task)
                .remindAt(remindAt)
                .isSent(false)
                .createdAt(Instant.now())
                .build();
    }
}
