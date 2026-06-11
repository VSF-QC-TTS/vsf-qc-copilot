package me.nghlong3004.vqc.api.evaluation.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
@Schema(name = "CreateEvaluationRunResponse", description = "Evaluation run creation result")
public record CreateEvaluationRunResponse(
    @Schema(description = "Run public identifier.") UUID runPublicId,
    @Schema(description = "Job public identifier.") UUID jobPublicId,
    @Schema(description = "Current run status.", example = "PENDING") String status,
    @Schema(description = "Human-readable message.", example = "Evaluation run queued successfully.")
        String message) {}
