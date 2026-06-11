package me.nghlong3004.vqc.api.evaluation.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
@Schema(name = "EvaluationResultPageResponse", description = "Paginated evaluation result list")
public record EvaluationResultPageResponse(
    @Schema(description = "Page items.") List<EvaluationResultListItemResponse> items,
    @Schema(description = "Current page number (0-indexed).", example = "0") int page,
    @Schema(description = "Page size.", example = "20") int size,
    @Schema(description = "Total items.", example = "10") long totalItems,
    @Schema(description = "Total pages.", example = "1") int totalPages) {}
