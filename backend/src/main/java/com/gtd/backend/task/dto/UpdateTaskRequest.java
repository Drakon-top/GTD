package com.gtd.backend.task.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gtd.backend.task.model.GtdList;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
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
@Schema(description = "Request to update an existing task. Only provided (non-null) fields are updated.")
public class UpdateTaskRequest {

    @Size(max = 500, message = "Title must be at most 500 characters")
    @Schema(description = "Task title", example = "Buy groceries")
    private String title;

    @Schema(description = "Task notes", example = "Updated notes")
    private String notes;

    @Schema(description = "GTD list", example = "NEXT_ACTIONS")
    private GtdList gtdList;

    @Schema(description = "Due date (ISO-8601 timestamp)")
    private Instant dueDate;

    @Schema(description = "Category ID")
    private UUID categoryId;

    @Schema(description = "Sort order", example = "1")
    private Integer sortOrder;

    @Schema(description = "Recurrence rule as JSON string. Send empty string \"\" to stop recurrence, null/omit to leave unchanged.")
    private String recurrenceRule;

    @JsonIgnore
    @Schema(hidden = true)
    private boolean recurrenceRuleProvided = false;

    public void setRecurrenceRule(String recurrenceRule) {
        this.recurrenceRule = recurrenceRule;
        this.recurrenceRuleProvided = true;
    }
}
