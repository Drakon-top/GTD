package com.gtd.backend.category.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
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
@Schema(description = "Request to update a category (partial update — only provided fields are changed)")
public class UpdateCategoryRequest {

    @Size(max = 100, message = "Name must be at most 100 characters")
    @Schema(description = "Category name", example = "Movies")
    private String name;

    @Size(max = 50, message = "Icon must be at most 50 characters")
    @Schema(description = "Icon identifier", example = "film")
    private String icon;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color must be a valid HEX code (e.g. #FF5733)")
    @Schema(description = "HEX color code", example = "#3366FF")
    private String color;

    @Schema(description = "Sort order", example = "2")
    private Integer sortOrder;
}
