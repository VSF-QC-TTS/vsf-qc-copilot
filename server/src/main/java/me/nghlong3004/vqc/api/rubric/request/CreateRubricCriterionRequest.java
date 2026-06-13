package me.nghlong3004.vqc.api.rubric.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
@Schema(name = "CreateRubricCriterionRequest", description = "Create rubric criterion payload")
public record CreateRubricCriterionRequest(
    @Schema(description = "Criterion name.", example = "Correctness")
        @NotBlank(message = "Criterion name is required.")
        @Size(max = 255, message = "Criterion name must be at most 255 characters.")
        String name,
    @Schema(description = "Optional criterion description.", nullable = true)
        @Size(max = 2000, message = "Criterion description must be at most 2000 characters.")
        String description,
    @Schema(description = "Relative importance weight. Higher value = more important. System normalizes automatically.", example = "3", nullable = true)
        @Min(value = 1, message = "Criterion weight must be at least 1.")
        @Max(value = 100, message = "Criterion weight must be at most 100.")
        Integer weight,
    @Schema(description = "Pass condition.", nullable = true)
        @Size(max = 4000, message = "Pass condition must be at most 4000 characters.")
        String passCondition,
    @Schema(description = "Fail condition.", nullable = true)
        @Size(max = 4000, message = "Fail condition must be at most 4000 characters.")
        String failCondition,
    @Schema(description = "Judge instruction.", example = "Compare actual answer with ground truth.")
        @NotBlank(message = "Judge instruction is required.")
        @Size(max = 8000, message = "Judge instruction must be at most 8000 characters.")
        String judgeInstruction,
    @Schema(description = "Stable metric key.", example = "correctness")
        @NotBlank(message = "Metric key is required.")
        @Size(max = 100, message = "Metric key must be at most 100 characters.")
        @Pattern(
            regexp = "[a-z][a-z0-9_]*",
            message = "Metric key must start with a lowercase letter and contain lowercase letters, numbers, or underscores.")
        String metricKey,
    @Schema(description = "Whether this criterion is critical.", example = "true", nullable = true)
        Boolean isCritical,
    @Schema(description = "Display/evaluation order.", example = "1", nullable = true)
        Integer sortOrder) {}
