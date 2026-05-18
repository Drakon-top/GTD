package com.gtd.backend.sync.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Response from sync push operation")
public class SyncPushResponse {

    @Schema(description = "Server timestamp of this sync operation")
    private Instant serverTimestamp;

    @Schema(description = "Results for each change in the push request")
    private List<SyncChangeResult> results;

    @Schema(description = "Number of changes applied successfully")
    private int appliedCount;

    @Schema(description = "Number of conflicts detected")
    private int conflictCount;
}
