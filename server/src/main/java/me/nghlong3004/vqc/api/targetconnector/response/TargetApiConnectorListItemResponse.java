package me.nghlong3004.vqc.api.targetconnector.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;
import me.nghlong3004.vqc.api.targetconnector.enums.HttpMethodType;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
@Schema(name = "TargetApiConnectorListItemResponse", description = "Target connector list item payload")
public record TargetApiConnectorListItemResponse(
    @Schema(description = "Public connector identifier.") UUID publicId,
    @Schema(description = "Public project identifier.") UUID projectPublicId,
    @Schema(description = "Connector name.") String name,
    @Schema(description = "HTTP method.", example = "POST") HttpMethodType method,
    @Schema(description = "Full target URL.") String url,
    @Schema(description = "Response selector.", example = "$.answer") String responseSelector,
    @Schema(description = "Whether the target streams responses.") Boolean isStreaming,
    @Schema(description = "Whether this connector is active.", example = "true") Boolean active,
    @Schema(description = "Connector creation time.") OffsetDateTime createdAt) {}
