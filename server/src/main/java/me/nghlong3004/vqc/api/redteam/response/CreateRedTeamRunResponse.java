package me.nghlong3004.vqc.api.redteam.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/15/2026
 */
@Schema(name = "CreateRedTeamRunResponse", description = "Queued red-team run")
public record CreateRedTeamRunResponse(
    UUID runPublicId, UUID jobPublicId, String status, String message) {}
