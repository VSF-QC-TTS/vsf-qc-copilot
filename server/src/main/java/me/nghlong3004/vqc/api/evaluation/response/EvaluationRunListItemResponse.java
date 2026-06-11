package me.nghlong3004.vqc.api.evaluation.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;
import me.nghlong3004.vqc.api.evaluation.enums.EvaluationRunStatus;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
@Schema(name = "EvaluationRunListItemResponse", description = "Evaluation run list item")
public record EvaluationRunListItemResponse(
    @Schema(description = "Run public identifier.") UUID publicId,
    @Schema(description = "Dataset public identifier.") UUID datasetPublicId,
    @Schema(description = "Rubric version public identifier.") UUID rubricVersionPublicId,
    @Schema(description = "Target connector public identifier.") UUID targetConnectorPublicId,
    @Schema(description = "Run status.", example = "PENDING") EvaluationRunStatus status,
    @Schema(description = "Total test cases.", example = "10") int totalCases,
    @Schema(description = "Run creation time.") OffsetDateTime createdAt) {}
