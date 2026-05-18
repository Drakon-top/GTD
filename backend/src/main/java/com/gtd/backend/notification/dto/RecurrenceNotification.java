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
public class RecurrenceNotification {

    private UUID originalTaskId;
    private UUID newTaskId;
    private UUID userId;
    private String taskTitle;
    private String recurrenceRule;
    private NotificationType type;

    @Builder.Default
    private Instant createdAt = Instant.now();
}
