package com.gtd.backend.export.controller;

import com.gtd.backend.auth.dto.ErrorResponse;
import com.gtd.backend.export.dto.ExportResponse;
import com.gtd.backend.export.service.ExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/export")
@RequiredArgsConstructor
@Tag(name = "Export", description = "Export user data as JSON")
public class ExportController {

    private final ExportService exportService;

    @Operation(summary = "Export data as JSON",
            description = "Exports all contexts or a specific context with tasks, subtasks, categories, and reminders. "
                    + "Soft-deleted data is excluded.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Export generated successfully",
                    content = @Content(schema = @Schema(implementation = ExportResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied to the specified context",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Context not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<ExportResponse> exportData(
            @Parameter(description = "Optional context ID to export a single context")
            @RequestParam(name = "context_id", required = false) UUID contextId,
            Authentication authentication) {

        UUID userId = (UUID) authentication.getPrincipal();

        ExportResponse response;
        if (contextId != null) {
            response = exportService.exportContext(contextId, userId);
        } else {
            response = exportService.exportAll(userId);
        }

        return ResponseEntity.ok(response);
    }
}
