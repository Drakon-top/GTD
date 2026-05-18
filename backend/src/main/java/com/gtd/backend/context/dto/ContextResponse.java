package com.gtd.backend.context.dto;

import com.gtd.backend.context.model.ContextTheme;
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
@Schema(description = "Context response")
public class ContextResponse {

    @Schema(description = "Context ID")
    private UUID id;

    @Schema(description = "Context name", example = "Work")
    private String name;

    @Schema(description = "Visual theme", example = "FORMAL")
    private ContextTheme theme;

    @Schema(description = "Icon identifier", example = "briefcase")
    private String icon;

    @Schema(description = "Sort order", example = "0")
    private int sortOrder;

    @Schema(description = "Creation timestamp")
    private Instant createdAt;

    @Schema(description = "Last update timestamp")
    private Instant updatedAt;
}
