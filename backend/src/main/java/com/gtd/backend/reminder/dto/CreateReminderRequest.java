package com.gtd.backend.reminder.dto;

import com.gtd.backend.reminder.model.ReminderOffsetType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
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
@Schema(description = "Request to create a new reminder")
public class CreateReminderRequest {

    @NotNull(message = "remind_at is required")
    @Schema(description = "When to send the reminder (ISO-8601 timestamp)", example = "2026-06-01T09:00:00Z")
    private Instant remindAt;

    @Schema(description = "Offset type relative to task due date", example = "HOURS_BEFORE")
    private ReminderOffsetType offsetType;

    @Schema(description = "Offset value (used with offsetType)", example = "1")
    private Integer offsetValue;
}
