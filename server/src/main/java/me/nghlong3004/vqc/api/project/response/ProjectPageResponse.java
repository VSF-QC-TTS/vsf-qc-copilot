package me.nghlong3004.vqc.api.project.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
@Schema(name = "ProjectPageResponse", description = "Paginated project list payload")
public record ProjectPageResponse(
    @Schema(description = "Current page items.") List<ProjectListItemResponse> items,
    @Schema(description = "Zero-based page index.", example = "0") int page,
    @Schema(description = "Requested page size.", example = "20") int size,
    @Schema(description = "Total item count.", example = "1") long totalItems,
    @Schema(description = "Total page count.", example = "1") int totalPages) {}
