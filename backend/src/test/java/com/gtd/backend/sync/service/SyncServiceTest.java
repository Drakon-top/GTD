package com.gtd.backend.sync.service;

import com.gtd.backend.auth.model.User;
import com.gtd.backend.auth.repository.UserRepository;
import com.gtd.backend.context.model.Context;
import com.gtd.backend.sync.dto.SyncChangeRequest;
import com.gtd.backend.sync.dto.SyncLogResponse;
import com.gtd.backend.sync.dto.SyncPullResponse;
import com.gtd.backend.sync.dto.SyncPushRequest;
import com.gtd.backend.sync.dto.SyncPushResponse;
import com.gtd.backend.sync.model.ConflictStatus;
import com.gtd.backend.sync.model.DeviceSource;
import com.gtd.backend.sync.model.SyncEntityType;
import com.gtd.backend.sync.model.SyncLog;
import com.gtd.backend.sync.repository.SyncLogRepository;
import com.gtd.backend.task.model.GtdList;
import com.gtd.backend.task.model.Task;
import com.gtd.backend.task.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SyncServiceTest {

    @Mock
    private SyncLogRepository syncLogRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SyncService syncService;

    private UUID userId;
    private UUID taskId;
    private User user;
    private Task task;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        taskId = UUID.randomUUID();

        user = User.builder().id(userId).email("test@test.com").build();

        Context context = Context.builder()
                .id(UUID.randomUUID())
                .user(user)
                .build();

        task = Task.builder()
                .id(taskId)
                .context(context)
                .title("Original Title")
                .notes("Original Notes")
                .gtdList(GtdList.INBOX)
                .nestingLevel(1)
                .sortOrder(0)
                .version(1)
                .build();
    }

    @Test
    void pushChanges_shouldApplyTitleChange() {
        when(userRepository.getReferenceById(userId)).thenReturn(user);
        when(taskRepository.findByIdAndIsDeletedFalse(taskId)).thenReturn(Optional.of(task));
        when(taskRepository.saveAndFlush(any(Task.class))).thenReturn(task);

        SyncChangeRequest change = SyncChangeRequest.builder()
                .entityType(SyncEntityType.TASK)
                .entityId(taskId)
                .fieldName("title")
                .oldValue("Original Title")
                .newValue("Updated Title")
                .deviceSource(DeviceSource.ANDROID)
                .clientTimestamp(Instant.now())
                .expectedVersion(1)
                .build();

        SyncPushRequest request = SyncPushRequest.builder()
                .changes(List.of(change))
                .build();

        SyncPushResponse response = syncService.pushChanges(request, userId);

        assertThat(response.getAppliedCount()).isEqualTo(1);
        assertThat(response.getConflictCount()).isEqualTo(0);
        assertThat(response.getResults()).hasSize(1);
        assertThat(response.getResults().get(0).isApplied()).isTrue();
        assertThat(task.getTitle()).isEqualTo("Updated Title");
    }

    @Test
    void pushChanges_shouldDetectVersionConflict() {
        when(userRepository.getReferenceById(userId)).thenReturn(user);
        when(taskRepository.findByIdAndIsDeletedFalse(taskId)).thenReturn(Optional.of(task));

        SyncChangeRequest change = SyncChangeRequest.builder()
                .entityType(SyncEntityType.TASK)
                .entityId(taskId)
                .fieldName("title")
                .oldValue("Original Title")
                .newValue("Client Updated Title")
                .deviceSource(DeviceSource.ANDROID)
                .clientTimestamp(Instant.now())
                .expectedVersion(0) // stale version
                .build();

        SyncPushRequest request = SyncPushRequest.builder()
                .changes(List.of(change))
                .build();

        SyncPushResponse response = syncService.pushChanges(request, userId);

        assertThat(response.getAppliedCount()).isEqualTo(0);
        assertThat(response.getConflictCount()).isEqualTo(1);
        assertThat(response.getResults().get(0).isApplied()).isFalse();
        assertThat(response.getResults().get(0).getConflictStatus()).isEqualTo(ConflictStatus.RESOLVED_NOTIFY);
        assertThat(response.getResults().get(0).getServerValue()).isEqualTo("Original Title");
    }

    @Test
    void pushChanges_shouldApplyWithoutVersionCheck() {
        when(userRepository.getReferenceById(userId)).thenReturn(user);
        when(taskRepository.findByIdAndIsDeletedFalse(taskId)).thenReturn(Optional.of(task));
        when(taskRepository.saveAndFlush(any(Task.class))).thenReturn(task);

        SyncChangeRequest change = SyncChangeRequest.builder()
                .entityType(SyncEntityType.TASK)
                .entityId(taskId)
                .fieldName("notes")
                .newValue("New notes content")
                .deviceSource(DeviceSource.WEB)
                .clientTimestamp(Instant.now())
                .build(); // no expectedVersion

        SyncPushRequest request = SyncPushRequest.builder()
                .changes(List.of(change))
                .build();

        SyncPushResponse response = syncService.pushChanges(request, userId);

        assertThat(response.getAppliedCount()).isEqualTo(1);
        assertThat(response.getResults().get(0).isApplied()).isTrue();
        assertThat(task.getNotes()).isEqualTo("New notes content");
    }

    @Test
    void pushChanges_shouldReturnErrorForNotFoundTask() {
        when(userRepository.getReferenceById(userId)).thenReturn(user);
        when(taskRepository.findByIdAndIsDeletedFalse(taskId)).thenReturn(Optional.empty());

        SyncChangeRequest change = SyncChangeRequest.builder()
                .entityType(SyncEntityType.TASK)
                .entityId(taskId)
                .fieldName("title")
                .newValue("New Title")
                .deviceSource(DeviceSource.ANDROID)
                .clientTimestamp(Instant.now())
                .build();

        SyncPushRequest request = SyncPushRequest.builder()
                .changes(List.of(change))
                .build();

        SyncPushResponse response = syncService.pushChanges(request, userId);

        assertThat(response.getAppliedCount()).isEqualTo(0);
        assertThat(response.getResults().get(0).isApplied()).isFalse();
        assertThat(response.getResults().get(0).getError()).contains("Task not found");
    }

    @Test
    void pushChanges_shouldReturnErrorForAccessDenied() {
        UUID otherUserId = UUID.randomUUID();
        User otherUser = User.builder().id(otherUserId).build();

        when(userRepository.getReferenceById(otherUserId)).thenReturn(otherUser);
        when(taskRepository.findByIdAndIsDeletedFalse(taskId)).thenReturn(Optional.of(task));

        SyncChangeRequest change = SyncChangeRequest.builder()
                .entityType(SyncEntityType.TASK)
                .entityId(taskId)
                .fieldName("title")
                .newValue("Hacked Title")
                .deviceSource(DeviceSource.ANDROID)
                .clientTimestamp(Instant.now())
                .build();

        SyncPushRequest request = SyncPushRequest.builder()
                .changes(List.of(change))
                .build();

        SyncPushResponse response = syncService.pushChanges(request, otherUserId);

        assertThat(response.getAppliedCount()).isEqualTo(0);
        assertThat(response.getResults().get(0).isApplied()).isFalse();
        assertThat(response.getResults().get(0).getError()).isEqualTo("Access denied");
    }

    @Test
    void pushChanges_shouldReturnErrorForUnknownField() {
        when(userRepository.getReferenceById(userId)).thenReturn(user);
        when(taskRepository.findByIdAndIsDeletedFalse(taskId)).thenReturn(Optional.of(task));

        SyncChangeRequest change = SyncChangeRequest.builder()
                .entityType(SyncEntityType.TASK)
                .entityId(taskId)
                .fieldName("nonExistentField")
                .newValue("value")
                .deviceSource(DeviceSource.ANDROID)
                .clientTimestamp(Instant.now())
                .build();

        SyncPushRequest request = SyncPushRequest.builder()
                .changes(List.of(change))
                .build();

        SyncPushResponse response = syncService.pushChanges(request, userId);

        assertThat(response.getAppliedCount()).isEqualTo(0);
        assertThat(response.getResults().get(0).getError()).contains("Unknown field");
    }

    @Test
    void pushChanges_shouldApplyGtdListChange() {
        when(userRepository.getReferenceById(userId)).thenReturn(user);
        when(taskRepository.findByIdAndIsDeletedFalse(taskId)).thenReturn(Optional.of(task));
        when(taskRepository.saveAndFlush(any(Task.class))).thenReturn(task);

        SyncChangeRequest change = SyncChangeRequest.builder()
                .entityType(SyncEntityType.TASK)
                .entityId(taskId)
                .fieldName("gtdList")
                .oldValue("INBOX")
                .newValue("NEXT_ACTIONS")
                .deviceSource(DeviceSource.ANDROID)
                .clientTimestamp(Instant.now())
                .expectedVersion(1)
                .build();

        SyncPushRequest request = SyncPushRequest.builder()
                .changes(List.of(change))
                .build();

        SyncPushResponse response = syncService.pushChanges(request, userId);

        assertThat(response.getAppliedCount()).isEqualTo(1);
        assertThat(task.getGtdList()).isEqualTo(GtdList.NEXT_ACTIONS);
    }

    @Test
    void pushChanges_shouldApplyDueDateChange() {
        when(userRepository.getReferenceById(userId)).thenReturn(user);
        when(taskRepository.findByIdAndIsDeletedFalse(taskId)).thenReturn(Optional.of(task));
        when(taskRepository.saveAndFlush(any(Task.class))).thenReturn(task);

        String dueDate = "2026-06-01T10:00:00Z";
        SyncChangeRequest change = SyncChangeRequest.builder()
                .entityType(SyncEntityType.TASK)
                .entityId(taskId)
                .fieldName("dueDate")
                .newValue(dueDate)
                .deviceSource(DeviceSource.WEB)
                .clientTimestamp(Instant.now())
                .build();

        SyncPushRequest request = SyncPushRequest.builder()
                .changes(List.of(change))
                .build();

        SyncPushResponse response = syncService.pushChanges(request, userId);

        assertThat(response.getAppliedCount()).isEqualTo(1);
        assertThat(task.getDueDate()).isEqualTo(Instant.parse(dueDate));
    }

    @Test
    void pushChanges_shouldClearDueDate() {
        task.setDueDate(Instant.now());
        when(userRepository.getReferenceById(userId)).thenReturn(user);
        when(taskRepository.findByIdAndIsDeletedFalse(taskId)).thenReturn(Optional.of(task));
        when(taskRepository.saveAndFlush(any(Task.class))).thenReturn(task);

        SyncChangeRequest change = SyncChangeRequest.builder()
                .entityType(SyncEntityType.TASK)
                .entityId(taskId)
                .fieldName("dueDate")
                .newValue("")
                .deviceSource(DeviceSource.ANDROID)
                .clientTimestamp(Instant.now())
                .build();

        SyncPushRequest request = SyncPushRequest.builder()
                .changes(List.of(change))
                .build();

        SyncPushResponse response = syncService.pushChanges(request, userId);

        assertThat(response.getAppliedCount()).isEqualTo(1);
        assertThat(task.getDueDate()).isNull();
    }

    @Test
    void pushChanges_shouldProcessMultipleChanges() {
        when(userRepository.getReferenceById(userId)).thenReturn(user);
        when(taskRepository.findByIdAndIsDeletedFalse(taskId)).thenReturn(Optional.of(task));
        when(taskRepository.saveAndFlush(any(Task.class))).thenReturn(task);

        SyncChangeRequest change1 = SyncChangeRequest.builder()
                .entityType(SyncEntityType.TASK)
                .entityId(taskId)
                .fieldName("title")
                .newValue("Title 1")
                .deviceSource(DeviceSource.ANDROID)
                .clientTimestamp(Instant.now())
                .build();

        SyncChangeRequest change2 = SyncChangeRequest.builder()
                .entityType(SyncEntityType.TASK)
                .entityId(taskId)
                .fieldName("notes")
                .newValue("Notes 2")
                .deviceSource(DeviceSource.ANDROID)
                .clientTimestamp(Instant.now())
                .build();

        SyncPushRequest request = SyncPushRequest.builder()
                .changes(List.of(change1, change2))
                .build();

        SyncPushResponse response = syncService.pushChanges(request, userId);

        assertThat(response.getAppliedCount()).isEqualTo(2);
        assertThat(response.getResults()).hasSize(2);
    }

    @Test
    void pushChanges_shouldLogSyncEntry() {
        when(userRepository.getReferenceById(userId)).thenReturn(user);
        when(taskRepository.findByIdAndIsDeletedFalse(taskId)).thenReturn(Optional.of(task));
        when(taskRepository.saveAndFlush(any(Task.class))).thenReturn(task);

        SyncChangeRequest change = SyncChangeRequest.builder()
                .entityType(SyncEntityType.TASK)
                .entityId(taskId)
                .fieldName("title")
                .oldValue("Original Title")
                .newValue("New Title")
                .deviceSource(DeviceSource.ANDROID)
                .clientTimestamp(Instant.now())
                .build();

        SyncPushRequest request = SyncPushRequest.builder()
                .changes(List.of(change))
                .build();

        syncService.pushChanges(request, userId);

        ArgumentCaptor<SyncLog> captor = ArgumentCaptor.forClass(SyncLog.class);
        verify(syncLogRepository).save(captor.capture());

        SyncLog saved = captor.getValue();
        assertThat(saved.getEntityType()).isEqualTo(SyncEntityType.TASK);
        assertThat(saved.getEntityId()).isEqualTo(taskId);
        assertThat(saved.getFieldName()).isEqualTo("title");
        assertThat(saved.getOldValue()).isEqualTo("Original Title");
        assertThat(saved.getNewValue()).isEqualTo("New Title");
        assertThat(saved.getDeviceSource()).isEqualTo(DeviceSource.ANDROID);
        assertThat(saved.getConflictStatus()).isEqualTo(ConflictStatus.NO_CONFLICT);
    }

    @Test
    void pushChanges_shouldLogConflictEntry() {
        when(userRepository.getReferenceById(userId)).thenReturn(user);
        when(taskRepository.findByIdAndIsDeletedFalse(taskId)).thenReturn(Optional.of(task));

        SyncChangeRequest change = SyncChangeRequest.builder()
                .entityType(SyncEntityType.TASK)
                .entityId(taskId)
                .fieldName("title")
                .oldValue("Original Title")
                .newValue("Conflicting Title")
                .deviceSource(DeviceSource.ANDROID)
                .clientTimestamp(Instant.now())
                .expectedVersion(0)
                .build();

        SyncPushRequest request = SyncPushRequest.builder()
                .changes(List.of(change))
                .build();

        syncService.pushChanges(request, userId);

        ArgumentCaptor<SyncLog> captor = ArgumentCaptor.forClass(SyncLog.class);
        verify(syncLogRepository).save(captor.capture());

        SyncLog saved = captor.getValue();
        assertThat(saved.getConflictStatus()).isEqualTo(ConflictStatus.RESOLVED_NOTIFY);
    }

    @Test
    void pushChanges_shouldNotSaveLogForNotFoundTask() {
        when(userRepository.getReferenceById(userId)).thenReturn(user);
        when(taskRepository.findByIdAndIsDeletedFalse(taskId)).thenReturn(Optional.empty());

        SyncChangeRequest change = SyncChangeRequest.builder()
                .entityType(SyncEntityType.TASK)
                .entityId(taskId)
                .fieldName("title")
                .newValue("New Title")
                .deviceSource(DeviceSource.ANDROID)
                .clientTimestamp(Instant.now())
                .build();

        SyncPushRequest request = SyncPushRequest.builder()
                .changes(List.of(change))
                .build();

        syncService.pushChanges(request, userId);

        verify(syncLogRepository, never()).save(any());
    }

    @Test
    void pullChanges_shouldReturnChangesAfterTimestamp() {
        Instant since = Instant.parse("2026-05-01T00:00:00Z");

        SyncLog log1 = SyncLog.builder()
                .id(UUID.randomUUID())
                .user(user)
                .entityType(SyncEntityType.TASK)
                .entityId(taskId)
                .fieldName("title")
                .oldValue("Old")
                .newValue("New")
                .deviceSource(DeviceSource.WEB)
                .conflictStatus(ConflictStatus.NO_CONFLICT)
                .createdAt(Instant.parse("2026-05-02T00:00:00Z"))
                .build();

        when(syncLogRepository.findByUserIdAndCreatedAtAfter(userId, since)).thenReturn(List.of(log1));

        SyncPullResponse response = syncService.pullChanges(since, userId);

        assertThat(response.getChangeCount()).isEqualTo(1);
        assertThat(response.getChanges()).hasSize(1);
        SyncLogResponse entry = response.getChanges().get(0);
        assertThat(entry.getEntityType()).isEqualTo(SyncEntityType.TASK);
        assertThat(entry.getEntityId()).isEqualTo(taskId);
        assertThat(entry.getFieldName()).isEqualTo("title");
        assertThat(entry.getOldValue()).isEqualTo("Old");
        assertThat(entry.getNewValue()).isEqualTo("New");
        assertThat(entry.getDeviceSource()).isEqualTo(DeviceSource.WEB);
    }

    @Test
    void pullChanges_shouldReturnEmptyWhenNoChanges() {
        Instant since = Instant.now();
        when(syncLogRepository.findByUserIdAndCreatedAtAfter(userId, since)).thenReturn(List.of());

        SyncPullResponse response = syncService.pullChanges(since, userId);

        assertThat(response.getChangeCount()).isEqualTo(0);
        assertThat(response.getChanges()).isEmpty();
        assertThat(response.getServerTimestamp()).isNotNull();
    }

    @Test
    void pushChanges_shouldReturnNotImplementedForContextType() {
        when(userRepository.getReferenceById(userId)).thenReturn(user);

        SyncChangeRequest change = SyncChangeRequest.builder()
                .entityType(SyncEntityType.CONTEXT)
                .entityId(UUID.randomUUID())
                .fieldName("name")
                .newValue("New name")
                .deviceSource(DeviceSource.WEB)
                .clientTimestamp(Instant.now())
                .build();

        SyncPushRequest request = SyncPushRequest.builder()
                .changes(List.of(change))
                .build();

        SyncPushResponse response = syncService.pushChanges(request, userId);

        assertThat(response.getAppliedCount()).isEqualTo(0);
        assertThat(response.getResults().get(0).getError()).contains("not yet implemented");
    }

    @Test
    void getTaskFieldValue_shouldReturnCorrectValues() {
        task.setDueDate(Instant.parse("2026-06-01T10:00:00Z"));
        task.setCategoryId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        task.setRecurrenceRule("{\"type\":\"daily\"}");

        assertThat(syncService.getTaskFieldValue(task, "title")).isEqualTo("Original Title");
        assertThat(syncService.getTaskFieldValue(task, "notes")).isEqualTo("Original Notes");
        assertThat(syncService.getTaskFieldValue(task, "gtdList")).isEqualTo("INBOX");
        assertThat(syncService.getTaskFieldValue(task, "dueDate")).isEqualTo("2026-06-01T10:00:00Z");
        assertThat(syncService.getTaskFieldValue(task, "categoryId")).isEqualTo("11111111-1111-1111-1111-111111111111");
        assertThat(syncService.getTaskFieldValue(task, "sortOrder")).isEqualTo("0");
        assertThat(syncService.getTaskFieldValue(task, "recurrenceRule")).isEqualTo("{\"type\":\"daily\"}");
        assertThat(syncService.getTaskFieldValue(task, "unknown")).isNull();
    }
}
