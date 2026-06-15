package me.nghlong3004.vqc.api.evaluation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
@Schema(name = "CreateEvaluationRunRequest", description = "Create evaluation run payload")
public record CreateEvaluationRunRequest(
    @Schema(description = "Dataset public identifier.", example = "ebd7f0f0-4924-4e81-9795-d1f060bec2f2")
        @NotNull(message = "Dataset public id is required.")
        UUID datasetPublicId,
    @Schema(description = "Rubric version public identifier.", example = "c3a1b2c3-d4e5-6f7a-8b9c-0d1e2f3a4b5c")
        @NotNull(message = "Rubric version public id is required.")
        UUID rubricVersionPublicId,
    @Schema(description = "Target connector public identifier.", example = "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d")
        @NotNull(message = "Target connector public id is required.")
        UUID targetConnectorPublicId,
    @Schema(description = "Judge model public identifier.", example = "8d2f6a2a-4974-4e9c-83ad-f2e1e58d39f0")
        @NotNull(message = "Judge model public id is required.")
        UUID judgeModelPublicId,
    @Schema(description = "Max concurrent evaluations.", example = "1", nullable = true)
        @Min(value = 1, message = "Max concurrency must be at least 1.")
        @Max(value = 10, message = "Max concurrency must be at most 10.")
        Integer maxConcurrency) {}
