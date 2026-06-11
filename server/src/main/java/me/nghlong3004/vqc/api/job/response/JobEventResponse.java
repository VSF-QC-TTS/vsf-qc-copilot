package me.nghlong3004.vqc.api.job.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/11/2026
 */
@Schema(name = "JobEventResponse", description = "A single job event")
public record JobEventResponse(
    @Schema(description = "Event public identifier.") UUID publicId,
    @Schema(description = "Event type.", example = "STARTED") String eventType,
    @Schema(description = "Event payload JSON.", nullable = true) String payloadJson,
    @Schema(description = "Event creation time.") OffsetDateTime createdAt) {}
