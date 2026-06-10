package me.nghlong3004.vqc.api.requirement.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;
import me.nghlong3004.vqc.api.requirement.enums.RequirementStatus;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
@Schema(name = "RequirementListItemResponse", description = "Business requirement list item")
public record RequirementListItemResponse(
    @Schema(
            description = "Public requirement identifier.",
            example = "ebd7f0f0-4924-4e81-9795-d1f060bec2f2")
        UUID publicId,
    @Schema(
            description = "Public project identifier.",
            example = "5a4edcc1-cd1e-44ef-a144-31f5f3d2f653")
        UUID projectPublicId,
    @Schema(
            description = "Free-text business requirement.",
            example = "Evaluate whether the chatbot can answer Apple Health step-count questions correctly.")
        String content,
    @Schema(description = "Requirement version.", example = "1") Integer version,
    @Schema(description = "Requirement lifecycle status.", example = "ACTIVE")
        RequirementStatus status,
    @Schema(description = "Requirement creation time.", example = "2026-06-08T10:30:00+07:00")
        OffsetDateTime createdAt) {}
