package me.nghlong3004.vqc.api.requirement.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
@Schema(name = "CreateRequirementRequest", description = "Create business requirement payload")
public record CreateRequirementRequest(
    @Schema(
            description = "Free-text business requirement.",
            example = "Evaluate whether the chatbot can answer Apple Health step-count questions correctly.")
        @NotBlank(message = "Requirement content is required.")
        String content) {}
