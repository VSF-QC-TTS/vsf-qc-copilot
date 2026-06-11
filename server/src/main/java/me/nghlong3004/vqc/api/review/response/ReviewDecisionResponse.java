package me.nghlong3004.vqc.api.review.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;
import me.nghlong3004.vqc.api.review.enums.QcStatus;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
@Schema(name = "ReviewDecisionResponse", description = "QC review decision detail")
public record ReviewDecisionResponse(
    @Schema(description = "Review decision public identifier.") UUID publicId,
    @Schema(description = "Evaluation result public identifier.") UUID evaluationResultPublicId,
    @Schema(description = "Human final QC status.") QcStatus qcStatus,
    @Schema(description = "QC note.", nullable = true) String qcNote,
    @Schema(description = "PIC bug user.", nullable = true) ReviewUserResponse picBug,
    @Schema(description = "Reviewer.") ReviewUserResponse reviewedBy,
    @Schema(description = "Review time.") OffsetDateTime reviewedAt,
    @Schema(description = "Last update time.") OffsetDateTime updatedAt) {}
