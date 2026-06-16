package me.nghlong3004.vqc.api.redteam.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/15/2026
 */
@Schema(name = "RedTeamRunPageResponse", description = "Page of red-team runs")
public record RedTeamRunPageResponse(
    List<RedTeamRunResponse> items,
    int page,
    int size,
    long totalItems,
    int totalPages) {}
