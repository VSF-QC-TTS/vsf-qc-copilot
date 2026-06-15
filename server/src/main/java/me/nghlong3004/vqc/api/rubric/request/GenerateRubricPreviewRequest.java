package me.nghlong3004.vqc.api.rubric.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/15/2026
 */
@Schema(name = "GenerateRubricPreviewRequest", description = "AI rubric preview payload")
public record GenerateRubricPreviewRequest(
    @Schema(description = "Rubric display name.", example = "Healthcare Chatbot Rubric")
        @NotBlank(message = "Rubric name is required.")
        @Size(max = 200, message = "Rubric name must be at most 200 characters.")
        String name,
    @Schema(description = "What the rubric should evaluate.")
        @NotBlank(message = "Evaluation goal is required.")
        @Size(max = 2000, message = "Evaluation goal must be at most 2000 characters.")
        String evaluationGoal,
    @Schema(description = "Domain or product context.", nullable = true)
        @Size(max = 5000, message = "Domain context must be at most 5000 characters.")
        String domainContext,
    @Schema(description = "Preferred rubric language.", nullable = true, example = "vi")
        @Size(max = 20, message = "Language must be at most 20 characters.")
        String language,
    @Schema(description = "Sample user question.", nullable = true)
        @Size(max = 2000, message = "Sample question must be at most 2000 characters.")
        String sampleQuestion,
    @Schema(description = "Sample expected answer.", nullable = true)
        @Size(max = 5000, message = "Sample expected answer must be at most 5000 characters.")
        String sampleExpectedAnswer,
    @Schema(description = "Additional generation instructions.", nullable = true)
        @Size(max = 5000, message = "Extra instructions must be at most 5000 characters.")
        String extraInstructions) {}
