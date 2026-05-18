package com.gtd.backend.export.dto;

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
@Schema(description = "Category data in export")
public class ExportCategoryData {

    @Schema(description = "Category ID")
    private UUID id;

    @Schema(description = "Category name", example = "Books")
    private String name;

    @Schema(description = "Icon identifier", example = "book")
    private String icon;

    @Schema(description = "HEX color code", example = "#FF5733")
    private String color;

    @Schema(description = "Sort order", example = "0")
    private int sortOrder;

    @Schema(description = "Creation timestamp")
    private Instant createdAt;

    @Schema(description = "Last update timestamp")
    private Instant updatedAt;
}
