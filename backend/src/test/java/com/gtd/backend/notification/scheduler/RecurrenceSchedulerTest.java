package com.gtd.backend.notification.scheduler;

import com.gtd.backend.auth.model.User;
import com.gtd.backend.config.RabbitMQProperties;
import com.gtd.backend.context.model.Context;
import com.gtd.backend.notification.dto.NotificationType;
import com.gtd.backend.notification.dto.RecurrenceNotification;
import com.gtd.backend.reminder.model.Reminder;
import com.gtd.backend.reminder.model.ReminderOffsetType;
import com.gtd.backend.reminder.repository.ReminderRepository;
import com.gtd.backend.task.model.GtdList;
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecurrenceSchedulerTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ReminderRepository reminderRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private RabbitMQProperties rabbitMQProperties;

    @InjectMocks
    private RecurrenceScheduler scheduler;

    @Captor
    private ArgumentCaptor<RecurrenceNotification> notificationCaptor;

    @Captor
    private ArgumentCaptor<Task> taskCaptor;

    private User user;
    private Context context;

    @BeforeEach
    void setUp() {
        user = User.builder().id(UUID.randomUUID()).email("test@test.com").build();
        context = Context.builder().id(UUID.randomUUID()).user(user).build();
    }

    @Test
    void processRecurringTasks_shouldCreateNewInstanceAndMarkOldAsOverdue() {
        Task recurring = buildRecurringTask("Daily standup", GtdList.NEXT_ACTIONS, "{\"type\":\"daily\"}");

        when(taskRepository.findActiveRecurringTasks()).thenReturn(List.of(recurring));
        when(taskRepository.countByContextIdAndIsDeletedFalse(context.getId())).thenReturn(5);
        when(taskRepository.saveAndFlush(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            if (t.getId() == null) {
                t.setId(UUID.randomUUID());
            }
            return t;
        });
        when(reminderRepository.findByTaskIdOrderByRemindAtAsc(recurring.getId()))
                .thenReturn(Collections.emptyList());
        when(rabbitMQProperties.getExchange()).thenReturn("notification.exchange");
        when(rabbitMQProperties.getRecurrenceRoutingKey()).thenReturn("notification.recurrence");

        scheduler.processRecurringTasks();

        verify(taskRepository, times(2)).saveAndFlush(taskCaptor.capture());
        List<Task> savedTasks = taskCaptor.getAllValues();

        Task newInstance = savedTasks.get(0);
        assertThat(newInstance.getTitle()).isEqualTo("Daily standup");
        assertThat(newInstance.getGtdList()).isEqualTo(GtdList.NEXT_ACTIONS);
        assertThat(newInstance.getRecurrenceRule()).isEqualTo("{\"type\":\"daily\"}");
        assertThat(newInstance.getContext()).isEqualTo(context);
        assertThat(newInstance.isCompleted()).isFalse();
        assertThat(newInstance.getSortOrder()).isEqualTo(5);

        Task overdue = savedTasks.get(1);
        assertThat(overdue.getId()).isEqualTo(recurring.getId());
        assertThat(overdue.isCompleted()).isTrue();
        assertThat(overdue.getCompletedAt()).isNotNull();
        assertThat(overdue.getGtdList()).isEqualTo(GtdList.DONE);
    }

    @Test
    void processRecurringTasks_shouldSendNotification() {
        Task recurring = buildRecurringTask("Weekly review", GtdList.PROJECTS, "{\"type\":\"weekly\"}");

        when(taskRepository.findActiveRecurringTasks()).thenReturn(List.of(recurring));
        when(taskRepository.countByContextIdAndIsDeletedFalse(context.getId())).thenReturn(0);
        when(taskRepository.saveAndFlush(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            if (t.getId() == null) t.setId(UUID.randomUUID());
            return t;
        });
        when(reminderRepository.findByTaskIdOrderByRemindAtAsc(recurring.getId()))
                .thenReturn(Collections.emptyList());
        when(rabbitMQProperties.getExchange()).thenReturn("notification.exchange");
        when(rabbitMQProperties.getRecurrenceRoutingKey()).thenReturn("notification.recurrence");

        scheduler.processRecurringTasks();

        verify(rabbitTemplate).convertAndSend(
                eq("notification.exchange"),
                eq("notification.recurrence"),
                notificationCaptor.capture());

        RecurrenceNotification sent = notificationCaptor.getValue();
        assertThat(sent.getOriginalTaskId()).isEqualTo(recurring.getId());
        assertThat(sent.getNewTaskId()).isNotNull();
        assertThat(sent.getUserId()).isEqualTo(user.getId());
        assertThat(sent.getTaskTitle()).isEqualTo("Weekly review");
        assertThat(sent.getRecurrenceRule()).isEqualTo("{\"type\":\"weekly\"}");
        assertThat(sent.getType()).isEqualTo(NotificationType.RECURRENCE);
    }

    @Test
    void processRecurringTasks_shouldDoNothingWhenNoRecurringTasks() {
        when(taskRepository.findActiveRecurringTasks()).thenReturn(Collections.emptyList());

        scheduler.processRecurringTasks();

        verify(taskRepository, never()).saveAndFlush(any(Task.class));
        verify(rabbitTemplate, never()).convertAndSend(any(String.class), any(String.class), any(Object.class));
    }

    @Test
    void processRecurringTasks_shouldProcessMultipleTasks() {
        Task t1 = buildRecurringTask("Task 1", GtdList.INBOX, "{\"type\":\"daily\"}");
        Task t2 = buildRecurringTask("Task 2", GtdList.CALENDAR, "{\"type\":\"weekly\",\"day\":\"MON\"}");

        when(taskRepository.findActiveRecurringTasks()).thenReturn(List.of(t1, t2));
        when(taskRepository.countByContextIdAndIsDeletedFalse(context.getId())).thenReturn(0);
        when(taskRepository.saveAndFlush(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            if (t.getId() == null) t.setId(UUID.randomUUID());
            return t;
        });
        when(reminderRepository.findByTaskIdOrderByRemindAtAsc(any(UUID.class)))
                .thenReturn(Collections.emptyList());
        when(rabbitMQProperties.getExchange()).thenReturn("notification.exchange");
        when(rabbitMQProperties.getRecurrenceRoutingKey()).thenReturn("notification.recurrence");

        scheduler.processRecurringTasks();

        verify(taskRepository, times(4)).saveAndFlush(any(Task.class));
        verify(rabbitTemplate, times(2)).convertAndSend(
                eq("notification.exchange"),
                eq("notification.recurrence"),
                any(RecurrenceNotification.class));
    }

    @Test
    void processRecurringTasks_shouldContinueAfterFailure() {
        Task t1 = buildRecurringTask("Task 1", GtdList.INBOX, "{\"type\":\"daily\"}");
        Task t2 = buildRecurringTask("Task 2", GtdList.INBOX, "{\"type\":\"daily\"}");

        when(taskRepository.findActiveRecurringTasks()).thenReturn(List.of(t1, t2));
        when(taskRepository.countByContextIdAndIsDeletedFalse(context.getId()))
                .thenThrow(new RuntimeException("DB error"))
                .thenReturn(0);
        when(taskRepository.saveAndFlush(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            if (t.getId() == null) t.setId(UUID.randomUUID());
            return t;
        });
        when(reminderRepository.findByTaskIdOrderByRemindAtAsc(t2.getId()))
                .thenReturn(Collections.emptyList());
        when(rabbitMQProperties.getExchange()).thenReturn("notification.exchange");
        when(rabbitMQProperties.getRecurrenceRoutingKey()).thenReturn("notification.recurrence");

        scheduler.processRecurringTasks();

        verify(rabbitTemplate, times(1)).convertAndSend(
                eq("notification.exchange"),
                eq("notification.recurrence"),
                any(RecurrenceNotification.class));
    }

    @Test
    void processRecurringTasks_shouldCopyReminders() {
        Task recurring = buildRecurringTask("Task with reminders", GtdList.INBOX, "{\"type\":\"daily\"}");

        Reminder r1 = Reminder.builder()
                .id(UUID.randomUUID())
                .task(recurring)
                .remindAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .offsetType(ReminderOffsetType.HOURS_BEFORE)
                .offsetValue(1)
                .isSent(true)
                .build();
        Reminder r2 = Reminder.builder()
                .id(UUID.randomUUID())
                .task(recurring)
                .remindAt(Instant.now().plus(1, ChronoUnit.DAYS))
                .offsetType(ReminderOffsetType.DAYS_BEFORE)
                .offsetValue(1)
                .isSent(false)
                .build();

        when(taskRepository.findActiveRecurringTasks()).thenReturn(List.of(recurring));
        when(taskRepository.countByContextIdAndIsDeletedFalse(context.getId())).thenReturn(0);
        when(taskRepository.saveAndFlush(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            if (t.getId() == null) t.setId(UUID.randomUUID());
            return t;
        });
        when(reminderRepository.findByTaskIdOrderByRemindAtAsc(recurring.getId()))
                .thenReturn(List.of(r1, r2));
        when(rabbitMQProperties.getExchange()).thenReturn("notification.exchange");
        when(rabbitMQProperties.getRecurrenceRoutingKey()).thenReturn("notification.recurrence");

        scheduler.processRecurringTasks();

        ArgumentCaptor<Reminder> reminderCaptor = ArgumentCaptor.forClass(Reminder.class);
        verify(reminderRepository, times(2)).save(reminderCaptor.capture());

        List<Reminder> copiedReminders = reminderCaptor.getAllValues();
        assertThat(copiedReminders).hasSize(2);

        assertThat(copiedReminders.get(0).getRemindAt()).isEqualTo(r1.getRemindAt());
        assertThat(copiedReminders.get(0).getOffsetType()).isEqualTo(ReminderOffsetType.HOURS_BEFORE);
        assertThat(copiedReminders.get(0).getOffsetValue()).isEqualTo(1);
        assertThat(copiedReminders.get(0).isSent()).isFalse();

        assertThat(copiedReminders.get(1).getRemindAt()).isEqualTo(r2.getRemindAt());
        assertThat(copiedReminders.get(1).getOffsetType()).isEqualTo(ReminderOffsetType.DAYS_BEFORE);
    }

    @Test
    void processRecurringTasks_shouldInheritFieldsFromOriginal() {
        UUID categoryId = UUID.randomUUID();
        Instant dueDate = Instant.now().plus(1, ChronoUnit.DAYS);

        Task recurring = Task.builder()
                .id(UUID.randomUUID())
                .context(context)
                .title("Complex task")
                .notes("Important notes")
                .gtdList(GtdList.CALENDAR)
                .categoryId(categoryId)
                .dueDate(dueDate)
                .recurrenceRule("{\"type\":\"weekly\",\"day\":\"FRI\"}")
                .nestingLevel(2)
                .build();

        when(taskRepository.findActiveRecurringTasks()).thenReturn(List.of(recurring));
        when(taskRepository.countByContextIdAndIsDeletedFalse(context.getId())).thenReturn(3);
        when(taskRepository.saveAndFlush(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            if (t.getId() == null) t.setId(UUID.randomUUID());
            return t;
        });
        when(reminderRepository.findByTaskIdOrderByRemindAtAsc(recurring.getId()))
                .thenReturn(Collections.emptyList());
        when(rabbitMQProperties.getExchange()).thenReturn("notification.exchange");
        when(rabbitMQProperties.getRecurrenceRoutingKey()).thenReturn("notification.recurrence");

        scheduler.processRecurringTasks();

        verify(taskRepository, times(2)).saveAndFlush(taskCaptor.capture());
        Task newInstance = taskCaptor.getAllValues().get(0);

        assertThat(newInstance.getContext()).isEqualTo(context);
        assertThat(newInstance.getTitle()).isEqualTo("Complex task");
        assertThat(newInstance.getNotes()).isEqualTo("Important notes");
        assertThat(newInstance.getGtdList()).isEqualTo(GtdList.CALENDAR);
        assertThat(newInstance.getCategoryId()).isEqualTo(categoryId);
        assertThat(newInstance.getDueDate()).isEqualTo(dueDate);
        assertThat(newInstance.getRecurrenceRule()).isEqualTo("{\"type\":\"weekly\",\"day\":\"FRI\"}");
        assertThat(newInstance.getNestingLevel()).isEqualTo(2);
        assertThat(newInstance.getSortOrder()).isEqualTo(3);
    }

    @Test
    void processRecurringTasks_shouldInheritParentTask() {
        Task parentTask = Task.builder().id(UUID.randomUUID()).context(context).title("Parent").build();
        Task recurring = Task.builder()
                .id(UUID.randomUUID())
                .context(context)
                .parentTask(parentTask)
                .title("Sub recurring")
                .recurrenceRule("{\"type\":\"daily\"}")
                .nestingLevel(2)
                .build();

        when(taskRepository.findActiveRecurringTasks()).thenReturn(List.of(recurring));
        when(taskRepository.countByContextIdAndIsDeletedFalse(context.getId())).thenReturn(0);
        when(taskRepository.saveAndFlush(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            if (t.getId() == null) t.setId(UUID.randomUUID());
            return t;
        });
        when(reminderRepository.findByTaskIdOrderByRemindAtAsc(recurring.getId()))
                .thenReturn(Collections.emptyList());
        when(rabbitMQProperties.getExchange()).thenReturn("notification.exchange");
        when(rabbitMQProperties.getRecurrenceRoutingKey()).thenReturn("notification.recurrence");

        scheduler.processRecurringTasks();

        verify(taskRepository, times(2)).saveAndFlush(taskCaptor.capture());
        Task newInstance = taskCaptor.getAllValues().get(0);
        assertThat(newInstance.getParentTask()).isEqualTo(parentTask);
    }

    @Test
    void processRecurringTasks_newInstanceShouldNotBeCompleted() {
        Task recurring = buildRecurringTask("Daily task", GtdList.INBOX, "{\"type\":\"daily\"}");

        when(taskRepository.findActiveRecurringTasks()).thenReturn(List.of(recurring));
        when(taskRepository.countByContextIdAndIsDeletedFalse(context.getId())).thenReturn(0);
        when(taskRepository.saveAndFlush(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            if (t.getId() == null) t.setId(UUID.randomUUID());
            return t;
        });
        when(reminderRepository.findByTaskIdOrderByRemindAtAsc(recurring.getId()))
                .thenReturn(Collections.emptyList());
        when(rabbitMQProperties.getExchange()).thenReturn("notification.exchange");
        when(rabbitMQProperties.getRecurrenceRoutingKey()).thenReturn("notification.recurrence");

        scheduler.processRecurringTasks();

        verify(taskRepository, times(2)).saveAndFlush(taskCaptor.capture());
        Task newInstance = taskCaptor.getAllValues().get(0);
        assertThat(newInstance.isCompleted()).isFalse();
        assertThat(newInstance.getCompletedAt()).isNull();
        assertThat(newInstance.isDeleted()).isFalse();
    }

    @Test
    void processRecurringTasks_shouldNotSendNotificationOnFailure() {
        Task recurring = buildRecurringTask("Task", GtdList.INBOX, "{\"type\":\"daily\"}");

        when(taskRepository.findActiveRecurringTasks()).thenReturn(List.of(recurring));
        when(taskRepository.countByContextIdAndIsDeletedFalse(context.getId()))
                .thenThrow(new RuntimeException("DB error"));

        scheduler.processRecurringTasks();

        verify(rabbitTemplate, never()).convertAndSend(any(String.class), any(String.class), any(Object.class));
    }

    private Task buildRecurringTask(String title, GtdList gtdList, String recurrenceRule) {
        return Task.builder()
                .id(UUID.randomUUID())
                .context(context)
                .title(title)
                .gtdList(gtdList)
                .recurrenceRule(recurrenceRule)
                .nestingLevel(1)
                .build();
    }
}
