package com.gtd.backend.sync.service;

import com.gtd.backend.auth.model.User;
import com.gtd.backend.auth.repository.UserRepository;
import com.gtd.backend.sync.dto.SyncChangeRequest;
import com.gtd.backend.sync.dto.SyncChangeResult;
import com.gtd.backend.sync.dto.SyncLogResponse;
import com.gtd.backend.sync.dto.SyncPullResponse;
import com.gtd.backend.sync.dto.SyncPushRequest;
import com.gtd.backend.sync.dto.SyncPushResponse;
import com.gtd.backend.sync.model.ConflictStatus;
import com.gtd.backend.sync.model.SyncEntityType;
import com.gtd.backend.sync.model.SyncLog;
import com.gtd.backend.sync.repository.SyncLogRepository;
import com.gtd.backend.task.model.Task;
import com.gtd.backend.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SyncService {

    private final SyncLogRepository syncLogRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    @Transactional
    public SyncPushResponse pushChanges(SyncPushRequest request, UUID userId) {
        User user = userRepository.getReferenceById(userId);
        Instant serverTimestamp = Instant.now();

        List<SyncChangeResult> results = new ArrayList<>();
        int appliedCount = 0;
        int conflictCount = 0;

        for (SyncChangeRequest change : request.getChanges()) {
            SyncChangeResult result = processChange(change, user);
            results.add(result);
            if (result.isApplied()) {
                appliedCount++;
            }
            if (result.getConflictStatus() != null && result.getConflictStatus() != ConflictStatus.NO_CONFLICT) {
                conflictCount++;
            }
        }

        return SyncPushResponse.builder()
                .serverTimestamp(serverTimestamp)
                .results(results)
                .appliedCount(appliedCount)
                .conflictCount(conflictCount)
                .build();
    }

    @Transactional(readOnly = true)
    public SyncPullResponse pullChanges(Instant since, UUID userId) {
        Instant serverTimestamp = Instant.now();
        List<SyncLog> logs = syncLogRepository.findByUserIdAndCreatedAtAfter(userId, since);

        List<SyncLogResponse> changes = logs.stream()
                .map(this::toSyncLogResponse)
                .toList();

        return SyncPullResponse.builder()
                .serverTimestamp(serverTimestamp)
                .changes(changes)
                .changeCount(changes.size())
                .build();
    }

    private SyncChangeResult processChange(SyncChangeRequest change, User user) {
        if (change.getEntityType() == SyncEntityType.TASK) {
            return processTaskChange(change, user);
        }

        return SyncChangeResult.builder()
                .entityType(change.getEntityType())
                .entityId(change.getEntityId())
                .fieldName(change.getFieldName())
                .applied(false)
                .conflictStatus(ConflictStatus.NO_CONFLICT)
                .error("Sync for entity type " + change.getEntityType() + " not yet implemented")
                .build();
    }

    private SyncChangeResult processTaskChange(SyncChangeRequest change, User user) {
        Optional<Task> taskOpt = taskRepository.findByIdAndIsDeletedFalse(change.getEntityId());

        if (taskOpt.isEmpty()) {
            return SyncChangeResult.builder()
                    .entityType(change.getEntityType())
                    .entityId(change.getEntityId())
                    .fieldName(change.getFieldName())
                    .applied(false)
                    .conflictStatus(ConflictStatus.NO_CONFLICT)
                    .error("Task not found: " + change.getEntityId())
                    .build();
        }

        Task task = taskOpt.get();

        if (!task.getContext().getUser().getId().equals(user.getId())) {
            return SyncChangeResult.builder()
                    .entityType(change.getEntityType())
                    .entityId(change.getEntityId())
                    .fieldName(change.getFieldName())
                    .applied(false)
                    .conflictStatus(ConflictStatus.NO_CONFLICT)
                    .error("Access denied")
                    .build();
        }

        if (change.getExpectedVersion() != null && change.getExpectedVersion() != task.getVersion()) {
            String serverValue = getTaskFieldValue(task, change.getFieldName());

            SyncLog logEntry = SyncLog.builder()
                    .user(user)
                    .entityType(change.getEntityType())
                    .entityId(change.getEntityId())
                    .fieldName(change.getFieldName())
                    .oldValue(change.getOldValue())
                    .newValue(change.getNewValue())
                    .deviceSource(change.getDeviceSource())
                    .conflictStatus(ConflictStatus.RESOLVED_NOTIFY)
                    .build();
            syncLogRepository.save(logEntry);

            return SyncChangeResult.builder()
                    .entityType(change.getEntityType())
                    .entityId(change.getEntityId())
                    .fieldName(change.getFieldName())
                    .applied(false)
                    .conflictStatus(ConflictStatus.RESOLVED_NOTIFY)
                    .serverValue(serverValue)
                    .newVersion(task.getVersion())
                    .build();
        }

        String oldValue = getTaskFieldValue(task, change.getFieldName());
        boolean applied = applyTaskFieldChange(task, change.getFieldName(), change.getNewValue());

        if (!applied) {
            return SyncChangeResult.builder()
                    .entityType(change.getEntityType())
                    .entityId(change.getEntityId())
                    .fieldName(change.getFieldName())
                    .applied(false)
                    .conflictStatus(ConflictStatus.NO_CONFLICT)
                    .error("Unknown field: " + change.getFieldName())
                    .build();
        }

        Task saved = taskRepository.saveAndFlush(task);

        SyncLog logEntry = SyncLog.builder()
                .user(user)
                .entityType(change.getEntityType())
                .entityId(change.getEntityId())
                .fieldName(change.getFieldName())
                .oldValue(oldValue)
                .newValue(change.getNewValue())
                .deviceSource(change.getDeviceSource())
                .conflictStatus(ConflictStatus.NO_CONFLICT)
                .build();
        syncLogRepository.save(logEntry);

        return SyncChangeResult.builder()
                .entityType(change.getEntityType())
                .entityId(change.getEntityId())
                .fieldName(change.getFieldName())
                .applied(true)
                .conflictStatus(ConflictStatus.NO_CONFLICT)
                .newVersion(saved.getVersion())
                .build();
    }

    String getTaskFieldValue(Task task, String fieldName) {
        return switch (fieldName) {
            case "title" -> task.getTitle();
            case "notes" -> task.getNotes();
            case "gtdList" -> task.getGtdList() != null ? task.getGtdList().name() : null;
            case "dueDate" -> task.getDueDate() != null ? task.getDueDate().toString() : null;
            case "categoryId" -> task.getCategoryId() != null ? task.getCategoryId().toString() : null;
            case "sortOrder" -> String.valueOf(task.getSortOrder());
            case "recurrenceRule" -> task.getRecurrenceRule();
            default -> null;
        };
    }

    boolean applyTaskFieldChange(Task task, String fieldName, String newValue) {
        switch (fieldName) {
            case "title" -> task.setTitle(newValue != null ? newValue.trim() : task.getTitle());
            case "notes" -> task.setNotes(newValue);
            case "gtdList" -> {
                if (newValue != null) {
                    task.setGtdList(com.gtd.backend.task.model.GtdList.valueOf(newValue));
                }
            }
            case "dueDate" -> {
                if (newValue != null && !newValue.isBlank()) {
                    task.setDueDate(Instant.parse(newValue));
                } else {
                    task.setDueDate(null);
                }
            }
            case "categoryId" -> {
                if (newValue != null && !newValue.isBlank()) {
                    task.setCategoryId(UUID.fromString(newValue));
                } else {
                    task.setCategoryId(null);
                }
            }
            case "sortOrder" -> {
                if (newValue != null) {
                    task.setSortOrder(Integer.parseInt(newValue));
                }
            }
            case "recurrenceRule" -> task.setRecurrenceRule(newValue != null && !newValue.isBlank() ? newValue : null);
            default -> {
                return false;
            }
        }
        return true;
    }

    private SyncLogResponse toSyncLogResponse(SyncLog syncLog) {
        return SyncLogResponse.builder()
                .id(syncLog.getId())
                .entityType(syncLog.getEntityType())
                .entityId(syncLog.getEntityId())
                .fieldName(syncLog.getFieldName())
                .oldValue(syncLog.getOldValue())
                .newValue(syncLog.getNewValue())
                .deviceSource(syncLog.getDeviceSource())
                .conflictStatus(syncLog.getConflictStatus())
                .createdAt(syncLog.getCreatedAt())
                .build();
    }
}
