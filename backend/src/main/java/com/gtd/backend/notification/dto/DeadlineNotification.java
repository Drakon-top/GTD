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
public class DeadlineNotification {

    private UUID taskId;
    private UUID userId;
    private String taskTitle;
    private Instant dueDate;
    private long hoursUntilDeadline;
    private NotificationType type;

    @Builder.Default
    private Instant createdAt = Instant.now();
}
