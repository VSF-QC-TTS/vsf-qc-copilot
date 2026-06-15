package me.nghlong3004.vqc.api.evaluation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/13/2026
 */
@Schema(name = "QuickEvaluateRequest", description = "Quick evaluate with auto-resolve")
public record QuickEvaluateRequest(
    @Schema(
            description = "Dataset to evaluate. Auto-resolved if omitted.",
            example = "0f6d90c2-7410-4db2-86be-8adfd3140f63",
            nullable = true)
        UUID datasetPublicId,
    @Schema(
            description = "Connector to call. Auto-resolved if omitted.",
            example = "b1c2d3e4-f5a6-7b8c-9d0e-1f2a3b4c5d6e",
            nullable = true)
        UUID connectorPublicId,
    @Schema(
            description = "Judge model for scoring. Auto-resolved if omitted.",
            example = "8d2f6a2a-4974-4e9c-83ad-f2e1e58d39f0",
            nullable = true)
        UUID judgeModelPublicId,
    @Schema(
            description = "Rubric version for scoring. Auto-resolved if omitted.",
            example = "5cfb4c51-3ac4-44bd-93b4-8eb4e3f46f3a",
            nullable = true)
        UUID rubricVersionPublicId,
    @Schema(
            description = "Max concurrent evaluations.",
            example = "3",
            nullable = true)
        @Min(value = 1, message = "Max concurrency must be at least 1.")
        @Max(value = 10, message = "Max concurrency must be at most 10.")
        Integer maxConcurrency) {}
