package me.nghlong3004.vqc.api.rubric.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import me.nghlong3004.vqc.api.rubric.enums.RubricVersionStatus;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
@Schema(name = "RubricVersionResponse", description = "Rubric version detail response")
public record RubricVersionResponse(
    @Schema(description = "Public rubric version identifier.") UUID publicId,
    @Schema(description = "Public rubric identifier.") UUID rubricPublicId,
    @Schema(description = "Version number.") int version,
    @Schema(description = "Rubric version lifecycle status.") RubricVersionStatus status,
    @Schema(description = "Version creation timestamp.") OffsetDateTime createdAt,
    @Schema(description = "Version publish timestamp.", nullable = true) OffsetDateTime publishedAt,
    @Schema(description = "Sorted criteria in this version.") List<RubricCriterionResponse> criteria) {}
