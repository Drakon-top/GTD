package com.gtd.backend.reminder.dto;

import com.gtd.backend.reminder.model.ReminderOffsetType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Reminder response")
public class ReminderResponse {

    @Schema(description = "Reminder ID")
    private UUID id;

    @Schema(description = "Task ID this reminder belongs to")
    private UUID taskId;

    @Schema(description = "When the reminder will fire")
    private Instant remindAt;

    @Schema(description = "Offset type relative to task due date")
    private ReminderOffsetType offsetType;

    @Schema(description = "Offset value")
    private Integer offsetValue;

    @Schema(description = "Whether the reminder has been sent")
    private boolean isSent;

    @Schema(description = "Creation timestamp")
    private Instant createdAt;
}
