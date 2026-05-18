package com.gtd.backend.export.dto;

import com.gtd.backend.context.model.ContextTheme;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Context data in export (with nested categories and tasks)")
public class ExportContextData {

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

    @Schema(description = "Categories in this context")
    private List<ExportCategoryData> categories;

    @Schema(description = "Top-level tasks in this context (with nested subtasks)")
    private List<ExportTaskData> tasks;
}
