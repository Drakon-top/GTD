package com.gtd.backend.task.dto;

import com.gtd.backend.task.model.GtdList;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
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
@Schema(description = "Request to create a new task")
public class CreateTaskRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 500, message = "Title must be at most 500 characters")
    @Schema(description = "Task title", example = "Buy groceries")
    private String title;

    @Schema(description = "Task notes", example = "Milk, eggs, bread")
    private String notes;

    @Schema(description = "GTD list (defaults to INBOX if not provided)", example = "INBOX")
    private GtdList gtdList;

    @Schema(description = "Due date (ISO-8601 timestamp)")
    private Instant dueDate;

    @Schema(description = "Category ID")
    private UUID categoryId;

    @Schema(description = "Recurrence rule as JSON string (e.g. {\"type\":\"daily\",\"time\":\"09:00\"})")
    private String recurrenceRule;
}
