package com.gtd.backend.reminder.service;

import com.gtd.backend.reminder.dto.CreateReminderRequest;
import com.gtd.backend.reminder.dto.ReminderResponse;
import com.gtd.backend.reminder.dto.UpdateReminderRequest;
import com.gtd.backend.reminder.exception.ReminderAccessDeniedException;
import com.gtd.backend.reminder.exception.ReminderNotFoundException;
import com.gtd.backend.reminder.model.Reminder;
import com.gtd.backend.reminder.repository.ReminderRepository;
import com.gtd.backend.task.exception.TaskAccessDeniedException;
import com.gtd.backend.task.exception.TaskNotFoundException;
import com.gtd.backend.task.model.Task;
import com.gtd.backend.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReminderService {

    private final ReminderRepository reminderRepository;
    private final TaskRepository taskRepository;

    @Transactional(readOnly = true)
    public List<ReminderResponse> getReminders(UUID taskId, UUID userId) {
        Task task = findTaskOrThrow(taskId);
        verifyTaskOwnership(task, userId);

        return reminderRepository.findByTaskIdOrderByRemindAtAsc(taskId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ReminderResponse createReminder(UUID taskId, CreateReminderRequest request, UUID userId) {
        Task task = findTaskOrThrow(taskId);
        verifyTaskOwnership(task, userId);

        Reminder reminder = Reminder.builder()
                .task(task)
                .remindAt(request.getRemindAt())
                .offsetType(request.getOffsetType())
                .offsetValue(request.getOffsetValue())
                .build();

        Reminder saved = reminderRepository.save(reminder);
        return toResponse(saved);
    }

    @Transactional
    public ReminderResponse updateReminder(UUID reminderId, UpdateReminderRequest request, UUID userId) {
        Reminder reminder = findReminderOrThrow(reminderId);
        verifyReminderOwnership(reminder, userId);

        if (request.getRemindAt() != null) {
            reminder.setRemindAt(request.getRemindAt());
            reminder.setSent(false);
        }
        if (request.getOffsetType() != null) {
            reminder.setOffsetType(request.getOffsetType());
        }
        if (request.getOffsetValue() != null) {
            reminder.setOffsetValue(request.getOffsetValue());
        }

        Reminder saved = reminderRepository.save(reminder);
        return toResponse(saved);
    }

    @Transactional
    public void deleteReminder(UUID reminderId, UUID userId) {
        Reminder reminder = findReminderOrThrow(reminderId);
        verifyReminderOwnership(reminder, userId);
        reminderRepository.delete(reminder);
    }

    private Task findTaskOrThrow(UUID taskId) {
        return taskRepository.findByIdAndIsDeletedFalse(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));
    }

    private Reminder findReminderOrThrow(UUID reminderId) {
        return reminderRepository.findById(reminderId)
                .orElseThrow(() -> new ReminderNotFoundException(reminderId));
    }

    private void verifyTaskOwnership(Task task, UUID userId) {
        if (!task.getContext().getUser().getId().equals(userId)) {
            throw new TaskAccessDeniedException(task.getId());
        }
    }

    private void verifyReminderOwnership(Reminder reminder, UUID userId) {
        if (!reminder.getTask().getContext().getUser().getId().equals(userId)) {
            throw new ReminderAccessDeniedException(reminder.getId());
        }
    }

    private ReminderResponse toResponse(Reminder reminder) {
        return ReminderResponse.builder()
                .id(reminder.getId())
                .taskId(reminder.getTask().getId())
                .remindAt(reminder.getRemindAt())
                .offsetType(reminder.getOffsetType())
                .offsetValue(reminder.getOffsetValue())
                .isSent(reminder.isSent())
                .createdAt(reminder.getCreatedAt())
                .build();
    }
}
