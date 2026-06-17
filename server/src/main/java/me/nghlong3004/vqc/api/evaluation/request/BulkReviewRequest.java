package me.nghlong3004.vqc.api.evaluation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import me.nghlong3004.vqc.api.review.enums.QcStatus;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/17/2026
 */
@Schema(name = "BulkReviewRequest", description = "Bulk update review decisions payload")
public record BulkReviewRequest(
    @Schema(description = "List of evaluation result public identifiers to update.")
        @NotEmpty(message = "Result IDs list cannot be empty.")
        List<UUID> resultIds,
    @Schema(description = "New QC status to apply to all selected results.", example = "PASS")
        @NotNull(message = "QC status is required.")
        QcStatus status,
    @Schema(description = "Optional note to apply to all selected results.", nullable = true)
        String note) {}
