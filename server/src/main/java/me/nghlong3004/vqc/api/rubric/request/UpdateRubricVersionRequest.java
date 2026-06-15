package me.nghlong3004.vqc.api.rubric.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import me.nghlong3004.vqc.api.rubric.enums.RubricVersionStatus;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
@Schema(name = "UpdateRubricVersionRequest", description = "Update rubric version lifecycle payload")
public record UpdateRubricVersionRequest(
    @Schema(description = "Rubric version lifecycle status.", example = "PUBLISHED")
        RubricVersionStatus status,
    @Schema(description = "Rubric judge prompt content.", nullable = true)
        @Size(max = 10000, message = "Rubric content must be at most 10000 characters.")
        String content,
    @Schema(description = "Output schema JSON.", nullable = true)
        @Size(max = 10000, message = "Output schema JSON must be at most 10000 characters.")
        String outputSchemaJson) {}
