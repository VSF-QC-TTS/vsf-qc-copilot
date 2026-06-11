package me.nghlong3004.vqc.api.evaluation.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import me.nghlong3004.vqc.api.evaluation.enums.JudgeStatus;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
@Schema(name = "EvaluationResultListItemResponse", description = "Evaluation result list item")
public record EvaluationResultListItemResponse(
    @Schema(description = "Result public identifier.") UUID publicId,
    @Schema(description = "Test case public identifier.") UUID testCasePublicId,
    @Schema(description = "Actual answer from the target.", nullable = true) String actualAnswer,
    @Schema(description = "Judge score.", example = "0.85", nullable = true) BigDecimal judgeScore,
    @Schema(description = "Judge status.", example = "PASS") JudgeStatus judgeStatus,
    @Schema(description = "Judge reason.", nullable = true) String judgeReason,
    @Schema(description = "Latency in milliseconds.", example = "120", nullable = true)
        Integer latencyMs,
    @Schema(description = "Error message if failed.", nullable = true) String errorMessage,
    @Schema(description = "Result creation time.") OffsetDateTime createdAt) {}
