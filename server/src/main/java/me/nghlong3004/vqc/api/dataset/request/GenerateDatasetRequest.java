package me.nghlong3004.vqc.api.dataset.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/13/2026
 */
@Schema(name = "GenerateDatasetRequest", description = "AI test case generation payload")
public record GenerateDatasetRequest(
    @Schema(
            description = "Context or prompt for AI generation (e.g. business rules, DB info, etc).",
            example = "Generate test cases for a health chatbot that answers questions about heart rate and blood pressure.")
        @NotBlank(message = "Generation prompt is required.")
        @Size(max = 8000, message = "Generation prompt must be at most 8000 characters.")
        String prompt,
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
