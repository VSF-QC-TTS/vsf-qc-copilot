package me.nghlong3004.vqc.api.dataset.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/13/2026
 */
@Schema(name = "GenerateDatasetRequest", description = "AI test case generation payload")
public record GenerateDatasetRequest(
    @Schema(
            description = "Requirement to base generation on.",
            example = "ebd7f0f0-4924-4e81-9795-d1f060bec2f2")
        @NotNull(message = "Requirement public id is required.")
        UUID requirementPublicId,
    @Schema(description = "Number of test cases to generate.", example = "30")
        @NotNull(message = "Count is required.")
        @Min(value = 5, message = "Count must be at least 5.")
        @Max(value = 100, message = "Count must be at most 100.")
        Integer count,
    @Schema(
            description = "Additional instructions for AI generation.",
            example = "Focus on edge cases and error scenarios.",
            nullable = true)
        @Size(max = 4000, message = "Additional prompt must be at most 4000 characters.")
        String additionalPrompt) {}
