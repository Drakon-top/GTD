package com.gtd.backend.context.controller;

import com.gtd.backend.auth.dto.ErrorResponse;
import com.gtd.backend.context.dto.ContextResponse;
import com.gtd.backend.context.dto.CreateContextRequest;
import com.gtd.backend.context.dto.UpdateContextRequest;
import com.gtd.backend.context.service.ContextService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/contexts")
@RequiredArgsConstructor
@Tag(name = "Contexts", description = "CRUD operations for user contexts (max 5 per user)")
public class ContextController {

    private final ContextService contextService;

    @Operation(summary = "List user contexts",
            description = "Returns all non-deleted contexts for the authenticated user, ordered by sort_order")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contexts retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ContextResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<List<ContextResponse>> getContexts(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(contextService.getContexts(userId));
    }

    @Operation(summary = "Get context by ID",
            description = "Returns a single context. Returns 403 if the context belongs to another user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Context retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ContextResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Context not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ContextResponse> getContext(@PathVariable UUID id, Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(contextService.getContext(id, userId));
    }

    @Operation(summary = "Create a new context",
            description = "Creates a context for the authenticated user. Maximum 5 contexts allowed.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Context created successfully",
                    content = @Content(schema = @Schema(implementation = ContextResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error or context limit exceeded",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<ContextResponse> createContext(@Valid @RequestBody CreateContextRequest request,
                                                         Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        ContextResponse response = contextService.createContext(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Update a context",
            description = "Updates context fields (name, theme, icon, sort_order). Only provided fields are updated.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Context updated successfully",
                    content = @Content(schema = @Schema(implementation = ContextResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Context not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<ContextResponse> updateContext(@PathVariable UUID id,
                                                         @Valid @RequestBody UpdateContextRequest request,
                                                         Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(contextService.updateContext(id, request, userId));
    }

    @Operation(summary = "Delete a context (soft delete)",
            description = "Marks the context as deleted. It will no longer appear in listings.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Context deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Context not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteContext(@PathVariable UUID id, Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        contextService.deleteContext(id, userId);
        return ResponseEntity.noContent().build();
    }
}
