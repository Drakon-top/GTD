package com.gtd.backend.notification.scheduler;

import com.gtd.backend.config.RabbitMQProperties;
import com.gtd.backend.notification.dto.NotificationType;
import com.gtd.backend.notification.dto.RecurrenceNotification;
import com.gtd.backend.reminder.model.Reminder;
import com.gtd.backend.reminder.repository.ReminderRepository;
import com.gtd.backend.task.model.GtdList;
import com.gtd.backend.task.model.Task;
import com.gtd.backend.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@ConditionalOnBean(ConnectionFactory.class)
@RequiredArgsConstructor
public class RecurrenceScheduler {

    private final TaskRepository taskRepository;
    private final ReminderRepository reminderRepository;
    private final RabbitTemplate rabbitTemplate;
    private final RabbitMQProperties rabbitMQProperties;

    @Scheduled(cron = "${scheduler.recurrence-cron:0 0 0 * * *}")
    @Transactional
    public void processRecurringTasks() {
        List<Task> recurringTasks = taskRepository.findActiveRecurringTasks();

        if (recurringTasks.isEmpty()) {
            return;
        }

        log.info("Found {} active recurring tasks to process", recurringTasks.size());

        int created = 0;
        for (Task task : recurringTasks) {
            try {
                Task newInstance = createNextInstance(task);
                markAsOverdue(task);

                sendRecurrenceNotification(task, newInstance);
                created++;

                log.info("Created recurring instance: originalTaskId={}, newTaskId={}", task.getId(), newInstance.getId());
            } catch (Exception e) {
                log.error("Failed to process recurring task taskId={}: {}", task.getId(), e.getMessage());
            }
        }

        if (created > 0) {
            log.info("Processed {} recurring task instances", created);
        }
    }

    private Task createNextInstance(Task original) {
        int sortOrder = taskRepository.countByContextIdAndIsDeletedFalse(original.getContext().getId());

        Task nextInstance = Task.builder()
                .context(original.getContext())
                .parentTask(original.getParentTask())
                .title(original.getTitle())
                .notes(original.getNotes())
                .gtdList(original.getGtdList())
                .categoryId(original.getCategoryId())
                .dueDate(original.getDueDate())
                .recurrenceRule(original.getRecurrenceRule())
                .nestingLevel(original.getNestingLevel())
                .sortOrder(sortOrder)
                .build();

        Task saved = taskRepository.saveAndFlush(nextInstance);

        List<Reminder> reminders = reminderRepository.findByTaskIdOrderByRemindAtAsc(original.getId());
        for (Reminder r : reminders) {
            Reminder copy = Reminder.builder()
                    .task(saved)
                    .remindAt(r.getRemindAt())
                    .offsetType(r.getOffsetType())
                    .offsetValue(r.getOffsetValue())
                    .build();
            reminderRepository.save(copy);
        }

        return saved;
    }

    private void markAsOverdue(Task task) {
        task.setGtdList(GtdList.DONE);
        task.setCompleted(true);
        task.setCompletedAt(Instant.now());
        taskRepository.saveAndFlush(task);
    }

    private void sendRecurrenceNotification(Task original, Task newInstance) {
        RecurrenceNotification notification = RecurrenceNotification.builder()
                .originalTaskId(original.getId())
                .newTaskId(newInstance.getId())
                .contextId(original.getContext().getId())
                .userId(original.getContext().getUser().getId())
                .taskTitle(original.getTitle())
                .recurrenceRule(original.getRecurrenceRule())
                .type(NotificationType.RECURRENCE)
                .build();

        rabbitTemplate.convertAndSend(
                rabbitMQProperties.getExchange(),
                rabbitMQProperties.getRecurrenceRoutingKey(),
                notification);
    }
}
