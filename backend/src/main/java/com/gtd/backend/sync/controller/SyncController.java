package com.gtd.backend.sync.controller;

import com.gtd.backend.auth.dto.ErrorResponse;
import com.gtd.backend.sync.dto.SyncPullResponse;
import com.gtd.backend.sync.dto.SyncPushRequest;
import com.gtd.backend.sync.dto.SyncPushResponse;
import com.gtd.backend.sync.service.SyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sync")
@RequiredArgsConstructor
@Tag(name = "Sync", description = "Synchronization API for multi-device support")
public class SyncController {

    private final SyncService syncService;

    @Operation(summary = "Push local changes to server",
            description = "Accepts an array of local field-level changes and applies them to the server. "
                    + "Uses optimistic locking via version field to detect conflicts.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Push processed (check individual results for conflicts)",
                    content = @Content(schema = @Schema(implementation = SyncPushResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/push")
    public ResponseEntity<SyncPushResponse> pushChanges(
            @Valid @RequestBody SyncPushRequest request,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        SyncPushResponse response = syncService.pushChanges(request, userId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Pull server changes since timestamp",
            description = "Returns all server-side changes that occurred after the given timestamp. "
                    + "Use the returned serverTimestamp as 'since' for the next pull.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Changes retrieved successfully",
                    content = @Content(schema = @Schema(implementation = SyncPullResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/pull")
    public ResponseEntity<SyncPullResponse> pullChanges(
            @Parameter(description = "ISO-8601 timestamp to pull changes since", required = true)
            @RequestParam("since") Instant since,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        SyncPullResponse response = syncService.pullChanges(since, userId);
        return ResponseEntity.ok(response);
    }
}
