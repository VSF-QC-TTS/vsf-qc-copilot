package me.nghlong3004.vqc.api.requirement.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import me.nghlong3004.vqc.api.requirement.enums.RequirementStatus;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
@Schema(name = "UpdateRequirementRequest", description = "Update business requirement payload")
public record UpdateRequirementRequest(
    @Schema(
            description = "Free-text business requirement.",
            example = "Updated requirement content.",
            nullable = true)
        @Pattern(regexp = ".*\\S.*", message = "Requirement content must not be blank.")
        String content,
    @Schema(description = "Requirement lifecycle status.", example = "ACTIVE", nullable = true)
        RequirementStatus status) {}
