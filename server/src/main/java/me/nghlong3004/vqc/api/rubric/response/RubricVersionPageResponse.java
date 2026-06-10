package me.nghlong3004.vqc.api.rubric.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
@Schema(name = "RubricVersionPageResponse", description = "Paginated rubric version response")
public record RubricVersionPageResponse(
    @Schema(description = "Current page items.") List<RubricVersionListItemResponse> items,
    @Schema(description = "Current zero-based page number.", example = "0") int page,
    @Schema(description = "Requested page size.", example = "20") int size,
    @Schema(description = "Total matching items.", example = "1") long totalItems,
    @Schema(description = "Total matching pages.", example = "1") int totalPages) {}
