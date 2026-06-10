package me.nghlong3004.vqc.api.requirement.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
@Schema(name = "RequirementPageResponse", description = "Paginated requirement list payload")
public record RequirementPageResponse(
    @Schema(description = "Current page items.") List<RequirementListItemResponse> items,
    @Schema(description = "Zero-based page index.", example = "0") int page,
    @Schema(description = "Requested page size.", example = "20") int size,
    @Schema(description = "Total item count.", example = "1") long totalItems,
    @Schema(description = "Total page count.", example = "1") int totalPages) {}
