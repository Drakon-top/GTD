package com.gtd.backend.sync.dto;

import com.gtd.backend.sync.model.ConflictStatus;
import com.gtd.backend.sync.model.DeviceSource;
import com.gtd.backend.sync.model.SyncEntityType;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "A single sync log entry representing a field-level change")
public class SyncLogResponse {

    @Schema(description = "Sync log entry ID")
    private UUID id;

    @Schema(description = "Entity type")
    private SyncEntityType entityType;

    @Schema(description = "Entity ID")
    private UUID entityId;

    @Schema(description = "Field that was changed")
    private String fieldName;

    @Schema(description = "Previous value")
    private String oldValue;

    @Schema(description = "New value")
    private String newValue;

    @Schema(description = "Source device")
    private DeviceSource deviceSource;

    @Schema(description = "Conflict status")
    private ConflictStatus conflictStatus;

    @Schema(description = "Timestamp of the change")
    private Instant createdAt;
}
