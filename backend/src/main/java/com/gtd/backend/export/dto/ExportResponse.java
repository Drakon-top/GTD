package com.gtd.backend.export.dto;

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
@Schema(description = "Full export response containing contexts with their tasks, categories, and reminders")
public class ExportResponse {

    @Schema(description = "Export generation timestamp")
    private Instant exportDate;

    @Schema(description = "Export format version", example = "1.0")
    private String version;

    @Schema(description = "Exported contexts with nested data")
    private List<ExportContextData> contexts;
}
