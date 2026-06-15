package me.nghlong3004.vqc.api.evaluation.response;

import com.fasterxml.jackson.annotation.JsonInclude;
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
    @Schema(description = "Dataset name.", nullable = true) String datasetName,
    @Schema(description = "Rubric version public identifier.") UUID rubricVersionPublicId,
    @Schema(description = "Rubric name.", nullable = true) String rubricName,
    @Schema(description = "Target connector public identifier.") UUID targetConnectorPublicId,
    @Schema(description = "Judge model public identifier.", nullable = true)
        @JsonInclude(JsonInclude.Include.NON_NULL) UUID judgeModelPublicId,
    @Schema(description = "Run status.", example = "PENDING") EvaluationRunStatus status,
    @Schema(description = "Total test cases.", example = "10") int totalCases,
    @Schema(description = "Completed test cases.", example = "8") int completedCases,
    @Schema(description = "Passed test cases.", example = "6") int passedCases,
    @Schema(description = "Failed test cases.", example = "2") int failedCases,
    @Schema(description = "Run creation time.") OffsetDateTime createdAt) {}
