package me.nghlong3004.vqc.api.rubric.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Creates a user-scoped rubric together with its first draft version.
 *
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/15/2026
 */
@Schema(
    name = "CreateRubricWithVersionRequest",
    description = "Create user-scoped rubric and first draft version payload")
public record CreateRubricWithVersionRequest(
    @Schema(description = "Rubric name.", example = "Health Answer Quality Rubric")
        @NotBlank(message = "Rubric name is required.")
        @Size(max = 255, message = "Rubric name must be at most 255 characters.")
        String name,
    @Schema(description = "Optional rubric description.", nullable = true)
        @Size(max = 2000, message = "Rubric description must be at most 2000 characters.")
        String description,
    @Schema(description = "Rubric prompt content.")
        @NotBlank(message = "Rubric content is required.")
        @Size(max = 20000, message = "Rubric content must be at most 20000 characters.")
        String content,
    @Schema(description = "Expected judge output schema JSON.", nullable = true)
        @Size(max = 20000, message = "Output schema must be at most 20000 characters.")
        String outputSchemaJson,
    @Schema(description = "Optional criteria. Default QC criteria are created when omitted.")
        @Valid
        List<CreateRubricCriterionRequest> criteria) {}
