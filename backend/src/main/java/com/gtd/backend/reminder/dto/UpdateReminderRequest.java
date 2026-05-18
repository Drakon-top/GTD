package com.gtd.backend.reminder.dto;

import com.gtd.backend.reminder.model.ReminderOffsetType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to update an existing reminder")
public class UpdateReminderRequest {

    @Schema(description = "When to send the reminder (ISO-8601 timestamp)", example = "2026-06-01T10:00:00Z")
    private Instant remindAt;

    @Schema(description = "Offset type relative to task due date", example = "DAYS_BEFORE")
    private ReminderOffsetType offsetType;

    @Schema(description = "Offset value (used with offsetType)", example = "2")
    private Integer offsetValue;
}
