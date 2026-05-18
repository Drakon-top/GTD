package com.gtd.backend.notification.scheduler;

import com.gtd.backend.config.RabbitMQProperties;
import com.gtd.backend.notification.dto.DeadlineNotification;
import com.gtd.backend.notification.dto.NotificationType;
import com.gtd.backend.notification.dto.ReminderNotification;
import com.gtd.backend.reminder.model.Reminder;
import com.gtd.backend.reminder.repository.ReminderRepository;
import com.gtd.backend.task.model.Task;
import com.gtd.backend.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@ConditionalOnBean(ConnectionFactory.class)
@RequiredArgsConstructor
public class NotificationScheduler {

    private final ReminderRepository reminderRepository;
    private final TaskRepository taskRepository;
    private final RabbitTemplate rabbitTemplate;
    private final RabbitMQProperties rabbitMQProperties;

    private final Set<UUID> recentlyNotifiedDeadlines = new HashSet<>();

    @Scheduled(fixedRateString = "${scheduler.reminder-check-ms:60000}")
    @Transactional
    public void checkReminders() {
        Instant now = Instant.now();
        List<Reminder> pendingReminders = reminderRepository.findPendingReminders(now);

        if (pendingReminders.isEmpty()) {
            return;
        }

        log.info("Found {} pending reminders to process", pendingReminders.size());

        for (Reminder reminder : pendingReminders) {
            try {
                Task task = reminder.getTask();
                ReminderNotification notification = ReminderNotification.builder()
                        .reminderId(reminder.getId())
                        .taskId(task.getId())
                        .userId(task.getContext().getUser().getId())
                        .taskTitle(task.getTitle())
                        .remindAt(reminder.getRemindAt())
                        .type(NotificationType.REMINDER)
                        .build();

                rabbitTemplate.convertAndSend(
                        rabbitMQProperties.getExchange(),
                        rabbitMQProperties.getReminderRoutingKey(),
                        notification);

                reminder.setSent(true);
                reminderRepository.save(reminder);

                log.info("Sent reminder notification: reminderId={}, taskId={}", reminder.getId(), task.getId());
            } catch (Exception e) {
                log.error("Failed to send reminder notification for reminderId={}: {}", reminder.getId(), e.getMessage());
            }
        }
    }

    @Scheduled(fixedRateString = "${scheduler.deadline-check-ms:3600000}")
    @Transactional(readOnly = true)
    public void checkDeadlines() {
        Instant now = Instant.now();
        Instant deadline = now.plus(Duration.ofHours(24));
        List<Task> tasks = taskRepository.findTasksWithUpcomingDeadlines(now, deadline);

        Set<UUID> currentBatch = new HashSet<>();
        int sent = 0;

        for (Task task : tasks) {
            if (recentlyNotifiedDeadlines.contains(task.getId())) {
                continue;
            }

            try {
                long hoursUntil = Duration.between(now, task.getDueDate()).toHours();

                DeadlineNotification notification = DeadlineNotification.builder()
                        .taskId(task.getId())
                        .userId(task.getContext().getUser().getId())
                        .taskTitle(task.getTitle())
                        .dueDate(task.getDueDate())
                        .hoursUntilDeadline(hoursUntil)
                        .type(NotificationType.DEADLINE)
                        .build();

                rabbitTemplate.convertAndSend(
                        rabbitMQProperties.getExchange(),
                        rabbitMQProperties.getDeadlineRoutingKey(),
                        notification);

                currentBatch.add(task.getId());
                sent++;

                log.info("Sent deadline notification: taskId={}, hoursUntilDeadline={}", task.getId(), hoursUntil);
            } catch (Exception e) {
                log.error("Failed to send deadline notification for taskId={}: {}", task.getId(), e.getMessage());
            }
        }

        recentlyNotifiedDeadlines.clear();
        recentlyNotifiedDeadlines.addAll(currentBatch);

        if (sent > 0) {
            log.info("Processed {} deadline notifications", sent);
        }
    }

    public void clearDeadlineCache() {
        recentlyNotifiedDeadlines.clear();
    }
}
