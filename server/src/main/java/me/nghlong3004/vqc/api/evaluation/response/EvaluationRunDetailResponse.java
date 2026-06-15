package me.nghlong3004.vqc.api.evaluation.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;
import me.nghlong3004.vqc.api.evaluation.enums.EvaluationRunStatus;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
@Schema(name = "EvaluationRunDetailResponse", description = "Full evaluation run detail")
public record EvaluationRunDetailResponse(
    @Schema(description = "Run public identifier.") UUID publicId,
    @Schema(description = "Project public identifier.") UUID projectPublicId,
    @Schema(description = "Dataset public identifier.") UUID datasetPublicId,
    @Schema(description = "Rubric version public identifier.") UUID rubricVersionPublicId,
    @Schema(description = "Target connector public identifier.") UUID targetConnectorPublicId,
    @Schema(description = "Judge model public identifier.", nullable = true) UUID judgeModelPublicId,
    @Schema(description = "Job public identifier.", nullable = true) UUID jobPublicId,
    @Schema(description = "Run status.", example = "PENDING") EvaluationRunStatus status,
    @Schema(description = "Total test cases.", example = "10") int totalCases,
    @Schema(description = "Max concurrent evaluations.", example = "1") int maxConcurrency,
    @Schema(description = "Run creation time.") OffsetDateTime createdAt,
    @Schema(description = "Last update time.") OffsetDateTime updatedAt) {}
