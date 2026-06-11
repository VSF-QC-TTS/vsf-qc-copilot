package me.nghlong3004.vqc.api.review.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import me.nghlong3004.vqc.api.review.enums.QcStatus;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
@Schema(name = "UpdateReviewDecisionRequest", description = "Update QC review decision payload")
public record UpdateReviewDecisionRequest(
    @NotNull(message = "QC status is required.")
        @Schema(description = "Human final QC status.", example = "PASS")
        QcStatus qcStatus,
    @Schema(description = "QC note.", example = "Confirmed by QC.", nullable = true)
        String qcNote,
    @Schema(description = "PIC bug user public identifier.", nullable = true)
        UUID picBugUserPublicId) {}
