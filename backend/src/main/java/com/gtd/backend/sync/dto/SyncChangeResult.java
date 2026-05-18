package com.gtd.backend.sync.dto;

import com.gtd.backend.sync.model.ConflictStatus;
import com.gtd.backend.sync.model.SyncEntityType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Result of a single sync change operation")
public class SyncChangeResult {

    @Schema(description = "Entity type")
    private SyncEntityType entityType;

    @Schema(description = "Entity ID")
    private UUID entityId;

    @Schema(description = "Field name")
    private String fieldName;

    @Schema(description = "Whether the change was applied successfully")
    private boolean applied;

    @Schema(description = "Conflict status")
    private ConflictStatus conflictStatus;

    @Schema(description = "Current server value (provided when conflict detected)")
    private String serverValue;

    @Schema(description = "New entity version after change (if applied)")
    private Integer newVersion;

    @Schema(description = "Error message (if change failed)")
    private String error;
}
