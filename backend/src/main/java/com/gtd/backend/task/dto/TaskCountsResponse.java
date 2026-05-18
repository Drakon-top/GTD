package com.gtd.backend.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Task counts per GTD list and category for a context")
public class TaskCountsResponse {

    @Schema(description = "Context ID")
    private UUID contextId;

    @Schema(description = "Task counts per GTD list (e.g. {\"INBOX\": 5, \"NEXT_ACTIONS\": 3})")
    private Map<String, Integer> byGtdList;

    @Schema(description = "Task counts per category ID (e.g. {\"uuid\": 2})")
    private Map<String, Integer> byCategory;

    @Schema(description = "Total active (non-deleted) tasks in context")
    private int total;
}
