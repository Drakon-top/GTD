package com.gtd.backend.sync.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to push local changes to the server")
public class SyncPushRequest {

    @NotEmpty(message = "Changes list must not be empty")
    @Valid
    @Schema(description = "List of local changes to push")
    private List<SyncChangeRequest> changes;
}
