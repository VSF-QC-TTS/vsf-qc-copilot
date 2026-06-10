package me.nghlong3004.vqc.api.rubric.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import me.nghlong3004.vqc.api.rubric.enums.RubricVersionStatus;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
@Schema(name = "UpdateRubricVersionRequest", description = "Update rubric version lifecycle payload")
public record UpdateRubricVersionRequest(
    @Schema(description = "Rubric version lifecycle status.", example = "PUBLISHED")
        @NotNull(message = "Rubric version status is required.")
        RubricVersionStatus status) {}
