package me.nghlong3004.vqc.api.rubric.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;
import me.nghlong3004.vqc.api.rubric.enums.RubricVersionStatus;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
@Schema(name = "RubricVersionListItemResponse", description = "Rubric version list item response")
public record RubricVersionListItemResponse(
    @Schema(description = "Public rubric version identifier.") UUID publicId,
    @Schema(description = "Public rubric identifier.") UUID rubricPublicId,
    @Schema(description = "Rubric name.") String rubricName,
    @Schema(description = "Version number.") int versionNumber,
    @Schema(description = "Rubric version lifecycle status.") RubricVersionStatus status,
    @Schema(description = "Criterion count.") long criteriaCount,
    @Schema(description = "Version creation timestamp.") OffsetDateTime createdAt,
    @Schema(description = "Version publish timestamp.", nullable = true) OffsetDateTime publishedAt) {}
