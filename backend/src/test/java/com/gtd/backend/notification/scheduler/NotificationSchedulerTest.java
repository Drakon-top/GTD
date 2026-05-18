package com.gtd.backend.notification.scheduler;

import com.gtd.backend.auth.model.User;
import com.gtd.backend.config.RabbitMQProperties;
import com.gtd.backend.context.model.Context;
import com.gtd.backend.notification.dto.DeadlineNotification;
import com.gtd.backend.notification.dto.NotificationType;
import com.gtd.backend.notification.dto.ReminderNotification;
import com.gtd.backend.reminder.model.Reminder;
import com.gtd.backend.reminder.repository.ReminderRepository;
import com.gtd.backend.task.model.Task;
import com.gtd.backend.task.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationSchedulerTest {

    @Mock
    private ReminderRepository reminderRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private RabbitMQProperties rabbitMQProperties;

    @InjectMocks
    private NotificationScheduler scheduler;

    @Captor
    private ArgumentCaptor<ReminderNotification> reminderCaptor;

    @Captor
    private ArgumentCaptor<DeadlineNotification> deadlineCaptor;

    private User user;
    private Context context;

    @BeforeEach
    void setUp() {
        user = User.builder().id(UUID.randomUUID()).email("test@test.com").build();
        context = Context.builder().id(UUID.randomUUID()).user(user).build();
    }

    @Test
    void checkReminders_shouldSendNotificationForPendingReminder() {
        Task task = Task.builder()
                .id(UUID.randomUUID())
                .context(context)
                .title("Buy groceries")
                .build();

        Instant remindAt = Instant.now().minus(5, ChronoUnit.MINUTES);
        Reminder reminder = Reminder.builder()
                .id(UUID.randomUUID())
                .task(task)
                .remindAt(remindAt)
                .isSent(false)
                .build();

        when(reminderRepository.findPendingReminders(any(Instant.class)))
                .thenReturn(List.of(reminder));
        when(rabbitMQProperties.getExchange()).thenReturn("notification.exchange");
        when(rabbitMQProperties.getReminderRoutingKey()).thenReturn("notification.reminder");

        scheduler.checkReminders();

        verify(rabbitTemplate).convertAndSend(
                eq("notification.exchange"),
                eq("notification.reminder"),
                reminderCaptor.capture());

        ReminderNotification sent = reminderCaptor.getValue();
        assertThat(sent.getReminderId()).isEqualTo(reminder.getId());
        assertThat(sent.getTaskId()).isEqualTo(task.getId());
        assertThat(sent.getUserId()).isEqualTo(user.getId());
        assertThat(sent.getTaskTitle()).isEqualTo("Buy groceries");
        assertThat(sent.getRemindAt()).isEqualTo(remindAt);
        assertThat(sent.getType()).isEqualTo(NotificationType.REMINDER);

        verify(reminderRepository).save(reminder);
        assertThat(reminder.isSent()).isTrue();
    }

    @Test
    void checkReminders_shouldProcessMultipleReminders() {
        Task task1 = Task.builder().id(UUID.randomUUID()).context(context).title("Task 1").build();
        Task task2 = Task.builder().id(UUID.randomUUID()).context(context).title("Task 2").build();

        Reminder r1 = Reminder.builder().id(UUID.randomUUID()).task(task1)
                .remindAt(Instant.now().minus(10, ChronoUnit.MINUTES)).isSent(false).build();
        Reminder r2 = Reminder.builder().id(UUID.randomUUID()).task(task2)
                .remindAt(Instant.now().minus(5, ChronoUnit.MINUTES)).isSent(false).build();

        when(reminderRepository.findPendingReminders(any(Instant.class)))
                .thenReturn(List.of(r1, r2));
        when(rabbitMQProperties.getExchange()).thenReturn("notification.exchange");
        when(rabbitMQProperties.getReminderRoutingKey()).thenReturn("notification.reminder");

        scheduler.checkReminders();

        verify(rabbitTemplate, times(2)).convertAndSend(
                eq("notification.exchange"), eq("notification.reminder"), any(ReminderNotification.class));
        verify(reminderRepository, times(2)).save(any(Reminder.class));
        assertThat(r1.isSent()).isTrue();
        assertThat(r2.isSent()).isTrue();
    }

    @Test
    void checkReminders_shouldDoNothingWhenNoPendingReminders() {
        when(reminderRepository.findPendingReminders(any(Instant.class)))
                .thenReturn(Collections.emptyList());

        scheduler.checkReminders();

        verify(rabbitTemplate, never()).convertAndSend(any(String.class), any(String.class), any(Object.class));
        verify(reminderRepository, never()).save(any(Reminder.class));
    }

    @Test
    void checkReminders_shouldContinueProcessingAfterFailure() {
        Task task1 = Task.builder().id(UUID.randomUUID()).context(context).title("Task 1").build();
        Task task2 = Task.builder().id(UUID.randomUUID()).context(context).title("Task 2").build();

        Reminder r1 = Reminder.builder().id(UUID.randomUUID()).task(task1)
                .remindAt(Instant.now().minus(10, ChronoUnit.MINUTES)).isSent(false).build();
        Reminder r2 = Reminder.builder().id(UUID.randomUUID()).task(task2)
                .remindAt(Instant.now().minus(5, ChronoUnit.MINUTES)).isSent(false).build();

        when(reminderRepository.findPendingReminders(any(Instant.class)))
                .thenReturn(List.of(r1, r2));
        when(rabbitMQProperties.getExchange()).thenReturn("notification.exchange");
        when(rabbitMQProperties.getReminderRoutingKey()).thenReturn("notification.reminder");

        doThrow(new RuntimeException("RabbitMQ down"))
                .doNothing()
                .when(rabbitTemplate).convertAndSend(eq("notification.exchange"), eq("notification.reminder"), any(ReminderNotification.class));

        scheduler.checkReminders();

        verify(rabbitTemplate, times(2)).convertAndSend(
                eq("notification.exchange"), eq("notification.reminder"), any(ReminderNotification.class));
        assertThat(r1.isSent()).isFalse();
        assertThat(r2.isSent()).isTrue();
    }

    @Test
    void checkDeadlines_shouldSendNotificationForUpcomingDeadline() {
        Instant dueDate = Instant.now().plus(12, ChronoUnit.HOURS);
        Task task = Task.builder()
                .id(UUID.randomUUID())
                .context(context)
                .title("Submit report")
                .dueDate(dueDate)
                .build();

        when(taskRepository.findTasksWithUpcomingDeadlines(any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(task));
        when(rabbitMQProperties.getExchange()).thenReturn("notification.exchange");
        when(rabbitMQProperties.getDeadlineRoutingKey()).thenReturn("notification.deadline");

        scheduler.checkDeadlines();

        verify(rabbitTemplate).convertAndSend(
                eq("notification.exchange"),
                eq("notification.deadline"),
                deadlineCaptor.capture());

        DeadlineNotification sent = deadlineCaptor.getValue();
        assertThat(sent.getTaskId()).isEqualTo(task.getId());
        assertThat(sent.getUserId()).isEqualTo(user.getId());
        assertThat(sent.getTaskTitle()).isEqualTo("Submit report");
        assertThat(sent.getDueDate()).isEqualTo(dueDate);
        assertThat(sent.getHoursUntilDeadline()).isBetween(11L, 12L);
        assertThat(sent.getType()).isEqualTo(NotificationType.DEADLINE);
    }

    @Test
    void checkDeadlines_shouldNotSendDuplicateForSameTask() {
        Instant dueDate = Instant.now().plus(12, ChronoUnit.HOURS);
        Task task = Task.builder()
                .id(UUID.randomUUID())
                .context(context)
                .title("Submit report")
                .dueDate(dueDate)
                .build();

        when(taskRepository.findTasksWithUpcomingDeadlines(any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(task));
        when(rabbitMQProperties.getExchange()).thenReturn("notification.exchange");
        when(rabbitMQProperties.getDeadlineRoutingKey()).thenReturn("notification.deadline");

        scheduler.checkDeadlines();
        scheduler.checkDeadlines();

        verify(rabbitTemplate, times(1)).convertAndSend(
                eq("notification.exchange"), eq("notification.deadline"), any(DeadlineNotification.class));
    }

    @Test
    void checkDeadlines_shouldDoNothingWhenNoUpcomingDeadlines() {
        when(taskRepository.findTasksWithUpcomingDeadlines(any(Instant.class), any(Instant.class)))
                .thenReturn(Collections.emptyList());

        scheduler.checkDeadlines();

        verify(rabbitTemplate, never()).convertAndSend(any(String.class), any(String.class), any(Object.class));
    }

    @Test
    void checkDeadlines_shouldProcessMultipleTasks() {
        Task task1 = Task.builder().id(UUID.randomUUID()).context(context).title("Task 1")
                .dueDate(Instant.now().plus(6, ChronoUnit.HOURS)).build();
        Task task2 = Task.builder().id(UUID.randomUUID()).context(context).title("Task 2")
                .dueDate(Instant.now().plus(20, ChronoUnit.HOURS)).build();

        when(taskRepository.findTasksWithUpcomingDeadlines(any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(task1, task2));
        when(rabbitMQProperties.getExchange()).thenReturn("notification.exchange");
        when(rabbitMQProperties.getDeadlineRoutingKey()).thenReturn("notification.deadline");

        scheduler.checkDeadlines();

        verify(rabbitTemplate, times(2)).convertAndSend(
                eq("notification.exchange"), eq("notification.deadline"), any(DeadlineNotification.class));
    }

    @Test
    void checkDeadlines_shouldResendAfterCacheClear() {
        Instant dueDate = Instant.now().plus(12, ChronoUnit.HOURS);
        Task task = Task.builder()
                .id(UUID.randomUUID())
                .context(context)
                .title("Submit report")
                .dueDate(dueDate)
                .build();

        when(taskRepository.findTasksWithUpcomingDeadlines(any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(task));
        when(rabbitMQProperties.getExchange()).thenReturn("notification.exchange");
        when(rabbitMQProperties.getDeadlineRoutingKey()).thenReturn("notification.deadline");

        scheduler.checkDeadlines();
        scheduler.clearDeadlineCache();
        scheduler.checkDeadlines();

        verify(rabbitTemplate, times(2)).convertAndSend(
                eq("notification.exchange"), eq("notification.deadline"), any(DeadlineNotification.class));
    }

    @Test
    void checkDeadlines_shouldContinueProcessingAfterFailure() {
        Task task1 = Task.builder().id(UUID.randomUUID()).context(context).title("Task 1")
                .dueDate(Instant.now().plus(6, ChronoUnit.HOURS)).build();
        Task task2 = Task.builder().id(UUID.randomUUID()).context(context).title("Task 2")
                .dueDate(Instant.now().plus(20, ChronoUnit.HOURS)).build();

        when(taskRepository.findTasksWithUpcomingDeadlines(any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(task1, task2));
        when(rabbitMQProperties.getExchange()).thenReturn("notification.exchange");
        when(rabbitMQProperties.getDeadlineRoutingKey()).thenReturn("notification.deadline");

        doThrow(new RuntimeException("RabbitMQ down"))
                .doNothing()
                .when(rabbitTemplate).convertAndSend(eq("notification.exchange"), eq("notification.deadline"), any(DeadlineNotification.class));

        scheduler.checkDeadlines();

        verify(rabbitTemplate, times(2)).convertAndSend(
                eq("notification.exchange"), eq("notification.deadline"), any(DeadlineNotification.class));
    }

    @Test
    void checkReminders_shouldMarkReminderAsSentBeforeMovingToNext() {
        Task task = Task.builder().id(UUID.randomUUID()).context(context).title("Test").build();
        Reminder reminder = Reminder.builder()
                .id(UUID.randomUUID()).task(task)
                .remindAt(Instant.now().minus(1, ChronoUnit.MINUTES)).isSent(false).build();

        when(reminderRepository.findPendingReminders(any(Instant.class)))
                .thenReturn(List.of(reminder));
        when(rabbitMQProperties.getExchange()).thenReturn("notification.exchange");
        when(rabbitMQProperties.getReminderRoutingKey()).thenReturn("notification.reminder");

        scheduler.checkReminders();

        assertThat(reminder.isSent()).isTrue();
        verify(reminderRepository).save(reminder);
    }
}
