package me.nghlong3004.vqc.api.redteam.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import me.nghlong3004.vqc.api.redteam.enums.RedTeamRunStatus;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/15/2026
 */
@Schema(name = "RedTeamRunResponse", description = "Red-team run detail")
public record RedTeamRunResponse(
    UUID publicId,
    UUID projectPublicId,
    UUID targetConnectorPublicId,
    String connectorName,
    UUID judgeModelPublicId,
    String judgeModelDisplayName,
    UUID jobPublicId,
    String name,
    String purpose,
    List<String> plugins,
    List<String> strategies,
    Integer numTests,
    RedTeamRunStatus status,
    Integer totalCases,
    Integer passedCases,
    Integer failedCases,
    Integer errorCases,
    String errorMessage,
    OffsetDateTime startedAt,
    OffsetDateTime completedAt,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {}
