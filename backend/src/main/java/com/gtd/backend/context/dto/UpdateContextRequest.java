package com.gtd.backend.context.dto;

import com.gtd.backend.context.model.ContextTheme;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to update an existing context")
public class UpdateContextRequest {

    @Size(max = 100, message = "Name must be at most 100 characters")
    @Schema(description = "Context name", example = "Office")
    private String name;

    @Schema(description = "Visual theme for the context", example = "DARK")
    private ContextTheme theme;

    @Size(max = 50, message = "Icon must be at most 50 characters")
    @Schema(description = "Icon identifier", example = "laptop")
    private String icon;

    @Schema(description = "Sort order for display", example = "2")
    private Integer sortOrder;
}
