package com.gtd.backend.reminder.controller;

import com.gtd.backend.auth.dto.ErrorResponse;
import com.gtd.backend.reminder.dto.CreateReminderRequest;
import com.gtd.backend.reminder.dto.ReminderResponse;
import com.gtd.backend.reminder.dto.UpdateReminderRequest;
import com.gtd.backend.reminder.service.ReminderService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Reminders", description = "CRUD operations for task reminders")
public class ReminderController {

    private final ReminderService reminderService;

    @Operation(summary = "List reminders for a task",
            description = "Returns all reminders for the specified task, ordered by remind_at.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reminders retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ReminderResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied to task",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Task not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/api/v1/tasks/{taskId}/reminders")
    public ResponseEntity<List<ReminderResponse>> getReminders(@PathVariable UUID taskId,
                                                                Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(reminderService.getReminders(taskId, userId));
    }

    @Operation(summary = "Create a reminder for a task",
            description = "Creates a new reminder for the specified task. Multiple reminders can be attached to a single task.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Reminder created successfully",
                    content = @Content(schema = @Schema(implementation = ReminderResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied to task",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Task not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/api/v1/tasks/{taskId}/reminders")
    public ResponseEntity<ReminderResponse> createReminder(@PathVariable UUID taskId,
                                                            @Valid @RequestBody CreateReminderRequest request,
                                                            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        ReminderResponse response = reminderService.createReminder(taskId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Update a reminder",
            description = "Updates reminder fields (remindAt, offsetType, offsetValue). Only provided fields are updated. Updating remindAt resets is_sent to false.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reminder updated successfully",
                    content = @Content(schema = @Schema(implementation = ReminderResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Reminder not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/api/v1/reminders/{id}")
    public ResponseEntity<ReminderResponse> updateReminder(@PathVariable UUID id,
                                                            @Valid @RequestBody UpdateReminderRequest request,
                                                            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(reminderService.updateReminder(id, request, userId));
    }

    @Operation(summary = "Delete a reminder",
            description = "Permanently deletes the reminder.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Reminder deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Reminder not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/api/v1/reminders/{id}")
    public ResponseEntity<Void> deleteReminder(@PathVariable UUID id, Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        reminderService.deleteReminder(id, userId);
        return ResponseEntity.noContent().build();
    }
}
