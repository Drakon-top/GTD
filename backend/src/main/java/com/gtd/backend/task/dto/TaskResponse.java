package com.gtd.backend.task.dto;

import com.gtd.backend.task.model.GtdList;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Task response")
public class TaskResponse {

    @Schema(description = "Task ID")
    private UUID id;

    @Schema(description = "Context ID")
    private UUID contextId;

    @Schema(description = "Parent task ID (null for top-level tasks)")
    private UUID parentTaskId;

    @Schema(description = "GTD list", example = "INBOX")
    private GtdList gtdList;

    @Schema(description = "Category ID")
    private UUID categoryId;

    @Schema(description = "Task title", example = "Buy groceries")
    private String title;

    @Schema(description = "Task notes")
    private String notes;

    @Schema(description = "Due date")
    private Instant dueDate;

    @Schema(description = "Nesting level (1-4)", example = "1")
    private int nestingLevel;

    @Schema(description = "Sort order", example = "0")
    private int sortOrder;

    @Schema(description = "Whether task is completed")
    private boolean completed;

    @Schema(description = "Completion timestamp")
    private Instant completedAt;

    @Schema(description = "Optimistic locking version")
    private int version;

    @Schema(description = "Subtasks (only included in single-task GET)")
    private List<TaskResponse> subtasks;

    @Schema(description = "Whether the task has incomplete subtasks (only included on complete)")
    private Boolean hasIncompleteSubtasks;

    @Schema(description = "Project progress percentage (0-100). Only populated for tasks in PROJECTS list that have subtasks.")
    private Integer progress;

    @Schema(description = "Recurrence rule as JSON string (e.g. {\"type\":\"daily\",\"time\":\"09:00\"}). Null if not recurring.")
    private String recurrenceRule;

    @Schema(description = "ID of the newly created next instance (only present when completing a recurring task)")
    private UUID nextInstanceId;

    @Schema(description = "Whether the completed instance was part of a recurring task")
    private Boolean isRecurring;

    @Schema(description = "Total number of direct subtasks (for list views)")
    private Integer subtaskCount;

    @Schema(description = "Number of completed direct subtasks (for list views)")
    private Integer completedSubtaskCount;

    @Schema(description = "Creation timestamp")
    private Instant createdAt;

    @Schema(description = "Last update timestamp")
    private Instant updatedAt;
}
