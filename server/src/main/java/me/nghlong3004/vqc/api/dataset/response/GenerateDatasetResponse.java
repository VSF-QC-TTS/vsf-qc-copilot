package me.nghlong3004.vqc.api.dataset.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/13/2026
 */
@Schema(name = "GenerateDatasetResponse", description = "AI generation async result")
public record GenerateDatasetResponse(
    @Schema(description = "Dataset public identifier.", example = "0f6d90c2-7410-4db2-86be-8adfd3140f63")
        UUID datasetPublicId,
    @Schema(description = "Job public identifier for polling progress.", example = "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d")
        UUID jobPublicId,
    @Schema(description = "Job status.", example = "PENDING")
        String status,
    @Schema(description = "Human-readable message.", example = "Dataset generation queued successfully.")
        String message) {}
