package me.nghlong3004.vqc.api.rubric.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import me.nghlong3004.vqc.api.rubric.request.CreateRubricCriterionRequest;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/15/2026
 */
@Schema(name = "GenerateRubricPreviewResponse", description = "AI generated rubric preview")
public record GenerateRubricPreviewResponse(
    @Schema(description = "Suggested rubric name.") String name,
    @Schema(description = "Suggested rubric description.", nullable = true) String description,
    @Schema(description = "Suggested rubric judge prompt.") String content,
    @Schema(description = "Suggested output schema JSON.", nullable = true) String outputSchemaJson,
    @Schema(description = "Suggested criteria.") List<CreateRubricCriterionRequest> criteria) {}
