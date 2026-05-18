package com.gtd.backend.context.dto;

import com.gtd.backend.context.model.ContextTheme;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
@Schema(description = "Request to create a new context")
public class CreateContextRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must be at most 100 characters")
    @Schema(description = "Context name", example = "Work")
    private String name;

    @NotNull(message = "Theme is required")
    @Schema(description = "Visual theme for the context", example = "FORMAL")
    private ContextTheme theme;

    @NotBlank(message = "Icon is required")
    @Size(max = 50, message = "Icon must be at most 50 characters")
    @Schema(description = "Icon identifier", example = "briefcase")
    private String icon;
}
