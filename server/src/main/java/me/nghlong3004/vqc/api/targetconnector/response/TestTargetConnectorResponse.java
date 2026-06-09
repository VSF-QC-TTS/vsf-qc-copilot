package me.nghlong3004.vqc.api.targetconnector.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/10/2026
 */
@Schema(name = "TestTargetConnectorResponse", description = "Target connector test-run result")
public record TestTargetConnectorResponse(
    @Schema(description = "Whether the target call succeeded.", example = "true") boolean success,
    @Schema(description = "Masked rendered request preview.") TargetConnectorRequestPreview requestPreview,
    @Schema(description = "Raw target response safe for display.") Map<String, Object> rawResponse,
    @Schema(description = "Answer extracted from the configured response selector.", nullable = true)
        String extractedAnswer,
    @Schema(description = "Target call latency in milliseconds.", example = "120") long latencyMs) {}
