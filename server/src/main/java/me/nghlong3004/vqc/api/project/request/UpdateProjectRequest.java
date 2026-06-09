package me.nghlong3004.vqc.api.project.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
@Schema(name = "UpdateProjectRequest", description = "Update project payload")
public record UpdateProjectRequest(
    @Schema(description = "Project name.", example = "AI Health Chatbot Demo v2", nullable = true)
        @Pattern(regexp = ".*\\S.*", message = "Project name must not be blank.")
        @Size(max = 255, message = "Project name must be at most 255 characters.")
        String name,
    @Schema(description = "Optional project description.", example = "Updated description", nullable = true)
        @Size(max = 2000, message = "Project description must be at most 2000 characters.")
        String description,
    @Schema(description = "Optional evaluation scope.", example = "Updated scope", nullable = true)
        @Size(max = 2000, message = "Evaluation scope must be at most 2000 characters.")
        String evaluationScope,
    @Schema(description = "How long evaluation artifacts should be retained.", example = "60", nullable = true)
        @Min(value = 1, message = "Retention days must be at least 1.")
        @Max(value = 3650, message = "Retention days must be at most 3650.")
        Integer retentionDays) {}
