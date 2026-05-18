package com.gtd.backend.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncConflictNotification {

    private UUID syncLogId;
    private UUID entityId;
    private UUID userId;
    private String entityType;
    private String fieldName;
    private String resolvedValue;
    private String conflictStatus;
    private NotificationType type;

    @Builder.Default
    private Instant createdAt = Instant.now();
}
