package com.gtd.backend.sync.dto;

import com.gtd.backend.sync.model.DeviceSource;
import com.gtd.backend.sync.model.SyncEntityType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
@Schema(description = "A single field change to be synced to the server")
public class SyncChangeRequest {

    @NotNull(message = "Entity type is required")
    @Schema(description = "Type of entity being changed", example = "TASK")
    private SyncEntityType entityType;

    @NotNull(message = "Entity ID is required")
    @Schema(description = "ID of the entity being changed")
    private UUID entityId;

    @NotBlank(message = "Field name is required")
    @Size(max = 100, message = "Field name must be at most 100 characters")
    @Schema(description = "Name of the field being changed", example = "title")
    private String fieldName;

    @Schema(description = "Old field value (for conflict detection)")
    private String oldValue;

    @Schema(description = "New field value")
    private String newValue;

    @NotNull(message = "Device source is required")
    @Schema(description = "Source device of the change", example = "ANDROID")
    private DeviceSource deviceSource;

    @NotNull(message = "Client timestamp is required")
    @Schema(description = "Timestamp when the change was made on the client")
    private Instant clientTimestamp;

    @Schema(description = "Expected entity version for optimistic locking")
    private Integer expectedVersion;
}
