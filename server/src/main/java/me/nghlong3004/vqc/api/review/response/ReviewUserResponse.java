package me.nghlong3004.vqc.api.review.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
@Schema(name = "ReviewUserResponse", description = "Public user summary for review decisions")
public record ReviewUserResponse(
    @Schema(description = "Public user identifier.") UUID publicId,
    @Schema(description = "Display name.") String displayName) {}
