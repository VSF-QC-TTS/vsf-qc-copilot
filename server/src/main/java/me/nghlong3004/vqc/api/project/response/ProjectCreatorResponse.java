package me.nghlong3004.vqc.api.project.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
@Schema(name = "ProjectCreatorResponse", description = "Public project creator payload")
public record ProjectCreatorResponse(
    @Schema(
            description = "Public user identifier. Internal numeric ids are never exposed.",
            example = "7b7b7d42-5f42-4c5a-9281-8d1d36f6f59d")
        UUID publicId,
    @Schema(description = "Creator display name.", example = "QC Demo") String displayName) {}
