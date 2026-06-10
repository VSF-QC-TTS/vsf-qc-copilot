package me.nghlong3004.vqc.api.rubric.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
@Schema(name = "CreateRubricRequest", description = "Create rubric payload")
public record CreateRubricRequest(
    @Schema(description = "Rubric name.", example = "Health Answer Quality Rubric")
        @NotBlank(message = "Rubric name is required.")
        @Size(max = 255, message = "Rubric name must be at most 255 characters.")
        String name,
    @Schema(
            description = "Optional rubric description.",
            example = "Checks correctness, completeness, clarity, and safety.",
            nullable = true)
        @Size(max = 2000, message = "Rubric description must be at most 2000 characters.")
        String description) {}
