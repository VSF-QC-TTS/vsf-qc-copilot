package me.nghlong3004.vqc.api.rubric.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
@Schema(name = "UpdateRubricRequest", description = "Update rubric payload")
public record UpdateRubricRequest(
    @Schema(description = "Rubric name.", example = "Health Answer Quality Rubric v2", nullable = true)
        @Pattern(regexp = ".*\\S.*", message = "Rubric name must not be blank.")
        @Size(max = 255, message = "Rubric name must be at most 255 characters.")
        String name,
    @Schema(description = "Optional rubric description.", example = "Updated rubric description.", nullable = true)
        @Size(max = 2000, message = "Rubric description must be at most 2000 characters.")
        String description) {}
