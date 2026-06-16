package me.nghlong3004.vqc.api.evaluation.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import me.nghlong3004.vqc.api.evaluation.enums.JudgeStatus;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/15/2026
 */
@Schema(name = "EvaluationCriteriaResultResponse", description = "Per-criterion judge result")
public record EvaluationCriteriaResultResponse(
    @Schema(description = "Criterion metric key.") String metricKey,
    @Schema(description = "Human criterion name.") String name,
    @Schema(description = "Criterion judge status.") JudgeStatus status,
    @Schema(description = "Criterion score.", nullable = true) BigDecimal score,
    @Schema(description = "Criterion judge reason.", nullable = true) String reason,
    @Schema(description = "Whether the criterion failed because the judge provider errored.")
        boolean graderError) {}
