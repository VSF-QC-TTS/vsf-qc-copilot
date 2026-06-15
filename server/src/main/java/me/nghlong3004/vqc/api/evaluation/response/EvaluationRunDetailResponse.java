package me.nghlong3004.vqc.api.evaluation.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
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
    @Schema(description = "Dataset name.", nullable = true) String datasetName,
    @Schema(description = "Rubric version public identifier.") UUID rubricVersionPublicId,
    @Schema(description = "Rubric name.", nullable = true) String rubricName,
    @Schema(description = "Rubric version number.", example = "1", nullable = true)
        Integer rubricVersionNumber,
    @Schema(description = "Target connector public identifier.") UUID targetConnectorPublicId,
    @Schema(description = "Target connector name.", nullable = true) String connectorName,
    @Schema(description = "Judge model public identifier.", nullable = true)
        @JsonInclude(JsonInclude.Include.NON_NULL) UUID judgeModelPublicId,
    @Schema(description = "Judge model display name.", nullable = true)
        @JsonInclude(JsonInclude.Include.NON_NULL) String judgeModelDisplayName,
    @Schema(description = "Job public identifier.", nullable = true)
        @JsonInclude(JsonInclude.Include.NON_NULL) UUID jobPublicId,
    @Schema(description = "Run status.", example = "PENDING") EvaluationRunStatus status,
    @Schema(description = "Run description.", nullable = true)
        @JsonInclude(JsonInclude.Include.NON_NULL) String description,
    @Schema(description = "Total test cases.", example = "10") int totalCases,
    @Schema(description = "Completed test cases.", example = "8") int completedCases,
    @Schema(description = "Passed test cases.", example = "6") int passedCases,
    @Schema(description = "Failed test cases.", example = "2") int failedCases,
    @Schema(description = "Warning test cases.", example = "0") int warningCases,
    @Schema(description = "Error test cases.", example = "0") int errorCases,
    @Schema(description = "Pass rate (0.0000 – 1.0000).", example = "0.7500", nullable = true)
        @JsonInclude(JsonInclude.Include.NON_NULL) BigDecimal passRate,
    @Schema(description = "Max concurrent evaluations.", example = "1") int maxConcurrency,
    @Schema(description = "Run start time.", nullable = true)
        @JsonInclude(JsonInclude.Include.NON_NULL) OffsetDateTime startedAt,
    @Schema(description = "Run completion time.", nullable = true)
        @JsonInclude(JsonInclude.Include.NON_NULL) OffsetDateTime completedAt,
    @Schema(description = "Run creation time.") OffsetDateTime createdAt,
    @Schema(description = "Last update time.") OffsetDateTime updatedAt) {}
