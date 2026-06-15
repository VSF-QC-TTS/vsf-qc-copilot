package me.nghlong3004.vqc.api.rubric.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;
import me.nghlong3004.vqc.api.rubric.enums.RubricStatus;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
@Schema(name = "RubricListItemResponse", description = "Rubric list item response")
public record RubricListItemResponse(
    @Schema(description = "Public rubric identifier.") UUID publicId,
    @Schema(description = "Public project identifier.", nullable = true) UUID projectPublicId,
    @Schema(description = "Project name.", nullable = true) String projectName,
    @Schema(description = "Whether this rubric is a system template.") Boolean isTemplate,
    @Schema(description = "Rubric name.") String name,
    @Schema(description = "Current published version.", nullable = true) Integer currentVersion,
    @Schema(description = "Rubric lifecycle status.") RubricStatus status,
    @Schema(description = "Rubric creation timestamp.") OffsetDateTime createdAt) {}
