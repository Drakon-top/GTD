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
@Schema(description = "Response from sync pull — returns server changes since a given timestamp")
public class SyncPullResponse {

    @Schema(description = "Server timestamp to use as 'since' in the next pull")
    private Instant serverTimestamp;

    @Schema(description = "List of changes since the requested timestamp")
    private List<SyncLogResponse> changes;

    @Schema(description = "Number of changes returned")
    private int changeCount;
}
