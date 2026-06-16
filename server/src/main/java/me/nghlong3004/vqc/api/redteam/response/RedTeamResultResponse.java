package me.nghlong3004.vqc.api.redteam.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/15/2026
 */
@Schema(name = "RedTeamResultResponse", description = "Raw Promptfoo red-team result artifact")
public record RedTeamResultResponse(
    String runPublicId,
    String status,
    Map<String, Object> summary,
    Object results) {}
