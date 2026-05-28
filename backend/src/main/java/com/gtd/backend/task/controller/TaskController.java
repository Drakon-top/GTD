package com.gtd.backend.task.controller;

import com.gtd.backend.auth.dto.ErrorResponse;
import com.gtd.backend.task.dto.CreateTaskRequest;
import com.gtd.backend.task.dto.MoveTaskRequest;
import com.gtd.backend.task.dto.TaskCountsResponse;
import com.gtd.backend.task.dto.TaskResponse;
import com.gtd.backend.task.dto.UpdateTaskRequest;
import com.gtd.backend.task.model.GtdList;
import com.gtd.backend.task.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "CRUD operations for tasks within a context")
public class TaskController {

    private final TaskService taskService;

    @Operation(summary = "List tasks in a context",
            description = "Returns all non-deleted top-level tasks for the given context. Optionally filter by GTD list.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tasks retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = TaskResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied to context",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Context not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/contexts/{contextId}/tasks")
    public ResponseEntity<List<TaskResponse>> getTasks(
            @PathVariable UUID contextId,
            @Parameter(description = "Filter by GTD list") @RequestParam(name = "gtd_list", required = false) GtdList gtdList,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(taskService.getTasks(contextId, gtdList, userId));
    }

    @Operation(summary = "Get task counts for a context",
            description = "Returns task counts grouped by GTD list and by category. Excludes soft-deleted tasks.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Counts retrieved successfully",
                    content = @Content(schema = @Schema(implementation = TaskCountsResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied to context",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Context not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/contexts/{contextId}/tasks/counts")
    public ResponseEntity<TaskCountsResponse> getTaskCounts(
            @PathVariable UUID contextId,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(taskService.getTaskCounts(contextId, userId));
    }

    @Operation(summary = "Create a task in a context",
            description = "Creates a new task. Only title is required; defaults to INBOX with nesting_level=1.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Task created successfully",
                    content = @Content(schema = @Schema(implementation = TaskResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied to context",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Context not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/contexts/{contextId}/tasks")
    public ResponseEntity<TaskResponse> createTask(
            @PathVariable UUID contextId,
            @Valid @RequestBody CreateTaskRequest request,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        TaskResponse response = taskService.createTask(contextId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get a task by ID",
            description = "Returns a single task with its direct subtasks.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task retrieved successfully",
                    content = @Content(schema = @Schema(implementation = TaskResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied to task",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Task not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/tasks/{id}")
    public ResponseEntity<TaskResponse> getTask(@PathVariable UUID id, Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(taskService.getTask(id, userId));
    }

    @Operation(summary = "Update a task",
            description = "Updates task fields (title, notes, gtd_list, due_date, category_id, sort_order). Only provided fields are updated.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task updated successfully",
                    content = @Content(schema = @Schema(implementation = TaskResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied to task",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Task not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/tasks/{id}")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTaskRequest request,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(taskService.updateTask(id, request, userId));
    }

    @Operation(summary = "Move a task to a different GTD list",
            description = "Changes the GTD list of the task. Cannot move a deleted task.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task moved successfully",
                    content = @Content(schema = @Schema(implementation = TaskResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied to task",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Task not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/tasks/{id}/move")
    public ResponseEntity<TaskResponse> moveTask(
            @PathVariable UUID id,
            @Valid @RequestBody MoveTaskRequest request,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(taskService.moveTask(id, request.getGtdList(), userId));
    }

    @Operation(summary = "Complete a task",
            description = "Marks the task as completed. Sets is_completed=true, completed_at=now(), gtd_list=DONE.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task completed successfully",
                    content = @Content(schema = @Schema(implementation = TaskResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied to task",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Task not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/tasks/{id}/complete")
    public ResponseEntity<TaskResponse> completeTask(
            @PathVariable UUID id,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(taskService.completeTask(id, userId));
    }

    @Operation(summary = "Reopen a completed task",
            description = "Marks the task as not completed. Sets is_completed=false, completed_at=null, gtd_list=INBOX.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task reopened successfully",
                    content = @Content(schema = @Schema(implementation = TaskResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied to task",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Task not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/tasks/{id}/reopen")
    public ResponseEntity<TaskResponse> reopenTask(
            @PathVariable UUID id,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(taskService.reopenTask(id, userId));
    }

    @Operation(summary = "Delete a task (soft delete)",
            description = "Marks the task as deleted. Cascades soft delete to all subtasks.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Task deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied to task",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Task not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/tasks/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable UUID id, Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        taskService.deleteTask(id, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "List subtasks of a task",
            description = "Returns all non-deleted direct subtasks of the given task.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Subtasks retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = TaskResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied to task",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Task not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/tasks/{taskId}/subtasks")
    public ResponseEntity<List<TaskResponse>> getSubtasks(
            @PathVariable UUID taskId,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(taskService.getSubtasks(taskId, userId));
    }

    @Operation(summary = "Create a subtask",
            description = "Creates a subtask under the given parent task. Inherits context from parent. Maximum nesting level is 4.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Subtask created successfully",
                    content = @Content(schema = @Schema(implementation = TaskResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error or max nesting level exceeded",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied to task",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Parent task not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/tasks/{taskId}/subtasks")
    public ResponseEntity<TaskResponse> createSubtask(
            @PathVariable UUID taskId,
            @Valid @RequestBody CreateTaskRequest request,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        TaskResponse response = taskService.createSubtask(taskId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
