package com.gtd.backend.task.dto;

import com.gtd.backend.task.model.GtdList;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
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
@Schema(description = "Request to move a task to a different GTD list")
public class MoveTaskRequest {

    @NotNull(message = "gtdList is required")
    @Schema(description = "Target GTD list", example = "NEXT_ACTIONS")
    private GtdList gtdList;
}
